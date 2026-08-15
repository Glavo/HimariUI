# Canonical FFI Schema Tool

This module owns the target-resolved ABI document consumed by HimariUI's generated desktop bindings. It has no native dependency and performs no runtime FFI provider selection.

Version 1 models:

- fixed-width primitive types and signedness;
- typed pointers and opaque handles;
- structures, unions, bitfields, alignment, and packing;
- enums and flags;
- fixed and variadic functions with calling convention and immediate native-error policy;
- callback/function-pointer signatures with explicit lifetime and exception containment;
- parameter direction, nullability, ownership, thread restriction, and platform-version availability.

`schema/ffi-schema.schema.json` defines the structural JSON format and is packaged at the same path in the tool JAR. `AbiSchemaValidator` adds cross-reference, layout, qualifier, and callback rules that JSON Schema cannot express. The codec rejects duplicate or unknown fields and writes declarations in a deterministic order.

`schema/abi-probe.schema.json` and `AbiProbeCodec` define the separate native-measurement protocol used by non-published C probes. Protocol version 1 records an exact target and compiler identity, type and aggregate layouts, field and bitfield positions, callback pointer properties, and fixed functional ABI checks. The codec rejects unknown, incomplete, duplicate, and out-of-range records and emits canonical JSON.

`FfmJavaGenerator` consumes a validated, target-resolved schema and emits one layout/descriptor class plus one binding class. The first profile generates Java 25 fixed-signature system-convention downcalls, exact `MethodHandle.invokeExact` calls, unbound callback/function-pointer downcalls, and upcall stubs that contain every `Throwable` through `CallbackFailureSink`. Aggregate parameters and returns use their declared layouts; aggregate-return callbacks receive exact allocator-aware downcalls and a process-lifetime zero fallback for contained upcall failures. Functions with `errno` or `get_last_error` policies return typed value/error records backed by `Linker.Option.captureCallState`, so the native error is captured before another call can overwrite it. The generated code performs no reflective or `Object[]` dispatch.

`NativeImageMetadataGenerator` consumes the same schema and emits deterministic GraalVM `reachability-metadata.json`. It registers every generated exported-function and unbound function-pointer downcall, generic upcalls for dynamically bound callback targets, the private generated callback adapters resolved through `MethodHandles.Lookup`, exact `ValueLayout.JAVA_*`/aggregate layouts, and native-error capture options. The FFM, Win32, and D3D12 spike JARs package their own generated metadata under `META-INF/native-image/`; no SVM-specific call implementation is generated.

The first profile fails closed for variadic functions without explicit variants and non-system calling conventions. Later generator work must add those features as typed schema-driven paths rather than weakening invocation typing.

Run the complete profile command from the repository root:

```text
./gradlew -g .gradle-user-home :tools:ffi-schema:check
```

The command validates `ffi-minimum-schema-v1`, executes positive and negative tests, and writes canonical `schema.json` and `report.json` artifacts under `build/conformance/m0-ffi-schema/`.

The `spikes/ffi-ffm` module provides the executable JVM profile. It generates bindings for a portable C runtime fixture and exercises primitive, pointer, structure-by-value, repeated callback, reentrant callback, failure-containment, and arena-lifetime behavior without a project-built native shim:

```text
./gradlew -g .gradle-user-home :spikes:ffi-ffm:conformance
```

The `spikes/abi-probe` module independently compiles a C17 probe, requires two byte-identical executions, and compares every measurement with both the canonical schema and generated Java layouts:

```text
./gradlew -g .gradle-user-home :spikes:abi-probe:conformance
```

The Win32 spike represents verified COM vtable slots with ordinary target-resolved aggregate offsets and invokes them through generated unbound function-pointer downcalls. Higher-level COM interface descriptors and Objective-C class/selector descriptors remain later versioned extensions. They must extend this canonical model without adding a runtime provider SPI or weakening the fixed-signature binding path.

With `GRAALVM_HOME` set to a GraalVM 25 installation, the Native Image profile builds in exact reachability mode and runs the portable callback/lifetime fixture together with the real Win32/D3D12 surface fixture:

```text
./gradlew -g .gradle-user-home :spikes:native-image-ffm:conformance
```

On Windows, the task locates Visual Studio through `vswhere`, initializes the x64 C toolchain with `vcvars64.bat`, and records the builder, generated metadata hashes, binary hash, direct system-library lookups, process budgets, and platform-clear evidence under `build/conformance/m0-native-image-ffm/`.
