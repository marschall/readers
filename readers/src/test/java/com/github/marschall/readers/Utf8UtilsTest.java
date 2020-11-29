package com.github.marschall.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class Utf8UtilsTest {

  @Test
  void getByteLength() throws IOException {
    for (int i = 0; i < 128; i++) {
      assertEquals(1, Utf8Utils.getByteLength((byte) i));
    }
    assertEquals(2, Utf8Utils.getByteLength((byte) 0b110_00000));
    assertEquals(2, Utf8Utils.getByteLength((byte) (0b111_00000 - 1)));

    assertEquals(3, Utf8Utils.getByteLength((byte) 0b1110_0000));
    assertEquals(3, Utf8Utils.getByteLength((byte) (0b1111_0000 - 1)));

    assertEquals(4, Utf8Utils.getByteLength((byte) 0b11110_000));
    assertEquals(4, Utf8Utils.getByteLength((byte) (0b11111_000 - 1)));

    assertEquals(5, Utf8Utils.getByteLength((byte) 0b11111_000));
    assertEquals(1, Utf8Utils.getByteLength((byte) 128));
    assertEquals(8, Utf8Utils.getByteLength((byte) -1));
//    assertThrows(IOException.class, () -> Utf8Utils.getByteLength((byte) 0b11111_000));
//    assertThrows(IOException.class, () -> Utf8Utils.getByteLength((byte) 128));
//    assertThrows(IOException.class, () -> Utf8Utils.getByteLength((byte) -1));
  }

}
