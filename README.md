Readers
=======

Specialized implementations of `java.io.Reader` with an emphasis on reducing intermediate allocations.


`java.io.InputStreamReader` relies on `sun.nio.cs.StreamDecoder` which relies on `java.nio.charset.CharsetDecoder` which is very generic but produces quite a few intermediate allocations. This can be a problem for small reads.

 * `com.github.marschall.readers.Utf8InputStreamReader` a UTF-8 decoding `Reader` on an `InputStream` that requires no buffering, eg. because the `InputStream` already buffers. Avoids intermediate allocations in favor of more `java.io.InputStream#read()` invocations.

The implementations are currently very biased towards ASCII input.

Caveats
=======

* no checks for [non-shortest form](https://unicode.org/versions/corrigendum1.html)
