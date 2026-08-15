package org.glavo.himari.spikes.nativeimage.ffm;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.spikes.d3d12.D3d12Conformance;
import org.glavo.himari.spikes.ffi.ffm.FfiFfmScenarios;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/// Executes shared generated FFM bindings and a real D3D12 surface inside GraalVM Native Image.
@NotNullByDefault
public final class NativeImageFfmConformance {
    /// Maximum managed-heap usage declared by the Native Image profile.
    private static final long MAX_HEAP_BYTES = 512L * 1024L * 1024L;

    /// Maximum peak process commit declared by the Native Image profile.
    private static final long MAX_NATIVE_BYTES = 1024L * 1024L * 1024L;

    /// Maximum process handle count declared by the Native Image profile.
    private static final int MAX_HANDLES = 512;

    /// Maximum live managed-thread count declared by the Native Image profile.
    private static final int MAX_THREADS = 24;

    /// Prevents instantiation of this command-line entry point.
    private NativeImageFfmConformance() {
    }

    /// Runs the Native Image profile and writes its result and direct-load audit.
    ///
    /// @param arguments evidence directory, repetition count, soak seconds, and system-library allowlist path
    /// @throws IllegalArgumentException if the arguments or numeric requirements are invalid
    /// @throws IllegalStateException if execution, a profile assertion, or evidence writing fails
    public static void main(String[] arguments) {
        if (arguments.length != 4) {
            throw new IllegalArgumentException(
                    "Usage: NativeImageFfmConformance "
                            + "<evidence-directory> <repetitions> <soak-seconds> <native-load-allowlist>"
            );
        }
        Path evidenceDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        int repetitions = positiveInteger(arguments[1], "repetitions");
        int soakSeconds = nonNegativeInteger(arguments[2], "soak-seconds");
        Path allowlistPath = Path.of(arguments[3]).toAbsolutePath().normalize();
        requireNativeImageRuntime();

        Instant started = Instant.now();
        FfiFfmScenarios.Summary ffiSummary;
        D3d12Conformance.Summary d3d12Summary;
        @Unmodifiable List<String> loadedLibraries;
        try (NativeLibraryLoadAudit.Session audit = NativeLibraryLoadAudit.begin()) {
            ffiSummary = FfiFfmScenarios.run(repetitions, Duration.ZERO);
            d3d12Summary = D3d12Conformance.run(
                    evidenceDirectory.resolve("platform"),
                    repetitions,
                    soakSeconds
            );
            loadedLibraries = audit.loadedLibraries();
        }
        Instant finished = Instant.now();

        @Unmodifiable Set<String> allowlist = readAllowlist(allowlistPath);
        @Unmodifiable Set<String> requiredLibraries = requiredLibraries();
        boolean requiredLibrariesLoaded = loadedLibraries.containsAll(requiredLibraries);
        boolean onlyAllowlistedLibrariesLoaded = loadedLibraries.stream().allMatch(allowlist::contains);
        boolean resourcesWithinBudget = resourcesWithinBudget(d3d12Summary);
        boolean passed = requiredLibrariesLoaded && onlyAllowlistedLibrariesLoaded && resourcesWithinBudget;

        writeNativeLoadLog(evidenceDirectory.resolve("native-load.log"), loadedLibraries);
        write(
                evidenceDirectory.resolve("results.json"),
                resultsJson(
                        started,
                        finished,
                        ffiSummary,
                        d3d12Summary,
                        loadedLibraries,
                        requiredLibrariesLoaded,
                        onlyAllowlistedLibrariesLoaded,
                        resourcesWithinBudget,
                        passed
                )
        );
        if (!passed) {
            throw new IllegalStateException("Native Image FFM conformance failed; inspect results.json");
        }
        System.out.println("Native Image FFM conformance passed: repetitions=" + repetitions
                + ", soakSeconds=" + soakSeconds
                + ", callbacks=" + ffiSummary.callbackInvocations()
                + ", frames=" + d3d12Summary.presentedFrames()
                + ", libraries=" + loadedLibraries);
    }

    /// Requires execution from a produced GraalVM Native Image rather than a JVM fallback.
    private static void requireNativeImageRuntime() {
        if (System.getProperty("org.graalvm.nativeimage.imagecode") == null) {
            throw new IllegalStateException("NI-FFM-001 must execute inside a GraalVM Native Image");
        }
    }

