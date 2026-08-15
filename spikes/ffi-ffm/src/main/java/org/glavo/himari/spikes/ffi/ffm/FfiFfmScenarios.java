package org.glavo.himari.spikes.ffi.ffm;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.spikes.ffi.generated.CRuntimeFfmBindings;
import org.glavo.himari.spikes.ffi.generated.CRuntimeLayouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.Serial;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/// Executes the primitive, structure-return, and reentrant-callback FFM fixtures.
@NotNullByDefault
public final class FfiFfmScenarios {
    /// The C `int` carrier used to initialize and inspect native fixture storage.
    private static final ValueLayout.OfInt C_INT = ValueLayout.JAVA_INT;

    /// Prevents instantiation of this utility class.
    private FfiFfmScenarios() {
    }

    /// Executes at least the requested repetitions and duration of the complete fixture set.
    ///
    /// @param minimumRepetitions the minimum number of successful `qsort` rounds
    /// @param minimumDuration the minimum callback-soak duration
    /// @return the verified execution summary
    /// @throws IllegalArgumentException if a minimum is negative
    /// @throws IllegalStateException if any ABI assertion fails
    public static Summary run(int minimumRepetitions, Duration minimumDuration) {
        if (minimumRepetitions < 0) {
            throw new IllegalArgumentException("minimumRepetitions must be non-negative");
        }
        if (minimumDuration.isNegative()) {
            throw new IllegalArgumentException("minimumDuration must be non-negative");
        }

        long start = System.nanoTime();
        long minimumNanos = minimumDuration.toNanos();
        try (Arena libraryArena = Arena.ofConfined();
             Arena dataArena = Arena.ofConfined()) {
            SymbolLookup symbols = HostCRuntime.open(libraryArena);
            CRuntimeFfmBindings bindings = new CRuntimeFfmBindings(symbols);
            verifyGeneratedLayouts();
            verifyPrimitiveAndPointerCalls(bindings, dataArena);
            verifyStructureReturn(bindings, dataArena);

            int[] sourceValues = sourceValues();
            MemorySegment values = dataArena.allocate(CRuntimeLayouts.I32, sourceValues.length);
            AtomicBoolean sameThread = new AtomicBoolean(true);
            AtomicLong callbackInvocations = new AtomicLong();
            AtomicLong reentrantDowncalls = new AtomicLong();
            CallbackFailureQueue unexpectedFailures = new CallbackFailureQueue();
            Thread owner = Thread.currentThread();
            int repetitions = 0;

            try (Arena callbackArena = Arena.ofConfined()) {
                MemorySegment comparator = bindings.createCompareI32Stub((left, right) -> {
                    sameThread.compareAndSet(true, Thread.currentThread() == owner);
                    callbackInvocations.incrementAndGet();
                    int leftValue = left.get(C_INT, 0);
                    int rightValue = right.get(C_INT, 0);
                    int leftMagnitude = bindings.abs(leftValue);
                    int rightMagnitude = bindings.abs(rightValue);
                    reentrantDowncalls.addAndGet(2);
                    int magnitudeOrder = Integer.compare(leftMagnitude, rightMagnitude);
                    return magnitudeOrder != 0 ? magnitudeOrder : Integer.compare(leftValue, rightValue);
                }, unexpectedFailures, callbackArena);

                do {
                    reset(values, sourceValues);
                    bindings.qsort(values, sourceValues.length, C_INT.byteSize(), comparator);
                    requireSortedByMagnitude(values, sourceValues.length);
                    repetitions++;
                } while (repetitions < minimumRepetitions || System.nanoTime() - start < minimumNanos);
            }

            require(unexpectedFailures.isEmpty(), "The ordinary comparator published a callback failure");
            boolean exceptionContained = verifyExceptionContainment(bindings, dataArena);
            boolean lifetimeRejected = verifyClosedCallbackLifetime(bindings, dataArena);
            long durationNanos = System.nanoTime() - start;
            require(sameThread.get(), "qsort delivered a callback on a different thread");
            require(lifetimeRejected, "A closed callback stub was accepted by the downcall");
            require(callbackInvocations.get() > 0, "qsort did not invoke the generated upcall stub");
            require(reentrantDowncalls.get() == callbackInvocations.get() * 2,
                    "The callback reentrant-downcall count is inconsistent");
            return new Summary(
                    HostCRuntime.libraryName(),
                    repetitions,
                    callbackInvocations.get(),
                    reentrantDowncalls.get(),
                    durationNanos,
                    sameThread.get(),
                    lifetimeRejected,
                    exceptionContained
            );
        }
    }

