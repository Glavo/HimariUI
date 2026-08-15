# HimariUI Implementation Plan

> Status: Draft 0.9<br>
> Runtime baseline: Java 25<br>
> Initial platforms: Windows, macOS, Linux, and Headless<br>
> Future mobile policy: Android and iOS are post-stable Java 25 AOT targets and do not define the core compatibility baseline<br>
> Future additional hosts: FreeBSD, OpenBSD, and OpenHarmony are post-stable, separately versioned platform profiles and do not block the first stable desktop release<br>
> Future remote policy: the scene boundary is transport-ready, while networking and remote-session products remain post-stable extensions<br>
> Future color policy: first-stable color values, scene encodings, render paths, and surface contracts preserve extended-range and HDR semantics, while production hardware HDR presentation is capability-gated and not a first-stable release requirement<br>
> Primary distribution constraint: published core and desktop artifacts must not ship project-built or third-party CPU-native libraries; future mobile bundles may contain only declared target-generated AOT code and host glue<br>
> Last reviewed: 2026-08-15

---

## 0. How to Use This Plan

This file is the repository-level execution plan for HimariUI. It is not a product vision or a collection of optional ideas. Convert its milestone IDs, work-package IDs, and exit criteria directly into issues, project-board items, and CI gates.

Use the following rules when executing the plan:

- Treat accepted ADRs and the non-negotiable constraints in this file as fixed until a replacement ADR is accepted.
- Do not build downstream work on a deliverable whose consumed exit criteria have not passed. Independent tracks proceed in parallel under the dependency declarations in the milestone plan.
- Future targets may constrain the shape of contracts, types, and encodings in early milestones, but they must not add implementation deliverables to a pre-stable milestone unless a first-stable consumer exists. Record every such deferral in the owning milestone.
- Give every issue an executable acceptance command or a reproducible manual procedure.
- Treat words such as “correct,” “complete,” “usable,” “stable,” or “does not grow” as summaries, not acceptance criteria; bind them to a versioned conformance profile, fixture set, duration, tolerance, or resource budget before implementation begins.
- Give every work package one globally unique canonical ID. Milestone tables, bootstrap backlogs, project boards, and CI tasks may reference that ID but must never redefine it with a different scope.
- Distinguish when a work package may start from what must pass before it can complete. Do not serialize independent implementation merely because its integration gate belongs to a later milestone.
- Keep reference implementations available while optimized implementations are developed and validated.
- Record source versions, licenses, generated data, and accepted behavioral differences as repository artifacts rather than relying on task history.
- Keep this file as the execution index. As canonical ADR, conformance, protocol, or workstream documents are created, replace duplicated normative detail here with a concise milestone summary and a stable link; do not maintain two independently editable contracts.

Terms used throughout this plan:

- **Pure-Java core**: all implementation code is Java; published core JARs contain no `.dll`, `.so`, `.dylib`, `.jnilib`, `.a`, `.lib`, or `.o` CPU-native artifacts and have no runtime dependency that transitively ships them.
- **System library**: a library supplied by the operating system or system graphics stack, such as `user32.dll`, `d3d12.dll`, AppKit, Metal, `libwayland-client.so`, or `libvulkan.so.1`. HimariUI may call these libraries through FFM but must not repackage them.
- **Test/tool dependency**: JNA, LWJGL, and similar dependencies used only by Oracle runners, tests, or development tools. They must not become production backends or enter the standard runtime dependency graph.
- **Optional integration**: a non-core feature such as an FFmpeg binding. It must use a separate artifact, remain non-transitive by default, and stay outside the strict pure-Java distribution.
- **Target-generated mobile artifact**: AOT application code, launcher code, or narrowly generated platform glue produced only while packaging a future Android or iOS application. These files may enter the final mobile application bundle but must not enter published core or desktop JARs, become a general native backend, or introduce a third-party graphics stack.
- **Reference implementation**: a correctness-first, readable, scalar Java implementation suitable for differential validation.
- **Optimized implementation**: a SIMD, parallel, GPU, cache-aware, or otherwise optimized implementation added only after the reference path passes its conformance gates.
- **Oracle**: a native or external reference implementation used by tests, such as FreeType, HarfBuzz, SDL, Skia, Impeller, a platform text API, or LWJGL. Oracles may be test dependencies but must not enter the published runtime graph.

---

## 1. Outcome and Execution Strategy

### 1.1 Target outcome

Deliver a modern declarative GUI framework whose runtime, layout engine, text stack, software renderer, GPU abstraction, platform backends, and generated native bindings are implemented in Java 25. Published Java artifacts and first-stable desktop distributions must contain no project-built or third-party CPU-native libraries. Desktop platforms must be reached through generated, strongly typed FFM bindings to system APIs.

The first stable release must support deterministic Headless execution, software fallback, modern GPU backends, complete input and accessibility paths, a practical control set, color-managed SDR presentation with HDR-safe values/codecs/capability contracts, JVM execution, and GraalVM Native Image packaging.

### 1.2 Execution order

Execute the project in this order:

1. Prove the distribution and ABI model with repository guards, Headless execution, and platform/FFM feasibility spikes.
2. Build the state, declarative-runtime, layout, display-list, software-rendering, and basic-text reference paths.
3. Complete one end-to-end desktop slice, provisionally Linux Wayland plus Vulkan.
4. Complete Windows/D3D12 and macOS/Metal without introducing native shims.
5. Expand complex text, controls, accessibility, tooling, performance, Native Image packaging, and release hardening.

The order states dependency, not strict serialization; the milestone plan organizes the same work into parallel tracks. Correctness gates precede optimization at every stage. A visible demo does not replace ABI, corpus, differential, lifetime, or accessibility evidence.

### 1.3 Fixed top-level decisions

1. **Use Java 25 as the minimum runtime.** Core and internal implementations may use stable Java 25 language and library features. For JVM and Native Image desktop targets, FFM is the only native-access mechanism. Future Android and iOS targets must use a Java 25-capable AOT toolchain instead of imposing Android Runtime class-library compatibility on common modules. A future browser/Wasm target uses generated host bindings rather than FFM and does not reintroduce an FFI provider SPI. The Vector API remains incubating and may appear only in optional optimization modules.
2. **Do not require a compiler plugin.** Runtime correctness and the baseline application API must be usable from ordinary Java source. M1 compared explicit grouped recomposition, one-shot signal ownership, and a hybrid of fine-grained bindings with small structural scopes; ADR-023 selects explicit grouped recomposition for topology while retaining fine-grained value and phase consumers. Annotation processors or `javac` plugins may add diagnostics and optimizations, but correctness and acceptable baseline ergonomics must not depend on them.
3. **Use several purpose-specific trees.** Keep the reactive-owner/mounted-element, layout, layer/display-list, and semantics structures separate.
4. **Make the software renderer normative.** Add every path, blend, filter, and glyph-raster operation to the pure-Java scalar path before accepting Vulkan, D3D12, or Metal implementations.
5. **Use an explicit, backend-neutral RHI.** Model resources, pipelines, passes, command buffers, resource usages, pass dependencies, submission order, and ownership directly. Let Vulkan and D3D12 materialize native barriers from that model instead of exposing their raw barrier APIs as the cross-backend contract.
6. **Implement production text processing in Java.** ICU4J may supply Unicode data, Bidi, and boundary analysis. Implement OpenType parsing, GSUB/GPOS, script shaping, TrueType/CFF interpretation, hinting, and glyph rasterization in Java. Use system text APIs, FreeType, and HarfBuzz only for discovery or testing.
7. **Generate strongly typed desktop FFM bindings.** The canonical ABI schema generates layouts, downcalls, upcalls, error capture, metadata, and verification code for JVM and Native Image desktop targets. Do not define a runtime FFI provider SPI or a generic `Object...` invocation layer.
8. **Share the same desktop FFM path between the JVM and Native Image.** Generate reachability and downcall/upcall registration metadata at build time. Do not maintain SVM- or JNA-based desktop system-call backends.
9. **Prove infrastructure before building controls.** Complete Headless, software rendering, and ABI feasibility work before investing in a broad widget catalog.
10. **Port in four stages.** Every port follows specification, Oracle runner, Java reference implementation, and optimized implementation. AI-generated screenshots are never sufficient evidence.
11. **Use the accepted Himari naming scheme.** Maven coordinates use `org.glavo.himari:himari-*`; JPMS modules and Java packages use `org.glavo.himari.*`.
12. **Preserve browser/Wasm portability seams.** Platform startup, event delivery, rendering execution, clipboard, resource loading, font discovery, and GPU initialization must permit asynchronous, host-driven, and single-threaded implementations. Do not expose JavaScript, DOM, WebGPU, or Wasm runtime objects from platform-neutral public APIs.
13. **Treat mobile as a post-stable AOT extension.** Android and iOS support is contingent on a mobile AOT toolchain compiling the representative Java 25 core without source rewrites or a reduced Java profile. If no such toolchain is viable, defer the mobile target instead of lowering the runtime baseline, banning stable Java 25 APIs, or maintaining an ART-compatible common implementation.
14. **Make the scene boundary transport-ready without putting networking in the core.** `SceneSnapshot`, display-list, resource, semantics, and normalized-input encodings must be versioned, pointer-free, bounded, and replayable outside the producing process. The default renderer remains in-process. Authentication, encryption, discovery, congestion control, codecs, and remote-session policy belong to post-stable extensions.
15. **Model animation as transaction-scoped presentation state.** A committed state or property value is the authoritative target; animation derives a time-varying presentation value without writing source state on every frame. Sample related animations atomically, preserve presentation-value and velocity continuity across compatible interruptions, and execute each property at its declared structure, layout, paint, semantics, hit-test, or composite impact. Compiler assistance must not be required for these semantics.
16. **Keep gamut, transfer, luminance, and presentation capabilities orthogonal.** Represent primaries/white point, transfer function, scene- or display-referred interpretation, reference white/content luminance, numeric range, pixel format, and output-surface capabilities explicitly. Do not model HDR as a Boolean, equate P3 or BT.2020 with HDR, clamp finite color components to `[0, 1]` before an explicit gamut/tone-mapping or quantization step, or bake an SDR-only assumption into public values, display lists, scenes, images, RHI surfaces, or remote negotiation.

### 1.4 Default technology choices

| Area | Default | Constraint or rationale |
|---|---|---|
| Language/runtime | Java 25 | Stable public APIs must not expose preview or incubator types |
| Desktop native access | FFM only | Enable native access per JPMS module; no runtime provider selection |
| Desktop Native Image | Shared FFM bindings plus generated metadata | No separate SVM system-call backend |
| Future Android/iOS runtime | Java 25-capable mobile AOT; GraalVM Native Image-derived tooling is the initial candidate | Toolchain feasibility gates the target; ART compatibility does not constrain common modules |
| Future Android/iOS host access | Generated target-specific launcher and JNI/NDK or Objective-C/C glue | Separate packaging boundary; not the desktop FFM contract or an FFI provider |
| Future local browser/Wasm host access | Generated Wasm imports and JavaScript/browser host bindings | Separate target-specific boundary; not an FFI provider |
| Unicode | ICU4J | Isolate it behind the `org.glavo.himari.unicode` SPI |
| Coordinates | `org.glavo.himari` and `himari-*` | Follow ADR-013 for Maven, JPMS, and packages |
| UI model | Declarative, signal-driven, phase-aware, unidirectional data flow | Select the structural-update strategy in M1; no mandatory compiler plugin |
| Layout | Downward constraints, upward sizes, one measure per child by default | Treat intrinsic measurement as explicit and expensive |
| Drawing | Immutable display lists plus a retained layer tree | Support partial repaint and caching |
| Color and dynamic range | Color-managed SDR presentation first; tagged extensible encodings and extended-linear floating-point reference paths | First-stable APIs/codecs must preserve wide-gamut and HDR information even where hardware HDR output remains unavailable |
| Scene/process boundary | Versioned `SceneSnapshot` envelopes plus content-addressed resources | In-process mailbox by default; canonical encoding supports replay, process isolation, and future transport |
| CPU rendering | Tile-based pure-Java rasterizer | Scalar normative path plus optional Vector API acceleration |
| GPU rendering | Vulkan, D3D12, and Metal | Use a backend-neutral explicit RHI |
| Future remote Web client | Scene/display-list stream rendered through WebGPU or Canvas/software | Java 25 runtime remains authoritative on the server; never stream RHI or native GPU commands |
| Text | Pure-Java OpenType, shaping, and rasterization | Differentially validate against FreeType and HarfBuzz |
| Testing | JUnit 5, jqwik/Jazzer, JMH, native Oracle runners | LWJGL/JNA/system libraries are test-only |
| Build | Gradle multi-project plus JPMS | Publish a Maven BOM and modular JARs |

---

## 2. Non-Negotiable Constraints and CI Gates

### 2.1 Runtime distribution constraints

Apply these constraints to `himari-ui`, `himari-runtime`, `himari-render-*`, `himari-font`, `himari-text`, the first-stable desktop `himari-platform-*` and `himari-rhi-*` modules, `himari-ffi`, and their aggregated internal implementation modules. They govern published Java artifacts and the desktop runtime graph. A post-stable mobile application bundle may contain target-generated AOT code and narrowly generated host glue, but those files must remain outside standard JARs and desktop dependencies:

- Ship no JNI C/C++ source and no precompiled native bridge.
- Do not call `System.load` or `System.loadLibrary` for framework-owned files.
- Do not extract native files from JARs into temporary directories.
- Do not depend on AWT, Swing, Java2D, JavaFX, or SWT.
- Keep core modules free of `java.desktop` and `jdk.unsupported`.
- Do not use `sun.misc.Unsafe` or other internal JDK APIs.
- Do not add JNA, LWJGL, Skija, JavaCV, or FFmpeg bindings as default or transitive runtime dependencies.
- Allow ICU4J, pure-Java compression/codecs, and other dependencies only after pure-Java allowlist review.
- Treat shader bytecode, Unicode tables, and test-font data as data resources rather than CPU-native libraries, but record their source, version, hash, and license.

### 2.2 Required CI gates

Create `build-logic/pure-java-guard` as a staged gate registry. Each gate declares the milestone that activates it; an inactive gate is reported as not applicable, never as evidence that the corresponding feature passed. Implement and activate at least these gates at the owning milestones:

1. `verifyNoNativeEntries`: reject CPU-native file formats in every publishable JAR and runtime dependency JAR. A future isolated Web artifact may contain `.wasm` target bytecode, and a future mobile application bundle may contain target-generated AOT code and host glue; neither may enter core or desktop JARs.
2. `verifyDependencyAllowlist`: lock runtime dependencies and compare them with the approved pure-Java allowlist; use separate allowlists for optional modules.
3. `verifyNoDesktopModule`: use `jdeps` to reject `java.desktop` from the core dependency graph.
4. `verifyNoUnsupportedJdkApi`: run `jdeps --jdk-internals` and static scans for internal JDK APIs.
5. `verifyNoNativeKeyword`: reject Java `native` methods in runtime modules; Oracle test modules are the only exception.
6. `verifyNoExtractionPattern`: scan for `System.load*`, temporary native-file writes, and common native classifier patterns.
7. `verifyNativeLoadTrace`: run platform smoke tests with JVM library-load logging and compare loaded libraries with the system-library allowlist.
8. `verifyReproducibleArtifacts`: build the same commit twice and require identical artifact hashes.
9. `verifyLicenseManifest`: require provenance records for generated tables, ports, shader blobs, and test fonts.
10. `verifyTestRuntimeIsolation`: allow Oracle, LWJGL, and JNA dependencies only on `testRuntimeClasspath` or isolated `oracle-*` configurations.
11. `verifySingleFfmPath`: for JVM and Native Image desktop production modules, reject JNA, GraalVM SVM interop, and handwritten JNI. Permit creation of `FunctionDescriptor` values and calls to `Linker.downcallHandle` or `Linker.upcallStub` only in generated bindings or allowlisted `himari-ffi` support code. Future browser/Wasm modules must use their isolated host-binding boundary and must not depend on `himari-ffi`.
12. `verifyWebHostIsolation`: activate with W0/W1; require an explicit browser-import allowlist, reject desktop/FFM dependencies from Web artifacts, validate generated linear-memory marshalling, and reject dynamic JavaScript evaluation or undeclared ambient browser access.
13. `verifyMobileAotIsolation`: activate with A0; require mobile packaging inputs to be explicit, reject mobile launchers and generated host glue from core or desktop JARs, reject target runtime types from common public APIs, and verify that the representative Java 25 core is compiled without an ART compatibility fork or source rewrite.
14. `verifySceneCodec`: activate with `SCENE-CODEC-001` in M3; round-trip canonical scene, display-list, resource-manifest, snapshot/resource-generation, semantics, and normalized-input fixtures; reject missing or mismatched bases, illegal generation transitions, native addresses, Java object identity, target handles, malformed lengths, unsupported required features, hash mismatches, and configured resource-limit violations; fuzz every decoder.

`GUARD-FRAMEWORK-001` in M0 implements the registry and activates gates 1–11 for every artifact and module that exists at that commit. `GUARD-SCENE-001`, `verifyWebHostIsolation`, and `verifyMobileAotIsolation` are owned by M3, W0/W1, and A0 respectively. Adding a later module must automatically place it under every already-active applicable gate.

### 2.3 Published artifacts and user entry points

Use these public coordinates; keep the complete mapping in ADR-013:

```text
org.glavo.himari:himari-bom
org.glavo.himari:himari-desktop
org.glavo.himari:himari-ui
org.glavo.himari:himari-runtime
org.glavo.himari:himari-layout
org.glavo.himari:himari-graphics
org.glavo.himari:himari-render-software
org.glavo.himari:himari-render-gpu
org.glavo.himari:himari-font
org.glavo.himari:himari-text
org.glavo.himari:himari-controls
org.glavo.himari:himari-platform-windows
org.glavo.himari:himari-platform-macos
org.glavo.himari:himari-platform-linux-wayland
org.glavo.himari:himari-platform-linux-x11
org.glavo.himari:himari-platform-freebsd
org.glavo.himari:himari-platform-openbsd
org.glavo.himari:himari-platform-openharmony
org.glavo.himari:himari-platform-headless
org.glavo.himari:himari-testing
```

`himari-render-gpu` contains the backend-neutral frame compiler and render graph. Keep Vulkan, D3D12, and Metal implementations in `himari-rhi-vulkan`, `himari-rhi-d3d12`, and `himari-rhi-metal`.

Implementation artifacts such as `himari-ffi`, `himari-rhi-vulkan`, `himari-rhi-d3d12`, and `himari-rhi-metal` may be published for dependency composition but are not normal application entry points. `himari-ffi` contains shared FFM support and internal facilities required by generated bindings; it defines no provider SPI and is not a dependency applications should declare directly. Record every implementation coordinate and its BOM relationship in ADR-013. Do not publish a JNA production artifact or include one in the BOM.

`himari-platform-linux-x11`, `himari-platform-freebsd`, `himari-platform-openbsd`, and `himari-platform-openharmony` are reserved optional coordinates. Do not include them transitively from `himari-desktop` or advertise a stable version until the corresponding later-extension profile passes. The Linux Wayland and Linux X11 modules are not FreeBSD, OpenBSD, or OpenHarmony backends.

---

## 3. Release Scope

### 3.1 First stable release

Deliver all of the following:

- Required desktop platform profiles for Windows, macOS, and Linux Wayland. Linux X11 is a separately versioned compatibility profile that may ship with 1.0 but does not block the first stable release.
- x86-64 and arm64 processes only.
- Multiple windows, DPI/scaling, mouse, touch, pen, keyboard, IME, clipboard, and drag-and-drop support.
- A first-class Headless platform and deterministic software rendering.
- Declarative UI, state, layout, animation, scrolling, virtualized lists, and common controls.
- An extensible complete Unicode text pipeline with Bidi, font fallback, variable fonts, and major color-font formats. The first stable release blocks only on the Tier-1 script set: default/Latin/Greek/Cyrillic, Arabic, Hebrew, Hangul, and Thai/Lao, with Han and Kana covered by the default shaping path. Indic, Universal Shaping Engine scripts, Khmer, Myanmar, and Tibetan form a separately versioned Tier-2 shaping profile that may ship with 1.0 but does not block it.
- Accessibility bridges for Windows UI Automation, macOS Accessibility, and Linux AT-SPI2.
- Vulkan, D3D12, and Metal GPU backends plus CPU fallback.
- Color-managed sRGB SDR presentation; tagged Display-P3 values and conversion with direct P3 presentation only on verified capable surfaces; tagged extended-range color and profile resources; an extended-linear software reference path; deterministic HDR/WCG-to-SDR fallback; and truthful per-surface color/HDR capability reporting.
- JVM and GraalVM Native Image distribution paths.
- Inspector, versioned transport-ready scene/frame traces, deterministic offline replay, and golden-test tooling.

The first stable release is blocked only by the required common, Windows, macOS, Linux Wayland, Headless, software-renderer, and declared GPU profiles recorded in `PLATFORM_CONFORMANCE.yaml`. Optional capabilities such as X11, native HDR, FreeBSD, OpenBSD, or OpenHarmony have independent profiles and release versions; omitting or disabling one must be reported truthfully but does not weaken a required profile.

### 3.2 Explicit non-goals for the first stable release

- Android or iOS product support; both belong to the post-stable AOT extension track.
- FreeBSD, OpenBSD, or OpenHarmony product support; each belongs to a separately versioned later platform profile.
- Completion of the optional Linux X11 compatibility profile.
- Completion of the Tier-2 complex-script shaping profile (Indic, USE, Khmer, Myanmar, Tibetan).
- TrueType and CFF hinting interpretation. First stable renders unhinted outlines with gamma-correct grayscale antialiasing, optional vertical-only grid fitting, and embedded bitmap strikes where fonts provide them; the TrueType and CFF hinting virtual machines form a separately versioned hinting fidelity profile.
- Live remote sessions, network transports, authentication, adaptive streaming, and remote-desktop product features.
- Browser DOM/CSS compatibility.
- Swing or JavaFX control interoperability as a core architectural requirement.
- Arbitrary user shaders; expose only a controlled set of brushes, filters, and effects initially.
- General video codecs or media-container parsing as a release blocker.
- Pixel-identical output between software and GPU backends. Require semantic agreement for geometry, coverage, color, and blending, with bounded GPU tolerances.
- Production HDR/EDR output on every desktop, browser, or mobile backend. The first stable release must preserve tagged extended-range values and validate HDR reference math and capability fallback, but may advertise only SDR presentation on a platform whose HDR path has not passed its dedicated conformance gates.
- Bundling MoltenVK, ANGLE, SwiftShader, Mesa, FreeType, HarfBuzz, or similar native components to fill platform gaps.
- Requiring application developers to understand FFM, COM, the Objective-C runtime, or Wayland protocols.

### 3.3 Later extensions

- **Android, AOT-first and feasibility-gated**: compile the unchanged Java 25 runtime, layout, text, display-list, semantics, and rendering modules with a Java 25-capable mobile AOT toolchain. Use a thin Android host shell plus generated JNI/NDK glue for lifecycle, surfaces, input, IME, accessibility, and Vulkan. The shell may execute on ART, but the HimariUI core is not required to be ART-compatible. Treat a full ART execution path as a separate future decision rather than a constraint on current code.
- **iOS, AOT-only and feasibility-gated**: compile the same unchanged Java 25 subsystems with a suitable mobile AOT toolchain and use generated Objective-C/C glue for application lifecycle, UIKit, Metal, input, IME, and accessibility. GraalVM Native Image-derived tooling, initially Gluon Substrate/GluonFX, is a candidate to validate rather than an accepted dependency or proof that the desktop FFM path works on iOS.
- **Mobile compatibility rule**: require representative mobile AOT spikes to cover stable Java 25 APIs used by the core, including `MemorySegment` and `Arena` where applicable. A toolchain failure postpones Android/iOS support; it must not cause the main source set to adopt an older Java profile, avoid stable Java 25 features, or maintain parallel ART-compatible algorithms.
- **Browser/Wasm**: compile the portable Java subsystems to WebAssembly; use generated Wasm imports and JavaScript/browser host bindings, WebGPU with a Canvas fallback, host-driven event delivery, asynchronous browser capabilities, and a DOM-backed semantics/IME bridge. Reuse the backend-neutral RHI contract without requiring FFM or runtime JPMS. This target does not provide DOM/CSS visual compatibility.
- **Remote scene rendering and Web client**: keep the authoritative Java 25 runtime, layout, text shaping, hit testing, focus, and application state on a JVM or Native Image host. Stream versioned scene/display-list envelopes, content-addressed resources, correlated semantics updates, and lifecycle/configuration changes to a browser client that presents through WebGPU or Canvas/software and returns normalized input and IME transactions. This path must not require the full Java runtime to execute in the browser and must not expose component trees, RHI commands, native GPU commands, or target handles on the wire. Pixel/video streaming may be an optional fallback, not the normative scene protocol.
- **Advanced color and HDR presentation**: add production BT.2020/BT.2100 PQ and HLG, extended-linear/EDR, high-bit-depth and floating-point swapchains, per-display luminance/headroom updates, static and future negotiated dynamic metadata, versioned gamut/tone-mapping policies, HDR image/media ingestion, and calibrated output matrices. Keep content encoding independent of output capability so unsupported surfaces receive an explicit deterministic SDR mapping rather than reinterpretation or clipping.
- **Linux X11 compatibility profile**: complete and release the XCB/XInput2/XIM/selection/XDnD/Vulkan-XCB/software-presentation backend independently of the required Wayland profile. It may be developed before 1.0, but an incomplete X11 profile does not delay the first stable release.
- **FreeBSD desktop profile**: a separately versioned, feasibility-gated host backend for FreeBSD windowing, input, IME, accessibility, and presentation. Java 25 and FFM must work on the host before the profile can pass. The backend may share Wayland or X11 protocol work only after a dedicated FreeBSD profile proves the ABI, compositor, and library surface; do not treat `himari-platform-linux-wayland` or `himari-platform-linux-x11` as a FreeBSD implementation. Software rendering and Vulkan are the expected presentation paths. An incomplete FreeBSD profile does not delay the first stable release.
- **OpenBSD desktop profile**: a separately versioned, feasibility-gated host backend for OpenBSD. Expect a more constrained graphics and packaging surface than FreeBSD. X11 is the more likely first host path; do not assume Wayland availability or reuse a Linux module. Software rendering is required; Vulkan is used only where the host stack is actually present and profile-tested. An incomplete OpenBSD profile does not delay the first stable release.
- **OpenHarmony profile**: a separately versioned, feasibility-gated host backend for OpenHarmony window, input, IME, accessibility, and presentation APIs through generated bindings. Do not treat OpenHarmony as Android, Linux Wayland, or commercial HarmonyOS. The profile is gated on a Java 25-capable JVM or AOT toolchain and on documented public system APIs. An incomplete OpenHarmony profile does not delay the first stable release.
- **Media**: add a pure-Java `himari-media` API, WAV/PCM baseline implementations, and optional FFmpeg, GStreamer, or platform-codec providers.
- **Tier-2 complex-script shaping profile**: complete SHAPE-INDIC, SHAPE-USE, Khmer, Myanmar, and Tibetan against the frozen HarfBuzz corpora as a separately versioned profile that upgrades text coverage without changing the shaping contract.
- **Hinting fidelity profile**: complete the TrueType VM (`fpgm`, `prep`, glyph programs, CVT, storage, twilight zone), CFF hinting, and auto-hinting port units with FreeType differential evidence as a separately versioned profile; the unhinted default path remains supported.
- **AWT/Swing embedding interop**: an optional `himari-interop-awt` artifact that presents HimariUI scenes inside existing AWT/Swing applications through the software renderer's `PixelBuffer` output and forwards normalized input and focus. It may depend on `java.desktop`, is excluded from core purity gates through its own allowlist, and must never become a core or BOM-default dependency.

### 3.4 Pre-stable standalone releases

Publish independently useful subsystems as explicitly unstable `0.x` artifacts before the first stable desktop release:

