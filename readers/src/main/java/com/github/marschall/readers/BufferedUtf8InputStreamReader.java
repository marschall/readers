package com.github.marschall.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

public final class BufferedUtf8InputStreamReader extends Reader {

  private boolean closed;

  private final InputStream in;

  private final byte[] buffer;

  private final int position;

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
  }

  @Override
  public boolean ready() throws IOException {
    int byteLength = Utf8Utils.getByteLength(this.buffer[this.position]);
    return (this.position + byteLength) <= this.buffer.length;
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    this.closedCheck();
    Objects.checkFromIndexSize(off, len, cbuf.length);
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long transferTo(Writer out) throws IOException {
    throw new IOException();
  }

  @Override
  public long skip(long n) throws IOException {
    throw new IOException();
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
