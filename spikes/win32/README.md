# Win32 window and Advanced Color spike

This M0 spike generates Java 25 FFM bindings from the target-resolved `win32-schema-v1.json`; it does not use JNI, JNA, an FFI provider, or a project-built native library. The profile creates a Unicode top-level window, drives its message pump, paints a solid system color, and verifies resize, pointer, keyboard, close, synchronous reentrancy, and callback-failure containment behavior. A deliberate invalid-window call also verifies that a generated typed result captures the immediate `GetLastError` value.

The same generated source models fixed COM vtable slots as schema aggregates and invokes them through exact unbound function-pointer downcalls. The profile creates an `IDXGIFactory1`, maps the window monitor to an `IDXGIOutput6`, and records the current `DXGI_OUTPUT_DESC1` color-space, chromaticity, and luminance snapshot. It independently uses `QueryDisplayConfig` and `DisplayConfigGetDeviceInfo` to record the matching active target's Advanced Color supported, enabled, policy, encoding, and precision state. Cloned active paths must report consistent target state. Capability data is dynamic: no format, color-space value, or monitor name is interpreted as a permanent HDR guarantee.

Run the complete Windows x64 profile from the repository root:

```text
./gradlew -g .gradle-user-home :spikes:win32:conformance
```

The full profile performs 25 event cycles over at least 120 seconds and writes `events.json`, `capabilities.json`, and the JDK native-library trace under `build/conformance/m0-win32-window/`. `check` runs a three-cycle smoke profile unless `-PfullWin32Conformance` is present. Native access is granted only to `org.glavo.himari.spikes.windows`; non-Windows hosts compile the schema and Java sources but skip Windows execution evidence. Version 1 of the fixture is Windows x64; Windows arm64 remains part of the profile matrix and requires its own target-resolved schema evidence.
