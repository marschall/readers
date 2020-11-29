package com.github.marschall.readers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;


/**
 * A {@link Reader} that decodes UTF-8 from an {@link InputStream} with buffering.
 *
 * <p>Only {@link #transferTo(java.io.Writer)} currently performs intermediate allocation.
 *
 * <p>The implementation is optimized for bulk copying ASCII characters.
 *
 * <p>Not thread-safe.
 *
 * @see InputStreamReader
 * @see BufferedInputStream
 */
public final class BufferedUtf8InputStreamReader extends Reader {

  private static final int MAX_BYTE_LENGTH = 4;

  /**
   * Unicode replacement character.
   */
  private static final int REPLACEMENT = 0xFFFD;

  static final VarHandle LONG_ACCESS = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());

  private boolean closed;

  private final InputStream in;

  private final byte[] buffer;

  // position in #buffer where the next read can occur
  private int position;

  // number of bytes in #buffer
  private int capacity;

  private boolean hasPendingLowSurrogate;

  private char lowSurrogate;

  /**
   * Constructs a new {@link BufferedUtf8InputStreamReader} with a default buffer size of 8192.
   *
   * @param in the input stream from which to read the bytes, not {@code null}
   * @throws NullPointerException if in is {@code null}
   */
  public BufferedUtf8InputStreamReader(InputStream in) {
    this(in, 8192);
  }

  public BufferedUtf8InputStreamReader(InputStream in, int bufferSize) {
    Objects.requireNonNull(in);
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("buffer size must be positive");
    }
    if (bufferSize < 4) {
      throw new IllegalArgumentException("buffer size too small");
    }
    this.in = in;
    this.buffer = new byte[bufferSize];
    this.closed = false;
    this.position = 0;
    this.capacity = 0;
    this.closed = false;
    this.hasPendingLowSurrogate = false;
  }

  /**
   * 
   * <p>The caller is responsible for checking {@link #hasPendingLowSurrogate}.
   * 
   * @return 1 if at least one char is in the buffer or not a full byte is left in the input,
   *         -1 if no longer a full char is available from the buffer
   * @throws IOException if reading fails
   */
  private int ensureNotEmpty() throws IOException {
    // at least one character is available
    if (this.capacity >= 4) {
      return 1;
    }

    // buffer is empty
    if (this.capacity == 0) {
      // contract guarantees at least one byte is read
      int read = this.in.read(this.buffer, 0, this.buffer.length);
      if (read == -1) {
        return -1;
      }
      this.capacity = read;
      this.position = 0;
      // we can't abort because on theory not enough bytes are read
    }

    int byteLength = Utf8Utils.getByteLength(this.buffer[this.position]);
    while (byteLength > this.capacity && byteLength <= MAX_BYTE_LENGTH) {
      // input is valid
      // not a full character is available
      
      // move the buffer to the start if not already done so
      if (this.position > 0) {
        System.arraycopy(this.buffer, this.position, this.buffer, 0, this.capacity);
        this.position = 0;
      }
      int read = this.in.read(this.buffer, this.capacity, this.buffer.length - this.capacity);
      if (read == -1) {
        // if there isn't a buffer char in the buffer
        // but the stream is at the end return 1 anyways
        return this.capacity == 0 ? -1 : 1;
      }
      this.capacity += read;
    }
    return 1;
  }

  @Override
  public boolean ready() throws IOException {
    this.closedCheck();
    if (this.hasPendingLowSurrogate) {
      return true;
    }
    if (this.capacity == 0) {
      return false;
    } else {
      int byteLength = Utf8Utils.getByteLength(this.buffer[this.position]);
      return byteLength <= MAX_BYTE_LENGTH && byteLength >= this.capacity;
    }
  }

  @Override
  public int read() throws IOException {
    this.closedCheck();
    if (this.hasPendingLowSurrogate) {
      this.hasPendingLowSurrogate = false;
      return this.lowSurrogate;
    }
    if (this.ensureNotEmpty() == -1) {
      return -1;
    }
    byte b = this.buffer[this.position++];
    this.capacity -= 1;
    int byteLength = Utf8Utils.getByteLength(b);
    if (byteLength == 1) {
      return (char) Byte.toUnsignedInt(b);
    } else if (byteLength > MAX_BYTE_LENGTH) {
      // invalid input
      return REPLACEMENT;
    } else {
      // non-ASCII multi-byte character
      // ensureNotEmpty did the buffer size checks
      int codePoint = this.readMultiByteCharacter(b, byteLength);
      if (Character.isBmpCodePoint(codePoint)) {
        // BMP character, single Java char
        return (char) codePoint;
      } else {
        this.hasPendingLowSurrogate = true;
        this.lowSurrogate = Character.lowSurrogate(codePoint);
        // non-BMP character, two Java char
        return Character.highSurrogate(codePoint);
      }
    }
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    this.closedCheck();
    Objects.checkFromIndexSize(off, len, cbuf.length);
    int read = 0;
    if (this.hasPendingLowSurrogate) {
      cbuf[off] = this.lowSurrogate;
      this.hasPendingLowSurrogate = false;
      read += 1;
    }
    if (this.ensureNotEmpty() == -1) {
      return -1;
    }
    while ((read < len) && (this.capacity > 0)) {
      if (isPowerOf8(this.position) && isPowerOf8(off + read) && ((len - read) >= 8) && (this.capacity >= 8) && isAsciiRange(this.buffer, this.position)) {
        // bulk copy 8 ASCII characters
        copy8(this.buffer, this.position, cbuf, off + read);
        this.position += 8;
        this.capacity -= 8;
        read += 8;
      } else {
        // slow path
        // go byte by byte, either because
        // - #position is not aligned
        // - #off + read is not aligned
        // - less than 8 character left to read
        // - buffer contains less than 8 bytes
        // - one of the next 8 bytes is not ASCII
        byte b = this.buffer[this.position++];
        this.capacity -= 1;
        int byteLength = Utf8Utils.getByteLength(b);
        if (byteLength == 1 || byteLength > MAX_BYTE_LENGTH) {
          // ASCII character, single type
          // or invalid input
          cbuf[off + read] = (char) Byte.toUnsignedInt(b);
          read += 1;
        } else if (byteLength <= this.capacity + 1) {
          // non-ASCII multi-byte character
          // enough bytes in the buffer left to decode the whole character
          int codePoint = this.readMultiByteCharacter(b, byteLength);
          if (Character.isBmpCodePoint(codePoint)) {
            // BMP character, single Java char
            cbuf[off + read] = (char) codePoint;
            read += 1;
          } else {
            // non-BMP character, two Java char
            cbuf[off + read] = Character.highSurrogate(codePoint);
            read += 1;
            if ((len - read) >= 1) {
              // we can read both characters
              cbuf[off + read] = Character.lowSurrogate(codePoint);
              read += 1;
            } else {
              // we can skip only the high surrogate pair
              this.hasPendingLowSurrogate = true;
              this.lowSurrogate = Character.lowSurrogate(codePoint);
              // we can abort
              return read;
            }
          }
        } else {
          // not enough bytes in the buffer left to decode the next character
          // we decoded at least 1 character, abort, let the caller deal with it
          return read;
        }
      }
    }
    return read;
  }

  private static boolean isAsciiRange(byte[] src, int srcPos) {
    long l = (long) LONG_ACCESS.get(src, srcPos);
    return (l & 0b10000000_10000000_10000000_10000000_10000000_10000000_10000000_10000000L) == 0L;
  }

  private static boolean isPowerOf8(int i) {
    return (i & 0b111) == 0;
  }

  private static void copy8(byte[] src, int srcPos, char[] dst, int destPos) {
    dst[destPos] = (char) src[srcPos];
    dst[destPos + 1] = (char) src[srcPos + 1];
    dst[destPos + 2] = (char) src[srcPos + 2];
    dst[destPos + 3] = (char) src[srcPos + 3];
    dst[destPos + 4] = (char) src[srcPos + 4];
    dst[destPos + 5] = (char) src[srcPos + 5];
    dst[destPos + 6] = (char) src[srcPos + 6];
    dst[destPos + 7] = (char) src[srcPos + 7];
  }

