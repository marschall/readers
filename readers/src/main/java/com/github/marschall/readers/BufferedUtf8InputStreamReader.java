package com.github.marschall.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;

public final class BufferedUtf8InputStreamReader extends Reader {

  private boolean closed;

  private final InputStream in;

  private final byte[] buffer;

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
  }
  
  static int getByteLength(byte b) throws IOException {
    int value = Byte.toUnsignedInt(b);
    if (value < 0b1000_0000) {
      return 1;
    } else if (value >= 0b110_00000) {
      if (value < 0b1110_0000) {
        return 2;
      } else if (value < 0b11110_000) {
        return 3;
      } else if (value < 0b111110_00) {
        return 4;
      }
    }
    throw new IOException("invalid utf-8 first byte");
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    this.closedCheck();
    Objects.checkFromIndexSize(off, len, cbuf.length);
    // TODO Auto-generated method stub
    return 0;
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
