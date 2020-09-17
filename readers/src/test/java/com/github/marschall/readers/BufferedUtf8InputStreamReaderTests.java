package com.github.marschall.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class BufferedUtf8InputStreamReaderTests {

  @Test
  void getByteLength() throws IOException {
    for (int i = 0; i < 128; i++) {
      assertEquals(1, BufferedUtf8InputStreamReader.getByteLength((byte) i));
    }
    assertEquals(2, BufferedUtf8InputStreamReader.getByteLength((byte) 0b110_00000));
    assertEquals(2, BufferedUtf8InputStreamReader.getByteLength((byte) (0b111_00000 - 1)));

    assertEquals(3, BufferedUtf8InputStreamReader.getByteLength((byte) 0b1110_0000));
    assertEquals(3, BufferedUtf8InputStreamReader.getByteLength((byte) (0b1111_0000 - 1)));

    assertEquals(4, BufferedUtf8InputStreamReader.getByteLength((byte) 0b11110_000));
    assertEquals(4, BufferedUtf8InputStreamReader.getByteLength((byte) (0b11111_000 - 1)));

    assertThrows(IOException.class, () -> BufferedUtf8InputStreamReader.getByteLength((byte) 0b11111_000));
    assertThrows(IOException.class, () -> BufferedUtf8InputStreamReader.getByteLength((byte) 128));
    assertThrows(IOException.class, () -> BufferedUtf8InputStreamReader.getByteLength((byte) -1));
  }

}
