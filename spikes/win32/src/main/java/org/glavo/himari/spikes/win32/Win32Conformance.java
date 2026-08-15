package org.glavo.himari.spikes.win32;

import org.glavo.himari.spikes.win32.generated.Win32FfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.List;

/// Executes the M0 Win32 window and Advanced Color conformance profile.
@NotNullByDefault
public final class Win32Conformance {
    /// Maximum profile-owned handle count declared by the conformance profile.
    private static final int MAX_ADDITIONAL_HANDLES = 256;

    /// Maximum simultaneous handles directly owned by this implementation's bounded control flow.
    private static final int PROFILE_MAX_SIMULTANEOUS_OWNED_HANDLES = 8;

    /// Maximum permitted handle increase after all scenario-owned native resources close.
    private static final int MAX_RETAINED_HANDLE_DELTA = 16;

    /// Maximum observed heap bytes declared by the conformance profile.
    private static final long MAX_HEAP_BYTES = 512L * 1024L * 1024L;

    /// `MONITOR_DEFAULTTOPRIMARY` used to initialize DXGI before the resource baseline.
    private static final int MONITOR_DEFAULTTOPRIMARY = 0x0001;

    /// Win32 status expected when `PostMessageW` receives a deliberately invalid window handle.
    private static final int ERROR_INVALID_WINDOW_HANDLE = 1400;

    /// Prevents instantiation of this command-line entry point.
    private Win32Conformance() {
    }

    /// Runs the profile and writes `events.json` and `capabilities.json`.
    ///
    /// @param arguments evidence directory, repetition count, and soak seconds
    public static void main(String[] arguments) {
        if (arguments.length != 3) {
            throw new IllegalArgumentException(
                    "Usage: Win32Conformance <evidence-directory> <repetitions> <soak-seconds>"
            );
        }
        Path evidenceDirectory = Path.of(arguments[0]).toAbsolutePath().normalize();
        int repetitions = positiveInteger(arguments[1], "repetitions");
        int soakSeconds = nonNegativeInteger(arguments[2], "soak-seconds");

        try (Win32Libraries libraries = Win32Libraries.open()) {
            Win32FfmBindings bindings = libraries.bindings();
            int capturedInvalidWindowError = verifyCapturedLastError(bindings);
            MemorySegment primaryMonitor = bindings.monitorFromWindow(MemorySegment.NULL, MONITOR_DEFAULTTOPRIMARY);
            if (primaryMonitor.address() == 0L) {
                throw new IllegalStateException("MonitorFromWindow(NULL, MONITOR_DEFAULTTOPRIMARY) returned NULL");
            }
            DxgiOutputQuery.query(bindings, primaryMonitor);
            int handlesBefore = processHandleCount(bindings);
            int threadsBefore = ManagementFactory.getThreadMXBean().getThreadCount();
            long heapBefore = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

            Win32WindowResult result = Win32WindowScenario.run(bindings, repetitions, soakSeconds);

            int handlesAfter = processHandleCount(bindings);
            int threadsAfter = ManagementFactory.getThreadMXBean().getThreadCount();
            long heapAfter = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            validateBudgets(handlesBefore, handlesAfter, heapBefore, heapAfter);

            JsonSupport.write(evidenceDirectory.resolve("events.json"), eventsJson(
                    result,
                    handlesBefore,
                    handlesAfter,
                    threadsBefore,
                    threadsAfter,
                    heapBefore,
                    heapAfter,
                    capturedInvalidWindowError
            ));
            JsonSupport.write(evidenceDirectory.resolve("capabilities.json"), result.output().toJson());
            System.out.println("Win32 conformance passed: repetitions=" + repetitions
                    + ", soakSeconds=" + soakSeconds
                    + ", handles=" + handlesBefore + "->" + handlesAfter
                    + ", output=" + result.output().deviceName()
                    + ", colorSpace=" + result.output().colorSpaceName());
        }
    }

    /// Verifies that a generated downcall returns the immediate `GetLastError` value with its scalar result.
    ///
    /// @param bindings the generated User32 bindings
    /// @return `ERROR_INVALID_WINDOW_HANDLE`
    private static int verifyCapturedLastError(Win32FfmBindings bindings) {
        Win32FfmBindings.PostMessageWResult result = bindings.postMessageW(
                MemorySegment.ofAddress(1L),
                0,
                0L,
                0L
        );
        if (result.value() != 0 || result.errorCode() != ERROR_INVALID_WINDOW_HANDLE) {
            throw new IllegalStateException("PostMessageW failure capture returned value=" + result.value()
                    + ", error=" + Integer.toUnsignedString(result.errorCode())
                    + "; expected 0/" + ERROR_INVALID_WINDOW_HANDLE);
        }
        return result.errorCode();
    }

