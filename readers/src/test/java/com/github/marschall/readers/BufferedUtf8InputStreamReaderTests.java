package com.github.marschall.readers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;

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

  @Test
  void read() throws IOException {
    Reader reader = new BufferedUtf8InputStreamReader(new ByteArrayInputStream(new byte[] {
        0x24,
        (byte) 0xC2, (byte) 0xA2,
        (byte) 0xE0, (byte) 0xA4, (byte) 0xB9,
        (byte) 0xE2, (byte) 0x82, (byte) 0xAC,
        (byte) 0xED, (byte) 0x95, (byte) 0x9C,
        // 0xF0, 0x90, 0x8D, 0x88
    }));
    try {
      assertTrue(reader.ready());
      assertEquals(0x0024, reader.read());
      assertEquals(0x00A2, reader.read());
      assertEquals(0x0939, reader.read());
      assertEquals(0x20AC, reader.read());
      assertEquals(0xD55C, reader.read());
//    assertEquals(Character.lowSurrogate(0x10348), reader.read());
//    assertEquals(Character.highSurrogate(0x10348), reader.read());
      assertFalse(reader.ready());
      assertEquals(-1, reader.read());
    } finally {
      reader.close();
    }
  }

}