- `himari-font` and `himari-text` once their M4 gates pass: pure-Java OpenType parsing and shaping are independently valuable to the wider Java ecosystem and attract the real-world font and text corpus that differential testing needs most.
- `himari-render-software` plus the graphics value types once the M3 gates pass.
- The ABI generator tooling once M0 proves the generation path.
- `himari-state` and `himari-runtime` once the remaining M1 gates pass under accepted ADR-023: the declarative runtime is the API surface that most needs external feedback, carries no native dependency, and must be exercised by real users before M9 stabilizes the public controls API. The Section 7 API-charter samples ship with it as executable documentation.

Pre-stable artifacts use `0.x` versions, carry an explicit instability notice, and pass the same pure-Java distribution gates as stable artifacts. They create no API-stability obligation and must never delay or veto a first-stable API change; their purpose is early corpus exposure, external users, and contributor acquisition for a multi-year project that would otherwise receive no external feedback before its first stable release.

---

## 4. Architectural Decisions

Create or maintain an ADR for each decision below. An `ADR-NNN` heading in this draft is an identifier, not proof that the decision is accepted; the status in Section 26 and the canonical file under `adr/` are authoritative. `ADR-BOOTSTRAP-001` materialized ADR-001 through ADR-022, and `RUNTIME-ADR-001` subsequently added ADR-023 and accepted ADR-020 and ADR-021 from checked evidence. Every future unresolved entry receives an evidence requirement and decision milestone rather than remaining an undated working default.

### ADR-001: Keep the core independent of `java.desktop`

Define HimariUI image, font, color, input, and window types. Java2D may appear in tests as an Oracle but never in production modules.

### ADR-002: Use FFM as the only desktop FFI and generate typed bindings

For JVM and Native Image desktop targets, use generated, fixed-signature Java calls or `MethodHandle.invokeExact`. Do not define an FFI provider SPI, runtime provider selection, reflection-based calls, or `Object[]` invocation. Future mobile AOT host glue and browser/Wasm host bindings are separate target and packaging boundaries, not alternative implementations of this desktop FFI contract.

### ADR-003: Keep compiler plugins optional

Runtime correctness and the baseline application API must not depend on source rewriting, bytecode rewriting, or generated application code. Optional tooling may provide source maps, stability diagnostics, static dependency analysis, skip optimization, or development-time hot reload. Evaluate all runtime candidates through ordinary Java samples before applying optional tooling; annotation processing alone is not a substitute for method-body instrumentation.

### ADR-004: Track reactive reads by execution phase

Invalidate only the phase consumer that read a reactive value and its required successors:

```text
STRUCTURE -> {MEASURE, PLACE, PAINT, COMPOSITE, SEMANTICS, HIT_TEST_INDEX} as topology or node results require
MEASURE   -> {PLACE, PAINT, COMPOSITE, SEMANTICS, HIT_TEST_INDEX} as geometry changes require
PLACE     -> {PAINT, COMPOSITE, SEMANTICS, HIT_TEST_INDEX} as geometry changes require
PAINT     -> {COMPOSITE}
COMPOSITE
SEMANTICS
HIT_TEST_INDEX
```

`STRUCTURE` denotes the smallest callback that may change mounted-node topology; it does not imply that component functions are always rerun. `COMPOSITE` updates retained-layer properties, damage, and presentation state without rerecording otherwise unchanged drawing commands. Keep structure, measure, placement, paint, composite, semantics, and hit-test consumers independently restartable. A reactive or animated property binding must declare which phases its value can affect instead of mutating a mounted node from an unclassified generic effect. ADR-020 defines the only permitted ordering exception: explicitly scoped measure-time structural materialization for viewport-driven containers.

### ADR-005: Make the software renderer normative

Add each drawing operation to the scalar software renderer and golden corpus first. A GPU implementation becomes eligible as a default only after passing differential gates.

### ADR-006: Use an explicit RHI resource model

Represent resource creation, lifetime, declared usage, pass dependencies, submission order, pass boundaries, and pipeline state explicitly. The frame compiler produces logical access transitions; each backend materializes native barriers or validation as required. Do not expose Vulkan/D3D12 barrier objects or leak OpenGL-style implicit state into the higher-level Canvas API.

### ADR-007: Keep low-level memory and host-interop types out of public APIs

Core and internal implementation modules may use `MemorySegment`, `Arena`, `MemoryLayout`, and other stable Java 25 APIs when ownership and lifetime are explicit. Do not remove or replace those uses solely to preserve possible ART compatibility. Confine `Linker`, `SymbolLookup`, `FunctionDescriptor`, and low-level method handles used for desktop system calls to generated desktop interop or allowlisted `himari-ffi` support packages. An explicit interop escape hatch may expose framework-defined typed native handles, but ordinary component APIs must not expose low-level memory, FFM linker, Android, Objective-C, JavaScript, DOM, WebGPU, or Wasm runtime types.

### ADR-008: Prefer verifiable correctness over porting speed

Require a readable reference implementation, Oracle runner, fixed corpus, and fuzz target before algorithmic rewrites, SIMD, parallelism, or GPU acceleration.

### ADR-009: Lay out in logical pixels

Use `float` logical pixels for layout. Resolve DPI, fractional scaling, pixel snapping, and subpixel placement during layer compilation and rendering. Cumulative scroll offsets and other coordinates that can grow beyond local component bounds must use anchor-relative computation or double-precision accumulation; `float` precision applies to local layout math, never to unbounded running sums.

### ADR-010: Use UTF-16 public indices with explicit grapheme and cluster maps

Keep public ranges compatible with Java `String` offsets. Route caret, selection, and deletion through grapheme boundaries, and preserve UTF-16 cluster start/end offsets in shaping results.

### ADR-011: Exclude preview and incubator types from stable APIs

Keep the Vector API inside optional implementations such as `himari-render-vector` in `modules/render/vector-jdk25`. Do not expose structured concurrency or other preview APIs in stable signatures.

### ADR-012: Report capability loss and fallback explicitly

When GPU, IME, color-font, accessibility, or platform extensions are unavailable, emit a structured capability report and select a documented fallback. Never silently drop input or substitute semantically incorrect behavior.

### ADR-013: Use consistent coordinates, module names, and packages

Use `org.glavo.himari` as the Maven group, `himari-<area>` as artifact IDs, and `org.glavo.himari.<area>` for JPMS modules and exported packages. Keep repository modules fine-grained while presenting coarse user entry points such as `himari-desktop` and `himari-ui`. Do not introduce a `gui-*` prefix.

### ADR-014: Keep host integration asynchronous and target-specific

Platform-neutral contracts must support host-driven event loops, asynchronous capability acquisition, optional rendering workers, and unavailable capabilities. Desktop targets use FFM behind generated native bindings. Future mobile AOT targets use generated target launchers and JNI/NDK or Objective-C/C glue outside the desktop FFM contract. A browser/Wasm target uses generated Wasm imports and JavaScript/browser bindings in a separate host module. None of these boundaries may leak target runtime objects into common public APIs or become a generic provider SPI.

### ADR-015: Separate value reactivity from structural updates

Use a fine-grained producer/consumer graph for `State`, `DerivedState`, phase dependencies, and owned effects. Source writes push invalidation through the graph; derived values recompute lazily when pulled and publish semantic version changes only when their equality policy observes a new value. Treat conditional branches, keyed collections, mounted-node identity, and lifecycle as a separate structural-update problem.

ADR-023 resolves the structural contract from the M1 comparison in favor of explicit grouped recomposition. Handwritten restartable groups own topology and local lifecycle, while typed value and phase consumers retain fine-grained invalidation under this ADR. The one-shot signal and hybrid scope prototypes remain reference evidence. A slot table may be an internal grouped-runtime store, but it is not a fixed public API or a requirement to copy Compose's compiler-dependent representation.

### ADR-016: Do not lower the Java baseline for speculative targets

The shared implementation may use stable Java 25 language and runtime features. Android and iOS become supported only when a mobile AOT toolchain can compile and run the representative core without source rewriting, an older common source set, or replacement of Java 25 APIs solely for target compatibility. The absence of such a toolchain defers the affected mobile target. An ART-compatible execution profile requires a future replacement or supplementary ADR and is not a current design constraint.

### ADR-017: Make scene output transport-ready without making remote rendering core policy

Define a canonical, versioned, pointer-free encoding for immutable scene/display-list data, resources, semantics snapshots, and normalized input. The encoding must survive a process and language boundary, use explicit feature negotiation and resource limits, and remain independent of Java object layout, `MemorySegment` identity, FFM handles, RHI objects, and native GPU commands. Internal implementations may continue to use Java 25 APIs and `MemorySegment`; only the encoded form is constrained. Core modules own deterministic codecs and offline replay, not sockets, TLS, authentication, discovery, congestion control, video codecs, clipboard/file redirection, or session policy. A future remote renderer is a target-specific consumer of this boundary, not a runtime renderer provider SPI.

### ADR-018: Make animation transaction-scoped, interruptible, and phase-aware

Treat each committed state or mounted-property value as an authoritative model target and derive a separate presentation value while animation is active. Propagate immutable animation metadata with the state-publication and `UiCommitTransaction` path; an implicit animation selects metadata from the current transaction rather than writing hidden global state. Sample all due presentation values against one monotonic timestamp and publish them as one internal presentation epoch. Animation sampling must not write application `State`, execute effects, or expose intermediate values.

Every animatable property declares a typed interpolation adapter and its earliest phase impact. Layout-affecting values may drive measure or placement on each presentation epoch; paint-affecting values rerecord only the required display lists; transform, opacity, clip, and other eligible layer properties may update only `COMPOSITE`. A declarative, bounded layer animation may be sampled by the render executor or a future remote client only when it contains no application callback and the UI runtime can reproduce the same presentation value for authoritative hit testing and reconciliation.

On interruption, first sample the running animation at the replacement timestamp. Compatible spring, decay, gesture-handoff, and custom motion models preserve the current presentation value and velocity; models without meaningful velocity must at least preserve the value. Restart, blend, preserve-velocity, and snap behavior must be explicit policies rather than incidental consequences of object replacement. Structural insertion/removal transitions and matched-geometry transitions use stable identity and lifecycle state machines; they are not ordinary scalar property interpolation.

SwiftUI's transaction, animatable-data, stateful custom-animation, spring-retargeting, phase/keyframe, transition, and matched-geometry semantics are primary design references. Do not copy its compiler- and macro-dependent surface API, per-frame content-closure model, or opaque identity behavior into the ordinary-Java API.

### ADR-019: Preserve extended-range color and HDR semantics without making HDR output a first-stable blocker

Model color encoding as structured data rather than a closed list of display names. A `ColorEncoding` combines a color model, primaries, white point, transfer function, numeric range, and scene- or display-referred interpretation. Luminance information such as SDR reference white, diffuse white, content peak, minimum luminance, and mastering/content-light metadata remains explicit and separate from gamut. ICC profiles are content-addressed transform resources, not substitutes for the dynamic-range and presentation contract. Named encodings are conveniences over this model; adding BT.2020, BT.2100 PQ/HLG, extended-linear spaces, ICC-based spaces, or future encodings must not require changing `Color`, display-list command layouts, the backend-neutral scene schema, or the RHI type hierarchy.

Framework color values and canonical scene records preserve finite extended-range components, including values below `0` or above `1`, until an explicit conversion, gamut mapping, tone mapping, or destination-format quantization step. Reject non-finite components at public and codec boundaries. Keep pixel storage format separate from color encoding and alpha interpretation. Blending and filters use an explicitly declared working encoding and operate in linear light by default; premultiplication, interpolation, adaptation, and any exception to linear processing must have specified behavior. The first-stable reference path uses extended-linear floating-point intermediates so an optimized SDR path can be checked for semantic equivalence without defining the architecture around 8-bit sRGB.

Each output surface reports supported format/encoding combinations, precision, color-volume information, current SDR reference white or HDR headroom where available, metadata support, and whether framework, system, or display tone mapping will occur. Capabilities are per surface/display and may change when a window moves, display settings change, or a browser/mobile host reconfigures. Selection returns an explicit effective `PresentationColorConfiguration` and fallback reason; it never silently treats unsupported HDR/WCG content as sRGB. A single `hdrSupported` flag is insufficient.

First stable requires tagged color values, extended-range scene round trips, reference BT.2020/PQ/HLG conversion math, a programmable Headless HDR-capability model, deterministic gamut/tone-mapped SDR fallback, and backend-neutral capability reporting. It does not require a backend to advertise production HDR output. A backend may advertise HDR/EDR only after its format/color-space pairing, display-change handling, tone-mapping ownership, precision, metadata, screenshot/trace behavior, and platform conformance tests pass.

### ADR-020: Make measure-time structural materialization an explicit contract

Viewport-driven containers such as `LazyList` decide which children exist only when constraints and available size are known. ADR-020 and ADR-023 select explicitly scoped current-measure materialization: only a layout container's declared materialization group may reconcile its own semantic-keyed descendants from the current constraints and viewport, and it stages topology, ownership, dependency, and property changes in the current `UiCommitTransaction` before placement. Failure or cooperative cancellation preserves the previously committed viewport, effects mount only after commit, and the general frame order remains unchanged outside this bounded subphase. The comparison retains the previous-viewport one-frame-lag scheme as a rejected baseline, not an implicit overload fallback.

### ADR-021: Contain application callback failures at declared error boundaries

Every rerunnable application callback across structure, measure, placement, paint, effects, input, cleanup, and measure-time materialization executes under ADR-021's explicit boundary chain. The nearest active boundary aborts the complete attempt, cleans staged work child-before-parent, and schedules declared fallback content as a fresh attempt; fallback failure escalates once to the parent, and retry requires an explicit reset or declared recovery action. An uncontained root failure retains the last committed scene and disables only the affected window runtime. Debug and release builds share atomicity, ownership, boundary, cleanup, and retry semantics while differing only in diagnostic detail and framework-owned presentation. Every native callback entry catches `Throwable`; no failure may unwind through an upcall.

### ADR-022: Run UI work on the platform main thread with explicit continuity and scoping rules

For first stable, reactive bindings, structural updates, layout, input, and semantics run on the platform-required main execution context as described in Section 5.3. A dedicated UI thread separate from the platform thread was considered and rejected for its IME, accessibility, and host-reentrancy marshaling cost. This choice carries two explicit obligations. First, modal-loop continuity: scheduled UI work, layout-affecting animation, and frame production must continue from within OS modal loops — Windows move/resize and menu loops, macOS live resize — through timer-driven reentry, and the render executor must keep presenting compositor-eligible animation throughout. Second, explicit scoping: one reactive graph and state-epoch domain per application, one frame scheduler per window, and cross-window invalidation routed through ordinary scheduling rather than shared mutable traversal of another window's trees. Re-evaluate this ADR only with profiling evidence from the M5–M7 conformance runs.

### ADR-023: Use explicit grouped recomposition for structural updates

Use ordinary-Java, handwritten restartable groups for topology and local lifecycle. Structural reads invalidate their owning group, while typed property bindings and phase callbacks remain fine-grained ADR-015 consumers. Positional identity is valid only within an unchanged explicit group; reorderable siblings and reparenting require semantic keys. Each attempt observes one stable state epoch and publishes topology, ownership, dependencies, property targets, effects, and animation metadata through one failure-atomic `UiCommitTransaction`. The selected contract includes ADR-020 scoped current-measure groups and ADR-021 declared error boundaries. The first scheduler is non-preemptive, with optional cooperative cancellation checkpoints but no speculative composition or conflict retry. Candidate storage remains replaceable implementation evidence rather than public API.

---

## 5. Target Architecture

### 5.1 Logical layers

```mermaid
flowchart TD
    App[Application Components] --> API[Public UI API]
    API --> Runtime[Reactive State + Structural Runtime]
    Runtime --> Element[Mounted Element Tree]
    Element --> Layout[Layout Tree]
    Element --> Semantics[Semantics Tree]
    Layout --> Paint[Paint Recording]
    Paint --> DisplayList[Immutable Display Lists]
    DisplayList --> Layer[Retained Layer Tree]
    Layer --> Scene[Immutable SceneSnapshot]
    Scene --> FrameCompiler[Frame Compiler / Render Graph]
    Scene --> SceneCodec[Canonical Scene Codec]
    Semantics --> SceneCodec
    SceneCodec --> Replay[Trace / Offline Replay]
    SceneCodec -. future .-> RemoteWeb[Remote Web Client]
    FrameCompiler --> Software[Pure Java Software Renderer]
    FrameCompiler --> RHI[Explicit GPU RHI]
    RHI --> Vulkan[Vulkan Backend]
    RHI --> D3D12[D3D12 Backend]
    RHI --> Metal[Metal Backend]
    RHI -. future .-> WebGPU[WebGPU Backend]
    Runtime --> Platform[Platform SPI]
    Platform --> Windows[Win32]
    Platform --> Mac[AppKit / Cocoa]
    Platform --> Wayland[Wayland]
    Platform -. future .-> Browser[Local Browser / Wasm]
    Windows --> NativeAccess[Generated Native Bindings]
    Mac --> NativeAccess
    Wayland --> NativeAccess
    Vulkan --> NativeAccess
    D3D12 --> NativeAccess
    Metal --> NativeAccess
    NativeAccess --> FFM[Generated Typed FFM Bindings]
    Browser --> WebHost[Browser Host Bindings]
    WebGPU --> WebHost
    RemoteWeb --> WebHost
    RemoteWeb --> WebGPU
    RemoteWeb --> Canvas[Canvas / Software Presentation]
```

### 5.2 Frame flow

```text
host / OS events
  -> normalized event queue
  -> input routing / gesture / focus / IME
  -> state transaction + optional animation transaction
  -> publish one state epoch
  -> push reactive invalidation
  -> pull derived values at affected consumers
  -> run affected property bindings or structural scopes
  -> commit model targets, topology, and animation metadata atomically
  -> sample due presentation values at one FrameClock timestamp
  -> incremental measure/place
  -> paint invalidation and display-list recording
  -> layer diff + composite-property updates + damage
  -> immutable SceneSnapshot
  -> render mailbox by default, or canonical encoded scene sink
  -> frame compiler / render graph
  -> CPU tiles or GPU command buffers
  -> declared gamut/tone mapping + output color transform
  -> present + timing feedback
```

### 5.3 Execution and threading model

- **Platform/UI execution context**: run window or host events, reactive bindings, structural updates, layout, input, and semantics updates on the main execution context required by the target. Desktop targets use the OS-required UI thread; a browser target uses the browser event loop.
- **Render execution capability**: desktop targets initially use a dedicated render thread that owns the GPU device, queue, and most GPU resources. Platform-neutral contracts must also permit rendering on the UI context or in a Web Worker.
- **Optional worker execution**: use a bounded platform-thread pool for desktop CPU work and virtual threads for blocking desktop I/O where appropriate. A target may provide no workers, limited workers, or browser workers; correctness must not depend on their presence.
- **Host-driven event loop**: platform scheduling must accept callbacks from a host event loop and must not require a blocking message-pump API.
- **Modal-loop continuity**: scheduled UI work, layout-affecting animation, and frame production continue from within OS modal loops through timer-driven reentry per ADR-022; the render executor keeps presenting compositor-eligible animation throughout.
- **Multi-window scoping**: one reactive graph and state-epoch domain per application; one frame scheduler per window; cross-window invalidation is routed through ordinary scheduling and never through shared mutable traversal of another window's trees (ADR-022).
- **No user callbacks in render execution**: never run application callbacks, component code, or state writes from the render executor, whether it is a thread, worker, or same-thread render phase.
- **Bounded compositor animation**: the render executor may sample an immutable framework-defined animation program for eligible retained-layer properties. It must not execute application interpolation code, effects, or state writes. The program uses a clock mapping shared with the UI runtime so authoritative hit testing, traces, and replacement generations can reproduce or supersede its presentation state.
- **Frame handoff**: when UI and rendering execute separately, scene snapshots may use latest-wins replacement while resource creation, upload, destruction, configuration, and correlated semantics updates remain ordered and non-droppable. A same-context implementation preserves the same ordering without requiring a mailbox. The logical contract must not require a shared address space even though the default implementation passes immutable Java objects in-process.
- **Explicit frame ownership**: hand off only immutable values or objects with documented ownership transfer.

Do not parallelize application component, binding, or structural-control callbacks in the first version. ADR-023 selects a non-preemptive grouped structural scheduler with atomic commit and deterministic failure cleanup. It may expose cooperative cancellation checkpoints for bounded staged work because the selected prototype proves their cleanup, but it does not claim speculative execution, preemption, or conflict retry. Require every rerunnable callback to be fast, reentrant, and free of implicit side effects; external work must be registered through owned lifecycle APIs rather than performed as an unmanaged side effect.

---

## 6. Repository and Module Layout

### 6.1 Target directory structure

```text
/
├─ PLANS.md
├─ README.md
├─ CONTRIBUTING.md
├─ ARCHITECTURE.md
├─ CONFORMANCE.md
├─ FUTURE_TARGETS.md
├─ REFERENCES.lock
├─ PROVENANCE.json
├─ DIFFERENCE_POLICY.md
├─ PERF_BUDGETS.toml
├─ PLATFORM_CONFORMANCE.yaml
├─ THIRD_PARTY.md
├─ NOTICE
├─ adr/
├─ build-logic/
│  ├─ pure-java-guard/
│  ├─ abi-codegen/
│  ├─ shader-codegen/
│  └─ benchmark-conventions/
├─ modules/
│  ├─ ui/
│  ├─ runtime/
│  ├─ state/
│  ├─ layout/
│  ├─ input/
│  ├─ semantics/
│  ├─ animation/
│  ├─ graphics/
│  │  ├─ api/
│  │  └─ path/
│  ├─ scene/
│  │  └─ codec/
│  ├─ render/
│  │  ├─ core/
│  │  ├─ software/
│  │  ├─ gpu/
│  │  └─ vector-jdk25/
│  ├─ rhi/
│  │  ├─ api/
│  │  ├─ vulkan/
│  │  ├─ d3d12/
│  │  └─ metal/
│  ├─ text/
│  │  ├─ api/
│  │  ├─ shaper/
│  │  └─ layout/
│  ├─ unicode/
│  │  ├─ api/
│  │  └─ icu4j/
│  ├─ font/
│  │  ├─ sfnt/
│  │  ├─ truetype/
│  │  ├─ cff/
│  │  └─ raster/
│  ├─ image/
│  │  ├─ api/
│  │  └─ codec-png/
│  ├─ platform/
│  │  ├─ api/
│  │  ├─ headless/
│  │  ├─ windows/
│  │  ├─ macos/
│  │  ├─ linux-wayland/
│  │  ├─ linux-x11/
│  │  ├─ freebsd/
│  │  ├─ openbsd/
│  │  └─ openharmony/
│  ├─ ffi/
│  ├─ controls/
│  │  └─ core/
│  ├─ theme/
│  │  └─ default/
│  ├─ inspector/
│  ├─ testing/
│  └─ desktop/
├─ tools/
│  ├─ abi-generator/
│  ├─ shader-compiler/
│  ├─ font-inspector/
│  ├─ scene-replay/
│  └─ golden-reviewer/
├─ oracles/
│  ├─ freetype-runner/
│  ├─ harfbuzz-runner/
│  ├─ sdl-runner/
│  ├─ platform-text-runner/
│  └─ lwjgl-render-runner/
├─ corpus/
│  ├─ fonts/
│  ├─ unicode/
│  ├─ paths/
│  ├─ images/
│  ├─ scenes/
│  └─ events/
└─ samples/
   ├─ hello-window/
   ├─ controls-gallery/
   ├─ text-lab/
   ├─ render-lab/
   └─ native-image-demo/
```

### 6.2 Dependency direction

Allow dependencies only in the following direction:

```text
controls/theme
    -> ui/runtime/layout/input/semantics/animation/text/graphics
runtime
    -> state + platform/api
layout
    -> graphics value types only
text/layout
    -> unicode + text/shaper + font modules
render/core
    -> graphics + text glyph data
scene/codec
    -> render/core + semantics snapshots + normalized input value types
render/software
    -> render/core
render/gpu
    -> render/core + rhi/api
rhi backends
    -> rhi/api + ffi/generated bindings
platform backends
    -> platform/api + ffi/generated bindings
ffi
    -> java.base FFM API only
future platform/web
    -> platform/api + host/web
future rhi/webgpu
    -> rhi/api + host/web
future host/web
    -> generated Wasm imports + JavaScript/browser bindings
future platform/android
    -> platform/api + host/android
future rhi/android-vulkan
    -> rhi/api + host/android
future platform/ios
    -> platform/api + host/ios
future rhi/ios-metal
    -> rhi/api + host/ios
future host/android + host/ios
    -> generated mobile launcher/host glue + target AOT entry points
future remote/server
    -> runtime + scene/codec
future remote/web-client
    -> scene/codec schema + host/web + browser presentation backend
```

Reject all of the following:

- Reverse dependencies such as `runtime -> platform/windows`.
- `text -> java.desktop`.
- `graphics/api -> Vulkan/D3D12/Metal`.
- `controls/core -> theme/default`.
- Platform or RHI modules that bypass generated bindings and construct arbitrary downcalls.
- Core or desktop production dependencies on JNA, LWJGL, GraalVM SVM interop, or `oracles/*`; future mobile toolchain-specific entry points must remain confined to `host/android` or `host/ios`.
- Browser/Wasm modules that depend on `himari-ffi` or expose host runtime objects through common APIs.
- Future mobile modules that leak Android, JNI, Objective-C, UIKit, Metal, or AOT-toolchain types into common public APIs.
- Changes to common modules whose only purpose is ART class-library or bytecode compatibility without a separately accepted ART-target ADR.
- Scene codecs that encode native addresses, Java object identity, `MemorySegment` identity, FFM/host handles, component trees, RHI resources, or native GPU commands.
- Core modules that depend on sockets, HTTP/WebSocket implementations, TLS providers, authentication, discovery, congestion control, video codecs, or remote-session policy.

### 6.3 JPMS rules

- Give every JVM-published artifact an explicit `module-info.java` named `org.glavo.himari.<area>`.
- Request native access only from `org.glavo.himari.ffi` and concrete platform/RHI modules that invoke restricted FFM methods.
- Use `uses`/`provides` for platform, renderer, and feature SPIs. Generate statically analyzable registries for Native Image when required. Do not define an FFI provider SPI.
- Do not export `org.glavo.himari.*.internal`; use narrow SPIs for cross-module internal access instead of `--add-exports`.
- A future Wasm build may link modules statically without runtime JPMS, but it must preserve the same logical dependency and encapsulation boundaries.
- Future mobile AOT builds may statically link modules and generated entry points without runtime JPMS, but they must compile the normal Java 25 source sets and preserve the same logical dependency and encapsulation boundaries.

### 6.4 Naming rules

Repository directories may be short, but Maven artifacts, JPMS modules, and Java packages must use the complete isomorphic naming scheme defined by ADR-013. BUILD-001 must not introduce an alternative convention.

### 6.5 Future local browser/Wasm boundaries

Reserve logical boundaries for `modules/platform/web`, `modules/rhi/webgpu`, and `modules/host/web`. Keep browser host bindings outside `modules/ffi`; they adapt generated Wasm imports and JavaScript/browser APIs rather than native system libraries. Defer public artifact names and the Java-to-Wasm toolchain until the post-stable feasibility milestone.

### 6.6 Future mobile/AOT boundaries

Reserve logical boundaries for `modules/platform/android`, `modules/platform/ios`, `modules/rhi/android-vulkan`, `modules/rhi/ios-metal`, `modules/host/android`, and `modules/host/ios`. Keep target launchers, AOT entry points, and generated JNI/NDK or Objective-C/C glue outside `modules/ffi` and outside all core JARs. These modules adapt platform lifecycle and services to target-neutral contracts; they do not define a runtime-selectable backend or an ART-compatible variant of the core. Defer public artifact names and the mobile AOT toolchain selection until A0 feasibility evidence exists.

### 6.7 Future remote-rendering boundaries

Keep the canonical scene codec in `modules/scene/codec` so Headless replay, inspector tooling, process isolation, and future transports share one conformance surface. Reserve `modules/remote/server`, `modules/remote/web-client`, and target-specific transport adapters for the post-stable remote track. The server adapts the authoritative runtime to encoded scene, resource, semantics, and interaction streams. The Web client adapts that protocol to the browser presentation backend and DOM-based IME/accessibility services. Transport adapters own networking, security, and session policy; the scene codec must not depend on them. A remote client is a build-time product target, not a runtime renderer provider selected from core code.

