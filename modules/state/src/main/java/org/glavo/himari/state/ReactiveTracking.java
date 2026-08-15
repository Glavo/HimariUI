package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/// Maintains the synchronous, non-inheritable dependency-capture context for the current thread.
@NotNullByDefault
final class ReactiveTracking {
    /// The innermost active dependency capture, or `null` outside a derived computation.
    private static final ThreadLocal<@Nullable Capture> CURRENT = new ThreadLocal<>();

    /// Prevents instantiation of this utility class.
    private ReactiveTracking() {
    }

    /// Begins dependency capture for one derived computation.
    ///
    /// @param consumer the consumer being executed
    /// @return the new capture, which must be ended exactly once
    static Capture begin(ReactiveConsumerNode consumer) {
        Capture capture = new Capture(consumer, CURRENT.get());
        CURRENT.set(capture);
        return capture;
    }

    /// Ends one capture and restores its enclosing context.
    ///
    /// @param capture the current capture
    static void end(Capture capture) {
        if (CURRENT.get() != capture) {
            throw new IllegalStateException("Reactive capture stack is unbalanced");
        }
        @Nullable Capture previous = capture.previous();
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }

    /// Records a producer read in the innermost collecting capture.
    ///
    /// @param producer the producer read after it became current
    static void recordRead(ReactiveProducerNode producer) {
        @Nullable Capture capture = CURRENT.get();
        if (capture == null || !capture.isCollecting()) {
            return;
        }
        if (capture.consumer().graph() != producer.graph()) {
            throw new IllegalStateException("Reactive dependencies cannot cross state domains");
        }
        capture.record(producer);
    }

    /// Verifies that no dependency capture is currently executing.
    ///
    /// A capture may allocate new reactive owners for staged UI structure, but it must not publish
    /// source mutations while the corresponding dependency set is still private.
    ///
    /// @throws IllegalStateException if a derived computation or leaf observer is being captured
    static void checkStateWriteAllowed() {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("State writes cannot occur while capturing reactive dependencies");
        }
    }

    /// Runs a supplier with the enclosing dependency capture temporarily removed.
    ///
    /// @param supplier the synchronous supplier
    /// @param <T> the result type
    /// @return the supplier result
    static <T> T untracked(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        @Nullable Capture previous = CURRENT.get();
        CURRENT.remove();
        try {
            return supplier.get();
        } finally {
            if (previous != null) {
                CURRENT.set(previous);
            }
        }
    }

    /// Runs an action with the enclosing dependency capture temporarily removed.
    ///
    /// @param action the synchronous action
    static void untracked(Runnable action) {
        Objects.requireNonNull(action, "action");
        untracked(() -> {
            action.run();
            return Boolean.TRUE;
        });
    }

    /// Collects unique producer reads for one consumer execution.
    @NotNullByDefault
    static final class Capture {
        /// The consumer whose dependencies are being discovered.
        private final ReactiveConsumerNode consumer;

        /// The enclosing capture restored at completion, or `null`.
        private final @Nullable Capture previous;

        /// The producers in first-read order.
        private final ArrayList<ReactiveProducerNode> producers = new ArrayList<>();

        /// The identity set used to coalesce repeated reads.
        private final IdentityHashMap<ReactiveProducerNode, Boolean> seen = new IdentityHashMap<>();

        /// Whether producer reads are still part of the computation dependency set.
        private boolean collecting = true;

        /// Creates a nested capture.
        ///
        /// @param consumer the executing consumer
        /// @param previous the enclosing capture, or `null`
        private Capture(ReactiveConsumerNode consumer, @Nullable Capture previous) {
            this.consumer = consumer;
            this.previous = previous;
        }

        /// Returns the executing consumer.
        ///
        /// @return the consumer
        private ReactiveConsumerNode consumer() {
            return consumer;
        }

        /// Returns the enclosing capture.
        ///
        /// @return the enclosing capture, or `null`
        private @Nullable Capture previous() {
            return previous;
        }

        /// Returns whether reads are still being collected.
        ///
        /// @return whether collection is active
        private boolean isCollecting() {
            return collecting;
        }

        /// Records the first read of a producer.
        ///
        /// @param producer the producer
        private void record(ReactiveProducerNode producer) {
            if (seen.put(producer, Boolean.TRUE) == null) {
                producers.add(producer);
            }
        }

        /// Stops collection and snapshots every producer with its observed version.
        ///
        /// @return the immutable dependency list in first-read order
        @Unmodifiable List<ReactiveDependency> finish() {
            if (!collecting) {
                throw new IllegalStateException("Reactive capture has already finished collection");
            }
            collecting = false;
            List<ReactiveDependency> dependencies = new ArrayList<>(producers.size());
            for (ReactiveProducerNode producer : producers) {
                dependencies.add(new ReactiveDependency(producer, producer.semanticVersion()));
            }
            return List.copyOf(dependencies);
        }
    }
}
