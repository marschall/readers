package com.github.marschall.readers;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Objects;

public final class Utf8InputStreamReader extends Reader {

  private boolean closed;

  private final InputStream in;

  private boolean pendingLowSurrogate;

  private char lowSurrogate;

  public Utf8InputStreamReader(InputStream in) {
    Objects.requireNonNull(in);
    this.in = in;
    this.closed = false;
    this.pendingLowSurrogate = false;
  }

  @Override
  public int read() throws IOException {
    this.closedCheck();
    if (this.pendingLowSurrogate) {
      this.pendingLowSurrogate = false;
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
        return this.readMultiByteCharacter(c1, byteLength);
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
      int value = next & 0b111111;
      // TODO verify top 10 bits
      codePoint = (codePoint << 6) | value;
    }
    if (Character.isBmpCodePoint(codePoint)) {
      return codePoint;
    } else {
      char highSurrogate = Character.highSurrogate(codePoint);
      this.pendingLowSurrogate = true;
      this.lowSurrogate = Character.lowSurrogate(codePoint);
      return highSurrogate;
    }
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    Objects.checkFromIndexSize(off, len, cbuf.length);
    for (int i = 0; i < len; i++) {
      int c = this.read();
      if (c == -1) {
        return i;
      }
      cbuf[off + i] = (char) c;
    }
    return len;
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