---

## 7. Declarative Runtime Workstream

### 7.1 Accepted public semantics

The ordinary Java API must express these concepts without generated application code:

```text
component or reactive owner
source state and derived state
phase-aware property binding
conditional structural scope
keyed collection scope
ambient inherited value scope
mounted node declaration
owned effect and cleanup
```

M1 determines the final names, callback shapes, and ownership rules. Do not accept an API merely because optional code generation makes it concise. Samples used for the decision must call the same runtime surface available through standard Java compilation without an annotation processor, compiler plugin, source generator, or bytecode transformer.

The API must preserve the point at which a reactive value is read. An eagerly evaluated argument such as a computed `String` cannot become a property-level binding: a grouped model can attribute it only to the current structural scope, while a one-shot model must reject or diagnose an uncaptured read. A deferred property getter may become a narrower phase consumer. The runtime must not claim property-level invalidation after the application has already erased the dependency by passing an eager value.

Ambient inherited values are part of the public execution model, not a controls-layer convenience. Theme tokens, density, locale, text direction, reduced-motion policy, and similar tree-scoped values must be readable through typed ambient keys, overridable for a subtree, and tracked as ordinary reactive dependencies. Each M1 candidate must demonstrate its ambient representation, and the selected model must define the invalidation scope of an ambient change — which consumers rerun, which phases are marked, and how overrides interact with structural identity — rather than treating context as an untracked global.

### 7.2 M1 structural-reactivity decision

`RUNTIME-ADR-001` compared three bounded prototypes over the same state graph, mounted-node abstraction, and Headless host:

1. **Explicit grouped recomposition**: rerunnable structural callbacks, explicit ordinary-Java group boundaries, positional memory, and keyed reconciliation. The prototype receives no compiler-generated groups, source keys, change masks, restart lambdas, or lambda memoization.
2. **One-shot signal ownership**: initialize each component owner once, bind deferred reactive expressions to typed node properties, and express changing branches and collections through explicit control-flow primitives equivalent to `Show` and `ForEach`. Changing component inputs must remain reactive rather than becoming frozen constructor values.
3. **Hybrid structural scopes**: use fine-grained bindings for values and phase callbacks, but rerun the smallest explicit structural scope for conditional branches, keyed collections, or other topology changes.

The shared behavioral fixtures and instrumentation were frozen before candidate implementation. The spikes do not share a structural abstraction that prejudges the result, and each implements behaviorally identical applications through its own ordinary-Java API variant.

The checked-in comparison suite runs all three prototypes through:

- a counter with a derived label and event handler;
- a diamond dependency graph that would expose an intermediate-value glitch;
- conditional insertion and removal with documented local-state retention and effect disposal;
- nested components whose inputs change after mounting;
- keyed list insertion, deletion, and reorder with item-local state;
- high-frequency text, color, size, and offset changes that exercise different phase impacts;
- cross-scope geometry propagation in which a simulated child measure result feeds a parent placement consumer, standing in for layout integration before M2 exists;
- a controlled text-editing fixture in which an editor-owned buffer synchronizes with application-owned value state through transactional updates that are asynchronously filtered, partially accepted, or rejected, without reentrant writes, lost updates, or composition corruption;
- ambient inherited values (a theme token, density, locale, and text direction) with subtree overrides, dynamic changes, and measured invalidation scope;
- viewport-driven lazy materialization in which available size determines which keyed children exist, exercising the ADR-020 measure-time structural contract as a stand-in for `LazyList` before M2 exists;
- an application callback that throws during structure, measure, placement, and paint, verifying the ADR-021 containment scope, fallback presentation, diagnostics, and cleanup;
- failed staged work with deterministic cleanup and retry, plus cancelled work only for a candidate that claims preemption or cancellation as part of its model.

The checked-in rubric was frozen before candidate implementation. It defines correctness disqualifiers, required diagnostics, measured ergonomics, performance and memory dimensions, the exact normalized score, a three-point tie range, and fixed tie-breakers. The reports record source lines, explicit keys, deferred getters, structural-control primitives, generic type noise, callbacks, nodes, dependency edges, steady-state allocations, retained memory, phase invalidations, trace quality, Native Image compatibility, and development-time reload claims. Performance cannot select a model whose ordinary-Java samples require pervasive accidental ceremony. The rubric also fixes early-disqualification checkpoints so a correctness or ceremony failure is recorded before a realistic-application port consumes more time.

Because micro-fixtures cannot expose ceremony at application scale, each candidate also includes the same settings form plus chat-style keyed-list application in the recorded metrics. These realistic samples remain in the repository as an API charter: later public-API changes must keep them compiling and comparably concise.

The reviewed comparison is complete. All candidates pass the correctness and Native Image gates and none fails the blinded ceremony review. ADR-023 selects explicit grouped recomposition through the frozen score and first tie-breaker while retaining ADR-015 fine-grained value and phase consumers. It defines component input reads, group and local-state ownership, positional and semantic-keyed identity, failure-atomic drafts, the value/structure boundary, ambient invalidation, ADR-020 current-measure materialization, and ADR-021 error containment. The selected candidate's materially higher callback and allocation counts remain explicit optimization targets rather than hidden selection evidence.

### 7.3 Runtime structures

Implement these model-independent structures before committing to a structural representation:

- **ReactiveGraph**: maintain producer/consumer edges, value versions, dynamic dependencies, liveness, and dirty propagation.
- **ReactiveOwner**: own computations, structural scopes, effects, and cleanup independently of any particular slot representation.
- **AmbientScope**: provide typed ambient keys, subtree overrides, and reactive read tracking for inherited values independently of the selected structural representation.
- **MountedElement**: connect the selected declaration or binding model to persistent layout and semantics nodes and hold local invalidation state.
- **StructuralRuntime**: implement the M1-selected branch, collection, identity, and local-state model.
- **NodeApplier**: apply staged structural changes incrementally to mounted, layout, and semantics nodes.
- **PhaseDependencyIndex**: map reactive and presentation versions to structure, measure, placement, paint, composite, semantics, and hit-test consumers.
- **UiCommitTransaction**: stage mounted-property targets, topology changes, and inherited animation metadata and commit atomically; failure must leave no nodes, animation instances, completions, or effects behind. If the selected model supports cancellation, cancellation has the same cleanup guarantee.
- **AnimationRegistry**: own active presentation values, velocities, transition states, completion groups, replacement generations, and next-frame deadlines without making them application state.
- **EffectRegistry**: define deterministic `mount`, `update`, and `dispose` ordering and aggregate failures for reporting.

`STRUCTURE-001` may implement group records, positional memory, semantic-keyed reconciliation, retained branches, and a slot-table-like store. Candidate storage classes remain evidence rather than production API: an implementation may replace them if it preserves ADR-023 identity, ownership, atomicity, diagnostics, and phase-attribution semantics.

### 7.4 State and propagation requirements

Provide object and primitive state types, including `State<T>`, `MutableState<T>`, `IntState`, `LongState`, `FloatState`, and `BooleanState`. Add `DerivedState<T>`, batched `StateTransaction.run(...)`, a safe external-update commit queue for thread or host callbacks, and consistent snapshot/version reads.

The reactive graph must provide:

- dynamic dependency discovery and reconciliation on every consumer execution;
- synchronous dependency capture with an explicit non-tracking read operation; asynchronous continuations must establish their own consumer context;
- eager push propagation of dirtiness without running effects or recomputing derived values;
- lazy pull recomputation of invalidated derived values;
- equality policies and monotonically advancing semantic value versions so unchanged derivations do not wake downstream consumers;
- dependency-version polling before consumer execution so a dirty notification whose inputs are semantically unchanged can be skipped;
- glitch-free reads across diamond and dynamically changing dependency graphs;
- cycle detection with deterministic diagnostics;
- side-effect-free derived computations that cannot write state;
- owner disposal and liveness rules that do not retain unreachable consumers;
- defined caching, propagation, and retry behavior for failed derivations.

`StateTransaction` and `UiCommitTransaction` are distinct. A state transaction atomically publishes source values as one epoch; a UI commit transaction atomically publishes property targets, topology changes, and inherited animation metadata derived from that epoch. Neither observers nor effects may observe an intermediate source combination, a partially updated set of bound properties, a partially applied tree, or only part of an animation group. Running animations publish separate internal presentation epochs sampled at one timestamp; those epochs never rewrite the committed source values.

Enforce these write rules:

1. Coalesce repeated writes to the same source within one transaction or event-loop tick before notifying consumers.
2. Give nested state transactions explicit flattening, rollback, and failure semantics.
3. Publish changes initiated outside the UI execution context through the commit queue; never execute application UI callbacks directly on the originating thread, worker, or host callback.
4. Keep every reactive version read by staged UI work stable for that attempt.
5. Prevent state-epoch interleaving during the selected non-preemptive UI attempt. Cooperative cancellation may stop only at declared checkpoints and must discard the complete draft without committing a partial tree or mixed state epoch; speculative execution, preemption, and conflict retry are outside the first production scheduler.
6. Schedule each affected effect at most once per committed epoch and run it only after affected UI work has committed or established that the epoch requires no UI mutation.
7. Reject reentrant writes and illegal side effects from derived computations or rerunnable structural callbacks in debug mode.

Validate the reactive graph and structural runtime with model-based differential testing in addition to fixed fixtures: run randomized operation sequences — writes, transactions, dependency-shape changes, branch flips, keyed reorders, disposals — against a naive recompute-everything reference evaluator and compare observable values, invalidation sets, effect schedules, and disposal behavior after every step.

### 7.5 Identity and dynamic structure

- Require application keys for semantic identity in reorderable collections or explicit reparenting, not merely to compensate for missing compiler-generated source positions.
- Make branch identity and the retain-versus-dispose policy for inactive local state explicit in the selected structural model.
- Keep the one-shot and hybrid control-flow implementations as reference evidence with stable anchors, deterministic child ownership, and child-before-parent disposal; they are not a second production structural model.
- Under ADR-023 grouped recomposition, derive positional identity only inside explicit runtime groups and require semantic keys wherever sibling execution order may change.
- Record source locations in debug builds when available, but never depend on generated source tokens for correctness.
- Never use stack inspection, line numbers, synthetic lambda class names, or allocation order as correctness-critical identity; they are diagnostic inputs only.
- Diagnose duplicate keys, unkeyed reorder, stale bindings, owner leaks, and local-state type changes with actionable errors.

### 7.6 Phase invalidation

Maintain independent node flags:

```text
NEEDS_STRUCTURE
NEEDS_MEASURE
NEEDS_PLACE
NEEDS_PAINT
NEEDS_COMPOSITE
NEEDS_SEMANTICS
NEEDS_HIT_TEST_INDEX
```

Track reactive and presentation reads in their execution context. Reads from structural, measure, placement, paint, composite, and semantics callbacks register the corresponding consumer. A typed property binding must declare phase-impact metadata; changing text may require measure, paint, and semantics, changing color may require paint, while a retained-layer opacity change may require only composite. Mark the earliest affected phase and its required successors rather than inferring impact from an unclassified setter.

A signal write only marks consumers dirty and requests scheduled work. It must never mutate mounted, layout, semantics, or render nodes directly. A scroll offset should therefore be able to invalidate placement, paint, and hit testing without rerunning an unrelated component or structural scope.

### 7.7 Effects and lifecycle

- Require component initialization and every rerunnable binding or structural callback to be externally side-effect free.
- Attach effects and resources to a `ReactiveOwner`; disposing the owner must sever graph edges and release all registered work.
- Start committed effects from parent to child and dispose them from child to parent.
- Dispose the old effect before starting a replacement when its identity changes.
- Coalesce effect scheduling per committed epoch and run effects only after derived values and affected staged UI work are consistent; an epoch with no UI mutation still crosses the same stabilization barrier.
- Catch effect failures at the runtime error boundary; never let them escape through a native callback.
- Register timers, subscriptions, and resources as effects. Do not rely on finalization.

---

## 8. Layout Workstream

### 8.1 Layout protocol

Implement a constraints-down, sizes-up protocol with a separate placement phase. By default, permit each child to be measured once per layout pass. Make intrinsic measurement explicit, cache it, and identify it as expensive in profiling data.

Treat measurement and placement as separate reactive consumers. Make baselines and alignment lines first-class results. Use `float` logical pixels throughout layout; packed integer-range optimizations may remain internal.

### 8.2 Primitive implementation order

Implement layout primitives in this order:

1. `Box` and `Stack`.
2. `Row` and `Column`.
3. Padding, size, min/max, aspect ratio, and alignment.
4. `ScrollViewport`.
5. `LazyList` and `LazyGrid`.
6. `Flex`.
7. `Grid`.
8. Flow/wrap.
9. Custom layout.
10. Overlay, popup, and portal placement.

### 8.3 Modifier and behavior chain

Use immutable, typed modifier chains and flatten them when mounted. Each modifier must declare the phases it participates in so an input or semantics modifier does not automatically invalidate layout and paint.

### 8.4 Virtualization requirements

`LazyList` and related primitives must provide stable item keys, viewport-based materialization, overscan/prefetch, variable-height estimation and correction, anchor-preserving updates, internal-node reuse without state-identity leakage, logical accessibility information for unmounted items, and deterministic Headless scrolling tests. Viewport-based materialization uses ADR-020's explicitly scoped current-measure group and commits current-viewport keyed descendants before placement. Anchor-preserving updates and cumulative scroll offsets follow the ADR-009 precision rule for unbounded running sums.

### 8.5 Hit testing

- Generate a spatial index from the layout tree.
- Include transforms, clips, z-order, and pointer behavior in hit testing.
- Update the index only when position, transform, or clip state changes.
- Make exact path hit testing an explicit cost; use bounds plus a shape policy by default.

---

## 9. Graphics and Display-List Workstream

### 9.1 Framework-owned value types

Implement framework types for points, sizes, rectangles, rounded rectangles, matrices, colors, color primaries, white points, transfer functions, structured color encodings, ICC profile resources, luminance/content-light metadata, presentation color configurations, gamut/tone-mapping policies, paths, brushes, strokes, paints, blend modes, images, pixel buffers, text blobs, glyph-run lists, and filters. Do not reuse `java.awt.*` types or expose backend color-space enums.

Keep `Color`, `PixelFormat`, `AlphaType`, and `ColorEncoding` independent. `Color` stores finite floating-point components plus an encoding; it does not clamp extended-range components on construction. `PixelBuffer` declares component layout/type/precision, alpha interpretation, row/plane layout, and encoding separately. Named spaces such as sRGB, linear-sRGB, Display-P3, linear Display-P3, BT.2020, BT.2100 PQ/HLG, and extended-linear encodings are canonical predefined values over the same extensible representation rather than Java enum exhaustiveness assumptions.

### 9.2 Recording Canvas

The Canvas records immutable drawing commands; it never renders directly to a GPU. Support save/restore, transforms, rectangle/path clips, rectangles, rounded rectangles, paths, images, glyph runs, and save-layer operations.

### 9.3 Display-list format

Use a compact primitive-buffer format that can be scanned sequentially:

```text
Header:
  magic:u32
  majorVersion:u16
  minorVersion:u16
  requiredFeatures:u64
  bounds
  opCount
  resourceCount

Operations:
  opcode:u16
  flags:u16
  payloadLength:u32
  payload:aligned bytes

Side tables:
  immutable paths
  content-addressed images
  color encodings + content-addressed ICC profiles
  pre-shaped text blobs and glyph resources
  filters
  nested display lists
```

Use a canonical little-endian encoding independent of Java object layout and native byte order. Require reusable builders that freeze on completion, no per-command Java object allocation, hashing and serialization, trace/replay support, conservative bounds for culling and damage, debug source/resource labels, and explicit format versioning. Resource references use stable IDs scoped by a manifest plus content hashes; they never use pointers or object identity. Text blobs carry authoritative glyph IDs, positions, clusters, and required glyph data rather than asking a consumer to reshape with ambient system fonts. Color payloads use finite floating-point components and stable encoding/profile references; the format must not assume 8-bit sRGB, `[0, 1]` component bounds, a fixed set of gamuts, or that source encoding equals presentation encoding. Reject unknown required features, non-finite values, oversized profiles/LUTs, and malformed payloads. Do not promise permanent compatibility across major versions.

### 9.4 Retained layer tree

Support transform, clip, opacity, picture, texture, backdrop-filter, explicit interop surface, and repaint-boundary layers. Implement subtree diffing, display-list reuse, raster caching, dirty regions, occlusion culling, offscreen-pass merging, opacity folding, and clip simplification.

### 9.5 Transport-ready scene envelope

Encode an immutable `SceneEnvelope` that can feed offline replay, another local process, or a future remote client:

```text
protocol/version/features
streamEpoch + snapshotId + optional baseSnapshotId
resourceGeneration + optional baseResourceGeneration
viewport + scale + requested/effective presentation color configuration
declared compositing + content encodings + reference white/luminance/content-light metadata
layer snapshot or delta + display-list references + damage
resource manifest + ordered add/release records + content hashes
correlated semantics snapshot/delta identifier
frame timing metadata and diagnostics
```

First stable delivers this envelope as a deterministic codec and ordered file/process stream for traces, offline replay, and process isolation. Every stream begins with a full snapshot and complete resource generation. A delta is valid only when its exact base snapshot and base resource generation were accepted earlier in the same ordered stream or are supplied in an explicit base bundle. Resource add/release records advance logical decoder state; a release forbids later references in that stream but does not imply a live acknowledgement or producer-side reclamation handshake. The in-process scene mailbox may still replace obsolete immutable frames latest-wins, but an encoded stream must retain every base, resource, color/presentation configuration, and semantics record needed by each retained frame. ICC profiles and versioned mapping parameters are content-addressed resources, while live display capability/headroom remains presentation configuration rather than scene content identity. The producing runtime remains authoritative for layout, text shaping, hit testing, focus, application state, animation targets, and any host-side color mapping.

Live-session semantics are deliberately not part of the first-stable codec. Acknowledgement flows, backpressure, capability-negotiation sequences, resource-reclamation handshakes, transmitted layer-animation program encodings, and reversible client-side prediction are session protocol, and R0 specifies and freezes them. The first-stable format reserves the required extension points — including the animation-program field and negotiation-sensitive feature bits — so R0 extends the encoding without invalidating recorded traces.

The encoded envelope may be produced from `MemorySegment`-backed internal storage, but it must contain no address, arena lifetime, Java reference, FFM handle, RHI object, or backend command. Keep transport framing, compression, encryption, authentication, and session policy outside this format.

---

## 10. Software Renderer Workstream

### 10.1 Responsibilities

The software renderer must serve as:

1. The production fallback when GPU rendering is unavailable.
2. The normative definition of drawing semantics.
3. The Headless golden-test backend.
4. The Oracle used to minimize and diagnose GPU differences.

### 10.2 Pipeline

```text
geometry normalization
  -> curve flattening / stroke expansion
  -> primitive binning
  -> tile scheduling
  -> coverage rasterization
  -> brush sampling
  -> conversion to declared linear working encoding
  -> blending / filters in declared working encoding
  -> gamut mapping + tone mapping when required
  -> output color transform + transfer function
  -> pixel output
```

### 10.3 Scalar reference implementation

Build the first implementation as a single-threaded readable scalar path. Include fixed-point edge coverage, non-zero/even-odd filling, quadratic/cubic/conic curves, cap/join/miter/dash stroke semantics, explicitly encoded extended-range floating-point color, linear-light premultiplied alpha, common Porter-Duff and blend modes, nearest/bilinear sampling, solid and linear/radial/sweep gradients, grayscale glyph masks, clip stacks, basic blur, color matrices, the `COLOR-SDR-REF-001` conversions/adaptation, and its deterministic versioned SDR output mapping. Preserve finite negative and above-one intermediate components; clamp only where the selected mapping or destination numeric format requires it. M10 adds the advanced BT.2020/PQ/HLG, ICC, and color-volume mapping implementations behind the same contracts through `COLOR-REF-001`.

Avoid opaque bit-level micro-optimizations in this path. It must remain debuggable and suitable for field-by-field differential comparison.

### 10.4 Optimized implementation

After the scalar path is stable, add 32x32 or 64x64 tiles, primitive-bounds binning, work-stealing execution, structure-of-arrays edge data, allocation-free hot loops, optional `himari-render-vector` acceleration, scalar/vector differential mode, cache-aware samplers, and tiled large filters with halo management.

The Vector module must be removable without changing functionality or correctness.

### 10.5 Path-rendering progression

- **PATH-REFERENCE**: pure-Java scanline coverage as the specification.
- **PATH-TESSELLATED**: pure-Java tessellation with GPU triangle/stencil rendering.
- **PATH-ANALYTIC**: analytic edge antialiasing and GPU tile/path rendering.
- **PATH-COMPUTE**: compute acceleration for large paths, complex clips, and blur.

Any referenced or ported tessellator must retain upstream mapping, licensing, and a differential corpus.

### 10.6 Software presentation

- Use `wl_shm` buffers on Wayland.
- Use a DIB or shared upload surface on Windows without Java2D.
- Upload CPU buffers to Metal textures or use an appropriate public system bitmap/display API on macOS.
- Emit `PixelBuffer` values or PNG files directly in Headless mode.
- Provide Headless extended-linear `RGBA16F` and `RGBA32F` capture surfaces plus a separately configured deterministic SDR mapping for PNG output; a PNG golden must not become the semantic definition of HDR values.

---

## 11. GPU and RHI Workstream

### 11.1 RHI object model

Define explicit device, queue, buffer, texture, texture-view, sampler, shader-module, pipeline-layout, graphics-pipeline, compute-pipeline, command-buffer, render-pass, compute-pass, transfer-pass, submission-completion token/timeline, swapchain/surface, resource-usage/access, pass-dependency, submission-order, and debug-label abstractions. Keep raw texture/pixel format separate from framework color encoding and presentation configuration. Device and surface acquisition, capability queries, and reconfiguration must support asynchronous completion.

Every surface exposes `SurfaceColorCapabilities`: supported format/encoding/alpha combinations; integer and floating-point precision; SDR, wide-gamut, extended-linear, PQ, and HLG presentation modes where actually available; current/maximum luminance or relative headroom when the host reports it; supported static or future metadata classes; system color-management and tone-mapping behavior; and a capability generation that changes with the display or host configuration. Surface selection returns an effective `PresentationColorConfiguration`, mapping ownership, and fallback reason. Keep backend constants such as `VkColorSpaceKHR`, `DXGI_COLOR_SPACE_TYPE`, and `CGColorSpaceRef` behind target modules.

### 11.2 Capability tiers

Use stable capability tiers instead of scattered platform checks:

- **Tier S**: software rendering with complete semantics and limited performance.
- **Tier G0**: render passes, textures, uniform/storage buffers, stencil, and MSAA.
- **Tier G1**: compute, storage images, timestamp queries, and pipeline caches.
- **Tier G2**: optional advanced blend, descriptor indexing, and modern synchronization features.

The frame compiler selects algorithms from capabilities, never from checks such as `isVulkan()`.

Treat presentation color as a surface capability axis rather than a GPU tier: an otherwise G2 device may expose only SDR on a particular surface, while a G0 device/surface pair may support a high-precision extended-linear target.

### 11.3 Frame compiler responsibilities

Implement culling, clip-strategy selection, save-layer/offscreen planning, transient-texture lifetime, draw batching, pipeline keys, upload coalescing, glyph/image-atlas updates, logical resource transitions, GPU-resource retirement, and damage-aware presentation. Track each imported texture's encoding and alpha interpretation, choose adequate intermediate precision, and insert explicit conversion, gamut mapping, tone mapping, output-transfer, and quantization passes only at declared boundaries. Include the effective presentation configuration and mapping algorithm/version in cache and replay keys. Emit a backend-neutral render graph with declared resource access and pass dependencies; each backend derives its required barriers, validation, and submission commands.

### 11.4 Shader toolchain

1. Define a typed pure-Java shader IR without SPIR-V-, HLSL-, MSL-, or WGSL-specific assumptions in its common model.
2. Maintain a small fixed shader set for built-in brushes, clips, glyphs, images, filters, color conversion, chromatic adaptation, and versioned gamut/tone mapping.
3. Generate SPIR-V in Java.
4. Generate HLSL and MSL source in Java.
5. Preserve WGSL as a first-class future target for the WebGPU backend.
6. Use official platform tools in release CI to produce DXIL/DXBC and metallib data artifacts.
7. Generate a common reflection manifest.
8. Load shaders and create pipelines during initialization; never compile in the frame path.
9. Permit an initialization-time MSL source fallback with system compilation and caching.
10. Record source hashes, tool versions, targets, and bindings for every shader binary.
11. Keep external compilers in build/verification tooling, not GUI runtime dependencies.

Do not expose arbitrary user shaders in the first stable release.

### 11.5 Backend deliverables

#### Vulkan

- Generate constants, structures, functions, and extension metadata from the official registry XML.
- Resolve functions through `vkGetInstanceProcAddr` and `vkGetDeviceProcAddr`.
- Select broadly available modern capabilities through runtime capability checks rather than hard-coded version assumptions.
- Enumerate surface format/color-space pairs and map extended color-space and HDR-metadata extensions when present; report unsupported combinations rather than manufacturing HDR capability.
- Use validation layers only in tests/development and never bundle them.
- Keep Wayland and X11 WSI adapters separate.

#### D3D12

- Generate Win32 typedefs, structures, enums, GUIDs, and COM vtable bindings.
- Create devices through `D3D12CreateDevice`, DXGI factories, and swapchains.
- Map DXGI Advanced Color output descriptions, supported swapchain format/color-space pairs, luminance data, color-space selection, and metadata/tone-mapping ownership into the common surface contract.
- Use typed COM handles with explicit `AddRef`/`Release` ownership.
- Manage command allocators/lists, descriptor heaps, barriers, and fences explicitly.
- Load offline-generated DXIL/DXBC resources.

#### Metal

- Resolve classes and selectors through the Objective-C runtime and generate typed `objc_msgSend` downcalls per signature.
- Create delegate/callback classes with `objc_allocateClassPair`, `class_addMethod`, and FFM upcalls.
- Use AppKit windows, `CAMetalLayer`, and Metal devices/queues.
- Map `CAMetalLayer` pixel format/color space, EDR enablement and metadata, per-display headroom, and display-change notifications into the common surface contract without exposing Core Graphics color-space objects.
- Manage autorelease pools on every thread entering Cocoa.
- Prefer delegates, selectors, and `dispatch_*_f`; require a separate ABI feasibility spike before using Objective-C blocks.

### 11.6 GPU resource lifetime

- Create and destroy GPU resources in the render execution context that owns the device, or enqueue those operations there.
- Treat `close()` as logical release and delay backend destruction until the relevant submission-completion point is reached.
- Use `Cleaner` only for leak reporting, never for correct release.
- Emit structured device-lost events and retain source descriptors/upload sources for rebuildable resources.
- Attach debug labels, optional sampled creation stacks, and memory estimates to resources.

---

## 12. Desktop FFM, Host Interop, ABI, and Binding Generation

### 12.1 Desktop binding architecture

Do not implement a generic reflective `invoke` abstraction. It would introduce boxing in hot paths, defer signature failures to runtime, weaken Native Image analysis, complicate callbacks and value-struct ABIs, and make layout mistakes difficult to detect.

Use one fixed generation path:

```text
Canonical ABI schema
  -> generated typed FFM bindings
  -> platform/RHI implementation
  -> system library
```

There is no runtime FFI provider selection. Platform-neutral modules depend on narrow HimariUI interfaces; concrete JVM and Native Image desktop platform/RHI modules depend on the relevant generated bindings.

### 12.2 Canonical ABI schema

The schema must represent:

- Primitive widths and signedness.
- Pointers and handles.
- Structures, unions, bitfields, alignment, and packing.
- Enums and flags.
- Functions and calling conventions.
- Callbacks and function pointers.
- Variadic boundaries.
- `errno` and `GetLastError` policies.
- COM interface inheritance and vtables.
- Objective-C classes, selectors, and signatures.
- Ownership, nullability, thread restrictions, availability, and platform versions.

Generate schema input from the Vulkan registry XML, Wayland protocol XML, Windows SDK metadata or curated definitions, and legally usable Apple SDK declarations plus reviewed descriptors. Maintain ownership and threading metadata explicitly where source formats do not provide it.