    /// Verifies that generated layouts match the canonical 64-bit fixture.
    private static void verifyGeneratedLayouts() {
        require(CRuntimeLayouts.I32.byteSize() == 4, "Generated C int size is not four bytes");
        require(CRuntimeLayouts.I32.byteAlignment() == 4, "Generated C int alignment is not four bytes");
        require(CRuntimeLayouts.I32_PTR.byteSize() == 8, "Generated pointer size is not eight bytes");
        require(CRuntimeLayouts.DIV_RESULT.byteSize() == 8, "Generated div_t size is not eight bytes");
        require(CRuntimeLayouts.DIV_RESULT.byteAlignment() == 4, "Generated div_t alignment is not four bytes");
    }

    /// Verifies exact scalar and pointer downcalls.
    ///
    /// @param bindings the linked C runtime bindings
    /// @param arena the temporary native-data arena
    private static void verifyPrimitiveAndPointerCalls(CRuntimeFfmBindings bindings, Arena arena) {
        require(bindings.abs(-37) == 37, "C abs returned an unexpected value");
        MemorySegment text = arena.allocateFrom("HimariUI");
        require(bindings.strlen(text) == 8, "C strlen returned an unexpected value");
    }

    /// Verifies the generated by-value `div_t` return contract.
    ///
    /// @param bindings the linked C runtime bindings
    /// @param arena the result allocator
    private static void verifyStructureReturn(CRuntimeFfmBindings bindings, Arena arena) {
        MemorySegment result = bindings.div(arena, 29, 5);
        int quotient = result.get(C_INT, CRuntimeLayouts.DIV_RESULT_QUOTIENT_OFFSET);
        int remainder = result.get(C_INT, CRuntimeLayouts.DIV_RESULT_REMAINDER_OFFSET);
        require(quotient == 5, "C div returned an unexpected quotient: " + quotient);
        require(remainder == 4, "C div returned an unexpected remainder: " + remainder);
    }

    /// Verifies that a Java callback failure is reported and cannot unwind through `qsort`.
    ///
    /// @param bindings the linked C runtime bindings
    /// @param dataArena the native-data arena
    /// @return whether the exact marker failure was contained
    private static boolean verifyExceptionContainment(CRuntimeFfmBindings bindings, Arena dataArena) {
        CallbackFailureQueue failures = new CallbackFailureQueue();
        CallbackProbeException marker = new CallbackProbeException("expected callback probe");
        MemorySegment values = dataArena.allocateFrom(C_INT, 2, 1);
        AtomicBoolean rejectingSinkObservedMarker = new AtomicBoolean();
        try (Arena callbackArena = Arena.ofConfined()) {
            MemorySegment comparator = bindings.createCompareI32Stub((left, right) -> {
                throw marker;
            }, failures, callbackArena);
            bindings.qsort(values, 2, C_INT.byteSize(), comparator);

            MemorySegment rejectingSinkComparator = bindings.createCompareI32Stub((left, right) -> {
                throw marker;
            }, failure -> {
                rejectingSinkObservedMarker.compareAndSet(false, failure == marker);
                throw new CallbackProbeException("expected failure-sink probe");
            }, callbackArena);
            bindings.qsort(values, 2, C_INT.byteSize(), rejectingSinkComparator);
        }
        List<Throwable> contained = failures.drain();
        require(!contained.isEmpty(), "The callback marker failure was not published");
        require(contained.stream().allMatch(failure -> failure == marker),
                "The callback failure queue contains an unexpected failure");
        require(rejectingSinkObservedMarker.get(), "The rejecting failure sink did not receive the callback marker");
        return true;
    }

