package com.github.marschall.readers;

import java.io.IOException;

final class Utf8Utils {

  private Utf8Utils() {
    throw new AssertionError("not instantiable");
  }

  static int getByteLength(byte b) throws IOException {
    int value = Byte.toUnsignedInt(b);
    return getByteLength(value);
  }

  static int getByteLength(int i) throws IOException {
    if (i < 0b1000_0000) {
      return 1;
    } else if (i >= 0b110_00000) {
      if (i < 0b1110_0000) {
        return 2;
      } else if (i < 0b11110_000) {
        return 3;
      } else if (i < 0b111110_00) {
        return 4;
      }
    }
    throw new IOException("invalid utf-8 first byte");
  }

}
