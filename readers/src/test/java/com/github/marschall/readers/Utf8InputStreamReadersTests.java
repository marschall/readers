package com.github.marschall.readers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class Utf8InputStreamReadersTests {

  private static final char[] EXPECTED_SAMPLE_OUTPUT = new char[] { 0x0024, 0x00A2, 0x0939, 0x20AC, 0xD55C, Character.highSurrogate(0x10348), Character.lowSurrogate(0x10348), 0x24};

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
      assertEquals(0x0024, reader.read());
      assertEquals(-1, reader.read());
    }
    assertThrows(IOException.class, () -> reader.read());
  }

  @ParameterizedTest
  @MethodSource("readers")
  void skipHighSurrogate(Reader reader) throws IOException {
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
      assertEquals(0x0024, reader.read());
      assertEquals(-1, reader.read());
    }
    assertThrows(IOException.class, () -> reader.skip(1L));
  }
  
  @ParameterizedTest
  @MethodSource("readers")
  void skipLowSurrogate(Reader reader) throws IOException {
    try (reader) {
      assertEquals(5L, reader.skip(5L));
//      assertEquals(0x0024, reader.read());
//      assertEquals(0x00A2, reader.read());
//      assertEquals(0x0939, reader.read());
//      assertEquals(0x20AC, reader.read());
//      assertEquals(0xD55C, reader.read());
      assertEquals(Character.highSurrogate(0x10348), reader.read());
      assertEquals(1L, reader.skip(1L));
//      assertEquals(Character.lowSurrogate(0x10348), reader.read());
      assertEquals(0x0024, reader.read());
      assertEquals(-1, reader.read());
    }
    assertThrows(IOException.class, () -> reader.skip(1L));
  }
  
  @ParameterizedTest
  @MethodSource("readers")
  void skipAtEnd(Reader reader) throws IOException {
    try (reader) {
      assertEquals(8L, reader.skip(8L));
//      assertEquals(0x0024, reader.read());
//      assertEquals(0x00A2, reader.read());
//      assertEquals(0x0939, reader.read());
//      assertEquals(0x20AC, reader.read());
//      assertEquals(0xD55C, reader.read());
//      assertEquals(Character.highSurrogate(0x10348), reader.read());
//      assertEquals(Character.lowSurrogate(0x10348), reader.read());
//      assertEquals(0x0024, reader.read());
      assertEquals(0L, reader.skip(1L));
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
  void readCharArrayExact(Reader reader) throws IOException {
    try (reader) {
      char[] expected = EXPECTED_SAMPLE_OUTPUT;
      char[] actual = new char[expected.length];
      assertEquals(expected.length, reader.read(actual));
      assertArrayEquals(expected, actual);
      assertEquals(-1, reader.read(actual));
    }
    assertThrows(IOException.class, () -> reader.read(new char[1]));
  }
  
  @ParameterizedTest
  @MethodSource("readers")
  void readCharArrayLarger(Reader reader) throws IOException {
    try (reader) {
      char[] expected = EXPECTED_SAMPLE_OUTPUT;
      char[] actual = new char[expected.length + 1];
      assertEquals(expected.length, reader.read(actual));
      assertArrayEquals(expected, Arrays.copyOfRange(actual, 0, expected.length));
      assertEquals(-1, reader.read(actual));
    }
    assertThrows(IOException.class, () -> reader.read(new char[1]));
  }

  @ParameterizedTest
  @MethodSource("readers")
  void readCharBuffer(Reader reader) throws IOException {
    try (reader) {
      char[] expected = EXPECTED_SAMPLE_OUTPUT;
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
      String expected = new String(EXPECTED_SAMPLE_OUTPUT);
      assertEquals(expected, stringWriter.toString());

      assertEquals(-1, reader.read());
    }
    assertThrows(IOException.class, () -> reader.transferTo(new StringWriter()));
  }

  @ParameterizedTest
  @MethodSource("invalidReaders")
  void invalid(Reader reader) throws IOException {
    try (reader) {
      assertEquals(0xFFFD, reader.read());
      assertEquals(-1, reader.read());
//      assertEquals('A', reader.read());
    }
  }
  
  private static List<Reader> makeReaders(byte[] b) {
    return List.of(
        new InputStreamReader(new ByteArrayInputStream(b.clone()), UTF_8),
        new BufferedUtf8InputStreamReader(new ByteArrayInputStream(b.clone())),
        new Utf8InputStreamReader(new ByteArrayInputStream(b.clone()))
        );
  }

  private static List<Reader> readers() {
    return makeReaders(sampleInput());
  }

  private static byte[] sampleInput() {
    return new byte[] {
        0x24,
        (byte) 0xC2, (byte) 0xA2,
        (byte) 0xE0, (byte) 0xA4, (byte) 0xB9,
        (byte) 0xE2, (byte) 0x82, (byte) 0xAC,
        (byte) 0xED, (byte) 0x95, (byte) 0x9C,
        (byte) 0xF0, (byte) 0x90, (byte) 0x8D, (byte) 0x88,
        0x24
    };
  }

  private static List<Reader> asciiReaders() {
    return makeReaders(asciiByteArray());
  }
  
  private static List<Reader> invalidReaders() {
    return invalidSequences().stream()
          .flatMap(b -> makeReaders(b).stream())
          .collect(Collectors.toList());
  }

  private static byte[] asciiByteArray() {
    byte[] data = new byte[('Z' - 'A') + 1];
    for (int i = 'A'; i <= 'Z'; i++) {
      data[i - 'A'] = (byte) i;
    }
    return data;
  }
  
  private static List<byte[]> invalidSequences() {
    List<byte[]> invalidSequences = new ArrayList<>();
    for (int i = 0xC0; i <= 0xC1; i++) {
      invalidSequences.add(new byte[] {(byte) i});
    }
    for (int i = 0xF5; i <= 0xFF; i++) {
      invalidSequences.add(new byte[] {(byte) i});
    }
    for (int i = 0xC2; i <= 0xDF; i++) {
      invalidSequences.add(new byte[] {(byte) i, (byte) 0x00});
      invalidSequences.add(new byte[] {(byte) i, (byte) 0x7F});
      
      invalidSequences.add(new byte[] {(byte) i, (byte) 0xC0});
      invalidSequences.add(new byte[] {(byte) i, (byte) 0xFF});
    }
    return invalidSequences;
  }

}
