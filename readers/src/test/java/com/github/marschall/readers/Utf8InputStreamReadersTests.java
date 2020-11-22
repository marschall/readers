package com.github.marschall.readers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class Utf8InputStreamReadersTests {

  @ParameterizedTest
  @MethodSource("readers")
  void read(Reader reader) throws IOException {
    try (reader) {
      assertEquals(0x0024, reader.read());
      assertEquals(0x00A2, reader.read());
      assertEquals(0x0939, reader.read());
      assertEquals(0x20AC, reader.read());
      assertEquals(0xD55C, reader.read());
      assertEquals(Character.highSurrogate(0x10348), reader.read());
      assertEquals(Character.lowSurrogate(0x10348), reader.read());
      assertEquals(-1, reader.read());
    }
    assertThrows(IOException.class, () -> reader.read());
  }

  @ParameterizedTest
  @MethodSource("readers")
  void skip(Reader reader) throws IOException {
    try (reader) {
      assertEquals(2L, reader.skip(2L));
//      assertEquals(0x0024, reader.read());
//      assertEquals(0x00A2, reader.read());
      assertEquals(0x0939, reader.read());
      assertEquals(3L, reader.skip(3L));
//      assertEquals(0x20AC, reader.read());
//      assertEquals(0xD55C, reader.read());
//      assertEquals(Character.highSurrogate(0x10348), reader.read());
      assertEquals(Character.lowSurrogate(0x10348), reader.read());
      assertEquals(-1, reader.read());
    }
    assertThrows(IOException.class, () -> reader.skip(1L));
  }

  @ParameterizedTest
  @MethodSource("asciiReaders")
  void skipAscii(Reader reader) throws IOException {
    try (reader) {
      char[] input = new char[] {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90};
      char[] actual = new char[input.length - 8];
      assertEquals(8, reader.skip(8L));
      assertEquals(input.length - 8, reader.read(actual));
      char[] expected = Arrays.copyOfRange(input, 8, input.length);
      assertArrayEquals(expected, actual);
      assertEquals(-1, reader.read());
    }
  }

  @ParameterizedTest
  @MethodSource("readers")
  void readCharArray(Reader reader) throws IOException {
    try (reader) {
      char[] expected = new char[] { 0x0024, 0x00A2, 0x0939, 0x20AC, 0xD55C, Character.highSurrogate(0x10348), Character.lowSurrogate(0x10348)};
      char[] actual = new char[expected.length];
      assertEquals(expected.length, reader.read(actual));
      assertArrayEquals(expected, actual);
      assertEquals(-1, reader.read(actual));
    }
    assertThrows(IOException.class, () -> reader.read(new char[1]));
  }

  @ParameterizedTest
  @MethodSource("readers")
  void readCharBuffer(Reader reader) throws IOException {
    try (reader) {
      char[] expected = new char[] { 0x0024, 0x00A2, 0x0939, 0x20AC, 0xD55C, Character.highSurrogate(0x10348), Character.lowSurrogate(0x10348)};
      char[] actual = new char[expected.length];
      CharBuffer buffer = CharBuffer.wrap(actual);
      assertEquals(expected.length, reader.read(buffer));
      assertArrayEquals(expected, actual);
      assertEquals(-1, reader.read(actual));
    }
    assertThrows(IOException.class, () -> reader.read(CharBuffer.allocate(1)));
  }

  @ParameterizedTest
  @MethodSource("asciiReaders")
  void readCharArrayAscii(Reader reader) throws IOException {
    try (reader) {
      char[] expected = new char[] {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90};
      char[] actual = new char[expected.length];
      assertEquals(expected.length, reader.read(actual));
      assertArrayEquals(expected, actual);
      assertEquals(-1, reader.read(actual));
    }
  }

  @ParameterizedTest
  @MethodSource("readers")
  void transferTo(Reader reader) throws IOException {
    try (reader) {
      StringWriter stringWriter = new StringWriter();
      reader.transferTo(stringWriter);
      String expected = new String(
              new char[] { 0x0024, 0x00A2, 0x0939, 0x20AC, 0xD55C, Character.highSurrogate(0x10348), Character.lowSurrogate(0x10348)});
      assertEquals(expected, stringWriter.toString());

      assertEquals(-1, reader.read());
    }
    assertThrows(IOException.class, () -> reader.transferTo(new StringWriter()));
  }

  private static List<Reader> readers() {
    return List.of(
        new InputStreamReader(newByteArrayInputStream(), UTF_8),
        new BufferedUtf8InputStreamReader(newByteArrayInputStream()),
        new Utf8InputStreamReader(newByteArrayInputStream())
        );
  }

  private static InputStream newByteArrayInputStream() {
    return new ByteArrayInputStream(new byte[] {
        0x24,
        (byte) 0xC2, (byte) 0xA2,
        (byte) 0xE0, (byte) 0xA4, (byte) 0xB9,
        (byte) 0xE2, (byte) 0x82, (byte) 0xAC,
        (byte) 0xED, (byte) 0x95, (byte) 0x9C,
        (byte) 0xF0, (byte) 0x90, (byte) 0x8D, (byte) 0x88
    });
  }

  private static List<Reader> asciiReaders() {
    return List.of(
            new InputStreamReader(newAsciiByteArrayInputStream(), UTF_8),
            new BufferedUtf8InputStreamReader(newAsciiByteArrayInputStream()),
            new Utf8InputStreamReader(newAsciiByteArrayInputStream())
            );
  }

  private static InputStream newAsciiByteArrayInputStream() {
    byte[] data = new byte[('Z' - 'A') + 1];
    for (int i = 'A'; i <= 'Z'; i++) {
      data[i - 'A'] = (byte) i;
    }
    return new ByteArrayInputStream(data);
  }

}