### 12.3 Generated outputs

For each system API, generate artifacts equivalent to:

```text
FooApi.java                         # concrete internal typed facade
FooTypes.java                       # records, enums, typed opaque handles
FooLayouts.java                     # canonical layouts and assertions
FooFfmBindings.java                 # FFM downcalls and upcalls
FooAbiTests.java                    # layout and signature tests
foo-reachability-metadata.json
foo-provenance.json
```

Concrete platform code may depend on `FooApi` and `FooTypes`. `FooApi` is an internal concrete facade, not a provider-neutral SPI. Do not propagate low-level FFM types into platform-neutral modules or public APIs.

### 12.4 FFM implementation rules

- Use `Linker.nativeLinker()` and `SymbolLookup.libraryLookup` through generated support.
- Generate `FunctionDescriptor` values as `static final` constants.
- Cache downcall handles statically or by extension function pointer.
- Invoke fixed signatures with `invokeExact`.
- Generate all upcall stubs.
- Give every arena an explicit lifetime.
- Immediately associate returned pointers with a known size or keep them as opaque typed handles.
- Capture `errno` or `GetLastError` immediately after the native call.
- Generate launcher documentation for the required `--enable-native-access=org.glavo.himari.ffi,org.glavo.himari.platform...` options.

### 12.5 Desktop Native Image path

- Reuse the JVM FFM bindings unchanged.
- Generate downcall/upcall registration and reachability metadata.
- Avoid runtime creation of signatures unknown at build time.
- Generate statically analyzable registries for platform and renderer SPIs when required.
- Build and execute a Native Image smoke test on each supported desktop platform.

Do not generate `@CFunction`, `CFunctionPointer`, or GraalVM native-interop views as a second system-call implementation. The JVM and Native Image paths must run the same binding and ABI test suites.

If a future C/C++ host needs to create a Java isolate and call HimariUI through the Native Image C API, place that reverse-direction capability in a separate embedding extension. It must not implement or replace the framework FFI path.

### 12.6 Future local browser/Wasm host-binding architecture

Use a separate generated host-binding path:

```text
Browser host schema
  -> generated Java-facing host stubs
  -> Wasm imports + minimal JavaScript glue
  -> browser APIs
```

- Keep the browser host schema and generated stubs in `host/web`, outside `himari-ffi`.
- Represent asynchronous browser operations as delayed completion in platform-internal contracts; do not block the browser event loop.
- Keep JavaScript objects, DOM nodes, WebGPU objects, and Wasm runtime handles opaque and internal.
- Normalize host callbacks into the same event, state-transaction, and error-reporting paths used by desktop backends.
- Keep JavaScript glue generated or narrowly reviewed, versioned, and free of application policy.
- Support static linking and closed-world analysis; do not rely on runtime classpath discovery.
- Treat this as a target-specific platform boundary, not a second FFI implementation or generic provider SPI.

### 12.7 Boundary for non-FFM interop libraries

- Desktop production modules must not depend on JNA, LWJGL, or GraalVM SVM interop APIs. Browser/Wasm modules use only the isolated `host/web` boundary.
- Oracle runners, ABI probes, and tests may use JNA or LWJGL.
- Confine those dependencies to `oracles/`, `testRuntimeClasspath`, or explicitly isolated development-tool configurations.
- Verify that standard samples and published runtime graphs contain none of them.

### 12.8 ABI verification

Compile non-published C/C++ probes in platform CI and compare machine-readable JSON results with generated Java layouts. Cover `sizeof`, `alignof`, `offsetof`, enum/constant values, function-pointer calls, callback round trips, variadic calls, structure returns, COM vtable indices, Objective-C method encodings, pointer width, and endianness.

Require explicit review for every difference introduced by an SDK upgrade.

### 12.9 Desktop native callback safety

- Catch `Throwable` at every callback boundary.
- Publish failures to a lock-free error queue; never unwind through native code.
- Tie callback-object lifetime to upcall-stub lifetime.
- Copy or enqueue events from OS callback threads; never run user components there.
- Guard callbacks that may reenter the UI loop.
- Let the `himari-ffi` support layer manage required JVM/Native Image attachment and upcall lifetime.

### 12.10 Future mobile AOT host-binding architecture

Treat mobile AOT as a separate packaging and host-integration direction:

```text
unchanged Java 25 HimariUI modules
  -> Java 25-capable mobile AOT toolchain
  -> generated target launcher and host bridge
  -> Android framework/NDK or iOS/UIKit/Metal
```

- Test Java 25 language and class-library coverage separately from native host-call support. Successful `MemorySegment` and `Arena` use in the core does not imply that mobile FFM downcalls or upcalls are available.
- Keep Android Activity, JNI, `ANativeWindow`, and Vulkan bridge types inside `host/android`; keep Objective-C runtime, UIKit, `CAMetalLayer`, and Metal bridge types inside `host/ios`.
- Permit a thin Android Java host to run on ART and load the target-generated AOT application library. Do not require HimariUI runtime, layout, text, or rendering bytecode to execute on ART.
- Generate and statically validate boundary signatures, callback lifetimes, ownership, exception containment, and thread attachment. Do not create a generic native invocation API or runtime FFI provider.
- Keep mobile host glue and AOT output out of `himari-ffi`, core JARs, desktop artifacts, and the desktop system-call test matrix.
- If the candidate toolchain cannot compile the representative Java 25 core unchanged, record the failure and defer that target. Do not introduce compatibility substitutions into common modules as part of the spike.

---

## 13. Text and Font Workstream

### 13.1 End-to-end pipeline

```text
styled UTF-16 text
  -> paragraph segmentation
  -> bidi resolution
  -> script/language segmentation
  -> font-fallback segmentation
  -> OpenType shaping
  -> glyph runs + clusters
  -> line-break opportunities
  -> line fitting / hyphenation / justification
  -> line boxes + caret map
  -> glyph raster/vector/color paint
```

### 13.2 Unicode layer

Define `org.glavo.himari.unicode` in `modules/unicode/api` for Bidi, grapheme/word/sentence/line boundaries, script properties, normalization, locale/language tags, and Unicode-property lookup.

Use `himari-unicode-icu4j` as the default provider. Keep ICU types behind the SPI so future builds may use trimmed data or specialized implementations.

### 13.3 Font data model

Implement these internal concepts:

- **FontData**: immutable bytes or a read-only segment.
- **SfntDirectory**: validated table index.
- **FontFace**: immutable, thread-safe face with lazy table caches.
- **FontInstance**: face, size, variation coordinates, features, and synthesis.
- **GlyphCacheKey**: face, variation, ppem, hinting mode, and subpixel phase.
- **FontSource**: bundled assets, application-provided bytes, fetched resources, or an optional platform catalog, with asynchronous loading where the host requires it.
- **FontCollection**: available font sources, resolved faces, and fallback policy; it must not assume that a system-font catalog exists.

The public font API must expose metadata, code-point-to-glyph mapping, metrics, and outline loading without leaking parser storage.

### 13.4 OpenType implementation order

Implement and independently test tables in this order:

1. SFNT/TTC directories and checked big-endian readers.
2. `head`, `maxp`, `hhea`, `hmtx`, `vhea`, `vmtx`, `OS/2`, `name`, and `post`.
3. Common `cmap` formats.
4. `loca`/`glyf`, including simple and composite glyphs.
5. Legacy `kern` compatibility.
6. A general GDEF/GSUB/GPOS lookup engine.
7. `fvar`, `avar`, `gvar`, HVAR, VVAR, and MVAR.
8. CFF/CFF2 Type 2 charstrings.
9. COLR/CPAL v0.
10. COLR v1.
11. CBDT/CBLC and sbix.
12. Optional SVG glyphs.
13. WOFF.
14. WOFF2 with pure-Java Brotli.

Give every table parser its own valid-input tests, malformed corpus, and upstream comparison.

### 13.5 Shaping engine

Design a Java-oriented `TextShaper` contract rather than copying the HarfBuzz public API. Keep Unicode buffers and clusters, direction/script/language, feature collection, normalization, nominal glyph mapping, GSUB, positioning, mark/cursive attachment, script-specific reordering, cluster preservation, and unsafe-to-break flags as separately testable stages.

Implement script support in this order:

1. Default, Latin, Greek, and Cyrillic.
2. Arabic.
3. Hebrew.
4. Hangul.
5. Thai and Lao.
6. Indic.
7. Universal Shaping Engine scripts.
8. Khmer.
9. Myanmar.
10. Tibetan.
11. Remaining upstream shapers.

Scripts 1–5 are the Tier-1 first-stable blocking set; Han and Kana require no complex shaper and are covered by the default path. Scripts 6–11 form the separately versioned Tier-2 shaping profile defined in Section 3, developed against the same frozen corpora without blocking the first stable release.

Convert HarfBuzz-generated tables with tools. Do not manually transcribe them with AI.

### 13.6 FreeType capability ports

Split the work into independently accepted units for the TrueType outline loader, composite transforms, TrueType VM (`fpgm`, `prep`, glyph programs, CVT, storage, twilight zone), fixed-point arithmetic, CFF/CFF2 charstrings, CFF hinting, outline decomposition, mono/grayscale/LCD coverage, auto-hinting, bitmap strikes, and color glyphs.

The TrueType VM, CFF hinting, and auto-hinting units belong to the optional, separately versioned hinting fidelity profile defined in Section 3 and do not block first stable. The first-stable default renders unhinted outlines with gamma-correct grayscale antialiasing, may apply vertical-only grid fitting, and uses embedded bitmap strikes where fonts provide them; `gasp`-style rendering hints are honored where meaningful without bytecode execution. The FreeType differential corpus for unhinted outlines and rasterization gates first stable; the hinting corpus gates the fidelity profile.

Keep a faithful reference path that follows FreeType stages and numeric formats where practical. Do not replace it with the shared optimized GUI path renderer unless a differential corpus proves equivalence.

### 13.7 Paragraph layout

Produce immutable paragraph layouts containing line boxes, visual/logical run order, baseline/ascent/descent/leading, caret stops, selection rectangles, hit testing, ellipsis, alignment, justification, tabs, soft-hyphen behavior, and locale tailoring. Treat vertical text as a later capability.

Use ICU/Unicode for break opportunities and glyph advances plus available width for line fitting. Keep Bidi, line breaking, and shaping as separate testable stages.

### 13.8 Font fallback and system discovery

Do not call DirectWrite, CoreText, or Pango for production shaping or rasterization. Platform APIs may enumerate and locate system font files or public descriptors when that capability exists.

- On Windows, use font directories, registry data, and public enumeration information.
- On macOS, use CoreText/AppKit only to enumerate or locate files/descriptors.
- On Linux, parse XDG/fontconfig configuration in a pure-Java catalog; a system fontconfig adapter may be transitional or optional.
- In a browser/Wasm target, default to bundled, application-provided, or fetched font bytes. The backend may report system-font discovery as unavailable rather than substituting browser text rasterization.
- Give application fonts priority over system fonts.
- Cache fallback by Unicode block, script, locale, and emoji preference.
- Diagnose missing glyphs and prevent recursive fallback loops.

### 13.9 Glyph caching

- Rasterize small glyphs on the CPU into atlases by default.
- Draw large or heavily transformed glyphs as vectors when appropriate.
- Partition atlases by format, font instance, and subpixel phase.
- Keep GPU atlases separate from CPU mask caches.
- Apply explicit memory budgets and fence-aware eviction.
- Enable LCD subpixel rendering only when pixel layout, opacity, transform, and platform policy permit it; otherwise use grayscale.

---

## 14. Platform Workstream

### 14.1 Platform SPI

Define a platform backend contract for capabilities, host-driven event scheduling, surface/window creation, clipboard, cursors, font sources, and accessibility. Backend initialization, GPU/surface acquisition, clipboard access, permission-gated operations, and resource loading must be able to complete asynchronously. A platform window or surface must expose logical/physical size, scale factor, visibility/title/state where supported, a target-neutral surface descriptor, redraw requests, cursor/IME/drag-and-drop controls, frame-timing callbacks, current display/presentation color capabilities and generation, color-capability change notifications, and explicit close. Moving a window between displays or changing host HDR/color settings must re-negotiate presentation without changing application color values or silently reinterpreting scene content.

Platform backends must distinguish toplevel and popup surface roles. A popup surface provides owner-relative positioning, activation/dismissal and grab semantics where the host defines them, and truthful capability reporting; where a host provides no popup surface, the platform reports the capability as unavailable so higher layers select the in-window overlay fallback.

Do not expose `HWND`, `NSWindow*`, `wl_surface*`, DOM nodes, JavaScript objects, or WebGPU handles from core APIs. Make typed target handles available only through explicit interop modules.

### 14.2 Headless

Treat Headless as a first-class platform, not a testing shortcut. Implement a deterministic clock, virtual displays and scale factors, programmable display primaries/luminance/reference-white/headroom and presentation modes, configurable software surface formats/encodings, programmable event injection, extended-linear and mapped-SDR frame capture, accessibility-tree capture, zero OS-library loading, and complete component/layout/text execution without a display server.

### 14.3 Linux Wayland

Implement:

- `libwayland-client` as the system transport.
- Generated typed proxies and event bindings from Wayland XML.
- xdg-shell, presentation timing/frame callbacks, pointer/keyboard/touch/tablet protocols, data-device clipboard/drag-and-drop, text-input-v3, fractional-scale/viewporter, `wl_shm`, Vulkan WSI, and negotiated color-management/output-description protocols when available.
- `xdg-decoration` negotiation plus complete client-side decorations — titlebar, window controls, and interactive move/resize regions — for compositors that provide no server-side decorations. M5 implements decorations with the private platform-neutral `UI-BOOTSTRAP-001` primitives so it does not depend on M9; the public controls/theme layer may later reuse the same behavior and tokens.
- `xdg_popup` surfaces with positioner and grab/dismiss semantics for menus, tooltips, and dropdowns.
- `cursor-shape-v1` where available, plus a pure-Java XCursor theme parser fallback for compositors without it.

For keyboard handling, consume compositor-provided XKB keymaps, implement a pure-Java parser/state machine, and differentially test it against `libxkbcommon`. A system xkbcommon adapter may be transitional but must not remain the only implementation.

Prefer Wayland text-input for IME. Add pure-Java D-Bus adapters for IBus/Fcitx where required. Normalize composition text, selection, surrounding text, and delete-surrounding operations through `TextInputSession`.

Expose the semantics tree to Linux assistive technology through an AT-SPI2 bridge built on a pure-Java D-Bus client; the same D-Bus client serves the IBus/Fcitx input-method adapters.

### 14.4 Linux X11

Implement X11 as an optional, separately versioned compatibility profile that does not block the first stable Wayland release. Prefer XCB to Xlib. Cover XKB, XInput2, XIM, selection/clipboard, XDnD, Vulkan XCB surfaces, and software-image upload. Confine X11-specific behavior to the backend and publish the profile only after its own compatibility gates pass.

### 14.5 Windows

Implement:

- User32 window classes, generated `WndProc` upcalls, message pumps, per-monitor DPI, DWM/window state, multiple windows, modal loops, clipboard, and OLE drag-and-drop.
- Unified mouse/touch/pen input through `WM_POINTER`, raw keyboard plus logical mapping, separate key and text events, TSF as the target IME, IMM32 as a transitional fallback, pointer capture, cursors, and high-resolution wheels.
- DXGI plus D3D12, frame-latency waitable objects/present feedback, Advanced Color/output capability and change detection, explicit swapchain color-space selection, and software-buffer upload fallback.
- A UI Automation provider whose COM vtables/callbacks are generated and whose TextPattern/TextRange implementation maps to the HimariUI text model.

### 14.6 macOS

Implement:

- Objective-C runtime access, NSApplication/NSWindow/NSView, dynamically registered delegate classes, main-thread enforcement, autorelease pools, backing-scale handling, and multiple displays.
- NSEvent, tracking areas, gestures/tablets, `NSTextInputClient`, NSPasteboard, and dragging.
- `CAMetalLayer`, Metal device/queue/command buffers, display timing, display color-space/EDR headroom and change detection, explicit layer color/pixel-format configuration, and CPU-buffer upload fallback.
- NSAccessibility roles/actions/values, text markers/ranges, and main-thread marshaling.

Validate Objective-C block ABI, method-return ABI, selector signatures, and dynamic-class lifetime during M0. Prefer block-free APIs until the spike establishes a safe contract.

### 14.7 Future local browser/Wasm backend

- Map the primary application surface to a browser canvas. Treat multiple top-level windows as a capability that may be unavailable rather than emulating desktop windows silently.
- Acquire WebGPU adapters, devices, canvas color/tone-mapping configuration, and display gamut/dynamic-range capabilities asynchronously. Use the software renderer plus Canvas presentation as the documented fallback, and never infer browser HDR support merely from GPU texture-format support.
- Receive pointer, keyboard, wheel, focus, visibility, resize, and timing events through generated browser host bindings and normalize them before they enter the runtime.
- Bridge IME through a narrowly controlled hidden textarea or content-editable element while keeping HimariUI's text model authoritative.
- Mirror the semantics tree into a DOM accessibility structure without using DOM/CSS for visual layout or rendering.
- Model clipboard, drag-and-drop, file selection, and permissions as asynchronous capabilities with explicit denial/unavailability results.
- Permit rendering on the browser event context or in a Web Worker. Do not require threads, `SharedArrayBuffer`, or worker support for correctness.
- Load application assets and fonts from bundled bytes or fetch-based sources; do not assume filesystem or system-font access.

### 14.8 Future Android and iOS AOT backends

- Share the normal Java 25 runtime, layout, text, graphics, software-rendering, RHI, and semantics implementations without an ART-specific common source set.
- On Android, use a thin platform host for Activity lifecycle, surfaces, touch, keyboard, IME, clipboard, permissions, accessibility, display density, safe areas, suspend/resume, and memory-pressure events. Present the software renderer first; add Vulkan through generated JNI/NDK glue only after the host boundary passes lifetime and threading tests.
- On iOS, use generated Objective-C/C host glue for application and scene lifecycle, UIKit surfaces and events, text input, clipboard, permissions, accessibility, display scale and safe areas, suspend/resume, and memory-pressure events. Present the software renderer first; add Metal after the same host-boundary gates pass.
- Treat surface loss, backgrounding, device loss, and host-driven main-thread scheduling as ordinary lifecycle states rather than desktop exceptions.
- Do not claim either target until its AOT toolchain, signing, device deployment, packaging, Java 25 coverage, and platform integration matrix pass. Failure leaves the desktop release and Java 25 design unchanged.

### 14.9 Future remote Web client

- Accept the canonical scene, resource, semantics, and interaction protocol from an authoritative JVM or Native Image host. Do not require the full HimariUI Java runtime, application components, layout engine, or text shaper to execute in the browser.
- Reuse the same browser presentation semantics, WebGPU mapping, Canvas/software fallback, device-loss handling, and visual differential corpus as the local browser/Wasm target. Code sharing is preferred when the selected toolchain permits it; protocol conformance and output equivalence are required even when the client decoder uses a different implementation language.
- Keep the server authoritative for state, layout, shaping, hit testing, focus, pointer capture, and IME transactions. Return normalized browser input with stream epoch, sequence, surface-configuration version, monotonic client time, and synthetic/real origin.
- Mirror correlated semantics deltas into DOM accessibility nodes and use a narrowly controlled hidden text-input element for IME. Do not use DOM/CSS for authoritative visual layout or rendering.
- Cache images, glyph data, paths, and nested display lists by declared resource ID and verified content hash. Request missing resources and recovery snapshots explicitly; never substitute ambient browser fonts for authoritative shaped text.
- Apply frame acknowledgements, latest-wins scene delivery, ordered resource/configuration records, bounded queues, and measured backpressure. Do not decode or synthesize RHI, WebGPU, Vulkan, D3D12, Metal, or other native GPU command streams from the network.

---

## 15. Input, Focus, IME, and Accessibility Workstream

### 15.1 Normalized event model

Preserve monotonic timestamp, device ID/type, physical/logical position, pressure, tilt, buttons, physical key, logical key, repeat state, modifiers, native sequence ID, and synthetic/real origin. Route events through:

```text
capture -> target -> bubble
```

Use reusable primitive storage internally and expose immutable views to applications.

### 15.2 Gesture arena

Implement Java-oriented recognizers for tap, double-tap, long press, drag, scale, rotation, and scroll. Support competition, teams, cancellation, pointer capture, velocity tracking, and replaceable platform scroll physics. Test gesture behavior with deterministically replayed event traces.

### 15.3 Focus

Keep the focus tree separate from layout. Model keyboard focus, text-input focus, and accessibility focus independently. Provide traversal policies, focus scopes/traps, popup/dialog restoration, focus-visible policy, and cross-window transfer.

### 15.4 Text input

Define a common text-input client model for current value, range replacement, selection updates, caret geometry, and composing range. Support marked/composing text, candidate-window placement, surrounding text, delete-surrounding, reconversion, password mode, attributed rich-text selection, and IME-aware undo coalescing.

Specify the state-ownership contract for controlled text input before any text-editing control is built: the editor owns the buffer, selection, and composing state as authoritative editing state; the application owns the committed value it observes and may transform or reject proposed changes only through the transactional update path. Define how asynchronous filtering, partial acceptance, rejection, and IME composition interact with reactive updates so no combination produces reentrant writes, lost updates, or composition corruption. The M1 controlled text-editing fixture exercises this contract, and `EDIT-001` must implement it unchanged or revise it by ADR.

### 15.5 Semantics tree

Maintain an independent incremental tree containing role, label/value/hint, state, actions, bounds/transform, reading order, live regions, text-range providers, descendant merge/clear behavior, and virtualized collection metadata.

Include semantics acceptance criteria from the first implementation of every control. Accessibility is never a final stabilization add-on.

### 15.6 Transported interaction and semantics

Give normalized input, focus, IME, and semantics records canonical bounded encodings correlated with `streamEpoch`, surface-configuration version, and scene snapshot IDs. Preserve client event sequence, monotonic client timestamp, receive timestamp, device identity, and synthetic/real origin so replay and latency analysis can distinguish production, transport, and scheduling delay. The authoritative runtime validates all remote input against current surface, focus, pointer-capture, permission, and session state; a client cannot select arbitrary internal nodes or mutate state directly.

Transmit semantics as full recovery snapshots plus ordered deltas with stable IDs scoped to the stream epoch. A visual scene may be dropped, but semantics, focus, and IME transitions referenced by an accepted scene must remain ordered. Password and otherwise sensitive text fields expose only the minimum semantics and surrounding-text data permitted by their API contract and session policy.

---

## 16. Controls, Themes, and Animation Workstream

### 16.1 Layering

```text
interaction primitives
  -> unstyled controls
  -> behavior/state machines
  -> theme tokens
  -> default themed controls
  -> optional Material/Cupertino/etc. design packs
```

Do not hard-code one visual design into core controls.

### 16.2 Control implementation order

1. Text, Image, Canvas, and Spacer.
2. Button and IconButton.
3. Checkbox, Radio, and Switch.
4. Scrollbar and ScrollView.
5. TextField and TextArea.
6. Slider and Progress.
7. List/Tree/Table primitives.
8. Menu, Popup, and Tooltip.
9. Dialog.
10. Tabs and SplitPane.
11. Date/time and other advanced controls later.

Every control must implement pointer and keyboard behavior, focus, disabled/read-only states, semantics, theme tokens, high contrast, reduced motion, RTL, and golden/interaction/accessibility tests.

### 16.3 Text editing structures

Use a gap buffer for small single-line fields and a piece table or rope for large documents. Store selection and composition as UTF-16 ranges, but perform operations on grapheme boundaries. Support undo/redo transactions, clipboard flavors, Bidi caret motion, and Unicode/locale-aware word and line selection.

### 16.4 Animation

Animation is the transaction-scoped evolution of presentation state toward committed model targets. It is not a loop that writes application `State`, and it must not depend on a component function or content closure running on every frame. The ordinary-Java API must make simple motion concise while retaining explicit identity, interruption, lifecycle, and phase behavior.

#### 16.4.1 Model, presentation, and transaction semantics

- Keep the committed model target distinct from the current presentation value. Application state reads observe the committed target by default; layout, paint, hit-test, semantics, and layer consumers receive the appropriate presentation value through typed bindings. Provide controlled presentation-value and velocity inspection for gesture handoff and diagnostics without turning them into ordinary reactive sources.
- Propagate an immutable `AnimationTransaction` from a state-changing action through `UiCommitTransaction`. It carries the effective motion specification, disabled/reduced-motion state, replacement policy, completion group, causal/trace identifier, and scoped overrides. It must not rely on process-global mutable animation state or thread identity that a browser event loop cannot reproduce.
- An explicit animation associates a transaction with an action. An implicit animation associates a default specification with a particular bound property and target change. The explicit transaction, local property policy, subtree policy, and accessibility policy must have documented precedence.
- Commit all model targets and structural changes first, then create, replace, reverse, or cancel their animation instances atomically. A failed or cancelled UI commit creates no visible animation and fires no success completion.
- A completion callback fires exactly once with an explicit completed, replaced, cancelled, failed, or skipped outcome. Completion criteria distinguish logical timeline completion from physical spring settling/removal. A transaction that produces no effective animation, including after reduced-motion transformation, completes after its UI commit stabilization barrier rather than waiting for a frame.

#### 16.4.2 Animatable values and motion specifications

- Define typed, deterministic interpolation adapters that map a framework value to and from allocation-free scalar or fixed-width vector storage. Provide specialized paths for primitive values, points, sizes, rectangles, transforms, colors, radii, and other hot types; do not require boxing or allocate generic vector objects per sample.
- Keep value interpolation separate from temporal motion. An adapter defines decomposition, reconstruction, equality tolerance, clamping/normalization, and valid velocity units. A motion specification defines progress or physical evolution over time.
- Interpolate colors in an explicitly selected color space, transforms with documented decomposition/fallback rules, and cyclic or constrained values with type-specific policies. Discrete values must use a defined threshold or structural transition instead of accidental numeric interpolation.
- Provide tween/unit-curve, perceptual and physical spring, decay/inertial, phase-sequence, and multi-track keyframe specifications. Support delay, speed, repeat, autoreverse, and sequencing as higher-order specifications without duplicating timeline engines.
- A custom motion model may retain per-instance state and must define deterministic sampling, completion, and, when supported, velocity and merge/retarget behavior. Arbitrary application sampling code runs only on the UI execution context. Only a framework-defined bounded declarative representation may be offloaded to a renderer or encoded into a scene.

#### 16.4.3 Clock, sampling, interruption, and gesture handoff

- Use a monotonic `FrameClock` and evaluate motion from elapsed time, not frame count. Define pause/resume, time scaling, delayed starts, missed frames, variable refresh, and surface-not-presented behavior. Headless supplies a manually advanced clock so any timestamp can be reproduced without sleeping.
- Sample every due UI-side animation against one timestamp and publish all results as one presentation epoch. Consumers must never observe half of a coordinated animation group at a different time.
- Request a new frame only while an animation has future work or another subsystem requires presentation. Sampling after a delayed or dropped frame advances directly to the value for the current time rather than replaying every missed frame.
- Before replacing an active animation, sample its current value and velocity at the replacement timestamp. Compatible springs, decays, gesture handoffs, and custom motion preserve both; other replacements preserve at least value continuity. Expose explicit preserve-velocity, blend, restart, and snap policies, with preserve-velocity as the default for compatible spring retargeting.
- Normalize gesture velocity into the animated property's coordinate space and timestamp domain. Drag-to-decay, drag-to-spring, and interruption by a new pointer gesture must not introduce a position or velocity discontinuity.
- Give springs both approachable perceptual duration/bounce parameters and physical mass/stiffness/damping parameters. Define a stable settling threshold separately from perceptual or logical duration so user-facing completion does not depend on an asymptotic tail.

#### 16.4.4 Phase execution and compositor eligibility

