# Fixed-signature FFM spike

This M0 spike generates Java 25 FFM layouts, downcalls, and contained upcalls from a canonical ABI schema. It invokes the host C runtime directly: UCRT on Windows, libc on Linux, and libSystem on macOS. No project-built native shim or packaged native library participates.

The three profile fixtures are exercised as follows:

- `ffi-primitives-v1`: `abs` and `strlen` verify exact scalar and pointer carriers;
- `ffi-struct-by-value-v1`: `div` verifies an eight-byte `div_t` structure return;
- `ffi-callback-reentrant-v1`: `qsort` verifies repeated callbacks, same-thread delivery, callback-arena lifetime rejection, exception containment, and a reentrant `abs` downcall from the comparator.

Run the short smoke through `check`, or run the complete profile from the repository root:

```text
./gradlew -g .gradle-user-home :spikes:ffi-ffm:conformance
```

The Gradle profile grants native access only to `org.glavo.himari.spikes.ffi.ffm`. Any standalone named-module launcher must pass `--enable-native-access=org.glavo.himari.spikes.ffi.ffm`; Gradle's classpath-based test worker uses `--enable-native-access=ALL-UNNAMED`.