    /// Verifies that a callback pointer cannot be invoked after its arena closes.
    ///
    /// @param bindings the linked C runtime bindings
    /// @param dataArena the native-data arena
    /// @return whether the closed scope was rejected before entering native code
    private static boolean verifyClosedCallbackLifetime(CRuntimeFfmBindings bindings, Arena dataArena) {
        MemorySegment expired;
        try (Arena callbackArena = Arena.ofConfined()) {
            expired = bindings.createCompareI32Stub((left, right) -> 0, failure -> {
            }, callbackArena);
        }
        MemorySegment values = dataArena.allocateFrom(C_INT, 2, 1);
        try {
            bindings.qsort(values, 2, C_INT.byteSize(), expired);
        } catch (IllegalStateException expected) {
            return true;
        }
        return false;
    }

    /// Returns the deterministic unsorted input values used by every callback round.
    ///
    /// @return a new mutable value array
    private static int[] sourceValues() {
        return new int[]{7, -1, 4, -3, 2, -6};
    }

    /// Restores native array contents before one `qsort` round.
    ///
    /// @param segment the native integer array
    /// @param values the source values
    private static void reset(MemorySegment segment, int[] values) {
        for (int index = 0; index < values.length; index++) {
            segment.setAtIndex(C_INT, index, values[index]);
        }
    }

    /// Requires native array contents to be ordered by magnitude and then signed value.
    ///
    /// @param segment the sorted native integer array
    /// @param count the element count
    private static void requireSortedByMagnitude(MemorySegment segment, int count) {
        int[] actual = new int[count];
        for (int index = 0; index < count; index++) {
            actual[index] = segment.getAtIndex(C_INT, index);
        }
        int[] expected = {-1, 2, -3, 4, -6, 7};
        require(Arrays.equals(actual, expected), "Unexpected qsort result: " + Arrays.toString(actual));
    }

    /// Requires one conformance condition.
    ///
    /// @param condition the condition to require
    /// @param message the failure message
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /// Summarizes one fully verified FFM scenario execution.
    ///
    /// @param systemLibrary the loaded system C runtime
    /// @param repetitions the completed `qsort` rounds
    /// @param callbackInvocations the observed native-to-Java calls
    /// @param reentrantDowncalls the Java-to-native calls made from callbacks
    /// @param durationNanos the complete scenario duration
    /// @param threadConfined whether every callback ran on the initiating thread
    /// @param callbackArenaLifetimeRejected whether a closed callback pointer was rejected
    /// @param exceptionContained whether a callback failure was contained and published
    @NotNullByDefault
    public record Summary(
            String systemLibrary,
            int repetitions,
            long callbackInvocations,
            long reentrantDowncalls,
            long durationNanos,
            boolean threadConfined,
            boolean callbackArenaLifetimeRejected,
            boolean exceptionContained
    ) {
        /// Creates a verified execution summary.
        public Summary {
            if (systemLibrary.isBlank()) {
                throw new IllegalArgumentException("systemLibrary must not be blank");
            }
        }
    }

    /// Marks the intentional callback failure used by the containment fixture.
    @NotNullByDefault
    private static final class CallbackProbeException extends RuntimeException {
        /// The serialization version of this test-only failure type.
        @Serial
        private static final long serialVersionUID = 1L;

        /// Creates an intentional callback probe failure.
        ///
        /// @param message the diagnostic message
        private CallbackProbeException(String message) {
            super(message);
        }
    }
}
