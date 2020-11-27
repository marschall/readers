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
    if ((i & 0b1000_0000) == 0) {
      return 1;
    } else {
      return Integer.numberOfLeadingZeros((i << 24) ^ -1);
    }
  }

}
