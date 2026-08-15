# Native ABI probe

This M0 spike compiles a non-published C17 executable and compares its machine-readable measurements with the canonical ABI schema and generated Java `MemoryLayout` constants. The executable is build evidence only: it is never packaged in a JAR or distributed with HimariUI.

`schema/abi-probe.schema.json` defines the structural wire format. `AbiProbeCodec` additionally rejects duplicate names, unsupported targets, invalid ranges, and incomplete bitfield records before any comparison runs.

The version 1 protocol records the exact operating system, architecture, byte order, compiler family, scalar and pointer layouts, structure/union layouts, field and bitfield positions, system-convention function-pointer behavior, a structure return, and a variadic call. Two byte-identical executions are required before comparison succeeds.

The compiler is selected from `HIMARI_CC` when set, then from `zig`, `cc`, `clang`, `gcc`, or `cl` on `PATH`. `HIMARI_CC_DRIVER` may be `zig`, `gnu`, or `msvc` when the executable name does not identify its command-line convention.

Run the complete profile from the repository root:

```text
./gradlew -g .gradle-user-home :spikes:abi-probe:conformance
```
