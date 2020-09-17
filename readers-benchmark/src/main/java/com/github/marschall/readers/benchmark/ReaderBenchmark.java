package com.github.marschall.readers.benchmark;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import com.github.marschall.readers.BufferedUtf8InputStreamReader;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ReaderBenchmark {
  
  private static final byte[] DATA = new byte[8192];
  
  static {
    Arrays.fill(DATA, (byte) 'A');
  }

  private Reader inputStreamReader;

  private Reader bufferedUtf8InputStreamReader;

  @Setup
  public void setup() {
    this.inputStreamReader = new InputStreamReader(new BufferedInputStream(new ByteArrayInputStream(DATA)), StandardCharsets.UTF_8);
    this.bufferedUtf8InputStreamReader = new BufferedUtf8InputStreamReader(new ByteArrayInputStream(DATA));
  }

  @Benchmark
  public void readSingleCharInputStreamReader(Blackhole blackhole) throws IOException {
    int c = inputStreamReader.read();
    while (c != -1) {
      blackhole.consume(c);
      c = inputStreamReader.read();
    }
  }
  
  @Benchmark
  public void readSingleCharBufferedUtf8InputStreamReader(Blackhole blackhole) throws IOException {
    int c = bufferedUtf8InputStreamReader.read();
    while (c != -1) {
      blackhole.consume(c);
      c = bufferedUtf8InputStreamReader.read();
    }
  }

}