- Each animated property declares the same earliest-impact metadata required by ADR-004. Size and intrinsic-content animation may require `MEASURE`; position may require `PLACE`; drawing parameters may require `PAINT`; retained transform, opacity, clip, and eligible filter changes may require only `COMPOSITE`. Geometry-affecting motion must also invalidate authoritative hit-test and semantics geometry as required.
- Layer-only animation must not rerun structural scopes, layout, or display-list recording. A renderer may sample a bounded layer-animation program without UI callbacks, but the program, clock mapping, replacement generation, and result must be reproducible by the UI runtime and deterministic replay.
- At input time, authoritative hit testing samples or obtains the same presentation transform used for the visible frame; it must not silently test only the final model geometry. Focus and accessibility remain UI-runtime decisions even when visual sampling is offloaded.
- A future browser or remote client may sample only negotiated compositor-eligible programs. A new authoritative target or generation supersedes client work deterministically. Unsupported, custom, layout-affecting, or non-reproducible animation remains host-sampled and is delivered through ordinary scene updates.

#### 16.4.5 Structural transitions and matched geometry

- Treat insertion/removal, visibility, and replacement as explicit transition state machines rather than scalar property changes. Support asymmetric enter/exit specifications, composition, interruption, reversal, and a declared policy for whether an exiting presentation participates in layout or moves to an overlay.
- Stable branch and collection identity determine whether a transition retargets an existing presentation or represents a new element. Diagnose duplicate identities and ambiguous transition sources instead of selecting by traversal or allocation order.
- On logical removal, dispose the element's `ReactiveOwner` and remove it from focus, hit testing, and semantics by default. An exit transition retains only the immutable presentation data and resource leases needed to draw it. Any alternative interactive or accessibility lifetime must be an explicit control policy, not a side effect of animation duration.
- Keep hidden, detached, and removed states distinct. Visibility changes may retain local state; removal follows normal lifecycle disposal even if an exit presentation is still visible.
- Provide matched-geometry/shared-element transitions keyed by an explicit namespace and stable identity. Capture source and destination geometry after their respective layout passes, animate through a transition-owned overlay or layer proxy, and specify anchors, coordinate-space conversion, clipping, z-order, scroll movement, resource ownership, and multiple-source conflict behavior. Geometry matching links presentation only; it does not transfer application state or element ownership.

#### 16.4.6 Motion policy, diagnostics, and performance

- Model reduced motion as a per-presentation-target transformation of motion specifications, not only a global Boolean. Resolve platform/user preference, application policy, and explicit essential-motion exceptions with documented precedence; apply dynamic policy changes at an atomic presentation epoch. Policy may shorten duration, remove bounce, replace large translation/scale with opacity, or snap nonessential motion while preserving state and completion semantics. Record both requested and effective specifications. A remote client reports its preference as session configuration so the authoritative host selects the effective specification; the client must not reinterpret an already transmitted program independently.
- Trace animation transaction IDs, target and presentation values, velocity, effective motion specification, start/sample/completion times, replacement generations, phase invalidations, offload decisions, and completion outcomes. Replay must reproduce presentation values and frames at declared timestamps.
- The inspector must show active animations and transition identities, model targets, current presentation values, velocity, remaining logical time, settling state, owning transaction, invalidated phases, compositor eligibility, and reduced-motion substitutions.
- Require zero or near-zero steady-state per-frame allocation. Benchmark 60 Hz, 120 Hz, variable-refresh, skipped-frame, simultaneous-animation, layout-animation, paint-animation, and compositor-only cases, including the cost of authoritative hit testing during offloaded motion.

---

## 17. Image, Color, and Media Workstream

### 17.1 Image baseline

- Keep `PixelBuffer` independent of `BufferedImage`.
- Define premultiplied/unpremultiplied semantics, stride, planes, and formats explicitly.
- Associate every decoded image or plane with an explicit `ColorEncoding`, alpha interpretation, and, where present, ICC/cICP-style primaries, transfer, matrix, range, reference-white, and content-light metadata. Preserve unknown-but-bounded metadata for a compatible codec round trip without allowing it to alter rendering implicitly.
- Implement a pure-Java PNG codec first.
- Add BMP or QOI as simple debug formats if useful.
- Add JPEG, GIF, WebP, and AVIF through independent codec providers.
- Enforce image-size, memory, decompression-ratio, and incremental-input limits.
- Accept framework resource sources or incremental byte input rather than requiring filesystem paths; browser/Wasm loading may complete through fetch-based asynchronous sources.

### 17.2 Color

#### 17.2.1 Encodings and values

- Define `ColorPrimaries`, `WhitePoint`, `TransferFunction`, `ColorEncoding`, `ColorProfile`, `LuminanceRange`, `ContentLightMetadata`, and scene/display-referred interpretation as framework-owned immutable values. Do not make supported spaces a closed enum or use one `isHdr` property as the semantic model.
- Treat gamut and dynamic range independently. Display-P3 or BT.2020 may carry SDR or extended/HDR values; an sRGB-primary extended-linear encoding may carry values above SDR white. A format or gamut name alone never proves HDR output capability.
- Support sRGB, linear-sRGB, Display-P3, and linear Display-P3 as first-stable named encodings. Implement and fixture BT.2020 plus BT.2100 PQ/HLG reference conversions and serialization during first stable so their later use does not change the value or scene model; the serialization and tagging contract lands with `COLOR-MODEL-001` in M3, and the conversion math lands with `COLOR-REF-001` by M10. Leave room for A98 RGB, ProPhoto RGB, XYZ/Lab-family, ACES-family, ICC, and future parameterized encodings without changing command layouts.
- Accept only finite components, but preserve valid negative and above-one values through interpolation and conversion. Clamp only at an explicit gamut/tone-mapping or destination-format boundary and report lossy fallback in diagnostics.

#### 17.2.2 Working space, alpha, and precision

- Use an explicitly tagged extended-linear floating-point working encoding in the scalar reference renderer; extended-linear sRGB is the first-stable default unless corpus evidence selects another ADR-compatible default. Conversions into it must retain out-of-gamut components rather than clipping.
- Perform Porter-Duff composition and ordinary blend/filter math in linear light with documented premultiplied-alpha semantics. A blend mode, filter, gradient, interpolation, or image operation that intentionally uses another space declares that space and conversion boundary.
- Provide `RGBA8`, 10-bit packed, `RGBA16F`, and `RGBA32F`-class pixel formats in the framework model even when a platform implements only a subset. Use adequate precision for every intermediate and test half-float/packed quantization separately from color-conversion error.
- Keep raw GPU texture formats untagged numeric storage at the RHI level; track content encoding and alpha interpretation in framework resources and render-graph edges so sampling or render-target hardware conversions cannot occur accidentally.

#### 17.2.3 Profiles and transforms

- In M3, implement the fixed sRGB/Display-P3 conversions and D50/D65 adaptation required by `COLOR-SDR-REF-001`. In M10, implement common pure-Java ICC v2/v4 profile parsing and matrix/TRC plus bounded LUT transforms through `COLOR-REF-001`; defer unusually complex profiles. Treat profiles as untrusted, content-addressed resources with explicit size, grid, channel, recursion, and work limits.
- Keep an extension seam for iccMAX or future profile architectures without requiring them for first stable. ICC processing does not replace explicit PQ/HLG, reference-white, content-luminance, or presentation metadata.
- Implement chromatic adaptation and conversion reference paths from published formulas and fixed vectors. Version gamut-mapping and tone-mapping algorithms because their output participates in goldens, traces, caches, and remote replay.

#### 17.2.4 Presentation and fallback

- Negotiate a requested and effective `PresentationColorConfiguration` per surface. It identifies format, encoding, alpha mode, reference white/headroom or luminance range, mapping ownership, metadata, and capability generation.
- Let the final presentation stage perform the declared output transform. If native HDR/EDR output is unavailable or unverified, apply the selected deterministic framework SDR tone/gamut mapping, preserve the original content/scene encoding for other outputs, and report the fallback. Never silently clip, relabel, or reinterpret content as sRGB.
- Re-evaluate presentation when the display, window placement, OS/browser color setting, headroom, or surface changes. Keep application colors and recorded display lists stable; invalidate only output conversion, caches, and presentation work that depend on the effective configuration.
- Fix source encoding, working encoding, reference white, luminance/headroom, mapping algorithm/version, and destination encoding in golden tests so ambient system color management cannot perturb results.

#### 17.2.5 Delivery boundary

First stable completes the extensible values/codecs, extended-linear software reference path, BT.2020/PQ/HLG conversion fixtures, Headless virtual HDR surfaces, deterministic HDR/WCG-to-SDR fallback, and platform/RHI capability reporting. Hardware HDR presentation remains disabled unless a backend passes its dedicated tests. Later advanced-color work enables and productizes native Vulkan HDR, Windows Advanced Color, Metal EDR, browser HDR, mobile HDR, calibrated workflows, and HDR image/media formats without changing these contracts.

### 17.3 Later media SPI

Define media contracts for timestamps/timebases, PCM audio buffers, video planes and color metadata, subtitles, backpressure, seeking, and texture/pixel interop. Start with pure-Java WAV/PCM, simple containers, or image sequences. Keep FFmpeg bindings in optional artifacts.

---

## 18. Verification Plan

### 18.1 Test layers

1. **Pure unit tests**: load no system GUI or GPU libraries.
2. **Conformance tests**: cover Unicode, OpenType, color/HDR reference math and codecs, ABI, and layout invariants.
3. **Differential tests**: compare with FreeType, HarfBuzz, SDL, platform APIs, and LWJGL.
4. **Golden tests**: require exact software output and bounded GPU differences.
5. **Property/fuzz tests**: target parsers, geometry, state, and events.
6. **Integration tests**: exercise real windows, input, IME, accessibility, and presentation.
7. **Native Image tests**: build and run on every platform.
8. **Performance tests**: use JMH plus full-frame harnesses.
9. **Long-running tests**: cover resource leaks, device loss, and repeated window creation/destruction.

### 18.2 Differential matrix

| Subsystem | Java reference | External Oracle | Comparison |
|---|---|---|---|
| SFNT/OpenType parser | Checked parser | FreeType/fonttools runner | Tables, metrics, outlines |
| TrueType VM (hinting fidelity profile) | Faithful Java VM | FreeType | Points, CVT, advances, bitmaps |
| CFF/CFF2 | Java charstrings | FreeType | Outlines, hint masks, bitmaps |
| Shaping | Java shaper | `hb-shape`/HarfBuzz runner | Glyph IDs, clusters, advances, offsets, flags |
| Bidi/segmentation | ICU4J adapter | Unicode conformance data | Exact boundaries and order |
| Path rasterization | Scalar software path | Skia/FreeType/Impeller runner | Coverage masks and goldens |
| Blending/filters | Scalar formulas | Reference image runner | Exact pixels or bounded tolerance |
| Color conversion/HDR math | Extended-linear scalar transforms plus versioned mapping | ICC reference implementation and published ICC/ITU/W3C vectors | Components, XYZ values, luminance, mapping output, and bounded quantization error |
| Layout | Invariants/reference policies | Curated Compose/Flutter cases plus Taffy/Yoga flex-grid corpora | Size, position, baseline |
| Event normalization | Recorded model | SDL/LWJGL/platform traces | Sequence, timestamp class, buttons |
| Vulkan/D3D12/Metal | CPU-rendered scene | GPU backend plus reference tooling | Frame image and resource diagnostics |
| Accessibility | Semantics snapshot | OS inspection harness | Role, name, value, actions, ranges |

### 18.3 Unicode corpus

Pin and run official Bidi, grapheme, word, sentence, line-break, normalization, emoji-sequence, and script-specific shaping data, including `BidiTest.txt`, `BidiCharacterTest.txt`, `GraphemeBreakTest.txt`, `WordBreakTest.txt`, `SentenceBreakTest.txt`, and `LineBreakTest.txt`.

Submit a dedicated data-change report whenever Unicode or ICU is upgraded.

### 18.4 Font corpus

Maintain separate groups for minimal synthetic fonts, open-source Latin/CJK/Arabic/Indic fonts, variable fonts, malformed/truncated fonts, color fonts, upstream regressions, and minimized fuzz cases. Record license, source, hash, and purpose for each file. Do not commit proprietary system fonts.

### 18.5 Golden policy

- Use fixed seeds, fonts, source/working/destination color encodings and profiles, reference white/luminance/headroom, and gamut/tone-mapping algorithm versions for exact software hashes.
- Store numeric extended-linear fixtures separately from mapped SDR reference images. PNG or the current display cannot define whether an HDR intermediate is correct.
- Store reference images, diff images, maximum error, mean error, edge-mask error, and color-conversion/mapping error for GPU goldens; compare pre-presentation linear values before mapped pixels where practical.
- Compare glyphs, clusters, and outlines numerically before relying on screenshots for text.
- Require reviewer inspection for every golden update; never mass-accept new output automatically.
- Provide a `golden-reviewer` that shows before/after, blink, and heatmap views.

### 18.6 Fuzzing and parser safety

Target font tables, charstrings, TrueType bytecode, image headers/compressed streams, ICC profiles/LUTs and color metadata, paths/dashes/transforms, display-list/scene/resource/semantics/input deserialization, Wayland/X11 message decoding, ABI string/array marshaling, and text-editing operations.

Use Jazzer and property-based tests, import relevant OSS-Fuzz corpora, run differential fuzzers, enforce time/allocation/recursion limits, minimize crashes into regression fixtures, and use checked `long` arithmetic for parser offsets.

### 18.7 ABI-test isolation

Oracle probes may depend on C compilers, SDKs, LWJGL, or JNA only if they live under `oracles/`, are not published to Maven, run in an isolated CI profile, produce JSON reports, and block only the corresponding platform release when they fail.

### 18.8 Performance baseline

`PERF-BASE-001` in M3 creates the benchmark harness, baseline-machine descriptors, measurement policy, and budgets for the scenarios available by M3. Each later owning work package adds its scenario and freezes its first budget before accepting an optimized implementation. The complete first-stable catalog covers:

- 10,000 mounted nodes with a 1% state change.
- A 100,000-item virtualized list scroll.
- Paragraph shaping/layout at 10,000 and 100,000 characters.
- Complex Arabic and Indic editing.
- 10,000 paths.
- Large sets of rounded rectangles and shadows.
- Image grids and blur/backdrop effects.
- Multiple windows.
- Coordinated animation groups with 100 layout-affecting and 10,000 compositor-only properties, including interruption and gesture handoff.
- 4K extended-linear compositing plus wide-gamut conversion, tone/gamut mapping, and `RGBA16F` presentation conversion.
- Software rendering at 1080p and 4K.
- GPU presentation at 60 Hz and 120 Hz.

Initial engineering targets:

- Do not request frames continuously while idle.
- Avoid objects whose count scales with draws, glyphs, or events in steady animation hot paths.
- Do not traverse the entire UI tree for one local state change.
- Design 120 Hz frame work around an 8.33 ms budget.
- Measure input-to-present latency in frames as well as CPU time.
- Record p50/p95/p99, allocation, GC, native memory, and GPU memory.

Record baseline-machine descriptors and the first absolute regression thresholds in `PERF_BUDGETS.toml` during M3, then append each later scenario when its reference implementation becomes runnable. Enforce every published threshold thereafter. `PERF-001` in M10 completes the catalog, expands the supported-machine set, and builds the release dashboard; it must not retroactively erase an earlier regression without a reviewed budget change.

### 18.9 Animation conformance

Use the Headless manual clock to sample every motion model at exact timestamps, at frame rates from 30 Hz through 240 Hz, and across variable-refresh and deliberately skipped frames. Require deterministic results independent of sampling frequency except where a specification explicitly models discrete steps.

Cover:

- atomic model-target commits and coordinated presentation epochs;
- model-versus-presentation read semantics and the absence of per-frame application-state writes;
- tween boundaries, keyframe-track alignment, phase sequencing, repeat/autoreverse, delays, and decay termination;
- analytic spring values and velocities, gesture velocity handoff, compatible retargeting, incompatible replacement, reversal, snapping, and settling thresholds;
- exactly-once completed, replaced, cancelled, failed, and reduced-motion-skipped outcomes, including groups that create no animation;
- earliest-phase invalidation counts proving that compositor-only motion performs no structure, measure, place, or paint work;
- authoritative hit testing and semantics geometry during layout-, placement-, and compositor-driven motion;
- insertion, removal, visibility, replacement, transition reversal, owner disposal, resource leases, focus transfer, and noninteractive exit presentations;
- matched-geometry identity, coordinate conversion, clipping, z-order, scrolling, duplicate-source diagnostics, interruption, and fallback behavior;
- requested-versus-effective reduced-motion policies and deterministic completion after substitution;
- trace/replay equality for target, presentation, velocity, transition state, phase invalidation, and frame output at each declared timestamp;
- allocation and retained-memory stability after repeated creation, interruption, completion, and disposal.

Where a layer animation is offloaded, run the same timestamp corpus through the UI reference sampler and the local render sampler; once the R0 program encoding exists, additionally run the canonical encoding round trip and any activated browser or remote sampler. Compare presentation values before comparing pixels, and reject offload when an implementation cannot meet the declared tolerance or replacement-generation semantics.

### 18.10 Color and HDR-readiness conformance

Keep hardware HDR enablement optional, but make the architecture and reference behavior first-stable gates. Cover:

- predefined sRGB, linear-sRGB, Display-P3, linear Display-P3, BT.2020, BT.2100 PQ, and BT.2100 HLG encodings against published matrices, transfer-function vectors, white points, and luminance values;
- chromatic adaptation, ICC matrix/TRC and bounded LUT transforms, profile hashing, malformed-profile rejection, and configured resource/work limits;
- finite negative and above-one component round trips through `Color`, animation interpolation, display lists, `SceneEnvelope`, traces, `PixelBuffer`, and `RGBA16F`/`RGBA32F` Headless surfaces without premature clamping;
- explicit rejection of NaN, infinities, unknown required encodings, invalid luminance/headroom, inconsistent metadata, and unsupported required presentation features;
- linear-light blending and premultiplied-alpha invariants for extended-range and out-of-gamut inputs;
- half-float, 10-bit packed, and 8-bit quantization error independently from conversion and mapping error;
- deterministic, versioned gamut/tone mapping from wide-gamut and PQ/HLG fixtures to SDR sRGB and Display-P3, including reference-white changes and highlight/color-volume stress cases;
- virtual Headless displays whose gamut, luminance, SDR reference white, headroom, metadata support, and capability generation change while a window is active;
- cache invalidation and frame/trace reproducibility across effective presentation changes without rerecording unchanged application colors or display lists;
- requested/effective configuration, mapping ownership, fallback reason, and original-content preservation in capability diagnostics;
- software/GPU agreement before any backend advertises a hardware HDR mode.

For a backend that opts into HDR/EDR, additionally test supported format/color-space pair selection, actual precision, display migration/configuration changes, screenshot/capture semantics, system-versus-framework tone-mapping ownership, metadata behavior, SDR UI overlay appearance, and deterministic fallback after device/surface loss. Failure disables that advertised mode rather than blocking an SDR release.

### 18.11 Future local browser/Wasm validation

When the post-stable Web track begins, add browser integration tests for host-driven single-thread execution, optional Web Worker rendering, asynchronous startup and permissions, WebGPU and Canvas fallback, pointer/keyboard/IME normalization, DOM semantics mirroring, fetched fonts/assets, device loss, and deterministic replay. Include canvas color-space/tone-mapping negotiation, extended-range texture-versus-output capability separation, display-gamut/dynamic-range changes, and explicit SDR fallback. Run a defined browser/WebGPU matrix and compare portable subsystem fixtures with JVM Headless results.

### 18.12 Future mobile AOT validation

When the post-stable mobile track begins, first compile and execute a representative slice of the unchanged Java 25 core on each candidate AOT toolchain. Cover every stable Java 25 API family used by production modules, including `MemorySegment`, `Arena`, concurrency, exceptions, garbage collection, resources, and static initialization where applicable. Test target host calls and callbacks separately; do not infer mobile downcall/upcall support from successful core memory access. After feasibility passes, run lifecycle, input, IME, accessibility, software/GPU differential, device-loss, suspend/resume, memory-pressure, color/HDR capability and display-change behavior, explicit SDR fallback, signing, installation, and package-reproducibility matrices on Android and iOS devices and simulators/emulators.

### 18.13 Future remote-rendering validation

When the post-stable remote track begins, validate the canonical scene protocol first through offline files and a separate local process, then through the browser client and real transports. Require byte-for-byte canonical encoding, cross-implementation fixtures, full/delta recovery, resource deduplication and reclamation, unknown-feature rejection, malformed-input fuzzing, and identical software output for decoded scenes. Exercise latency, bandwidth limits, fragmentation, disconnect/reconnect, stale input, missing resources, dropped frames, bounded backpressure, stream-epoch changes, and recovery snapshots. Negotiate color encodings, output gamut/dynamic range, precision, reference white/headroom, mapping ownership, profile resources, and HDR metadata; test capability changes, authoritative mapped fallbacks, and rejection of unsupported required encodings without relabeling or clipping. For transmitted layer animations, additionally test clock mapping, timestamp sampling, interruption, replacement generations, unsupported-program fallback, long suspension, reconnection, and reconciliation against authoritative host snapshots; no callback or arbitrary executable payload may cross the boundary. Compare browser WebGPU and Canvas/software output with Headless, verify correlated DOM semantics and IME behavior, and measure input-to-present latency by production, transport, client queue, and presentation stages. Assert that no component, Java runtime, FFM, RHI, or native GPU object appears in the wire format.

---

## 19. Porting Plan

### 19.1 Port units

Never create a task named only "Port FreeType," "Port HarfBuzz," or "Port Impeller." Split work into independently testable port units with this issue template:

```text
Port ID:
Upstream project/version/commit:
Upstream files/symbols:
License/provenance:
Behavioral responsibility:
Inputs/outputs:
Numeric representation:
Known edge cases:
Oracle command/API:
Corpus subset:
Reference implementation target:
Optimization deferred:
Exit criteria:
```

### 19.2 Four required stages

#### Stage 1: Behavioral specification

- Read upstream documentation, tests, and implementation.
- Specify inputs, outputs, and invariants.
- Identify undefined and platform-dependent behavior.
- Collect minimal fixtures.
- Map upstream symbols to Java classes and methods.

#### Stage 2: Oracle runner

- Build the smallest useful CLI or test binding around the upstream implementation.
- Emit stable JSON or binary fixtures.
- Run it over corpus subsets in batch.
- Record upstream versions and build options.

#### Stage 3: Java reference implementation

- Keep it scalar, direct, and readable.
- Preserve correspondence with upstream stages where practical.
- Defer parallelism, SIMD, and cache tricks.
- Test every branch.
- Compare fields individually with the Oracle.

#### Stage 4: Optimized implementation

- Demonstrate the bottleneck with an independent benchmark.
- Support reference/optimized dual execution.
- Run differential fuzzing.
- Require a separate ADR before deleting a reference path; retain it by default.

### 19.3 Independent review roles

Use at least two independent roles for every port unit. Available roles include a specification agent, implementation agent, adversarial reviewer, and test generator. The adversarial review must look specifically for overflow, signedness, off-by-one behavior, lifetime errors, and failure paths.

An implementation plus tests generated by the same agent is not sufficient merge evidence.

### 19.4 Traceability files

Maintain:

- `UPSTREAM_MAP.md`: Java symbols mapped to upstream symbols.
- `PORT_STATUS.yaml`: feature and version status.
- `PROVENANCE.json`: source, license, and hashes.
- `ORACLE_FORMAT.md`: fixture protocol.
- `DIFFERENCE_POLICY.md`: accepted differences and rationale, plus the frozen differential pass-rate thresholds for each corpus, fixed from Oracle baseline reports before the consuming milestone begins.
- `PERF_BUDGETS.toml`: baseline-machine descriptors, benchmark commands, warmup/measurement policy, absolute budgets, and reviewed revisions.
- `PLATFORM_CONFORMANCE.yaml`: required platform profiles, fixture identifiers, supported capability modes, soak durations, resource budgets, and platform-specific waivers.
- `UPSTREAM_SYNC.md`: upgrade procedure.

### 19.5 Upstream upgrades

- Let automation report new versions or commits, but never rewrite ports automatically.
- Upgrade the Oracle first.
- Run the existing Java implementation and produce a difference inventory.
- Update one port unit at a time.
- Retain a report of behavior changes, fixes, performance effects, and license changes.

### 19.6 License policy

- License original framework code under the selected project license, provisionally Apache-2.0.
- Preserve upstream licenses, copyright notices, and notices for translations or derivative ports.
- Do not treat AI rewriting as a way to avoid license obligations.
- When clean-room work is required, separate specification and implementation roles and retain process records.
- Review the legal status of Apple SDK-derived metadata, test fonts, and shader-tool output separately.

---

## 20. Observability and Developer Tools

### 20.1 Runtime diagnostics

Emit a structured startup report containing:

```text
Java runtime/version
Selected platform backend
Host integration mechanism: FFM or browser/Wasm host bindings
Host integration status
Loaded system libraries
Selected renderer
GPU adapter/capabilities
Working/content color encodings
Requested/effective presentation format + color encoding
SDR reference white / luminance / HDR headroom where available
Color-management and gamut/tone-mapping ownership + fallback reason
Font catalog summary
IME/accessibility capability
Fallback reasons
```

### 20.2 Inspector

Inspect reactive owners, structural scopes, mounted elements, layout, layer, and semantics trees; dependency edges and versions; binding and structural-scope execution counts; measure/place/paint/composite invalidations; animation transactions, transition identities, model targets, presentation values, velocities, replacement generations, completion state, reduced-motion substitutions, and offload decisions; color/profile resources, working and content encodings, extended-range values, requested/effective surface configurations, luminance/headroom, capability generations, mapping algorithms/ownership, and fallback reasons; bounds, clips, and hit testing; frame timelines; display lists; render graphs; GPU resources/caches; font fallback and shaping runs; and accessibility properties.

Use a versioned pure-Java protocol. The inspector UI may be built with HimariUI or exposed through WebSocket/JSON to an external tool.

### 20.3 Capture and replay

Record normalized input events, state-transaction summaries, animation transactions and outcomes, requested and effective motion specifications, presentation timestamps/values/velocities, transition identities and states, replacement generations, canonical scene/display-list envelopes, sampled presentation epochs for render-executor-sampled animation, color/profile resources, working/content encodings, reference white/luminance/headroom and content-light metadata, requested/effective presentation configurations, mapping algorithm/version/ownership and fallback reasons, resource manifests and hashes, correlated semantics snapshots, frame timing, platform scale/configuration, and renderer capabilities in `FrameTrace`.

Replay traces with Headless and the software renderer so platform or GPU failures can become deterministic repository fixtures. The `scene-replay` tool must render from the encoded trace and declared resources alone, without references to producer-process objects or ambient system fonts. This offline boundary is the first-stable proof of transport readiness; it is not a live network implementation.

---

## 21. Security, Robustness, and Resource Limits

### 21.1 Untrusted input

Treat fonts, images, color profiles/LUTs, HDR/content-light metadata, clipboard data, drag-and-drop data, and protocol messages as untrusted.

- Use checked arithmetic for every length and offset.
- Configure limits for table count, glyph count, outline points, recursion depth, image dimensions, decompression ratio, ICC/profile size, transform stages, LUT dimensions/grid points/channels, metadata records, and color-conversion work.
- Avoid single large allocations whose size is controlled by input.
- Return structured parser failures.
- Budget memory before operations that could trigger OOM.
- Limit TrueType VM instructions, calls, and loops.

### 21.2 FFI failure handling

- Validate required symbols during startup.
- Convert optional symbols into capability flags.
- Capture native error codes immediately.
- Never let callbacks throw across the native boundary.
- Fail fast on ABI mismatch in debug builds.
- Do not attempt to recover from detected native-memory corruption.
- Permit direct downcall construction only in generated bindings.

### 21.3 Resource leaks

- Make native, GPU, window, font, and image resources explicitly `AutoCloseable`.
- Document ownership in types and API contracts.
- Provide a debug leak tracker.
- Soak-test repeated window and device creation/destruction.
- Test shutdown order.
- Validate lifetime behavior separately on the JVM and Native Image.

### 21.4 Future local browser/Wasm host boundary

- Treat browser messages, fetched resources, clipboard content, drag-and-drop data, and JavaScript callback payloads as untrusted input.
- Validate every value copied across Wasm linear memory, including offsets, lengths, encodings, object-handle generations, and callback identifiers.
- Keep generated JavaScript glue compatible with browser content-security policies; do not rely on dynamic code evaluation.
- Model permission denial, context loss, detached canvases, aborted fetches, and page lifecycle changes as ordinary capability or lifecycle failures.
- Do not grant the Wasm module ambient access to browser globals beyond the imports declared by the host schema.

