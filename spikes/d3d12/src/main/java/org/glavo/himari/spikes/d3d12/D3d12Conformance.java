package org.glavo.himari.spikes.d3d12;

import org.glavo.himari.spikes.d3d12.generated.D3d12FfmBindings;
import org.glavo.himari.spikes.d3d12.generated.D3d12Layouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.List;

/// Executes the M0 D3D12 device and swapchain conformance profile.
@NotNullByDefault
public final class D3d12Conformance {
    /// Number of frames used to initialize driver-global state before the resource baseline.
    private static final int WARMUP_FRAMES = 1;

    /// Maximum process handle count declared by the profile.
    private static final int MAX_HANDLES = 1024;

    /// Maximum retained handle increase after the warmed scenario closes.
    private static final int MAX_RETAINED_HANDLE_DELTA = 16;

    /// Maximum live JVM thread count declared by the profile.
    private static final int MAX_THREADS = 32;

    /// Maximum observed Java heap bytes declared by the profile.
    private static final long MAX_HEAP_BYTES = 512L * 1024L * 1024L;

    /// Maximum peak process committed bytes declared by the profile.
    private static final long MAX_NATIVE_BYTES = 2L * 1024L * 1024L * 1024L;

    /// Prevents instantiation of this command-line entry point.
    private D3d12Conformance() {
    }

    /// Runs the profile and writes `capabilities.json`, `debug-layer.log`, and `presentation.json`.
    ///
    /// @param arguments evidence directory, repetition count, and soak seconds
    public static void main(String[] arguments) {
        if (arguments.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: D3d12Conformance <evidence-directory> <repetitions> <soak-seconds>"
            );
        }
        Path evidenceDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        int repetitions = positiveInteger(arguments[1], "repetitions");
        int soakSeconds = nonNegativeInteger(arguments[2], "soak-seconds");

