package com.github.marschall.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;

public final class BufferedUtf8InputStreamReader extends Reader {

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

  private int ensureNotEmpty() throws IOException {
    if (this.hasPendingLowSurrogate) {
      return 1;
    }
    // at least one character is available
    if (this.capacity >= 4) {
      return 1;
    }

    // buffer is empty
    if (this.capacity == 0) {
      int read = this.in.read(this.buffer, 0, this.buffer.length);
      if (read == -1) {
        return -1;
      }
      this.capacity = read;
      this.position = 0;
      return 1;
    }

    int byteLength = Utf8Utils.getByteLength(this.buffer[this.position]);
    if (byteLength > this.capacity) {
      // not a full character is available
      if (this.position > 0) {
        System.arraycopy(this.buffer, this.position, this.buffer, 0, this.capacity);
        this.position = 0;
        int read = this.in.read(this.buffer, this.capacity, this.buffer.length - this.capacity);
        if (read == -1) {
          return -1;
        }
        this.capacity += read;
      }
      return 1;
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
      return byteLength >= this.capacity;
    }
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    this.closedCheck();
    if (this.ensureNotEmpty() == -1) {
      return -1;
    }
    Objects.checkFromIndexSize(off, len, cbuf.length);
    int read = 0;
    if (this.hasPendingLowSurrogate) {
      cbuf[off] = this.lowSurrogate;
      this.hasPendingLowSurrogate = false;
      read += 1;
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
        byte b = this.buffer[this.position];
        int byteLength = Utf8Utils.getByteLength(b);
        if (byteLength == 1) {
          this.position += 1;
          this.capacity -= 1;
          // ASCII character, single type
          cbuf[off + read] = (char) Byte.toUnsignedInt(b);
          read += 1;
        } else if (byteLength <= this.capacity) {
          // non-ASCII multi-byte character
          int codePoint = this.readMultiByteCharacter(byteLength);
          this.position += byteLength;
          this.capacity -= byteLength;
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
        int byteLength = Utf8Utils.getByteLength(this.buffer[this.position]);
        if (byteLength == 1) {
          this.position += 1;
          this.capacity -= 1;
          // ASCII character, single type
          skipped += 1;
        } else if (byteLength <= this.capacity) {
          // non-ASCII multi-byte character
          int codePoint = this.readMultiByteCharacter(byteLength);
          this.position += byteLength;
          this.capacity -= byteLength;
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

  private int readMultiByteCharacter(int byteLength) throws IOException {
    int codePoint = Byte.toUnsignedInt(this.buffer[this.position]) & ((1 << (7 - byteLength)) - 1);
    for (int i = 0; i < (byteLength - 1); i++) {
      int next = Byte.toUnsignedInt(this.buffer[this.position + i + 1]);
      if (next == -1) {
        return -1;
      }
      if ((next & 0b11000000) != 0b10000000) {
        throw new IOException("malformed input");
      }
      int value = next & 0b111111;
      codePoint = (codePoint << 6) | value;
    }
    return codePoint;
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