### 21.5 Future mobile AOT host boundary

- Validate every length, offset, handle, object reference, callback identifier, and encoding crossing generated JNI/NDK or Objective-C/C glue.
- Contain exceptions on both sides of the boundary and make thread attachment, main-thread dispatch, callback lifetime, and shutdown order explicit.
- Treat intents, clipboard content, drag-and-drop data, file-provider results, URLs, platform callbacks, and restored state as untrusted input.
- Record all target-generated native inputs and outputs in the mobile package manifest; prohibit undeclared native libraries and runtime code downloads.
- Keep mobile signing credentials outside the repository and require reproducible unsigned package inputs before signing.

### 21.6 Future remote scene boundary

- Treat scene envelopes, color encodings/profiles/LUTs and luminance metadata, resource payloads, semantics deltas, input events, acknowledgements, capability messages, and session-control records as untrusted regardless of transport security.
- Validate magic, protocol version, required features, stream epoch, snapshot/resource generations and their bases, sequence IDs, lengths, counts, offsets, hashes, finite color/luminance values, profile/LUT dimensions and transform depth, supported encoding/mapping identifiers, compression ratios, recursion, total retained resources, and per-frame work before allocating or dispatching.
- Authenticate peers and apply authorization, origin, rate, replay, timeout, and resource quotas in the remote extension. Encryption and authentication do not belong to the scene codec and must not be replaced by a custom cryptographic protocol.
- Prevent remote input from naming arbitrary runtime objects or semantics nodes. Resolve actions only through capability-scoped, generation-checked IDs valid for the current authoritative snapshot and session.
- Redact password fields, private semantics, clipboard content, logs, traces, and diagnostic labels according to explicit session policy. Remote diagnostics must not silently widen the data exposed to a client.
- Bound producer, transport, decoder, resource, and presentation queues independently; disconnect or request recovery on protocol abuse instead of allowing unbounded memory growth.

---

## 22. Milestone Plan

The milestones are dependency-ordered, not calendar-bound, and are organized into three tracks that may proceed in parallel:

- **Platform track**: M0 platform/FFM feasibility, then Linux, Windows, and macOS implementation proceeds as individual prerequisites pass; M5 closes the first Linux convergence profile and M6/M7 close the remaining required desktop profiles.
- **Runtime track**: M1 reactivity/structure and M2 layout/input/semantics.
- **Graphics and text track**: M3 software graphics and M4 fonts/basic text, then M8.

A milestone gates only the work that consumes its exit criteria; each milestone declares two dependency sets below:

- **Starts after** lists the minimum evidence needed to begin useful work. Work packages without a dependency on another track proceed in parallel.
- **Completes after** lists integration evidence required before the milestone may close. It must not be interpreted as a reason to delay independent work.

Work in one track proceeds while an unrelated criterion in another track remains open, with two exceptions: M0's feasibility verdict is a project-level veto — if the shim-free FFM model fails on a platform, the affected platform work stops until a replacement ADR exists — and no track may merge work that violates a non-negotiable constraint from Section 2. M5 is the first convergence point of all three tracks; M9 through M11 integrate and stabilize. Where only part of a milestone consumes another deliverable, the dependency names that work package explicitly.

### M0 — Repository baseline and shim-free feasibility

**Objective:** Prove that Java plus FFM can call the required system APIs on all three desktop platforms without a project-built native shim.

**Deliverables:**

- **GOV-001**: Select the project license, contribution rules, and ADR template. Do not reopen the coordinates and naming accepted by ADR-013.
- **ADR-BOOTSTRAP-001**: Materialize every accepted inline decision as a canonical file under `adr/`, with status, date, evidence, and replacement links; add decision milestones for unresolved entries.
- **REFERENCE-LOCK-001**: Create `REFERENCES.lock` and pin the edition, release, commit, or retrieval date of every rolling source used as normative evidence.
- **PROVENANCE-001**: Define `PROVENANCE.json` and its CI validator.
- **CONFORMANCE-POLICY-001**: Create `CONFORMANCE.md` plus the `PLATFORM_CONFORMANCE.yaml` schema for profile ownership, fixtures, tolerances, durations, budgets, and reviewed waivers; populate the initial M0 spike profiles.
- **BUILD-001**: Create the Gradle multi-project build, JPMS setup, and Java 25 toolchain according to ADR-013.
- **GUARD-FRAMEWORK-001**: Implement the staged `pure-java-guard` registry and activate gates 1–11 for every artifact and module present in M0.
- **FFI-SCHEMA-001**: Define the minimum canonical primitive, pointer, structure, function, and callback schema.
- **FFI-FFM-001**: Generate typed FFM downcalls and upcalls for the minimum schema and execute them on the JVM.
- **ABI-PROBE-001**: Define the C-probe JSON protocol and compare its output with generated Java layouts.
- **SPIKE-WAYLAND-001**: Open a Wayland window, receive events, enumerate available output/color-management information, clear a software surface, and present it.
- **SPIKE-VK-001**: Create a Vulkan device and swapchain, enumerate surface format/color-space pairs without assuming HDR extensions, and present a clear.
- **SPIKE-WIN-001**: Open a Win32 window, receive `WndProc` callbacks, and query the current DXGI output/Advanced Color description.
- **SPIKE-D3D12-001**: Create a D3D12 device and swapchain, map its effective SDR presentation configuration, and present a clear.
- **SPIKE-MAC-001**: Open an NSWindow, attach a `CAMetalLayer`, and query display color space/EDR headroom.
- **SPIKE-METAL-001**: Create a Metal device and queue, map the layer's effective SDR presentation configuration, and present a clear.
- **SPIKE-OBJC-BLOCK-001**: Exercise representative Objective-C block creation, invocation, lifetime, and exception containment, then record an ADR that either forbids blocks or defines their verified use policy.
- **NI-FFM-001**: Build and run at least one platform spike with Native Image using the same generated FFM bindings and prototype the required reachability/downcall/upcall metadata.

**Exit criteria:**

- All three platforms run without a project-built native library.
- Accepted ADR files and `REFERENCES.lock` exist, carry reviewed status/evidence metadata, and have no unresolved internal links.
- `PROVENANCE.json` validation passes for every generated schema, binding fixture, shader/data resource, and Oracle input present in M0.
- `PLATFORM_CONFORMANCE.yaml` validates and names the commands, fixtures, and evidence required by every M0 platform spike.
- Generated FFM bindings are the only system-call path; no runtime FFI provider registry exists.
- JAR and dependency scans pass.
- Each platform can open a window, receive close/resize/input events, and present a solid color.
- Each platform reports a truthful target-neutral surface color capability snapshot and a tested SDR fallback; discovering an HDR-capable display is not required.
- Native callbacks remain stable under repeated execution and reentrancy tests.
- Native Image uses the same FFM bindings as the JVM and has reproducible run evidence.
- The macOS block-ABI spike produces an ADR that either avoids blocks or defines a verified use policy.

### M1 — Headless, state, reactivity, structural runtime, and scheduling

**Starts after:** M0 `BUILD-001`, `GUARD-FRAMEWORK-001`, `ADR-BOOTSTRAP-001`, `REFERENCE-LOCK-001`, and `CONFORMANCE-POLICY-001`; the remaining platform spikes proceed in parallel on the platform track.

**Completes after:** the same five M0 work packages; M1 does not wait for platform-spike completion.

**Deliverables:**

- **HEADLESS-001**: Virtual display, window, event loop, and clock.
- **STATE-001**: Primitive/object state, atomic transactions, epochs, and external commits.
- **REACTIVE-001**: Dynamic producer/consumer graph, `DerivedState`, push/pull propagation, equality, liveness, and cycle detection.
- **RUNTIME-SAMPLE-001**: Shared ordinary-Java comparison suite, frozen decision rubric, instrumentation, and reporting format.
- **RUNTIME-SPIKE-GROUPED-001**: Explicit grouped-recomposition prototype with positional memory and no compiler assistance.
- **RUNTIME-SPIKE-ONESHOT-001**: One-shot owner prototype with fine-grained bindings and explicit structural control flow.
- **RUNTIME-SPIKE-HYBRID-001**: Fine-grained binding prototype with small rerunnable structural scopes.
- **RUNTIME-ADR-001**: Accept ADR-023 explicit grouped recomposition, ADR-020 scoped current-measure materialization, and ADR-021 declared error-boundary containment from reviewed evidence.
- **STRUCTURE-001**: Implement the selected branch, keyed-collection, identity, local-state, and failure-recovery semantics.
- **MOUNT-001**: Mounted elements, typed property bindings, phase impacts, and incremental apply.
- **EFFECT-001**: Effect lifecycle.
- **SCHED-001**: UI scheduling and frame-request coalescing.
- **ANIM-CORE-001**: Animation-transaction propagation, model/presentation separation, manual-clock sampling, presentation epochs, allocation-free scalar adapters, and reference tween/spring retargeting.
- **RUNTIME-MBT-001**: Model-based differential harness running randomized operation sequences against a naive recompute-everything reference evaluator.
- **TRACE-001**: Initial deterministic trace format.

**Exit criteria:**

- The three prototypes compile and run without application code generation or transformation and publish the same source-ceremony, execution, allocation, memory, and phase-invalidation metrics.
- ADR-023, ADR-020, and ADR-021 are accepted before `STRUCTURE-001` begins; their checked evidence remains reproducible through the `m1-runtime-decision` conformance profile.
- Dynamic-dependency, lazy-derived, equality-suppression, diamond-glitch, batching, nested-transaction, effect-coalescing, cycle, and owner-disposal tests pass.
- Conditional, loop, keyed-reordering, changing-input, and local-state-retention tests pass under the selected model.
- Failed staged UI work leaks no nodes, graph edges, or effects; every candidate that claims cancellation or preemption passes the same cleanup gate for cancelled attempts.
- Local value changes invalidate only their dependent bindings or phase consumers; topology changes rerun only the selected structural scope.
- Ambient-value propagation and override fixtures pass with measured invalidation scope; viewport-driven materialization passes under the selected ADR-020 contract; per-phase application exceptions stay within their declared ADR-021 containment scope with deterministic fallback and diagnostics.
- The model-based differential harness completes randomized operation sequences against the recompute-everything reference evaluator without divergence.
- Headless animation tests prove atomic presentation epochs, no per-frame application-state writes, deterministic timestamp sampling, value-continuous replacement, and velocity-continuous compatible spring retargeting.
- A Headless sample runs deterministically.
- Runtime-core execution loads no native library.

### M2 — Layout, input, focus, and semantics skeleton

**Starts after:** M1 `RUNTIME-ADR-001`, `STRUCTURE-001`, and `MOUNT-001`.

**Completes after:** the same M1 work packages.

**Deliverables:**

- **LAYOUT-001**: Constraints, measurement, and placement.
- **LAYOUT-002**: Box, Row, Column, and modifiers.
- **LAYOUT-003**: Baselines, intrinsic measurement, and transforms.
- **INPUT-001**: Normalized events and capture/target/bubble routing.
- **FOCUS-001**: Focus tree and traversal.
- **SEM-001**: Semantics tree and snapshots.
- **HIT-001**: Hit testing and spatial indexing.
- **UI-BOOTSTRAP-001**: Private, non-stable interaction and window-chrome primitives used by architecture samples before the public controls API exists; build them only from M2 layout, input, focus, and semantics contracts.

**Exit criteria:**

- Single-measure rules and violation diagnostics pass.
- Placement-only invalidation works.
- Basic RTL layout works.
- Deterministic pointer and keyboard tests pass.
- Semantics bounds match layout bounds.

### M3 — Software graphics MVP

**Starts after:** M0 `BUILD-001`, `GUARD-FRAMEWORK-001`, `ADR-BOOTSTRAP-001`, `REFERENCE-LOCK-001`, and `CONFORMANCE-POLICY-001`; graphics values, display lists, scalar rasterization, and color reference work proceed alongside M1 and M2.

**Completes after:** M1 `TRACE-001` and `ANIM-CORE-001`; `SCENE-CODEC-001` consumes M2 `INPUT-001` and `SEM-001`, and the Headless integration exit consumes M2 `UI-BOOTSTRAP-001`, before M3 closes.

**Deliverables:**

- **GFX-001**: Geometry, color, `Path`, and `PathBuilder`.
- **COLOR-MODEL-001**: Extensible tagged color encodings, finite extended-range values, first-stable named spaces, linear-light alpha/blending rules, and encoding/pixel-format/alpha separation. Non-sRGB encodings, luminance metadata, and ICC payloads pass through values, display lists, and scenes as validated tagged data.
- **COLOR-SDR-REF-001**: Deterministic reference conversion among sRGB, linear-sRGB, Display-P3, linear Display-P3, and the first-stable extended-linear working encoding; D50/D65 chromatic adaptation; linear-light premultiplied composition; and a simple explicitly versioned extended-range/WCG-to-SDR mapping suitable for early Headless and platform fallback tests.
- **DL-001**: Canonical pointer-free display-list encoding and resource references.
- **SCENE-CODEC-001**: Canonical `SceneEnvelope`, ordered snapshot/delta and resource-generation state machine, semantics/input correlation, configured limits, and offline replay codec.
- **GUARD-SCENE-001**: Activate `verifySceneCodec` with canonical, malformed, limit, and fuzz fixtures.
- **SW-001**: Solid rectangle, rounded-rectangle, and path filling.
- **SW-002**: Clip, transform, and blend operations.
- **SW-003**: Images and gradients.
- **SW-004**: Tile scheduler.
- **CODEC-001**: PNG encoding and decoding.
- **GOLDEN-001**: Golden infrastructure and reviewer.
- **ANIM-SCENE-001**: Bounded retained-layer animation programs as immutable framework values, shared clock mappings, replacement generations, and Headless/software/render-executor reference sampling for compositor-eligible properties; the canonical program encoding is an R0 extension of the scene format.
- **PERF-BASE-001**: Check in benchmark scenes, baseline-machine descriptors, measurement commands, and initial regression budgets for M3 and later work.

**Exit criteria:**

- A Headless bootstrap interaction sample renders to PNG without depending on the public M9 controls API.
- Headless extended-linear float captures preserve tagged negative and above-one components; separately configured SDR PNG output uses the explicit `COLOR-SDR-REF-001` mapping rather than implicit clipping.
- Display lists and full `SceneEnvelope` fixtures serialize canonically, reject configured limit violations, and replay without producer-process object references.
- sRGB/Display-P3 reference conversions, adaptation, linear-light blending, and the initial versioned SDR mapping pass fixed numeric fixtures. Tagged BT.2020/PQ/HLG encodings, luminance/content-light metadata, and bounded ICC payloads survive canonical scene/trace round trips as validated data; their advanced conversion math and transforms remain `COLOR-REF-001` in M10.
- Transform and opacity animation fixtures produce the same presentation values at declared timestamps from the UI reference sampler and the render-executor sampler, while invalidating only `COMPOSITE` plus required hit-test or semantics geometry.
- Path/property fuzz tests produce no crash.
- Scalar and tiled outputs agree.
- Exact goldens remain stable for core scenes.
- `PERF-BASE-001` publishes reproducible numbers and budgets before optimized paths are accepted.

### M4 — Fonts and basic text

**Starts after:** M0 `BUILD-001`, `GUARD-FRAMEWORK-001`, `ADR-BOOTSTRAP-001`, `REFERENCE-LOCK-001`, and `CONFORMANCE-POLICY-001`; font parsing, shaping, Unicode, and Oracle work do not wait for graphics.

**Completes after:** M3 `SW-001` and `GOLDEN-001` for glyph rasterization and text goldens.

**Deliverables:**

- **FONT-001**: Checked SFNT reader and directory.
- **FONT-002**: Metrics, `cmap`, and `name`.
- **FONT-003**: `glyf`, `loca`, and composite outlines.
- **SHAPE-001**: Shaping buffer, clusters, and default shaper.
- **OT-001**: Baseline general GDEF/GSUB/GPOS engine.
- **TEXT-001**: Styled runs, line breaking, and paragraph layout.
- **RASTER-GLYPH-001**: Grayscale glyph rasterization and atlas.
- **ORACLE-FT-001 / ORACLE-HB-001**: FreeType and HarfBuzz runners plus corpora.
- **PORT-POLICY-001**: Create `DIFFERENCE_POLICY.md` from recorded Oracle baselines and freeze the M8 differential thresholds before M8 work begins.

**Exit criteria:**

- Latin, Greek, and Cyrillic glyph IDs, clusters, and positions compare successfully with HarfBuzz.
- Outlines and metrics compare successfully with FreeType.
- Unicode boundary corpora pass.
- Basic selection and caret behavior work.
- Text rendered with fixed fonts has stable goldens.
- `DIFFERENCE_POLICY.md` records the reviewed baseline, threshold, and rationale for every M8 corpus.

### M5 — First complete desktop vertical slice

Use Linux Wayland plus Vulkan by default because both expose explicit C ABIs. Change the order only if M0 evidence justifies an ADR.

**Starts after:** M0 `FFI-SCHEMA-001`, `FFI-FFM-001`, `SPIKE-WAYLAND-001`, and `SPIKE-VK-001`; Wayland bindings, window lifecycle, Vulkan device work, and RHI implementation proceed while M2–M4 run.

**Completes after:** M2, M3, and M4. This is the first convergence point of all three tracks.

**Deliverables:**

- **WAYLAND-001**: Generated protocol bindings and registry.
- **WAYLAND-002**: Window lifecycle, scaling, and frame callbacks.
- **WAYLAND-003**: Pointer, keyboard, and touch.
- **XKB-001**: Pure-Java keymap parser and state machine.
- **WAYLAND-004**: Clipboard and drag-and-drop.
- **WAYLAND-DECOR-001**: `xdg-decoration` negotiation and complete client-side decorations with titlebar, window controls, and interactive move/resize regions.
- **WAYLAND-IME-001**: `text-input-v3` sessions and composition/candidate-rectangle plumbing through `TextInputSession`, validated with a test editing surface before full text controls exist.
- **WAYLAND-POPUP-001**: `xdg_popup` surfaces, positioner semantics, and grab/dismiss behavior for menus, tooltips, and dropdowns.
- **WAYLAND-CURSOR-001**: `cursor-shape-v1` where available plus a pure-Java XCursor theme parser fallback.
- **VULKAN-001**: Registry generator, loader, and device.
- **VULKAN-002**: Swapchain, passes, pipelines, resources, and target-neutral surface color capability/effective-configuration mapping.
- **RHI-001**: RHI API and render-graph MVP.
- **GPU-DIFF-001**: CPU/GPU scene comparison.

**Exit criteria:**

- `CounterApp` and the private bootstrap interaction/window-chrome primitives run in a real Wayland session without depending on the M9 public controls API.
- Users can select software or Vulkan rendering.
- The checked-in Wayland platform-conformance profile passes for resize, DPI/fractional scaling, presentation timing, and surface reconfiguration.
- The demo presents usable window decorations on compositors with and without server-side decorations.
- Moving or reconfiguring the output updates the surface capability generation and preserves deterministic SDR presentation; Vulkan HDR remains disabled unless its optional conformance profile passes.
- The scrolling soak fixture remains within the memory, resource-count, and frame-allocation budgets established by `PERF-BASE-001`.
- Host frame callbacks pace a compositor-only animation, no application callback runs on the render executor, and frame requests stop after completion.
- Vulkan validation reports no errors.
- JVM and Native Image FFM smoke tests pass.

### M6 — Complete Windows backend

**Starts after:** M0 `FFI-SCHEMA-001`, `FFI-FFM-001`, `SPIKE-WIN-001`, and `SPIKE-D3D12-001`; generator, window, input, and device work may proceed before M5 closes.

**Completes after:** the M5 RHI and frame compiler.

**Deliverables:**

- **WINABI-001**: Win32 and COM generator.
- **WIN-001**: Windows, DPI, and message loop.
- **WIN-002**: Pointer, keyboard, cursor, and clipboard.
- **D3D12-001**: Device, swapchain, queue, fences, and DXGI Advanced Color capability/effective-configuration mapping.
- **D3D12-002**: Resources, pipelines, and descriptors.
- **WIN-IME-001**: TSF with IMM32 fallback.
- **WIN-A11Y-001**: UI Automation.
- **WIN-DND-001**: OLE drag-and-drop.
- **WIN-POPUP-001**: Owned popup windows with activation, z-order, and dismissal policy for menus and tooltips.

**Exit criteria:**

- The checked-in Windows platform-conformance profile passes for multiple windows, per-monitor DPI, move/resize modal loops, pointer/keyboard input, and resource-lifetime soaks.
- D3D12 validation/debug layers report no errors.
- Advanced Color and ordinary SDR displays report distinct truthful capabilities and deterministic SDR fallback; Windows HDR remains disabled unless its optional conformance profile passes.
- The Windows IME corpus passes composition, surrounding-text, candidate-rectangle, reconversion, and cancellation fixtures.
- The UI Automation inspection corpus passes.
- Scheduled UI work and animation continue during move/resize modal loops within the ADR-022 continuity contract.
- The Native Image sample runs.

### M7 — Complete macOS backend

**Starts after:** M0 `FFI-SCHEMA-001`, `FFI-FFM-001`, `SPIKE-MAC-001`, `SPIKE-METAL-001`, and `SPIKE-OBJC-BLOCK-001`; generator, window, input, and device work may proceed before M5 closes.

**Completes after:** the M5 RHI and frame compiler.

**Deliverables:**

- **OBJC-001**: Class, selector, and typed `objc_msgSend` generator.
- **COCOA-001**: NSApplication, NSWindow, and NSView.
- **METAL-001**: Device, layer, swapchain-equivalent presentation, and `CAMetalLayer` color-space/EDR capability/effective-configuration mapping.
- **MAC-INPUT-001**: NSEvent, gestures, and tablet input.
- **MAC-IME-001**: `NSTextInputClient`.
- **MAC-A11Y-001**: NSAccessibility.
- **MAC-DND-001**: Pasteboard and dragging.
- **MAC-POPUP-001**: NSPanel/child-window popup surfaces with activation and dismissal policy.

**Exit criteria:**

- The checked-in macOS platform-conformance profile passes on arm64 and x86-64 for scaling, multi-display migration, input, and resource-lifetime soaks.
- Metal validation/capture contains no error in the declared validation allowlist; every warning waiver is recorded in `PLATFORM_CONFORMANCE.yaml`.
- Display color-space/headroom changes update the surface capability generation and preserve deterministic SDR fallback; Metal EDR remains disabled unless its optional conformance profile passes.
- IME and accessibility corpora pass.
- Autorelease and native-resource soak tests pass.
- The Native Image path has a documented, evidence-backed status.

### M8 — Complex text and font completeness

**Starts after:** M4 `SHAPE-001`, `OT-001`, the relevant Oracle runners, and `PORT-POLICY-001`; script and font-capability units begin as soon as those individual prerequisites and their frozen comparison thresholds pass.

**Completes after:** M4.

**Deliverables:**

- **SHAPE-ARABIC-001** and the remaining Tier-1 script modules as first-stable blockers; **SHAPE-INDIC-001 / SHAPE-USE-001** and later scripts belong to the separately versioned Tier-2 shaping profile.
- **TT-VM-001**: TrueType interpreter, owned by the optional hinting fidelity profile; it does not block first stable.
- **CFF-001**: CFF and CFF2 charstring interpretation for outlines; CFF hinting application belongs to the hinting fidelity profile.
- **VARFONT-001**: Variation tables.
- **COLORFONT-001**: COLR, CPAL, CBDT, and sbix.
- **TEXT-BIDI-001**: Complete visual caret and selection behavior.
- **TEXT-JUSTIFY-001**: Justification and hyphenation policy.
- **FONT-FALLBACK-001**: System catalog and fallback.

**Exit criteria:**

- The HarfBuzz upstream shaping corpus for Tier-1 scripts reaches the pass rate frozen in `DIFFERENCE_POLICY.md` before M8 begins, with every difference recorded; Tier-2 corpora gate the Tier-2 shaping profile instead of first stable.
- The FreeType unhinted outline and raster corpus reaches the pass rate frozen in `DIFFERENCE_POLICY.md` before M8 begins; the hinting corpus gates the hinting fidelity profile.
- Arabic and Bidi editing scenarios pass; Indic editing scenarios gate the Tier-2 shaping profile.
- Variable- and color-font goldens pass.
- Malformed-font fuzzing is a release gate.

### M9 — Controls, IME, accessibility, and themes

**Starts after:** M2 `LAYOUT-002`, `INPUT-001`, `FOCUS-001`, and `SEM-001` plus M4 `TEXT-001` and `RASTER-GLYPH-001`; platform-neutral controls, editing-state machines, gestures, themes, and animation completion may proceed while platform and complex-text work continues.

**Completes after:** M5, M6, M7, and M8. Platform-independent deliverables may close earlier, but the M9 milestone does not close until all three accessibility and IME profiles pass.

**Deliverables:**

- **CTRL-001**: Unstyled interaction primitives.
- **CTRL-002**: Buttons, toggles, and sliders.
- **CTRL-003**: Scrolling, lazy-list, and table primitives.
- **EDIT-001**: TextField, TextArea, and undo.
- **POPUP-001**: Popup, menu, dialog, and tooltip, consuming the platform popup surfaces from `WAYLAND-POPUP-001`, `WIN-POPUP-001`, and `MAC-POPUP-001` with an in-window overlay fallback where a host provides no popup surface.
- **THEME-001**: Tokens, default theme, and high contrast.
- **A11Y-CORE-001**: Semantics actions, ranges, and live regions.
- **DBUS-001**: Pure-Java D-Bus client shared by Linux accessibility and input-method integration.
- **LINUX-A11Y-001**: AT-SPI2 bridge over `DBUS-001` exposing semantics roles, states, actions, and text ranges.
- **LINUX-IME-001**: IBus/Fcitx adapters over `DBUS-001` complementing `text-input-v3` where compositor IME support is unavailable.
- **GESTURE-001**: Gesture arena.
- **ANIM-001**: Complete motion specifications, gesture handoff, transaction precedence, completion outcomes, phase/compositor integration, reduced-motion transformation, tracing, and replay.
- **TRANSITION-001**: Enter/exit/visibility state machines plus matched-geometry/shared-element transitions and retained-presentation resource ownership.

**Exit criteria:**

- The controls-gallery interaction corpus produces the declared common outcomes on all three required desktop profiles, with every platform-specific difference recorded.
- Every control passes its keyboard-only traversal and activation fixtures.
- Basic screen-reader flows pass on Windows UI Automation, macOS Accessibility, and Linux AT-SPI2.
- The multilingual editing corpus passes the declared IME, selection, composition, rejection, and undo fixtures on all required profiles.
- The controls matrix passes its RTL, high-contrast, and reduced-motion fixtures.
- The animation conformance suite passes for interruption, velocity continuity, phase isolation, structural lifecycle, matched geometry, completion outcomes, and deterministic replay.
- Compositor-only animation triggers no structure, measure, placement, or paint callbacks, and authoritative hit testing follows its visible presentation geometry.
- Control accessibility tests are required merge gates.

### M10 — Performance, tools, and Native Image productization

**Starts after:** M3 `PERF-BASE-001`, `SCENE-CODEC-001`, and `COLOR-SDR-REF-001` plus M5 `RHI-001` and `GPU-DIFF-001`; independent tooling, advanced color, and Native Image work may begin before M9 closes.

**Completes after:** M5 through M9.

**Deliverables:**

- **PERF-001**: Expand the M3 baseline-machine set into the release regression dashboard and finalize supported-profile budgets without discarding earlier history.
- **CACHE-001**: Raster, glyph, and pipeline cache budgets.
- **COLOR-REF-001**: BT.2020/PQ/HLG reference conversion math and fixtures, ICC v2/v4 baseline parsing and transforms, completion of the chromatic-adaptation corpus beyond the M3 D50/D65 baseline, and the production versioned gamut/tone-mapped SDR fallback required by ADR-019.
- **VECTOR-001**: Optional `himari-render-vector` renderer.
- **INSPECT-001**: Tree, frame, and render inspector.
- **REPLAY-001**: Canonical scene/resource/color-profile/presentation/semantics/event trace and offline replay in a fresh process.
- **NI-001**: Reachability generator and static platform/renderer backend registries.
- **PACK-001**: jlink and Native Image packaging plugin.
- **DIAG-001**: Capability and fallback report, including requested/effective surface color configuration, precision, luminance/headroom, mapping ownership, and disabled HDR reasons.

