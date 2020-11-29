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
  
  static boolean isValidTwoByteSequence(int c1, int c2) {
//    return (c1 >= 0xC2 & c1 <= 0xDF) & (c2 >= 0x80 & c2 <= 0xBF);
    return (c1 >= 0xC2 & c1 <= 0xDF) & ((c2 & 0b11000000) == 0b10000000);
  }
  
  static boolean isValidThreeByteSequence(int c1, int c2, int c3) {
//    return ((c1 == 0xE0 & c2 >= 0xA0 & c2 <= 0xBF)
//        | (c1 >= 0xE1 & c1 <= 0xEF & c2 >= 0x80 & c2 <= 0xBF))
//        & (c3 >= 0x80 | c3 <= 0xBF);
    return ((c1 == 0xE0 & c2 >= 0xA0 & c2 <= 0xBF)
        | (c1 >= 0xE1 & c1 <= 0xEF & c2 >= 0x80 & c2 <= 0xBF))
        & ((c3 & 0b11000000) == 0b10000000);
  }
  
  static boolean isValidFourByteSequence(int c1, int c2, int c3, int c4) {
//    return ((c1 == 0xF0 & c2 >= 0x90 & c2 <= 0xBF)
//        | (c1 >= 0xF1 & c1 <= 0xF3 & c2 >= 0x80 & c2 <= 0xBF)
//        | (c1 == 0xF4 & c2 >= 0x80 & c2 <= 0x8F))
//        & (c3 >= 0x80 & c3 <= 0xBF)
//        & (c4 >= 0x80 & c4 <= 0xBF);
    return ((c1 == 0xF0 & c2 >= 0x90 & c2 <= 0xBF)
        | (c1 >= 0xF1 & c1 <= 0xF3 & ((c2 & 0b11000000) == 0b10000000))
        | (c1 == 0xF4 & c2 >= 0x80 & c2 <= 0x8F))
        & ((c3 & 0b11000000) == 0b10000000)
        & ((c4 & 0b11000000) == 0b10000000);
  }

}