//  @Override
//  public long transferTo(Writer out) throws IOException {
//    // TODO implement
//    throw new IOException();
//  }

  @Override
  public long skip(long n) throws IOException {
    this.closedCheck();
    if (n < 0L) {
      throw new IllegalArgumentException("skip value is negative");
    }
    if (n == 0L) {
      return 0L;
    }
    if (this.ensureNotEmpty() == -1) {
      return 0L;
    }
    long skipped = 0L;
    if (this.hasPendingLowSurrogate) {
      this.hasPendingLowSurrogate = false;
      skipped += 1L;
    }

    while ((skipped < n) && (this.capacity > 0)) {
      if (isPowerOf8(this.position) && (this.capacity >= 8) && ((n - skipped) >= 8L) && isAsciiRange(this.buffer, this.position)) {
        // bulk skip 8 ASCII characters
        this.position += 8;
        this.capacity -= 8;
        skipped += 8;
      } else {
        // slow path
        // go byte by byte, either because
        // - #position is not aligned
        // - less than 8 character left to skip
        // - buffer contains less than 8 bytes
        // - one of the next 8 bytes is not ASCII
        byte b = this.buffer[this.position++];
        this.capacity -= 1;
        int byteLength = Utf8Utils.getByteLength(b);
        if (byteLength == 1 || byteLength > MAX_BYTE_LENGTH) {
          // ASCII character, single byte
          // or invalid input
          skipped += 1;
        } else if (byteLength <= this.capacity + 1) {
          // non-ASCII multi-byte character
          // enough bytes in the buffer left to decode the whole character
          int codePoint = this.readMultiByteCharacter(b, byteLength);
          if (Character.isBmpCodePoint(codePoint)) {
            // BMP character, single Java char
            skipped += 1;
          } else {
            // non-BMP character, two Java char
            if ((n - skipped) >= 2) {
              // we can skip both characters
              skipped += 2;
            } else {
              skipped += 1;
              // we can skip only the high surrogate pair
              this.hasPendingLowSurrogate = true;
              this.lowSurrogate = Character.lowSurrogate(codePoint);
              // we can abort
              return skipped;
            }
          }
        } else {
          // not enough bytes in the buffer left to decode the next character
          // we decoded at least 1 character, abort, let the caller deal with it
          return skipped;
        }
      }
    }
    return skipped;
  }

  private int readMultiByteCharacter(byte b1, int byteLength) throws IOException {
    // https://unicode.org/versions/corrigendum1.html
    if (byteLength - 1 > this.capacity) {
      this.position += this.capacity;
      this.capacity = 0;
      return REPLACEMENT;
    }
    int c1 = Byte.toUnsignedInt(b1);
    switch (byteLength) {
    case 2: {
      int c2 = Byte.toUnsignedInt(this.buffer[this.position]);
      this.position += 1;
      this.capacity -= 1;
      
      if (Utf8Utils.isValidTwoByteSequence(c1, c2)) {
        return ((c1 & 0b00011111) << 6) | (c2 & 0b00111111);
      } else {
        return REPLACEMENT;
      }
    }

    case 3: {
      int c2 = Byte.toUnsignedInt(this.buffer[this.position]);
      int c3 = Byte.toUnsignedInt(this.buffer[this.position + 1]);
      this.position += 2;
      this.capacity -= 2;
      
      if (Utf8Utils.isValidThreeByteSequence(c1, c2, 3)) {
        return ((c1 & 0b00001111) << 12) | ((c2 & 0b00111111) << 6) | (c3 & 0b00111111);
      } else {
        return REPLACEMENT;
      }

    }

    case 4: {
      int c2 = Byte.toUnsignedInt(this.buffer[this.position]);
      int c3 = Byte.toUnsignedInt(this.buffer[this.position + 1]);
      int c4 = Byte.toUnsignedInt(this.buffer[this.position + 2]);
      this.position += 3;
      this.capacity -= 3;
      
      if (Utf8Utils.isValidFourByteSequence(c1, c2, 3, 4)) {
        return ((c1 & 0b00000111) << 18) | ((c2 & 0b00111111) << 12) | ((c3 & 0b00111111) << 6) | (c4 & 0b00111111);
      } else {
        return REPLACEMENT;
      }

    }

    }
    
    
    int codePoint = b1 & ((1 << (7 - byteLength)) - 1);
    boolean valid = true;
    for (int i = 0; i < (byteLength - 1); i++) {
      int next = Byte.toUnsignedInt(this.buffer[this.position + i]);
      if (next == -1) {
        this.position += i + 1;
        this.capacity -= i + 1;
        return REPLACEMENT;
      }
      // all bytes except the first must start with 10xxxxxx
      valid &= (next & 0b11000000) == 0b10000000;
      int value = next & 0b111111;
      codePoint = (codePoint << 6) | value;
    }
    if (valid) {
      this.position += byteLength - 1;
      this.capacity -= byteLength - 1;
      return codePoint;
    } else {
      return REPLACEMENT;
    }
  }

  private void closedCheck() throws IOException {
    if (this.closed) {
      throw new IOException("closed writer");
    }
  }

  @Override
  public void close() throws IOException {
    this.in.close();
    this.closed = true;
  }

}