**Exit criteria:**

- Regression budgets are fixed and enforced.
- Idle, scrolling, animation, and large-text scenarios meet their targets.
- Extended-linear compositing, color conversion, mapped-SDR output, and profile/resource limits meet their correctness and performance targets.
- The complete color and HDR-readiness conformance corpus passes with `COLOR-REF-001` in place.
- JVM and Native Image sample matrices pass.
- The inspector can localize reactive propagation, structural-update, layout, and render faults.
- `scene-replay` reproduces reference frames from encoded traces and declared resources alone, with no ambient font or producer-object access.
- Every pure-Java release-artifact gate passes.

### M11 — Beta and stabilization

**Starts after:** release-candidate APIs and platform profiles from M5–M10 are available.

**Completes after:** all prior milestones.

**Deliverables:**

- **API-REVIEW-001**: Public API compatibility review.
- **DOC-001**: Tutorials, architecture, platform limits, and migration guidance.
- **SECURITY-001**: Parser, canonical scene-codec, and FFI threat review.
- **SOAK-001**: Long-running, multi-window, device-lost, sleep, and wake tests.
- **RELEASE-001**: BOM, SBOM, NOTICE, signing, and reproducible release build.
- **COMPAT-001**: OS, JDK, and GPU compatibility matrix.

**Exit criteria:**

- All P0 and P1 defects are closed.
- The API compatibility checker passes.
- Documentation covers every core user path.
- License and provenance audits pass.
- Core artifacts contain no native payload or dependency.
- Public limitations and accepted differences are complete.
- Documentation distinguishes color gamut, transfer function, precision, and dynamic range and does not imply production HDR support on a backend that advertises only the SDR fallback.

### Post-stable A0–A4 — Android/iOS AOT extension track

This track begins only after the stable desktop release unless a separate project decision changes the priority. It does not block M0–M11. The track targets the unchanged Java 25 implementation; it does not establish ART compatibility as a prerequisite or fallback requirement.

- **A0 — Java 25 mobile AOT feasibility**: evaluate candidate AOT toolchains, initially GraalVM Native Image-derived tooling such as Gluon Substrate/GluonFX, against representative runtime, state, layout, text, software-rendering, RHI, and resource-loading code. Cover stable Java 25 APIs in actual use, including `MemorySegment` and `Arena`; closed-world analysis; static initialization; exceptions; garbage collection; threads; callbacks; code size; startup; debugging; and target packaging. Reject any path that requires rewriting or downgrading common source sets unless a replacement ADR explicitly changes the project baseline.
- **A1 — Android host baseline**: build a thin Activity/host shell, generated JNI/NDK boundary, host-driven lifecycle, normalized input, IME, clipboard, permissions, accessibility, assets, and software-renderer presentation on Android AArch64 devices and emulators.
- **A2 — iOS host baseline**: build generated Objective-C/C host glue, application and scene lifecycle, UIKit surface/event integration, text input, clipboard, permissions, accessibility, assets, and software-renderer presentation on iOS arm64 devices and supported simulators.
- **A3 — Mobile GPU and lifecycle completion**: add Android Vulkan and iOS Metal behind the existing RHI, map per-display color/dynamic-range capabilities and deterministic SDR fallback, then complete surface/device loss, background/foreground transitions, memory pressure, safe areas, scale changes, orientation, and CPU/GPU differential scenes. Enable native mobile HDR only after the shared optional HDR conformance profile passes.
- **A4 — Productization**: define isolated mobile artifacts, AOT and host-glue manifests, signing and store packaging, compatibility matrices, diagnostics, deployment samples, performance budgets, and reproducible package generation.

**Track exit criteria:**

- The ordinary Java 25 source sets compile without ART compatibility branches, source rewriting, or replacement of stable Java 25 APIs solely for mobile.
- Android and iOS run the representative application and portable subsystem suites through the selected AOT toolchain.
- Target launchers, generated host glue, and AOT output remain outside core and desktop JARs and are covered by explicit provenance and boundary tests.
- Software presentation is complete before Vulkan or Metal is accepted; GPU implementations pass the existing RHI and differential contracts.
- Lifecycle, input, IME, accessibility, permissions, packaging, signing, installation, compatibility, and performance gates pass on the defined device matrix.
- Mobile presentation preserves the common extended-range scene contract and reports color/HDR capability and fallback truthfully even when native HDR output remains disabled.
- Failure at any feasibility gate defers the affected mobile target without changing the Java 25 baseline or blocking desktop releases.

### Post-stable W0–W4 — Browser/Wasm extension track

This track begins only after the stable desktop release unless a separate project decision changes the priority. It does not block M0–M11.

- **W0 — Toolchain and host-binding feasibility**: evaluate Java 25 language/runtime coverage, closed-world linking, exceptions, garbage collection, code size, startup, browser debugging, generated Wasm imports, and content-security-policy constraints. Select the Java-to-Wasm toolchain only after this evidence exists.
- **W1 — Browser platform baseline**: implement `host/web` and `platform/web`, host-driven single-thread scheduling, canvas surface creation and color/dynamic-range capability reporting, normalized browser events, fetch-based assets, and deterministic software-renderer SDR presentation for scenes produced by the local Wasm runtime.
- **W2 — WebGPU backend**: implement asynchronous adapter/device acquisition, canvas format/color/tone-mapping negotiation, WGSL output, WebGPU resource mapping, render-graph validation, device/context loss, bounded compositor-animation sampling, extended-range preservation, and CPU/GPU differential scenes.
- **W3 — Browser integration**: implement clipboard/permissions, drag-and-drop, hidden text-input bridge, DOM semantics mirror, application/downloaded fonts and profiles, display gamut/dynamic-range and lifecycle/visibility changes, and optional Web Worker rendering.
- **W4 — Productization**: define Web artifacts, loader/bootstrap code, cache/version policy, browser compatibility matrix, diagnostics, deployment samples, performance budgets, reproducible packaging, and the logical browser-presentation conformance surface reusable by a future remote Web client.

**Track exit criteria:**

- The same representative application and portable subsystem tests run on JVM Headless and browser/Wasm.
- Browser execution uses no FFM, JNI, JNA, or native desktop module.
- Software/Canvas and WebGPU rendering have documented selection and fallback behavior.
- Single-thread execution is fully functional; workers improve capability or performance but are not required for correctness.
- IME and accessibility operate through target-specific bridges while HimariUI retains authoritative text, layout, paint, and semantics models.
- No JavaScript, DOM, WebGPU, or Wasm runtime object appears in common public APIs.
- Browser scene presentation passes the same canonical display-list, resource, and visual fixtures later consumed by the remote Web client; this does not require live transport support in W0–W4.
- Browser animation sampling passes the shared timestamp, phase-isolation, replacement-generation, and reduced-motion corpus; unsupported offload falls back to local UI-runtime sampling.
- Browser color presentation passes the shared encoding/extended-range/mapped-SDR corpus; native browser HDR remains capability-gated and never inferred from raw WebGPU texture support.
- Browser integration, security, compatibility, and performance gates pass on the defined matrix.

### Post-stable R0–R4 — Remote scene rendering and Web client track

This track begins only after the stable desktop release unless a separate project decision changes the priority. It does not block M0–M11 and may proceed even if compiling the full Java runtime to browser/Wasm remains infeasible. It reuses the canonical scene boundary and browser rendering semantics without placing networking in core modules.

- **R0 — Protocol and threat-model hardening**: freeze the supported-major-version scene, resource, color encoding/profile, luminance/content-light, requested/effective presentation, bounded layer-animation, clock-mapping, replacement-generation, semantics, interaction, capability, acknowledgement, and recovery records for the first remote experiment. Define quotas, required-feature negotiation, acknowledgement and backpressure semantics, stream epochs, full/delta rules, resource generations, redaction, fuzz corpora, cross-implementation fixtures, and compatibility policy. R0 also specifies and freezes the canonical layer-animation program encoding reserved by the first-stable scene format. Do not expose callbacks, arbitrary executable payloads, RHI, or native GPU commands.
- **R1 — Authoritative host and reference transport**: implement `remote/server`, a separate-process client, full and delta scene delivery, resource deduplication, acknowledgements, latest-wins frames, ordered non-droppable records, bounded backpressure, disconnect/reconnect recovery, and per-stage latency diagnostics. Keep transport, authentication, and session policy behind the remote extension rather than the scene codec.
- **R2 — Remote Web client**: decode the canonical protocol in a browser without the full HimariUI Java runtime, render through WebGPU with Canvas/software fallback, preserve negotiated extended-range color or apply the authoritative mapped-SDR fallback, sample negotiated bounded layer-animation programs, verify resource/profile hashes, request missing data and recovery snapshots, and pass the shared browser color, presentation, and animation timestamp corpora. Reuse W-track artifacts when practical but require protocol conformance rather than a particular implementation language.
- **R3 — Interaction, IME, semantics, and responsiveness**: return normalized input and presentation preferences/capabilities such as reduced motion, output gamut, dynamic range, precision, and headroom to the authoritative runtime, bridge text input through a controlled browser element, mirror correlated semantics into DOM accessibility nodes, handle focus and pointer capture, reconcile client color configuration and animation clocks/replacement generations against authoritative presentation snapshots, add other reversible client prediction only where evidence justifies it, and test latency, stale input, permissions, privacy, and reconnect behavior.
- **R4 — Productization**: define remote artifacts, standard secure transport adapters, authentication/authorization integration points, deployment topology, session lifecycle, observability, compatibility matrices, bandwidth/memory/latency budgets, reproducible browser assets, and optional pixel/video fallback policy.

**Track exit criteria:**

- A Java 25 application running on the JVM and, where supported, Native Image renders and accepts input through the remote Web client without requiring the application or full runtime to execute in the browser.
- Headless, local browser/Wasm where available, remote WebGPU, and remote Canvas/software consume the same scene conformance corpus and meet documented visual tolerances.
- The server remains authoritative for application state, layout, shaping, hit testing, focus, pointer capture, and IME; client prediction is bounded and recoverable.
- Remote animation sampling passes the shared timestamp corpus and deterministically yields to newer target/replacement generations or authoritative snapshots.
- Remote color negotiation preserves tagged source values, profiles, and luminance metadata and produces the same versioned mapped-SDR result as Headless when the client cannot accept the requested output intent.
- Full/delta recovery, resource lifetime, acknowledgements, backpressure, reconnect, capability negotiation, semantics, and input ordering pass deterministic and impaired-network tests.
- Decoders pass fuzzing and configured CPU, memory, bandwidth, resource, recursion, and retained-state limits; security and privacy threat reviews are complete.
- No component tree, Java runtime object, `MemorySegment` identity, FFM handle, target handle, RHI object, shader/pipeline command, or native GPU command appears in the wire format.
- Core modules remain free of networking, authentication, codecs, and remote-session policy; remote artifacts are optional and non-transitive by default.

---

## 23. Initial Issue Backlog

These issues are the initial project-board view of canonical work packages, not a second place to define their scope. They are grouped by their earliest owning milestone; ordering inside a group does not override `Starts after`. A fine-grained child issue that appears only here must name its milestone deliverable parent in project-board metadata. Do not add A0–A4, W0–W4, or R0–R4 work until the corresponding post-stable extension track is activated.

### 23.1 M0 bootstrap

- **GOV-001**: Select the project license, contribution rules, and ADR template. Do not reopen the coordinates and naming accepted by ADR-013.
- **ADR-BOOTSTRAP-001**: Materialize every accepted inline decision as a canonical file under `adr/`, with status, date, evidence, and replacement links; add decision milestones for unresolved entries.
- **REFERENCE-LOCK-001**: Create `REFERENCES.lock` and pin the edition, release, commit, or retrieval date of every rolling source used as normative evidence.
- **PROVENANCE-001**: Define `PROVENANCE.json` and its CI validator.
- **CONFORMANCE-POLICY-001**: Create `CONFORMANCE.md` plus the `PLATFORM_CONFORMANCE.yaml` schema for profile ownership, fixtures, tolerances, durations, budgets, and reviewed waivers; populate the initial M0 spike profiles.
- **BUILD-001**: Create the Gradle multi-project build, JPMS setup, and Java 25 toolchain according to ADR-013.
- **GUARD-FRAMEWORK-001**: Implement the staged `pure-java-guard` registry and activate gates 1–11 for every artifact and module present in M0.
- **FFI-SCHEMA-001**: Define the minimum canonical primitive, pointer, structure, function, and callback schema.
- **FFI-FFM-001**: Generate typed FFM downcalls and upcalls for the minimum schema and execute them on the JVM.
- **ABI-PROBE-001**: Define the C-probe JSON protocol and compare its output with generated Java layouts.
- **WAYLAND-GEN-001**: Generate interface/opcode/event decoding from minimal Wayland XML.
- **WIN-GEN-001**: Generate Win32 structures, functions, and `WndProc` bindings.
- **OBJC-GEN-001**: Generate typed selector and `objc_msgSend` bindings.
- **SPIKE-WAYLAND-001**: Open a Wayland window, receive events, enumerate available output/color-management information, clear a software surface, and present it.
- **SPIKE-VK-001**: Create a Vulkan device and swapchain, enumerate surface format/color-space pairs without assuming HDR extensions, and present a clear.
- **SPIKE-WIN-001**: Open a Win32 window, receive `WndProc` callbacks, and query the current DXGI output/Advanced Color description.
- **SPIKE-D3D12-001**: Create a D3D12 device and swapchain, map its effective SDR presentation configuration, and present a clear.
- **SPIKE-MAC-001**: Open an NSWindow, attach a `CAMetalLayer`, and query display color space/EDR headroom.
- **SPIKE-METAL-001**: Create a Metal device and queue, map the layer's effective SDR presentation configuration, and present a clear.
- **SPIKE-OBJC-BLOCK-001**: Exercise representative Objective-C block creation, invocation, lifetime, and exception containment, then record an ADR that either forbids blocks or defines their verified use policy.
- **NI-FFM-001**: Build and run at least one platform spike with Native Image using the same generated FFM bindings and prototype the required reachability/downcall/upcall metadata.

### 23.2 M1 runtime bootstrap

- **STATE-001**: Primitive/object state, atomic transactions, epochs, and external commits.
- **REACTIVE-001**: Dynamic producer/consumer graph, `DerivedState`, push/pull propagation, equality, liveness, and cycle detection.
- **HEADLESS-001**: Virtual display, window, event loop, and clock.
- **RUNTIME-SAMPLE-001**: Shared ordinary-Java comparison suite, frozen decision rubric, instrumentation, and reporting format.
- **RUNTIME-SPIKE-GROUPED-001**: Explicit grouped-recomposition prototype with positional memory and no compiler assistance.
- **RUNTIME-SPIKE-ONESHOT-001**: One-shot owner prototype with fine-grained bindings and explicit structural control flow.
- **RUNTIME-SPIKE-HYBRID-001**: Fine-grained binding prototype with small rerunnable structural scopes.
- **RUNTIME-ADR-001**: Accept ADR-023 explicit grouped recomposition, ADR-020 scoped current-measure materialization, and ADR-021 declared error-boundary containment from reviewed evidence.
- **STRUCTURE-001**: Implement the selected branch, keyed-collection, identity, local-state, and failure-recovery semantics.
- **ANIM-CORE-001**: Animation-transaction propagation, model/presentation separation, manual-clock sampling, presentation epochs, allocation-free scalar adapters, and reference tween/spring retargeting.
- **RUNTIME-MBT-001**: Model-based differential harness running randomized operation sequences against a naive recompute-everything reference evaluator.
- **TRACE-001**: Initial deterministic trace format.
- **SAMPLE-001**: Build a deterministic Headless counter sample and golden using the selected runtime model.

### 23.3 M2 layout and interaction bootstrap

- **LAYOUT-001**: Constraints, measurement, and placement.
- **UI-BOOTSTRAP-001**: Private, non-stable interaction and window-chrome primitives used by architecture samples before the public controls API exists; build them only from M2 layout, input, focus, and semantics contracts.

### 23.4 M3 graphics bootstrap

- **DL-001**: Canonical pointer-free display-list encoding and resource references.
- **ANIM-SCENE-001**: Bounded retained-layer animation programs as immutable framework values, shared clock mappings, replacement generations, and Headless/software/render-executor reference sampling for compositor-eligible properties; the canonical program encoding is an R0 extension of the scene format.
- **COLOR-MODEL-001**: Extensible tagged color encodings, finite extended-range values, first-stable named spaces, linear-light alpha/blending rules, and encoding/pixel-format/alpha separation. Non-sRGB encodings, luminance metadata, and ICC payloads pass through values, display lists, and scenes as validated tagged data.
- **COLOR-SDR-REF-001**: Deterministic reference conversion among sRGB, linear-sRGB, Display-P3, linear Display-P3, and the first-stable extended-linear working encoding; D50/D65 chromatic adaptation; linear-light premultiplied composition; and a simple explicitly versioned extended-range/WCG-to-SDR mapping suitable for early Headless and platform fallback tests.
- **PATH-001**: Implement `PathBuilder`, bounds, and reference flattening.
- **RASTER-001**: Implement scalar rectangle and path coverage.
- **PNG-001**: Implement a pure-Java PNG writer for explicitly mapped SDR golden output.
- **GOLDEN-001**: Golden infrastructure and reviewer.
- **SCENE-CODEC-001**: Canonical `SceneEnvelope`, ordered snapshot/delta and resource-generation state machine, semantics/input correlation, configured limits, and offline replay codec.
- **GUARD-SCENE-001**: Activate `verifySceneCodec` with canonical, malformed, limit, and fuzz fixtures.
- **PERF-BASE-001**: Check in benchmark scenes, baseline-machine descriptors, measurement commands, and initial regression budgets for M3 and later work.
- **FUZZ-001**: Add starter Jazzer targets for fonts, color profiles/LUTs, paths, and canonical scene decoding.

### 23.5 M4 text bootstrap

- **FONT-READER-001**: Implement a checked big-endian font reader.
- **FONT-SFNT-001**: Implement table directories, metrics, and `cmap`.
- **HB-ORACLE-001**: Build a HarfBuzz JSON runner.
- **FT-ORACLE-001**: Build a FreeType outline/bitmap JSON runner.
- **UNICODE-001**: Add the ICU4J provider and Unicode conformance-data harness.
- **PORT-POLICY-001**: Create `DIFFERENCE_POLICY.md` from recorded Oracle baselines and freeze the M8 differential thresholds before M8 work begins.

Every issue must use the standard work-package template or Port Unit template and include an executable acceptance command.

---

## 24. Definition of Done

### 24.1 General feature

- Specification and non-goals are explicit.
- Unit tests cover success and failure paths.
- No native dependency is implicit or undocumented.
- Public APIs have accurate Javadoc.
- Debug diagnostics expose relevant state.
- No sustained hot-path allocation regression is introduced.
- The feature is testable in Headless where applicable.
- Platform capabilities and fallbacks are documented.
- Relevant ADR and architecture documentation is updated.

### 24.2 Ported feature

Meet the general DoD and all of the following:

- Pin the upstream version or commit.
- Record license and provenance.
- Make the Oracle runner reproducible.
- Complete upstream symbol mapping.
- Meet the differential-corpus threshold.
- Record every accepted difference with rationale and a fixture.
- Add a fuzz target.
- Retain the reference implementation.
- Allow the optimized path to be disabled and run both paths in differential mode.

### 24.3 Desktop platform or FFM feature

- ABI probes pass.
- Native callbacks are exception-safe.
- Ownership and lifetime tests pass.
- Missing-symbol and older-OS fallbacks are defined.
- JVM and Native Image reuse the same generated FFM bindings and pass separate smoke tests.
- No runtime FFI provider registry or second system-call implementation exists.
- Loaded system libraries are reported.
- No native payload is shipped.
- Repeated create/destroy soak tests pass.

### 24.4 Rendering feature

- A software reference implementation exists.
- Display-list bounds and culling tests pass.
- Exact software goldens pass.
- GPU differential tests and a tolerance policy exist.
- Resource-lifetime, declared-usage, pass-dependency, and backend-synchronization tests pass.
- Debug labels are present.
- A benchmark scene exists.
- Device-lost and fallback behavior are defined.

### 24.5 Control

- Pointer, keyboard, and focus behavior are complete.
- IME behavior is complete where applicable.
- Semantics and accessibility are complete.
- RTL, high contrast, and reduced motion are covered.
- Theme tokens are used.
- Interaction tests and goldens pass.
- Disabled, read-only, and error states are defined.

### 24.6 Animation feature

- Model targets and presentation values remain distinct, and sampling never writes application state.
- Transaction precedence, interruption, replacement, cancellation, reversal, completion outcomes, and failure cleanup are specified and tested.
- The manual-clock corpus covers exact values and velocities across variable sample rates, skipped frames, gesture handoff, and retargeting.
- Phase-impact tests prove that each property performs no earlier work than declared; compositor-only motion executes no structure, layout, or paint callbacks.
- Hit testing, semantics geometry, focus, lifecycle disposal, and resource ownership remain correct throughout value and structural transitions.
- Reduced-motion substitution preserves final state and exactly-once completion behavior.
- Trace/replay reproduces presentation values and images at declared timestamps.
- Steady-state allocation, scheduling, 60/120 Hz frame budgets, and idle frame-request behavior meet the established thresholds.

### 24.7 Color and presentation feature

- Gamut/primaries, white point, transfer function, numeric range, scene/display interpretation, luminance/reference white, pixel format, alpha type, and surface capability remain explicit and are never collapsed into an HDR Boolean or native enum.
- Finite extended-range and out-of-gamut components survive values, interpolation, display lists, scenes, traces, pixel buffers, and reference rendering until an explicit mapped/quantized boundary; non-finite values are rejected.
- Working-space, alpha, blending, filtering, interpolation, chromatic-adaptation, gamut-mapping, tone-mapping, and quantization semantics are specified and tested independently.
- Requested/effective presentation configuration, capability generation, mapping ownership, and fallback reasons are observable and replayable; display changes do not alter application color values.
- ICC/profile and metadata parsing is bounded, fuzzed, content-addressed, and independent of ambient system profiles during deterministic tests.
- A backend advertises only modes whose actual format/encoding/precision, display-change, mapping, capture, and fallback conformance profiles pass. Unverified HDR remains disabled without weakening SDR support.
- Hardware-unavailable tests use programmable Headless surfaces and exercise the same selection and fallback contract.

### 24.8 Future local browser/Wasm feature

- Single-thread, host-driven execution passes before worker acceleration is considered complete.
- Asynchronous initialization, permissions, clipboard, resource loading, and GPU acquisition cover success, denial, cancellation, and unavailability.
- Generated host bindings validate linear-memory bounds, object handles, callback IDs, and encodings.
- No FFM dependency or target runtime object leaks into common APIs.
- WebGPU and Canvas/software paths have differential tests and documented fallback behavior.
- Canvas/WebGPU color configuration, extended-range preservation, gamut/dynamic-range capability changes, and deterministic SDR fallback pass the shared color corpus; HDR is advertised only when the browser path passes its optional profile.
- IME and DOM semantics bridges pass browser integration and accessibility tests.
- Browser lifecycle, device/context loss, detached surfaces, and aborted fetches have defined recovery or shutdown behavior.
- Packaging is reproducible and passes the supported browser/security-policy matrix.

### 24.9 Future Android/iOS AOT feature

- The selected toolchain compiles the normal Java 25 source sets; no ART-compatible common fork, source rewrite, or compatibility substitution is required.
- Every stable Java 25 API family used by the included modules has a representative compile-and-run test on each supported mobile target.
- `MemorySegment`/`Arena` core behavior and target host-call/callback behavior have separate tests and diagnostics.
- Generated launcher and JNI/NDK or Objective-C/C boundaries validate signatures, ownership, lifetimes, thread attachment, exception containment, and untrusted inputs.
- Mobile AOT output and host glue remain isolated from core and desktop JARs and have complete provenance manifests.
- Lifecycle, input, IME, accessibility, permissions, software presentation, GPU differential behavior, color/dynamic-range capability changes and SDR fallback, suspend/resume, surface/device loss, and memory pressure pass on the supported matrix.
- Packaging is reproducible before signing; installation, signing, and store-oriented validation procedures are documented and repeatable.

### 24.10 Future remote-rendering feature

- The canonical scene protocol is versioned, pointer-free, deterministic, bounded, and independent of Java object layout, implementation language, FFM, RHI, and native GPU APIs.
- Full snapshots, deltas, resource manifests, content hashes, acknowledgements, reclamation, required-feature negotiation, stream epochs, recovery, and backpressure pass cross-process and cross-implementation tests.
- Decoders reject malformed or unsupported input before unbounded allocation or work and pass fuzzing plus configured CPU, memory, bandwidth, recursion, decompression, and retained-resource limits.
- Remote WebGPU and Canvas/software output pass the shared scene corpus against Headless references; optional pixel/video fallback is tested and documented separately.
- Color/profile resources, extended-range values, luminance metadata, client output capabilities, mapping ownership, and requested/effective configurations negotiate and recover deterministically; unsupported HDR/WCG produces the authoritative mapped fallback or a required-feature rejection, never relabeling.
- Normalized input, focus, pointer capture, IME, semantics, lifecycle, configuration changes, disconnect/reconnect, stale events, and redaction preserve authoritative server state and defined ordering.
- Transmitted layer-animation programs are bounded and declarative, carry explicit clock and replacement generations, contain no callback or executable payload, reconcile with authoritative snapshots, and pass the shared animation timestamp corpus.
- Authentication, authorization, encryption, transport, discovery, codecs, and session policy remain isolated in optional remote artifacts and use documented standard mechanisms.
- Per-stage production, encoding, transport, decode, queue, render, and presentation latency plus bandwidth and memory diagnostics are available and meet the supported-profile budgets.

---

## 25. Risk Register