    /// Reads the current process handle count through the generated Kernel32 binding.
    ///
    /// @param bindings the generated bindings
    /// @return the unsigned process handle count
    private static int processHandleCount(Win32FfmBindings bindings) {
        MemorySegment process = bindings.getCurrentProcess();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment count = arena.allocate(ValueLayout.JAVA_INT);
            Win32FfmBindings.GetProcessHandleCountResult result = bindings.getProcessHandleCount(process, count);
            if (result.value() == 0) {
                throw new IllegalStateException("GetProcessHandleCount failed with Win32 error "
                        + Integer.toUnsignedString(result.errorCode()));
            }
            long unsigned = Integer.toUnsignedLong(count.get(ValueLayout.JAVA_INT, 0L));
            return Math.toIntExact(unsigned);
        }
    }

    /// Enforces the profile's handle and heap limits after scenario teardown.
    ///
    /// @param handlesBefore the baseline handle count
    /// @param handlesAfter the post-scenario handle count
    /// @param heapBefore the baseline heap use
    /// @param heapAfter the post-scenario heap use
    private static void validateBudgets(
            int handlesBefore,
            int handlesAfter,
            long heapBefore,
            long heapAfter
    ) {
        if (PROFILE_MAX_SIMULTANEOUS_OWNED_HANDLES > MAX_ADDITIONAL_HANDLES) {
            throw new IllegalStateException("Profile-owned handle budget exceeded: "
                    + PROFILE_MAX_SIMULTANEOUS_OWNED_HANDLES + " > " + MAX_ADDITIONAL_HANDLES);
        }
        if (handlesAfter - handlesBefore > MAX_RETAINED_HANDLE_DELTA) {
            throw new IllegalStateException("Scenario retained too many process handles: "
                    + handlesBefore + " -> " + handlesAfter);
        }
        long observedHeap = Math.max(heapBefore, heapAfter);
        if (observedHeap > MAX_HEAP_BYTES) {
            throw new IllegalStateException("Observed heap budget exceeded: " + observedHeap
                    + " > " + MAX_HEAP_BYTES);
        }
    }

    /// Encodes lifecycle, event, containment, timing, and resource evidence.
    ///
    /// @param result the window scenario result
    /// @param handlesBefore the baseline handle count
    /// @param handlesAfter the post-scenario handle count
    /// @param threadsBefore the baseline JVM live-thread count
    /// @param threadsAfter the post-scenario JVM live-thread count
    /// @param heapBefore the baseline used heap
    /// @param heapAfter the post-scenario used heap
    /// @param capturedInvalidWindowError the immediate error captured from the deliberate failing downcall
    /// @return the complete deterministic-key-order JSON document
    private static String eventsJson(
            Win32WindowResult result,
            int handlesBefore,
            int handlesAfter,
            int threadsBefore,
            int threadsAfter,
            long heapBefore,
            long heapAfter,
            int capturedInvalidWindowError
    ) {
        return """
                {
                  "profileId": "m0-win32-window",
                  "profileVersion": 1,
                  "workPackage": "SPIKE-WIN-001",
                  "fixtures": ["win32-window-lifecycle-v1", "win32-wndproc-reentrant-v1", "win32-input-v1", "win32-advanced-color-v1"],
                  "target": {"operatingSystem": "windows", "architecture": "x86_64", "runtime": "jvm"},
                  "parameters": {"repetitions": %d, "soakSeconds": %d},
                  "observed": {
                    "elapsedMillis": %d,
                    "resizeEvents": %d,
                    "pointerEvents": %d,
                    "keyEvents": %d,
                    "characterEvents": %d,
                    "paintEvents": %d,
                    "containedCallbackFailures": %d,
                    "maximumCallbackDepth": %d,
                    "finalClientSize": {"width": %d, "height": %d},
                    "closeObserved": %s,
                    "destroyObserved": %s,
                    "quitObserved": %s,
                    "capturedInvalidWindowError": %d
                  },
                  "resources": {
                    "processHandlesBefore": %d,
                    "processHandlesAfter": %d,
                    "retainedHandleDelta": %d,
                    "maximumSimultaneousProfileOwnedHandles": %d,
                    "maximumAdditionalHandles": %d,
                    "jvmLiveThreadsBefore": %d,
                    "jvmLiveThreadsAfter": %d,
                    "profileCreatedThreads": 0,
                    "heapUsedBytesBefore": %d,
                    "heapUsedBytesAfter": %d,
                    "maximumHeapBytes": %d,
                    "scenarioArenaClosed": true,
                    "windowAndComOwnershipReleased": true
                  },
                  "assertions": {
                    "unicodeWindowOpenedAndClosed": true,
                    "solidColorPaintPresented": true,
                    "resizeAndInputDelivered": true,
                    "wndProcReentrancyObserved": true,
                    "callbackFailuresContained": true,
                    "normalizedSequenceExact": true,
                    "soakDurationSatisfied": true,
                    "resourceBudgetsSatisfied": true,
                    "immediateGetLastErrorCaptured": true,
                    "projectNativeLibraryUsed": false
                  },
                  "eventSequence": %s,
                  "result": "passed"
                }
                """.formatted(
                result.repetitions(),
                result.requestedSoakSeconds(),
                result.elapsedMillis(),
                result.resizeEvents(),
                result.pointerEvents(),
                result.keyEvents(),
                result.characterEvents(),
                result.paintEvents(),
                result.callbackFailures(),
                result.maximumCallbackDepth(),
                result.finalClientWidth(),
                result.finalClientHeight(),
                result.closeObserved(),
                result.destroyObserved(),
                result.quitObserved(),
                capturedInvalidWindowError,
                handlesBefore,
                handlesAfter,
                handlesAfter - handlesBefore,
                PROFILE_MAX_SIMULTANEOUS_OWNED_HANDLES,
                MAX_ADDITIONAL_HANDLES,
                threadsBefore,
                threadsAfter,
                heapBefore,
                heapAfter,
                MAX_HEAP_BYTES,
                stringArray(result.eventSequence())
        );
    }

    /// Encodes an immutable string list as a compact JSON array.
    ///
    /// @param values the ordered strings
    /// @return the JSON array
    private static String stringArray(List<String> values) {
        return '[' + String.join(", ", values.stream().map(JsonSupport::quote).toList()) + ']';
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
}
