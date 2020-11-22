package com.github.marschall.readers.benchmark;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import com.github.marschall.readers.BufferedUtf8InputStreamReader;
import com.github.marschall.readers.Utf8InputStreamReader;

@BenchmarkMode(Throughput)
@OutputTimeUnit(MICROSECONDS)
@State(Benchmark)
public class ReaderBenchmark {

  private static final byte[] DATA = new byte[8192];

  private static final int BUFFER_SIZE = 1024;

  static {
    Arrays.fill(DATA, (byte) 'A');
  }

  private Reader inputStreamReader;

  private char[] inputStreamReaderBuffer;

  private Reader bufferedInputStreamReader;

  private char[] bufferedInputStreamReaderBuffer;

  private Reader bufferedUtf8InputStreamReader;

  private char[] bufferedUtf8InputStreamReaderBuffer;

  private Reader utf8InputStreamReader;

  private char[] utf8InputStreamReaderBuffer;

  @Setup
  public void setup() {
    this.inputStreamReader = new InputStreamReader(new ByteArrayInputStream(DATA), StandardCharsets.UTF_8);
    this.inputStreamReaderBuffer = new char[BUFFER_SIZE];

    this.bufferedInputStreamReader = new InputStreamReader(new BufferedInputStream(new ByteArrayInputStream(DATA)), StandardCharsets.UTF_8);
    this.bufferedInputStreamReaderBuffer = new char[BUFFER_SIZE];

    this.utf8InputStreamReader = new Utf8InputStreamReader(new ByteArrayInputStream(DATA));
    this.utf8InputStreamReaderBuffer = new char[BUFFER_SIZE];

    this.bufferedUtf8InputStreamReader = new BufferedUtf8InputStreamReader(new ByteArrayInputStream(DATA));
    this.bufferedUtf8InputStreamReaderBuffer = new char[BUFFER_SIZE];
  }

  @Benchmark
  public void readSingleCharBufferedInputStreamReader(Blackhole blackhole) throws IOException {
    int c = this.bufferedInputStreamReader.read();
    while (c != -1) {
      blackhole.consume(c);
      c = this.bufferedInputStreamReader.read();
    }
  }

  @Benchmark
  public void readMultipleCharBufferedInputStreamReader(Blackhole blackhole) throws IOException {
    int c = this.bufferedInputStreamReader.read(this.bufferedInputStreamReaderBuffer);
    while (c != -1) {
      blackhole.consume(this.bufferedInputStreamReaderBuffer);
      c = this.bufferedInputStreamReader.read(this.bufferedInputStreamReaderBuffer);
    }
  }

  @Benchmark
  public void readSingleCharInputStreamReader(Blackhole blackhole) throws IOException {
    int c = this.inputStreamReader.read();
    while (c != -1) {
      blackhole.consume(c);
      c = this.inputStreamReader.read();
    }
  }

  @Benchmark
  public void readMultipleCharInputStreamReader(Blackhole blackhole) throws IOException {
    int c = this.inputStreamReader.read(this.inputStreamReaderBuffer);
    while (c != -1) {
      blackhole.consume(this.inputStreamReaderBuffer);
      c = this.inputStreamReader.read(this.inputStreamReaderBuffer);
    }
  }

  @Benchmark
  public void readSingleCharUtf8InputStreamReader(Blackhole blackhole) throws IOException {
    int c = this.utf8InputStreamReader.read();
    while (c != -1) {
      blackhole.consume(c);
      c = this.utf8InputStreamReader.read();
    }
  }

  @Benchmark
  public void readMultipleCharUtf8InputStreamReader(Blackhole blackhole) throws IOException {
    int c = this.utf8InputStreamReader.read(this.utf8InputStreamReaderBuffer);
    while (c != -1) {
      blackhole.consume(this.utf8InputStreamReaderBuffer);
      c = this.utf8InputStreamReader.read(this.utf8InputStreamReaderBuffer);
    }
  }

//  @Benchmark
  public void readSingleCharBufferedUtf8InputStreamReader(Blackhole blackhole) throws IOException {
    int c = this.bufferedUtf8InputStreamReader.read();
    while (c != -1) {
      blackhole.consume(c);
      c = this.bufferedUtf8InputStreamReader.read();
    }
  }

//  @Benchmark
  public void readMultipleCharBufferedUtf8InputStreamReader(Blackhole blackhole) throws IOException {
    int c = this.bufferedUtf8InputStreamReader.read(this.bufferedUtf8InputStreamReaderBuffer);
    while (c != -1) {
      blackhole.consume(this.bufferedUtf8InputStreamReaderBuffer);
      c = this.bufferedUtf8InputStreamReader.read(this.bufferedUtf8InputStreamReaderBuffer);
    }
  }

}