| Risk | Impact | Early signal | Mitigation |
|---|---|---|---|
| Total scope exceeds sustained execution capacity | The project stalls before any release | Milestones accumulate partially met exit criteria, or V0/V1 gains requirements across plan revisions | Enforce the contract-versus-implementation rule from Section 0, keep the V0/V1 profiles fixed and minimal, run the parallel milestone tracks, and ship the pre-stable standalone artifacts from Section 3.4 for early feedback |
| Text-stack effort dominates the schedule | M4–M9 and the stable release slip by quarters | Shaping, hinting, or fallback port units slip while other tracks idle | Publish the text stack as a pre-stable standalone artifact, keep script coverage explicitly tiered, track port-unit velocity from M4 onward, and re-scope stable-release script coverage by ADR if velocity is insufficient |
| macOS Objective-C ABI or block APIs cannot be covered safely without a shim | Platform blocker | Unstable M0 callbacks or completion handlers | Generate typed message sends and dynamic delegates; prefer `_f`/selector APIs; isolate block ABI in its own spike |
| D3D12 shader tooling becomes too heavy | Windows GPU delay | Runtime DXC becomes necessary | Keep a fixed shader set, compile offline in release CI, ship versioned blobs, and define HLSL/MSL source fallback policy |
| Native Image FFM registrations are incomplete | AOT execution failure | `MissingForeignRegistrationError` | Generate metadata from the schema, test the full signature set, and generate static backend registries |
| JNA/LWJGL Oracle dependencies leak into production | Distribution constraint violation | They appear in a standard sample or published runtime graph | Isolate test configurations, ban dependencies in CI, and audit release graphs |
| Wayland protocol fragmentation creates inconsistent behavior | Linux feature gaps | Required extensions are missing on compositors | Model capabilities, prefer stable protocols, document fallbacks, and test a real compositor matrix |
| XKB or IME complexity is underestimated | Incorrect keyboard/text behavior | Non-US layouts or compose keys fail | Port XKB in Java, compare with xkbcommon, and build IME corpora early |
| Large AI-assisted HarfBuzz/FreeType ports drift semantically | Broken text | Screenshots appear plausible while clusters or metrics differ | Use small port units, field-level Oracles, official corpora, reference paths, and fuzzing |
| CPU and GPU rendering semantics drift | Cross-platform graphics differences | Clip/blend edge differences accumulate | Keep software normative, compare complete scenes and individual operations, and enforce difference budgets |
| Color APIs or scene/RHI formats harden around 8-bit sRGB or a Boolean HDR flag | Wide-gamut/HDR lockout, clipped highlights, hue shifts, and incompatible wire/API changes | Components are clamped to `[0, 1]`, color spaces become a closed enum, textures lose encoding metadata, or a backend advertises HDR from format support alone | Enforce ADR-019 through `COLOR-MODEL-001`, `COLOR-SDR-REF-001`, and `COLOR-REF-001`, preserve tagged extended-range floats and profiles through canonical codecs, separate surface format from encoding/luminance, require deterministic mapped-SDR fallback, and capability-gate every native HDR mode |
| Optimization starts before behavior is verifiable | Long-term maintenance cost | Dense bit tricks or parallel code appear without a reference path | Require reference-first gates and benchmark evidence |
| `java.desktop` enters accidentally | Headless/pure-Java goal failure | Utility code imports ImageIO or Color | Enforce JPMS and `jdeps` gates and use framework-owned types |
| The ABI schema is wrong | JVM crash or memory corruption | Intermittent platform crashes | Run C probes, generator tests, SDK matrices, and debug fail-fast checks |
| Native callbacks reenter the UI runtime | State-tree corruption | Nested dispatch during resize, IME, or modal loops | Add reentrancy guards, queue events, and commit through transactions |
| Font, image, or color-profile/LUT parsers permit denial of service | Security failure | Extreme memory or CPU use | Enforce dimensional/stage/work limits, checked arithmetic, fuzzing, and timeouts |
| Required system libraries are absent | Linux startup failure | Vulkan or Wayland libraries cannot be resolved | Report capabilities and fall back to software, Headless, or X11 where documented |
| The compiler-free structural runtime has unacceptable ceremony | Public API lock-in and poor usability | Grouped samples require pervasive keys or boundaries, or signal samples require pervasive deferred getters and control-flow wrappers | Complete all M1 runtime prototypes with ordinary Java, publish ceremony and execution metrics, and select the production model before building widgets; treat optional tooling only as a later enhancement |
| Fine-grained dependencies introduce glitches, cycles, or owner leaks | Inconsistent UI or unbounded memory growth | Diamond, dynamic-branch, equality, or disposal tests observe intermediate values or retained consumers | Use two-phase push/pull propagation, semantic versions, cycle diagnostics, explicit ownership, and adversarial graph/liveness tests |
| Animation is implemented as curves without transaction, presentation, or lifecycle semantics | Visible jumps, incorrect input geometry, leaked exit nodes, inaccessible motion, and public API lock-in | Retargeting restarts from stale values, gestures lose velocity, animation writes `State` every frame, or opacity triggers layout/paint | Enforce ADR-018 in M1, keep model and presentation values separate, test atomic timestamp sampling and replacement outcomes, require phase metadata, and complete structural/reduced-motion conformance before control stabilization |
| Published modules are too granular | Dependency confusion | Users must understand internal module boundaries | Use ADR-013, the BOM, and `himari-desktop`; keep fine-grained artifacts out of the default user surface |
| Mobile AOT tooling cannot compile the unchanged Java 25 core or required stable APIs | Android/iOS targets are delayed | A0 requires an older common source set, rejects `MemorySegment`/`Arena`, or needs source rewriting | Keep mobile post-stable and feasibility-gated; evaluate updated or alternative AOT tooling and defer the target rather than lowering the Java baseline or adding ART compatibility constraints |
| Mobile host integration requires a separate native ABI path | Lifecycle, input, graphics, or accessibility gaps | A0–A2 cannot express required Android/iOS callbacks through the candidate toolchain | Generate narrow target host glue outside desktop FFM and core artifacts; validate it independently and do not turn it into a runtime provider SPI |
| The scene format becomes a disguised RHI or permanent public wire ABI | Backend lock-in and unsafe remote compatibility | Encoded records contain GPU resources, synchronization, pointers, or backend commands, or every internal change is treated as wire-compatible forever | Keep the protocol at immutable scene/display-list semantics, reject target handles in CI, negotiate required features, and promise compatibility only under an explicit version policy |
| Remote rendering cannot meet interaction latency or bandwidth budgets | Unusable remote UI | Queues grow, intermediate frames arrive stale, or input-to-present latency is dominated by round trips | Use latest-wins scene frames, ordered resource/control records, bounded backpressure, content-addressed reuse, recovery snapshots, per-stage telemetry, and only evidence-backed reversible client prediction |
| Remote codecs or diagnostics expose an untrusted attack and privacy surface | Resource exhaustion or data disclosure | Fuzzing finds unbounded work, clients can name runtime objects, or traces expose private text | Keep decoders bounded and pointer-free, generation-check all capabilities, isolate authentication/session policy, redact sensitive data, and complete a dedicated R-track threat review |
| The Java-to-Wasm toolchain cannot support the required Java 25/runtime subset | Web track blocked or fragmented | W0 needs source rewrites or incompatible runtime substitutions | Keep the Web track post-stable, define a portable-core profile, and select a toolchain only after representative feasibility tests |
| Desktop threading or synchronous APIs leak into common contracts | Browser deadlocks or unusable APIs | Web prototypes require blocking, threads, or `SharedArrayBuffer` | Require host-driven scheduling, asynchronous capabilities, and a correct single-thread execution path in ADR-014 |
| WebGPU capability and browser behavior vary | Rendering gaps or unstable performance | Adapter acquisition, limits, or presentation differs across the browser matrix | Use capability tiers, logical resource dependencies, Canvas/software fallback, and a real browser/WebGPU matrix |
| Browser system fonts are unavailable as font bytes | Incorrect fallback or inconsistent text | The backend attempts to delegate shaping/rasterization to browser text APIs | Use bundled/application/fetched fonts and report system-font discovery as unavailable |
| Wasm/JavaScript host glue expands the attack surface | Security or CSP failures | Dynamic code generation, unchecked linear-memory offsets, or ambient browser access appears | Generate narrow imports, validate every boundary value, avoid dynamic evaluation, and test strict content-security policies |
| Unhinted text looks inferior on low-DPI Windows | User-perceived quality gap versus DirectWrite | Small-ppem Latin/CJK screenshots compare poorly on 96–120 DPI displays | Use gamma-correct grayscale AA, embedded bitmap strikes, and optional vertical grid fitting; ship the hinting fidelity profile when the TT/CFF VM ports pass their FreeType gates |
| Popup semantics are discovered late | M9 menu/tooltip/dialog rework against missing platform features | `POPUP-001` needs surface roles absent from M5–M7 deliverables | Deliver popup surfaces and dismissal policy per platform milestone (`WAYLAND-POPUP-001`, `WIN-POPUP-001`, `MAC-POPUP-001`) with a documented in-window overlay fallback |

---

## 26. Decision Register and Working Defaults

ADR-001 through ADR-023 are accepted and materialized under `adr/`. ADR-015 defines the fine-grained value and phase graph; ADR-023 resolves its structural subdecision in favor of explicit grouped recomposition. ADR-020 fixes scoped current-measure materialization, and ADR-021 fixes declared error-boundary containment. Their checked M1 evidence and reproducible selection profile are canonical inputs to `STRUCTURE-001`.

The remaining entries below must not block M0 unless marked accepted. Use a working default only until the decision milestone recorded by `ADR-BOOTSTRAP-001`; no default may remain undated.

| Decision | Status/default |
|---|---|
| Coordinates and module naming | **Accepted:** ADR-013 (`org.glavo.himari` / `himari-*`) |
| Project license | Apache-2.0 for original modules; preserve upstream licenses and NOTICE files for ports |
| Build system | Gradle multi-project with Kotlin DSL or Java convention plugins |
| Pre-stable standalone artifacts | Text/font stack, software rasterizer, and ABI tooling may ship as `0.x` artifacts under Section 3.4 without creating API-stability obligations |
| Minimum OS versions | Decide after M0 capability spikes; do not hard-code them into public APIs |
| First complete platform | Linux Wayland plus Vulkan, subject to M0 evidence |
| Unicode provider | ICU4J |
| Default renderer | GPU when available, otherwise software; allow explicit override |
| Desktop native-access mechanism | FFM only; no provider SPI |
| Native Image system calls | Reuse the JVM FFM bindings and ABI tests |
| JNA | Oracle/test tooling only; no production backend or artifact |
| Future Android/iOS runtime | Java 25 AOT first; initial candidate is GraalVM Native Image-derived tooling; full ART execution is not a compatibility baseline |
| Future Android/iOS Java compatibility | Compile the normal Java 25 implementation unchanged; defer a target rather than lower the baseline or prohibit stable Java 25 features |
| Future Android/iOS host access | Generated, isolated JNI/NDK or Objective-C/C glue plus target launchers; never a desktop FFM replacement or runtime provider |
| Future local browser/Wasm host access | Generated Wasm imports plus isolated JavaScript/browser bindings under ADR-014; never an FFI provider |
| Future local browser renderer | WebGPU with Canvas/software fallback |
| Future local browser execution | Correct on the browser event context without workers; Web Workers are optional acceleration/capability |
| Future local browser fonts/resources | Bundled, application-provided, or fetched bytes; system catalogs are optional |
| Java-to-Wasm toolchain and Web artifact names | Defer to W0 feasibility evidence |
| Core scene/process boundary | Canonical versioned `SceneEnvelope`, color/profile/presentation, resource, semantics, and input codecs; in-process mailbox remains the default |
| Future remote rendering level | Stream scene/display-list semantics, tagged color/luminance intent, and content-addressed resources; never component trees, RHI objects, or native GPU commands |
| Future remote authority | JVM or Native Image host owns state, layout, shaping, hit testing, focus, and IME; browser prediction is optional and recoverable |
| Future remote Web client | WebGPU with Canvas/software fallback; independent of compiling the full Java runtime to Wasm and logically conformant with the local browser renderer |
| Public coordinate precision | `float` logical pixels |
| Text indices | UTF-16 offsets plus grapheme/cluster APIs |
| Value reactivity | **Accepted:** ADR-015 fine-grained producer/consumer graph with push invalidation and lazy pull recomputation |
| Structural reactivity | **Accepted:** ADR-023 explicit grouped recomposition for topology, retaining ADR-015 fine-grained value and phase consumers |
| Animation semantics | **Accepted:** ADR-018 transaction-scoped model/presentation separation, atomic presentation epochs, velocity-preserving compatible retargeting, phase-aware execution, and explicit structural transitions |
| Color and HDR semantics | **Accepted:** ADR-019 structured extensible encodings, finite extended-range values, explicit luminance/presentation capability, linear-light reference composition, and deterministic mapped fallback |
| First-stable HDR output | Optional and disabled per backend until its dedicated conformance profile passes; tagged HDR reference math, codecs, Headless capability simulation, and truthful SDR fallback are mandatory |
| Reference working color | Extended-linear floating point, initially extended-linear sRGB unless corpus evidence selects another ADR-019-compatible default |
| Compiler plugin | Optional; never a correctness or baseline-usability dependency |
| Arbitrary user shaders | Defer until after the stable release |
| Linux keyboard | Pure-Java XKB target; system xkbcommon is transitional or an Oracle only |
| X11 | Optional separately versioned compatibility profile; Wayland is the required Linux profile for first stable |
| First-stable script coverage | Tier-1 blocking set (default/Latin/Greek/Cyrillic, Arabic, Hebrew, Hangul, Thai/Lao); Tier-2 (Indic, USE, Khmer, Myanmar, Tibetan) is a separately versioned shaping profile |
| Hinting | Unhinted default with embedded-bitmap and vertical-snap mitigations; TrueType/CFF hinting VMs form an optional separately versioned fidelity profile |
| Measure-time structural materialization | **Accepted:** ADR-020 explicitly scoped current-measure reconciliation of semantic-keyed descendants |
| Application error containment | **Accepted:** ADR-021 nearest declared boundary, fresh fallback attempt, explicit retry, aggregated cleanup, and per-window root isolation |
| UI execution context | **Accepted:** ADR-022 platform main thread plus modal-loop continuity and per-application graph / per-window scheduler scoping |
| AWT/Swing embedding | Optional post-stable `himari-interop-awt` with its own allowlist; never a core or BOM-default dependency |

---

## 27. Increment Ladder

Use three named demonstrable increments instead of one “first” increment that silently depends on most of the roadmap. These increments are cross-workstream integration cuts, not additional milestone gates.

### V0 — Headless architecture seed

V0 is complete after the required M1–M4 work packages pass when:

1. `CounterApp` runs on Headless with the selected structural runtime, layout, normalized input, focus, semantics, and private `UI-BOOTSTRAP-001` interaction primitives.
2. Pointer and keyboard injection activate the counter, focus is observable, and the semantics snapshot exposes the action without a public M9 control dependency.
3. Text uses HimariUI's SFNT parser, basic shaper, and grayscale glyph rasterizer.
4. The pure-Java software renderer produces a deterministic SDR PNG and a separate extended-linear numeric capture.
5. The display list and one full `SceneEnvelope` round-trip canonically through offline replay.
6. A counter change exercises animation-transaction and model/presentation separation, and the Headless manual clock reproduces declared intermediate timestamps.
7. Runtime-core execution and the sample load no native library, and the active M0/M3 guard set passes.

### V1 — First desktop vertical slice

V1 is the first public desktop architecture demonstration and coincides with M5 completion:

1. The same `CounterApp` runs in a real Linux Wayland session through generated FFM bindings.
2. The application can select software or Vulkan rendering, and the same scene passes the declared CPU/GPU differential profile.
3. Window lifecycle, scaling, input, frame callbacks, bootstrap client-side decorations, and deterministic SDR presentation pass the Wayland profile.
4. A compositor-eligible property is sampled by the render executor without application callbacks or unnecessary structure/layout/paint work.
5. Native Image runs the same FFM path on the selected M5 platform.
6. Published-artifact and runtime-dependency guards prove that no native library is bundled.

### V2 — Cross-platform architecture proof

V2 is complete only after the relevant M6, M7, and M10 work packages pass:

1. The same application runs on Linux, Windows, macOS, and Headless through the required software and native GPU profiles.
2. Each desktop platform reaches system APIs only through the generated FFM path and reports truthful presentation capabilities and fallback reasons.
3. The inspector displays reactive owners, structural scopes, mounted elements, layout, layers, semantics, animation, and effective presentation-color state.
4. The counter scene, declared color/profile and other resources, presentation configuration, correlated semantics, normalized input, and sampled presentation epochs replay in a fresh process without ambient fonts or producer objects.
5. JVM and Native Image packaging matrices pass for the profiles owned by M10.

M11 and the remaining M9 release profiles are still required for the first stable desktop release; V2 is an architecture proof, not the stable-release definition. Android/iOS AOT, browser/Wasm, and live remote rendering are intentionally absent from V0–V2 and remain in the post-stable A0–A4, W0–W4, and R0–R4 tracks.

---

## 28. Primary References

Use these sources to confirm API status, derive behavioral specifications, and build Oracles. A referenced design is not permission to copy its public abstractions. `REFERENCES.lock` records the reviewed specification edition, release, repository commit, or retrieval date for every rolling `latest`, `main`, or unversioned URL used as normative evidence; this list remains the human-readable index.

### Java and GraalVM

- [JEP 454: Foreign Function & Memory API](https://openjdk.org/jeps/454)
- [Java 25 Foreign Function and Memory API Guide](https://docs.oracle.com/en/java/javase/25/core/foreign-function-and-memory-api.html)
- [Java 25 Restricted Methods and Native Access](https://docs.oracle.com/en/java/javase/25/core/restricted-methods.html)
- [JEP 508: Vector API (Tenth Incubator)](https://openjdk.org/jeps/508)
- [GraalVM Native Image Reachability Metadata](https://www.graalvm.org/latest/reference-manual/native-image/metadata/)
- [GraalVM Native Image C API](https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/C-API/)
- [GraalVM Native Image FFM Support](https://www.graalvm.org/jdk25/reference-manual/native-image/native-code-interoperability/ffm-api/)
- [Gluon Documentation](https://docs.gluonhq.com/)
- [Gluon Substrate](https://github.com/gluonhq/substrate)

### Reactive systems and declarative UI

- [A Survey on Reactive Programming](https://dl.acm.org/doi/10.1145/2501654.2501666)
- [Angular Signals Implementation](https://github.com/angular/angular/blob/main/packages/core/primitives/signals/README.md)
- [SolidJS State Management](https://docs.solidjs.com/guides/state-management)
- [Svelte Lifecycle Hooks](https://svelte.dev/docs/svelte/lifecycle-hooks)
- [Svelte Derived State and Push-Pull Propagation](https://svelte.dev/docs/svelte/%24derived)
- [Vue Core Releases and Vapor Mode Status](https://github.com/vuejs/core/releases)
- [TC39 Signals Proposal](https://github.com/tc39/proposal-signals)
- [Xilem: an Architecture for UI in Rust](https://raphlinus.github.io/rust/gui/2022/05/07/ui-architecture.html)
- [How Compose Works](https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/runtime/design/how-compose-works.md)
- [Thinking in Compose](https://developer.android.com/develop/ui/compose/mental-model)
- [Compose UI Architecture](https://developer.android.com/develop/ui/compose/architecture)
- [Jetpack Compose Phases](https://developer.android.com/develop/ui/compose/phases)
- [Compose Layout Basics](https://developer.android.com/develop/ui/compose/layouts/basics)
- [Flutter Architectural Overview](https://docs.flutter.dev/resources/architectural-overview)

### Animation and motion

- [SwiftUI `Transaction`](https://developer.apple.com/documentation/swiftui/transaction)
- [SwiftUI `withAnimation` Completion Semantics](https://developer.apple.com/documentation/SwiftUI/withAnimation%28_%3AcompletionCriteria%3A_%3Acompletion%3A%29)
- [SwiftUI `Animatable`](https://developer.apple.com/documentation/swiftui/animatable)
- [SwiftUI `VectorArithmetic`](https://developer.apple.com/documentation/swiftui/vectorarithmetic)
- [SwiftUI `CustomAnimation`](https://developer.apple.com/documentation/swiftui/customanimation)
- [Explore SwiftUI Animation](https://developer.apple.com/videos/play/wwdc2023/10156/)
- [Animate with Springs](https://developer.apple.com/videos/play/wwdc2023/10158/)
- [Controlling the Timing and Movements of SwiftUI Animations](https://developer.apple.com/documentation/swiftui/controlling-the-timing-and-movements-of-your-animations)
- [SwiftUI `Transition`](https://developer.apple.com/documentation/swiftui/transition)
- [SwiftUI `matchedGeometryEffect`](https://developer.apple.com/documentation/swiftui/view/matchedgeometryeffect%28id%3Ain%3Aproperties%3Aanchor%3Aissource%3A%29)

### Color management and HDR

- [ITU-R BT.2020: UHDTV Parameter Values](https://www.itu.int/rec/R-REC-BT.2020/en)
- [ITU-R BT.2100: HDR Television](https://www.itu.int/rec/R-REC-BT.2100/en)
- [CSS Color Module Level 4](https://www.w3.org/TR/css-color-4/)
- [CSS Color HDR Module Level 1](https://www.w3.org/TR/css-color-hdr-1/)
- [WebGPU Color Spaces, Canvas Configuration, and Tone Mapping](https://www.w3.org/TR/webgpu/)
- [ICC Specifications, including v4 and iccMAX](https://www.color.org/icc_specs2.xalter)
- [Windows Advanced Color](https://learn.microsoft.com/en-us/windows/win32/direct3darticles/high-dynamic-range)
- [DXGI Swapchain Color-Space Selection](https://learn.microsoft.com/en-us/windows/win32/api/dxgi1_4/nf-dxgi1_4-idxgiswapchain3-setcolorspace1)
- [Apple Metal HDR Color-Space Configuration](https://developer.apple.com/documentation/metal/using-color-spaces-to-display-hdr-content)
- [Vulkan Extended Swapchain Color Spaces](https://registry.khronos.org/vulkan/specs/latest/man/html/VK_EXT_swapchain_colorspace.html)
- [Vulkan HDR Metadata](https://registry.khronos.org/vulkan/specs/latest/man/html/VK_EXT_hdr_metadata.html)

### Rendering architecture

- [Impeller Rendering Engine](https://docs.flutter.dev/perf/impeller)
- [Firefox Rendering Overview and WebRender Display Lists](https://firefox-source-docs.mozilla.org/gfx/RenderingOverview.html)
- [Chromium GPU-Accelerated Compositing and GPU Process](https://www.chromium.org/developers/design-documents/gpu-accelerated-compositing-in-chrome/)
- [Remote Desktop Graphics Pipeline Extension](https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpegfx/da5c75f9-cd99-450c-98c4-014a496942b0)
- [Vello GPU Vector Renderer](https://github.com/linebender/vello)

### Text, fonts, and Unicode

- [FreeType Documentation](https://freetype.org/freetype2/docs/documentation.html)
- [FreeType Glyph Outlines](https://freetype.org/freetype2/docs/glyphs/glyphs-2.html)
- [FreeType Outline Processing](https://freetype.org/freetype2/docs/reference/ft2-outline_processing.html)
- [HarfBuzz Manual](https://harfbuzz.github.io/)
- [HarfBuzz Shaping Concepts](https://harfbuzz.github.io/shaping-concepts.html)
- [Fontations: Memory-Safe Font Parsing (read-fonts/skrifa)](https://github.com/googlefonts/fontations)
- [Parley Rich Text Layout](https://github.com/linebender/parley)
- [ICU4J Documentation](https://unicode-org.github.io/icu/userguide/icu4j/)
- [ICU Boundary Analysis](https://unicode-org.github.io/icu/userguide/boundaryanalysis/)
- [Unicode Bidirectional Algorithm](https://www.unicode.org/reports/tr9/)
- [Unicode Line Breaking Algorithm](https://www.unicode.org/reports/tr14/)
- [Unicode Text Segmentation](https://www.unicode.org/reports/tr29/)

### SDL, platforms, and GPU APIs

- [SDL3 Event Queue](https://wiki.libsdl.org/SDL3/CategoryEvents)
- [SDL3 GPU API](https://wiki.libsdl.org/SDL3/CategoryGPU)
- [Win32 API Reference](https://learn.microsoft.com/en-us/windows/win32/api/)
- [Direct3D 12 Programming Guide](https://learn.microsoft.com/en-us/windows/win32/direct3d12/directx-12-programming-guide)
- [Apple Objective-C Runtime](https://developer.apple.com/documentation/objectivec)
- [Apple Metal](https://developer.apple.com/documentation/metal)
- [Android Runtime Architecture](https://developer.android.com/guide/platform)
- [Android Vulkan Guide](https://developer.android.com/ndk/guides/graphics/)
- [Wayland Architecture](https://wayland.freedesktop.org/docs/book/Architecture.html)
- [Wayland Message Definition Language](https://wayland.freedesktop.org/docs/book/Message_XML.html)
- [Vulkan Specification](https://registry.khronos.org/vulkan/specs/latest/html/vkspec.html)
- [Vulkan Registry](https://registry.khronos.org/vulkan/specs/latest/registry.html)

### Layout, input, and accessibility

- [CSS Flexible Box Layout Module Level 1](https://www.w3.org/TR/css-flexbox-1/)
- [CSS Grid Layout Module Level 2](https://www.w3.org/TR/css-grid-2/)
- [Taffy Layout Engine](https://github.com/DioxusLabs/taffy)
- [Yoga Layout Engine](https://www.yogalayout.dev/)
- [W3C UI Events](https://www.w3.org/TR/uievents/)
- [AccessKit Cross-Platform Accessibility Schema](https://github.com/AccessKit/accesskit)
- [Windows UI Automation](https://learn.microsoft.com/en-us/windows/win32/winauto/entry-uiauto-win32)
- [Apple Accessibility (NSAccessibility)](https://developer.apple.com/documentation/appkit/nsaccessibility)
- [AT-SPI2 Core Documentation](https://gitlab.gnome.org/GNOME/at-spi2-core)
- [D-Bus Specification](https://dbus.freedesktop.org/doc/dbus-specification.html)
- [libxkbcommon Documentation](https://xkbcommon.org/doc/current/)

### Test tooling

- [JNA Project](https://github.com/java-native-access/jna)

---

## 29. Final Acceptance Statements

### 29.1 First stable desktop release

At the first stable desktop release, automated evidence must support this public statement:

> HimariUI's core, text engine, software renderer, GPU abstraction, required Windows, macOS, and Linux Wayland platform profiles, and sole desktop FFM binding path are implemented in Java. Standard artifacts contain no project-built or third-party CPU-native libraries. The desktop framework calls operating system and system graphics APIs through generated, strongly typed FFM bindings and defines no FFI provider SPI. Animation keeps committed model targets separate from atomically sampled presentation values, preserves value and compatible spring velocity across interruption, respects declared phase impacts and reduced-motion policy, and replays deterministically without per-frame application-state writes. Color values, image/display-list/scene encodings, the extended-linear reference renderer, and RHI surfaces keep gamut, transfer function, luminance, precision, and presentation capability explicit; finite extended-range values and HDR metadata survive canonical replay, and every backend provides a deterministic SDR fallback without claiming unverified hardware HDR support. A versioned, bounded, pointer-free scene/display-list codec plus offline replay proves that scenes, declared color/profile and other resources, presentation configuration, correlated semantics, and normalized input survive a process boundary without placing networking or remote-session policy in the core. FreeType, HarfBuzz, SDL, Impeller, JNA, and LWJGL are used only as design references, test Oracles, or development tools and do not enter the core runtime graph. Every critical port pins its upstream version, records provenance and symbol mapping, retains a pure-Java reference implementation, and has reproducible differential-corpus evidence.

### 29.2 Future local browser/Wasm release

When the post-stable Web track is complete, automated evidence must additionally support this statement:

> HimariUI's browser/Wasm target reuses the portable Java runtime, layout, text, display-list, color, semantics, and rendering subsystems. It accesses browser capabilities only through generated, target-specific Wasm imports and JavaScript/browser host bindings; it does not use FFM, JNI, JNA, or desktop platform modules. WebGPU and Canvas/software rendering follow the same backend-neutral resource-usage, color/presentation, and scene semantics as desktop backends, preserve tagged extended-range content, and fall back through the versioned SDR mapping unless a native browser HDR mode passes its conformance profile. Browser-specific DOM integration is limited to host services such as IME and accessibility and does not replace HimariUI's layout or visual rendering model.

### 29.3 Future Android/iOS AOT release

When the post-stable mobile track is complete, automated evidence must additionally support this statement:

> HimariUI's Android and iOS targets compile the ordinary Java 25 runtime, layout, text, display-list, color, semantics, software-rendering, and RHI implementations through a validated mobile AOT toolchain without an ART-compatible common fork or restrictions on stable Java 25 features. Android and iOS platform services are reached through generated, target-specific host glue that is isolated from the desktop FFM path and from core JARs. Mobile surfaces preserve the common tagged extended-range and presentation-capability contract and advertise native HDR only after target-specific conformance, otherwise using the same deterministic SDR fallback. Target-generated AOT code and host glue appear only in mobile application bundles, have complete provenance and boundary validation, and do not introduce a runtime FFI provider or third-party graphics stack.

### 29.4 Future remote scene rendering release

When the post-stable remote track is complete, automated evidence must additionally support this statement:

> HimariUI can keep an application and its authoritative Java 25 runtime on a JVM or Native Image host while presenting and interacting with the same GUI in a browser through a versioned, bounded, pointer-free scene protocol. The browser renders immutable scene/display-list semantics and content-addressed color/profile and other resources through WebGPU or Canvas/software, negotiates output gamut/dynamic range and an authoritative mapped fallback without relabeling source content, samples only negotiated bounded layer-animation programs with explicit clocks and replacement generations, mirrors correlated semantics for accessibility, and returns normalized input and IME transactions. No callback, arbitrary executable payload, component tree, Java runtime object, FFM handle, RHI object, or native GPU command crosses the wire. Networking, security, codecs, and session policy remain isolated in optional remote artifacts and do not become core renderer providers or dependencies.