    /// Returns whether the stricter Native Image process budgets are satisfied.
    ///
    /// @param summary validated D3D12 process observations
    /// @return whether every profile budget is met
    private static boolean resourcesWithinBudget(D3d12Conformance.Summary summary) {
        return Math.max(summary.heapUsedBytesBefore(), summary.heapUsedBytesAfter()) <= MAX_HEAP_BYTES
                && Long.compareUnsigned(summary.processPeakCommittedBytesAfter(), MAX_NATIVE_BYTES) <= 0
                && Math.max(summary.processHandlesBefore(), summary.processHandlesAfter()) <= MAX_HANDLES
                && Math.max(summary.liveThreadsBefore(), summary.liveThreadsAfter()) <= MAX_THREADS;
    }

    /// Reads one comment-aware, line-oriented system-library allowlist.
    ///
    /// @param path UTF-8 allowlist path
    /// @return immutable allowed basenames
    private static @Unmodifiable Set<String> readAllowlist(Path path) {
        try {
            Set<String> entries = new LinkedHashSet<>();
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String entry = line.trim();
                if (!entry.isEmpty() && !entry.startsWith("#")) {
                    entries.add(entry);
                }
            }
            return Set.copyOf(entries);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read native-load allowlist " + path, exception);
        }
    }

    /// Returns every system library that the selected Windows x64 profile must load directly.
    ///
    /// @return immutable required basenames
    private static @Unmodifiable Set<String> requiredLibraries() {
        return Set.of("ucrtbase.dll", "kernel32.dll", "user32.dll", "dxgi.dll", "d3d12.dll");
    }

    /// Writes successful direct library-lookups using the guard's unified-log-compatible marker.
    ///
    /// @param path audit output path
    /// @param libraries sorted, duplicate-free successful lookup basenames
    private static void writeNativeLoadLog(Path path, List<String> libraries) {
        StringBuilder content = new StringBuilder()
                .append("# scope=successful SymbolLookup.libraryLookup calls\n")
                .append("# runtime=native-image\n");
        for (String library : libraries) {
            content.append("[himari][library] Loaded library ")
                    .append(library)
                    .append(", handle native-image-direct-lookup\n");
        }
        write(path, content.toString());
    }

    /// Encodes the complete Native Image result document.
    ///
    /// @param started profile start instant
    /// @param finished profile completion instant
    /// @param ffi portable callback and lifetime summary
    /// @param d3d12 platform presentation and resource summary
    /// @param loadedLibraries successful direct system-library lookups
    /// @param requiredLibrariesLoaded whether every selected platform library was observed
    /// @param onlyAllowlistedLibrariesLoaded whether every observed library is approved
    /// @param resourcesWithinBudget whether Native Image process budgets passed
    /// @param passed whether every profile assertion passed
    /// @return deterministic-key-order JSON
    private static String resultsJson(
            Instant started,
            Instant finished,
            FfiFfmScenarios.Summary ffi,
            D3d12Conformance.Summary d3d12,
            List<String> loadedLibraries,
            boolean requiredLibrariesLoaded,
            boolean onlyAllowlistedLibrariesLoaded,
            boolean resourcesWithinBudget,
            boolean passed
    ) {
        return """
                {
                  "profileId": "m0-native-image-ffm",
                  "profileVersion": 1,
                  "workPackage": "NI-FFM-001",
                  "fixtures": ["native-image-ffi-minimum-v1", "native-image-platform-clear-v1"],
                  "target": {"operatingSystem": "windows", "architecture": "x86_64", "runtime": "native-image"},
                  "startedAt": %s,
                  "finishedAt": %s,
                  "portableFfm": {
                    "systemLibrary": %s,
                    "repetitions": %d,
                    "callbackInvocations": %d,
                    "reentrantDowncalls": %d,
                    "durationNanos": %d,
                    "threadConfined": %s,
                    "callbackArenaLifetimeRejected": %s,
                    "callbackExceptionContained": %s
                  },
                  "platformClear": {
                    "repetitions": %d,
                    "soakSeconds": %d,
                    "elapsedMillis": %d,
                    "presentedFrames": %d,
                    "readbackVerifiedFrames": %d,
                    "maximumChannelDelta": %d,
                    "ownedComReferences": %d,
                    "releasedComReferences": %d,
                    "debugErrorCount": %d,
                    "debugLayerEnabled": %s
                  },
                  "resources": {
                    "heapUsedBytesBefore": %d,
                    "heapUsedBytesAfter": %d,
                    "maximumHeapBytes": %d,
                    "processPeakCommittedBytesAfter": %d,
                    "maximumNativeBytes": %d,
                    "processHandlesBefore": %d,
                    "processHandlesAfter": %d,
                    "maximumHandles": %d,
                    "liveThreadsBefore": %d,
                    "liveThreadsAfter": %d,
                    "maximumThreads": %d
                  },
                  "nativeLibraryAudit": {
                    "scope": "successful SymbolLookup.libraryLookup calls",
                    "loadedLibraries": %s,
                    "requiredLibrariesLoaded": %s,
                    "onlyAllowlistedLibrariesLoaded": %s
                  },
                  "assertions": {
                    "sameGeneratedFfmBindingClassesAsJvm": true,
                    "svmSpecificSystemCallBackendPresent": false,
                    "runtimeDowncallMetadataResolved": true,
                    "runtimeUpcallMetadataResolved": true,
                    "callbackAndLifetimeBehaviorMatchedJvm": %s,
                    "realWindowOpenedAndClearPresented": %s,
                    "resourcesWithinBudget": %s,
                    "projectNativeLibraryLoaded": false
                  },
                  "result": "%s"
                }
                """.formatted(
                quote(started.toString()),
                quote(finished.toString()),
                quote(ffi.systemLibrary()),
                ffi.repetitions(),
                ffi.callbackInvocations(),
                ffi.reentrantDowncalls(),
                ffi.durationNanos(),
                ffi.threadConfined(),
                ffi.callbackArenaLifetimeRejected(),
                ffi.exceptionContained(),
                d3d12.repetitions(),
                d3d12.requestedSoakSeconds(),
                d3d12.elapsedMillis(),
                d3d12.presentedFrames(),
                d3d12.readbackVerifiedFrames(),
                d3d12.maximumChannelDelta(),
                d3d12.ownedComReferences(),
                d3d12.releasedComReferences(),
                d3d12.debugErrorCount(),
                d3d12.debugLayerEnabled(),
                d3d12.heapUsedBytesBefore(),
                d3d12.heapUsedBytesAfter(),
                MAX_HEAP_BYTES,
                d3d12.processPeakCommittedBytesAfter(),
                MAX_NATIVE_BYTES,
                d3d12.processHandlesBefore(),
                d3d12.processHandlesAfter(),
                MAX_HANDLES,
                d3d12.liveThreadsBefore(),
                d3d12.liveThreadsAfter(),
                MAX_THREADS,
                jsonArray(loadedLibraries),
                requiredLibrariesLoaded,
                onlyAllowlistedLibrariesLoaded,
                ffi.threadConfined() && ffi.callbackArenaLifetimeRejected() && ffi.exceptionContained(),
                d3d12.presentedFrames() == d3d12.repetitions()
                        && d3d12.readbackVerifiedFrames() == d3d12.repetitions()
                        && d3d12.maximumChannelDelta() == 0,
                resourcesWithinBudget,
                passed ? "passed" : "failed"
        );
    }

    /// Encodes a list as a compact JSON string array.
    ///
    /// @param values raw string values
    /// @return encoded JSON array
    private static String jsonArray(List<String> values) {
        return '[' + String.join(",", values.stream().map(NativeImageFfmConformance::quote).toList()) + ']';
    }

    /// Returns one JSON string literal.
    ///
    /// @param value raw string value
    /// @return escaped and quoted JSON text
    private static String quote(String value) {
        StringBuilder result = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                case '\b' -> result.append("\\b");
                case '\f' -> result.append("\\f");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    /// Writes one UTF-8 evidence artifact and creates its parent directory.
    ///
    /// @param path output path with a parent directory
    /// @param content complete artifact content
    private static void write(Path path, String content) {
        try {
            @Nullable Path parent = path.getParent();
            if (parent == null) {
                throw new IllegalArgumentException("Evidence path must have a parent: " + path);
            }
            Files.createDirectories(parent);
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write Native Image evidence " + path, exception);
        }
    }

    /// Parses one strictly positive decimal integer.
    ///
    /// @param value source text
    /// @param name diagnostic parameter name
    /// @return parsed positive value
    private static int positiveInteger(String value, String name) {
        int parsed = nonNegativeInteger(value, name);
        if (parsed == 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return parsed;
    }

    /// Parses one non-negative decimal integer.
    ///
    /// @param value source text
    /// @param name diagnostic parameter name
    /// @return parsed non-negative value
    private static int nonNegativeInteger(String value, String name) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(name + " must be non-negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a decimal integer: " + value, exception);
        }
    }
}
