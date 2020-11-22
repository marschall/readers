Readers
=======

Specialized implementations of `java.io.Reader` with an emphasis on reducing intermediate allocations.

`java.io.InputStreamReader` relies on `sun.nio.cs.StreamDecoder` which relies on `java.nio.charset.CharsetDecoder` which is very generic but produces quite a few intermediate allocations. This can be a problem for small reads.

 * `com.github.marschall.readers.Utf8InputStreamReader` a UTF-8 decoding `Reader` on an `InputStream` that performs no buffering, eg. because the `InputStream` already buffers. Avoids intermediate allocations in favor of more `java.io.InputStream#read()` invocations.
 * `com.github.marschall.readers.BufferedUtf8InputStreamReader` a UTF-8 decoding `Reader` that also buffers. Avoids intermediate allocations except for the one time buffer allocation.


The implementations are currently very biased towards ASCII input.
The implementations fully support non-BMP code points that result in two Java `char` (high and low surrogate).
The implementations are currently not thread-safe.

Caveats
=======

* no checks for [non-shortest form](https://unicode.org/versions/corrigendum1.html)
