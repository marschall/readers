package com.github.marschall.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Objects;

/**
 * A {@link Reader} that decodes UTF-8 from an {@link InputStream} without buffering.
 *
 * <p>Avoids any intermediate allocation.
 *
 * <p>Not thread-safe.
 *
 * @see InputStreamReader
 */
public final class Utf8InputStreamReader extends Reader {

  private boolean closed;

  private final InputStream in;

  private boolean hasPendingLowSurrogate;

  private char lowSurrogate;

  /**
   * Constructs a new {@link Utf8InputStreamReader}
   *
   * @param in the input stream from which to read the bytes, not {@code null}
   * @throws NullPointerException if in is {@code null}
   */
  public Utf8InputStreamReader(InputStream in) {
    Objects.requireNonNull(in);
    this.in = in;
    this.closed = false;
    this.hasPendingLowSurrogate = false;
  }

  @Override
  public int read() throws IOException {
    this.closedCheck();
    return this.readIml();
  }

  private int readIml() throws IOException {
    if (this.hasPendingLowSurrogate) {
      this.hasPendingLowSurrogate = false;
      return this.lowSurrogate;
    } else {
      int c1 = this.in.read();
      if (c1 == -1) {
        return -1;
      }
      int byteLength = Utf8Utils.getByteLength(c1);
      if (byteLength == 1) {
        return c1;
      } else {
        int codePoint = this.readMultiByteCharacter(c1, byteLength);
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
  }

  private int readMultiByteCharacter(int c1, int byteLength) throws IOException {
    int codePoint = c1 & ((1 << (7 - byteLength)) - 1);
    for (int i = 0; i < (byteLength - 1); i++) {
      int next = this.in.read();
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

  @Override
  public long skip(long n) throws IOException {
    this.closedCheck();
    if (n < 0L) {
      throw new IllegalArgumentException("skip value is negative");
    }
    if (n == 0L) {
      return 0L;
    }
    long skipped = 0L;
    if (this.hasPendingLowSurrogate) {
      this.hasPendingLowSurrogate = false;
      skipped += 1L;
    }
    while (skipped < n) {
      int c1 = this.in.read();
      if (c1 == -1) {
        return skipped;
      }
      int byteLength = Utf8Utils.getByteLength(c1);
      if (byteLength == 1) {
        // ASCII character, single byte
        skipped += 1L;
      } else {
        int codePoint = this.readMultiByteCharacter(c1, byteLength);
        if (codePoint == -1) {
          return skipped;
        } else if (Character.isBmpCodePoint(codePoint)) {
          // BMP character, single Java char
          skipped += 1L;
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
      }
    }
    return skipped;
  }

  @Override
  public long transferTo(Writer out) throws IOException {
    this.closedCheck();
    long transferred = 0L;
    while (true) {
      int c = this.readIml();
      if (c == -1) {
        return transferred;
      } else {
        out.write(c);
      }
      transferred += 1L;
    }
  }

  @Override
  public int read(CharBuffer target) throws IOException {
    this.closedCheck();
    // TODO should probably go into JDK
    if (target.hasArray()) {
      return this.readIntoHeapBuffer(target);
    } else {
      return this.readIntoDirectBuffer(target);
    }
  }

  private int readIntoHeapBuffer(CharBuffer target) throws IOException {
    char[] cbuf = target.array();
    int off = target.arrayOffset();
    int len = target.remaining();
    return this.read(cbuf, off, len);
  }

  private int readIntoDirectBuffer(CharBuffer target) throws IOException {
    int len = target.remaining();
    int read = 0;
    while (read < len) {
      int c = this.readIml();
      if (c == -1) {
        return read;
      } else {
        target.put((char) c);
      }
    }
    return read;
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    this.closedCheck();
    Objects.checkFromIndexSize(off, len, cbuf.length);
    int read = 0;
    while (read < len) {
      int c = this.readIml();
      if (c == -1) {
        if (read == 0) {
          return -1;
        } else {
          return read;
        }
      }
      cbuf[off + read] = (char) c;
      read += 1;
    }
    return read;
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