        run(evidenceDirectory, repetitions, soakSeconds);
    }

    /// Runs, validates, and records one D3D12 device and swapchain profile.
    ///
    /// @param evidenceDirectory directory that receives `capabilities.json`, `debug-layer.log`, and `presentation.json`
    /// @param repetitions required positive frame count
    /// @param soakSeconds required non-negative minimum scenario duration in seconds
    /// @return the validated execution and resource summary
    /// @throws IllegalArgumentException if a numeric requirement is outside its accepted range
    /// @throws IllegalStateException if any conformance assertion or evidence write fails
    public static Summary run(Path evidenceDirectory, int repetitions, int soakSeconds) {
        if (repetitions <= 0) {
            throw new IllegalArgumentException("repetitions must be positive");
        }
        if (soakSeconds < 0) {
            throw new IllegalArgumentException("soakSeconds must be non-negative");
        }
        evidenceDirectory = evidenceDirectory.toAbsolutePath().normalize();

        D3d12ScenarioResult result;
        D3d12ResourceMetrics resources;
        try (D3d12Libraries libraries = D3d12Libraries.open()) {
            D3d12FfmBindings bindings = libraries.bindings();
            D3d12ScenarioResult warmup = D3d12Scenario.run(WARMUP_FRAMES, 0);
            validateWarmup(warmup);
            int handlesBefore = processHandleCount(bindings);
            ProcessMemory memoryBefore = processMemory(bindings);
            int threadsBefore = ManagementFactory.getThreadMXBean().getThreadCount();
            long heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

            result = D3d12Scenario.run(repetitions, soakSeconds);

            int handlesAfter = processHandleCount(bindings);
            ProcessMemory memoryAfter = processMemory(bindings);
            int threadsAfter = ManagementFactory.getThreadMXBean().getThreadCount();
            long heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            resources = new D3d12ResourceMetrics(
                    handlesBefore,
                    handlesAfter,
                    threadsBefore,
                    threadsAfter,
                    heapBefore,
                    heapAfter,
                    memoryBefore.privateBytes(),
                    memoryAfter.privateBytes(),
                    memoryAfter.peakCommittedBytes()
            );
        }

        boolean passed = profilePassed(result, resources);
        JsonSupport.write(evidenceDirectory.resolve("capabilities.json"), capabilitiesJson(result, passed));
        JsonSupport.write(evidenceDirectory.resolve("debug-layer.log"), debugLog(result, passed));
        JsonSupport.write(
                evidenceDirectory.resolve("presentation.json"),
                presentationJson(result, resources, passed)
        );
        validateProfile(result, resources);
        System.out.println("D3D12 conformance passed: repetitions=" + repetitions
                + ", soakSeconds=" + soakSeconds
                + ", handles=" + resources.processHandlesBefore() + "->" + resources.processHandlesAfter()
                + ", COM=" + result.ownedComReferences() + '/' + result.releasedComReferences()
                + ", debugMessages=" + result.debugMessages().size()
                + ", debugLayer=" + result.debugLayerEnabled());
        return new Summary(
                result.repetitions(),
                result.requestedSoakSeconds(),
                result.elapsedMillis(),
                result.presentedFrames(),
                result.readbackVerifiedFrames(),
                result.maximumChannelDelta(),
                result.ownedComReferences(),
                result.releasedComReferences(),
                result.declaredResourceBytes(),
                result.debugErrorCount(),
                result.debugLayerEnabled(),
                resources.processHandlesBefore(),
                resources.processHandlesAfter(),
                resources.jvmLiveThreadsBefore(),
                resources.jvmLiveThreadsAfter(),
                resources.heapUsedBytesBefore(),
                resources.heapUsedBytesAfter(),
                resources.processPrivateBytesBefore(),
                resources.processPrivateBytesAfter(),
                resources.processPeakCommittedBytesAfter()
        );
    }

    /// Verifies the unrecorded driver-initialization lifecycle before taking resource baselines.
    ///
    /// @param result the warmup observation
    private static void validateWarmup(D3d12ScenarioResult result) {
        if (result.presentedFrames() != WARMUP_FRAMES
                || result.readbackVerifiedFrames() != WARMUP_FRAMES
                || result.maximumChannelDelta() != 0
                || result.deviceRemovedReason() != 0
                || result.ownedComReferences() != result.releasedComReferences()
                || result.debugErrorCount() != 0L) {
            throw new IllegalStateException("D3D12 warmup lifecycle failed");
        }
    }

    /// Reads the current process handle count through the generated Kernel32 binding.
    ///
    /// @param bindings the generated bindings
    /// @return the unsigned process handle count
    private static int processHandleCount(D3d12FfmBindings bindings) {
        MemorySegment process = bindings.getCurrentProcess();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
            D3d12FfmBindings.GetProcessHandleCountResult result = bindings.getProcessHandleCount(process, count);
            if (result.value() == 0) {
                throw new IllegalStateException("GetProcessHandleCount failed with Win32 error "
                        + Integer.toUnsignedString(result.errorCode()));
            }
            return Math.toIntExact(Integer.toUnsignedLong(count.get(ValueLayout.JAVA_INT, 0L)));
        }
    }

    /// Reads Windows process-private and peak-commit counters.
    ///
    /// @param bindings the generated bindings
    /// @return the copied process-memory snapshot
    private static ProcessMemory processMemory(D3d12FfmBindings bindings) {
        MemorySegment process = bindings.getCurrentProcess();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment counters = arena.allocate(D3d12Layouts.PROCESS_MEMORY_COUNTERS_EX);
            counters.fill((byte) 0);
            counters.set(
                    ValueLayout.JAVA_INT,
                    D3d12Layouts.PROCESS_MEMORY_COUNTERS_EX_CB_OFFSET,
                    Math.toIntExact(D3d12Layouts.PROCESS_MEMORY_COUNTERS_EX.byteSize())
            );
            D3d12FfmBindings.GetProcessMemoryInfoResult result = bindings.getProcessMemoryInfo(
                    process,
                    counters,
                    Math.toIntExact(D3d12Layouts.PROCESS_MEMORY_COUNTERS_EX.byteSize())
            );
            if (result.value() == 0) {
                throw new IllegalStateException("K32GetProcessMemoryInfo failed with Win32 error "
                        + Integer.toUnsignedString(result.errorCode()));
            }
            return new ProcessMemory(
                    counters.get(ValueLayout.JAVA_LONG, D3d12Layouts.PROCESS_MEMORY_COUNTERS_EX_PRIVATE_USAGE_OFFSET),
                    counters.get(
                            ValueLayout.JAVA_LONG,
                            D3d12Layouts.PROCESS_MEMORY_COUNTERS_EX_PEAK_PAGEFILE_USAGE_OFFSET
                    ),
                    counters.get(
                            ValueLayout.JAVA_LONG,
                            D3d12Layouts.PROCESS_MEMORY_COUNTERS_EX_WORKING_SET_SIZE_OFFSET
                    ),
                    counters.get(
                            ValueLayout.JAVA_LONG,
                            D3d12Layouts.PROCESS_MEMORY_COUNTERS_EX_PEAK_WORKING_SET_SIZE_OFFSET
                    )
            );
        }
    }

    /// Determines whether every mandatory M0 assertion and budget is satisfied.
    ///
    /// The debug layer is an optional Windows developer component. Its availability is recorded separately; when it
    /// is available, any error or corruption message fails this profile.
    ///
    /// @param result the scenario observation
    /// @param resources the surrounding process-resource observation
    /// @return whether all mandatory checks passed
    private static boolean profilePassed(D3d12ScenarioResult result, D3d12ResourceMetrics resources) {
        boolean frameCountsExact = result.presentedFrames() == result.repetitions()
                && result.readbackVerifiedFrames() == result.repetitions();
        boolean soakSatisfied = result.elapsedMillis() >= result.requestedSoakSeconds() * 1000L;
        boolean formatMapped = !result.formats().isEmpty() && result.formats().getFirst().renderTarget();
        boolean colorSpaceMapped = !result.colorSpaces().isEmpty() && result.colorSpaces().getFirst().present();
        boolean resourceBudgetsSatisfied = Math.max(resources.processHandlesBefore(), resources.processHandlesAfter())
                    <= MAX_HANDLES
                && resources.processHandlesAfter() - resources.processHandlesBefore()
                    <= MAX_RETAINED_HANDLE_DELTA
                && Math.max(resources.jvmLiveThreadsBefore(), resources.jvmLiveThreadsAfter()) <= MAX_THREADS
                && Math.max(resources.heapUsedBytesBefore(), resources.heapUsedBytesAfter()) <= MAX_HEAP_BYTES
                && Long.compareUnsigned(resources.processPeakCommittedBytesAfter(), MAX_NATIVE_BYTES) <= 0
                && Long.compareUnsigned(result.declaredResourceBytes(), MAX_NATIVE_BYTES) <= 0;
        return frameCountsExact
                && soakSatisfied
                && result.maximumChannelDelta() == 0
                && result.finalFenceValue() == result.repetitions()
                && result.deviceRemovedReason() == 0
                && result.ownedComReferences() == result.releasedComReferences()
                && result.debugErrorCount() == 0L
                && formatMapped
                && colorSpaceMapped
                && resourceBudgetsSatisfied;
    }

    /// Throws a focused diagnostic when a mandatory profile check failed.
    ///
    /// @param result the scenario observation
    /// @param resources the surrounding resource observation
    private static void validateProfile(D3d12ScenarioResult result, D3d12ResourceMetrics resources) {
        if (!profilePassed(result, resources)) {
            throw new IllegalStateException("D3D12 conformance failed; inspect presentation.json and debug-layer.log");
        }
    }

    /// Encodes the device, format, color-space, and effective SDR configuration evidence.
    ///
    /// @param result the scenario observation
    /// @param passed whether all mandatory checks passed
    /// @return the complete deterministic-key-order JSON document
    private static String capabilitiesJson(D3d12ScenarioResult result, boolean passed) {
        return """
                {
                  "profileId": "m0-d3d12-surface",
                  "profileVersion": 1,
                  "workPackage": "SPIKE-D3D12-001",
                  "fixture": "dxgi-surface-formats-v1",
                  "target": {"operatingSystem": "windows", "architecture": "x86_64", "runtime": "%s"},
                  "device": {"minimumFeatureLevel": "D3D_FEATURE_LEVEL_11_0", "removedReason": %d},
                  "debug": {"layerEnabled": %s, "dxgiFactoryDebugEnabled": %s, "infoQueueAvailable": %s},
                  "queriedFormats": %s,
                  "colorSpacesAgainstSelectedSdrSwapChain": %s,
                  "effectivePresentation": {
                    "format": {"name": "DXGI_FORMAT_R8G8B8A8_UNORM", "code": 28},
                    "colorSpace": {"name": "DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709", "code": 0},
                    "bufferCount": 2,
                    "swapEffect": "DXGI_SWAP_EFFECT_FLIP_DISCARD",
                    "hdrMetadataApplied": false,
                    "mode": "color-managed-sdr"
                  },
                  "limitations": {
                    "colorSpaceQueriesApplyToSelectedSdrSwapChainOnly": true,
                    "wideGamutOrHdrOutputClaimed": false,
                    "windowsArm64EvidencePending": true
                  },
                  "result": "%s"
                }
                """.formatted(
                runtimeKind(),
                result.deviceRemovedReason(),
                result.debugLayerEnabled(),
                result.dxgiFactoryDebugEnabled(),
                result.infoQueueAvailable(),
                jsonArray(result.formats().stream().map(D3d12FormatSupport::toJson).toList()),
                jsonArray(result.colorSpaces().stream().map(D3d12ColorSpaceSupport::toJson).toList()),
                passed ? "passed" : "failed"
        );
    }

    /// Encodes debug-layer availability and all retrieved messages as a stable text log.
    ///
    /// @param result the scenario observation
    /// @param passed whether all mandatory checks passed
    /// @return the complete debug log
    private static String debugLog(D3d12ScenarioResult result, boolean passed) {
        StringBuilder log = new StringBuilder(512)
                .append("profileId=m0-d3d12-surface\n")
                .append("workPackage=SPIKE-D3D12-001\n")
                .append("debugLayerEnabled=").append(result.debugLayerEnabled()).append('\n')
                .append("dxgiFactoryDebugEnabled=").append(result.dxgiFactoryDebugEnabled()).append('\n')
                .append("infoQueueAvailable=").append(result.infoQueueAvailable()).append('\n')
                .append("messageCount=").append(result.debugMessages().size()).append('\n')
                .append("errorCount=").append(result.debugErrorCount()).append('\n')
                .append("warningCount=").append(result.debugWarningCount()).append('\n');
        for (D3d12DebugMessage message : result.debugMessages()) {
            log.append('[').append(message.severityName()).append("] category=")
                    .append(message.category()).append(" id=").append(message.id()).append(' ')
                    .append(singleLine(message.description())).append('\n');
        }
        log.append("inspectionStatus=")
                .append(result.infoQueueAvailable() ? "completed" : "unavailable")
                .append('\n')
                .append("result=").append(passed ? "passed" : "failed").append('\n');
        return log.toString();
    }

    /// Encodes presentation, synchronization, readback, and process-resource evidence.
    ///
    /// @param result the scenario observation
    /// @param resources the surrounding resource observation
    /// @param passed whether all mandatory checks passed
    /// @return the complete deterministic-key-order JSON document
    private static String presentationJson(
            D3d12ScenarioResult result,
            D3d12ResourceMetrics resources,
            boolean passed
    ) {
        return """
                {
                  "profileId": "m0-d3d12-surface",
                  "profileVersion": 1,
                  "workPackage": "SPIKE-D3D12-001",
                  "fixtures": ["d3d12-device-v1", "d3d12-sdr-clear-v1"],
                  "target": {"operatingSystem": "windows", "architecture": "x86_64", "runtime": "%s"},
                  "parameters": {"warmupFrames": %d, "repetitions": %d, "soakSeconds": %d},
                  "presentation": {
                    "elapsedMillis": %d,
                    "width": %d,
                    "height": %d,
                    "rowPitch": %d,
                    "presentedFrames": %d,
                    "readbackVerifiedFrames": %d,
                    "verifiedPixelCount": %d,
                    "maximumChannelDelta": %d,
                    "clearRgbaBytes": [17, 83, 149, 255],
                    "clearRgbaNormalized": [0.06666667, 0.3254902, 0.58431375, 1.0],
                    "finalFenceValue": %d,
                    "deviceRemovedReason": %d
                  },
                  "resources": {
                    "ownedComReferences": %d,
                    "releasedComReferences": %d,
                    "declaredResourceBytes": %d,
                    "processHandlesBefore": %d,
                    "processHandlesAfter": %d,
                    "retainedHandleDelta": %d,
                    "maximumHandles": %d,
                    "maximumRetainedHandleDelta": %d,
                    "baselineTakenAfterDriverWarmup": true,
                    "jvmLiveThreadsBefore": %d,
                    "jvmLiveThreadsAfter": %d,
                    "maximumThreads": %d,
                    "heapUsedBytesBefore": %d,
                    "heapUsedBytesAfter": %d,
                    "maximumHeapBytes": %d,
                    "processPrivateBytesBefore": %d,
                    "processPrivateBytesAfter": %d,
                    "processPeakCommittedBytesAfter": %d,
                    "maximumNativeBytes": %d,
                    "driverAndJvmAllocationsIncludedInProcessPeak": true
                  },
                  "assertions": {
                    "deviceQueueFactorySwapchainCreated": true,
                    "effectiveSdrConfigurationMapped": %s,
                    "deterministicClearPresentedAndReadBackExactly": %s,
                    "fenceSynchronizationCompleted": %s,
                    "deviceRemovalAbsent": %s,
                    "debugLayerErrorsAbsentWhenAvailable": %s,
                    "comOwnershipBalanced": %s,
                    "soakDurationSatisfied": %s,
                    "resourceBudgetsSatisfied": %s,
                    "projectNativeLibraryUsed": false
                  },
                  "result": "%s"
                }
                """.formatted(
                runtimeKind(),
                WARMUP_FRAMES,
                result.repetitions(),
                result.requestedSoakSeconds(),
                result.elapsedMillis(),
                result.width(),
                result.height(),
                result.rowPitch(),
                result.presentedFrames(),
                result.readbackVerifiedFrames(),
                result.verifiedPixelCount(),
                result.maximumChannelDelta(),
                result.finalFenceValue(),
                result.deviceRemovedReason(),
                result.ownedComReferences(),
                result.releasedComReferences(),
                result.declaredResourceBytes(),
                resources.processHandlesBefore(),
                resources.processHandlesAfter(),
                resources.processHandlesAfter() - resources.processHandlesBefore(),
                MAX_HANDLES,
                MAX_RETAINED_HANDLE_DELTA,
                resources.jvmLiveThreadsBefore(),
                resources.jvmLiveThreadsAfter(),
                MAX_THREADS,
                resources.heapUsedBytesBefore(),
                resources.heapUsedBytesAfter(),
                MAX_HEAP_BYTES,
                resources.processPrivateBytesBefore(),
                resources.processPrivateBytesAfter(),
                resources.processPeakCommittedBytesAfter(),
                MAX_NATIVE_BYTES,
                !result.formats().isEmpty() && result.formats().getFirst().renderTarget()
                        && !result.colorSpaces().isEmpty() && result.colorSpaces().getFirst().present(),
                result.presentedFrames() == result.repetitions()
                        && result.readbackVerifiedFrames() == result.repetitions()
                        && result.maximumChannelDelta() == 0,
                result.finalFenceValue() == result.repetitions(),
                result.deviceRemovedReason() == 0,
                result.debugErrorCount() == 0L,
                result.ownedComReferences() == result.releasedComReferences(),
                result.elapsedMillis() >= result.requestedSoakSeconds() * 1000L,
                Math.max(resources.processHandlesBefore(), resources.processHandlesAfter()) <= MAX_HANDLES
                        && resources.processHandlesAfter() - resources.processHandlesBefore()
                            <= MAX_RETAINED_HANDLE_DELTA
                        && Math.max(resources.jvmLiveThreadsBefore(), resources.jvmLiveThreadsAfter()) <= MAX_THREADS
                        && Math.max(resources.heapUsedBytesBefore(), resources.heapUsedBytesAfter()) <= MAX_HEAP_BYTES
                        && Long.compareUnsigned(resources.processPeakCommittedBytesAfter(), MAX_NATIVE_BYTES) <= 0
                        && Long.compareUnsigned(result.declaredResourceBytes(), MAX_NATIVE_BYTES) <= 0,
                passed ? "passed" : "failed"
        );
    }

    /// Joins already encoded JSON values into one array.
    ///
    /// @param values the encoded values
    /// @return the JSON array
    private static String jsonArray(List<String> values) {
        return '[' + String.join(",", values) + ']';
    }

    /// Replaces native message line breaks with spaces for one-record-per-line logging.
    ///
    /// @param value the original description
    /// @return the single-line description
    private static String singleLine(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    /// Returns the runtime category recorded in reusable D3D12 evidence.
    ///
    /// @return `native-image` inside a produced image, otherwise `jvm`
    private static String runtimeKind() {
        return System.getProperty("org.graalvm.nativeimage.imagecode") == null ? "jvm" : "native-image";
    }

    /// Parses one strictly positive decimal integer.
    ///
    /// @param value the command-line spelling
    /// @param name the diagnostic parameter name
    /// @return the parsed value
    private static int positiveInteger(String value, String name) {
        int parsed = nonNegativeInteger(value, name);
        if (parsed == 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return parsed;
    }

    /// Parses one non-negative decimal integer.
    ///
    /// @param value the command-line spelling
    /// @param name the diagnostic parameter name
    /// @return the parsed value
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

    /// Summarizes one validated D3D12 execution for composition into a broader conformance profile.
    ///
    /// @param repetitions requested and completed frame count
    /// @param requestedSoakSeconds requested minimum duration in seconds
    /// @param elapsedMillis observed scenario duration in milliseconds
    /// @param presentedFrames frames successfully presented
    /// @param readbackVerifiedFrames frames read back and verified exactly
    /// @param maximumChannelDelta greatest observed 8-bit color-channel difference
    /// @param ownedComReferences COM references acquired by the scenario
    /// @param releasedComReferences COM references released by the scenario
    /// @param declaredResourceBytes explicitly sized scenario-native resources
    /// @param debugErrorCount debug-layer error and corruption messages
    /// @param debugLayerEnabled whether the optional D3D12 debug layer was active
    /// @param processHandlesBefore process handles after driver warmup and before the recorded scenario
    /// @param processHandlesAfter process handles after the recorded scenario closed
    /// @param liveThreadsBefore live Java threads before the recorded scenario
    /// @param liveThreadsAfter live Java threads after the recorded scenario
    /// @param heapUsedBytesBefore used managed heap before the recorded scenario
    /// @param heapUsedBytesAfter used managed heap after the recorded scenario
    /// @param processPrivateBytesBefore private committed process bytes before the recorded scenario
    /// @param processPrivateBytesAfter private committed process bytes after the recorded scenario
    /// @param processPeakCommittedBytesAfter peak committed process bytes observed after the recorded scenario
    @NotNullByDefault
    public record Summary(
            int repetitions,
            int requestedSoakSeconds,
            long elapsedMillis,
            int presentedFrames,
            int readbackVerifiedFrames,
            int maximumChannelDelta,
            int ownedComReferences,
            int releasedComReferences,
            long declaredResourceBytes,
            long debugErrorCount,
            boolean debugLayerEnabled,
            int processHandlesBefore,
            int processHandlesAfter,
            int liveThreadsBefore,
            int liveThreadsAfter,
            long heapUsedBytesBefore,
            long heapUsedBytesAfter,
            long processPrivateBytesBefore,
            long processPrivateBytesAfter,
            long processPeakCommittedBytesAfter
    ) {
    }

    /// Describes one Windows process-memory sample.
    ///
    /// @param privateBytes current process-private committed bytes
    /// @param peakCommittedBytes peak page-file-backed commit bytes
    /// @param workingSetBytes current working-set bytes
    /// @param peakWorkingSetBytes peak working-set bytes
    @NotNullByDefault
    private record ProcessMemory(
            long privateBytes,
            long peakCommittedBytes,
            long workingSetBytes,
            long peakWorkingSetBytes
    ) {
    }
}
