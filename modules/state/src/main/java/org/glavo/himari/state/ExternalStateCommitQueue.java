package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/// Transfers state-update callbacks from arbitrary threads to a [StateDomain] owner thread.
///
/// Enqueue is lock-free and never invokes the submitted callback. Drain atomically detaches the
/// callbacks present at its start, restores their FIFO publication order, and executes them on the
/// owner thread. Callbacks enqueued after detachment remain for a later drain. Successful callbacks
/// publish together as at most one epoch; each failed callback rolls back to its own savepoint and
/// does not prevent later callbacks in the batch from running.
@NotNullByDefault
public final class ExternalStateCommitQueue {
    /// The domain whose owner drains this queue.
    private final StateDomain domain;

    /// The newest pending entry in a lock-free stack.
    private final AtomicReference<@Nullable Entry> head;

    /// Creates an empty queue for a domain.
    ///
    /// @param domain the owning domain
    ExternalStateCommitQueue(StateDomain domain) {
        this.domain = domain;
        this.head = new AtomicReference<>();
    }

    /// Enqueues a callback without running it.
    ///
    /// The callback may be submitted from any thread. It must perform only bounded synchronous work
    /// suitable for the domain owner thread. Its state writes may target only this queue's domain.
    ///
    /// @param update the callback to execute during a future drain
    public void enqueue(Runnable update) {
        Objects.requireNonNull(update, "update");
        @Nullable Entry observed;
        Entry replacement;
        do {
            observed = head.get();
            replacement = new Entry(update, observed);
        } while (!head.compareAndSet(observed, replacement));
    }

    /// Detaches and executes one FIFO batch on the domain owner thread.
    ///
    /// Exceptions and errors thrown by individual callbacks are returned in the result and do not
    /// escape this method. A failure that occurs while publishing the outer transaction itself is
    /// not attributable to an individual callback and may escape.
    ///
    /// @return the batch result, including an empty result when no callback was pending
    /// @throws IllegalStateException if called outside the owner thread or inside a state transaction
    public ExternalCommitResult drain() {
        domain.checkOwnerThread();
        StateTransaction.checkNoActiveTransaction(domain, "External commit drain");
        long epochBefore = domain.epoch();
        @Nullable Entry detached = head.getAndSet(null);
        if (detached == null) {
            return new ExternalCommitResult(0, 0, epochBefore, epochBefore, new ArrayList<>());
        }

        @Nullable Entry fifoHead = null;
        int capturedCount = 0;
        @Nullable Entry cursor = detached;
        while (cursor != null) {
            fifoHead = new Entry(cursor.update, fifoHead);
            cursor = cursor.next;
            capturedCount++;
        }
        Entry first = Objects.requireNonNull(fifoHead, "fifoHead");
        DrainAccumulator accumulator = new DrainAccumulator();
        StateTransaction.run(domain, () -> accumulator.execute(domain, first));
        return new ExternalCommitResult(
                capturedCount,
                accumulator.successfulCount,
                epochBefore,
                domain.epoch(),
                accumulator.failures
        );
    }

    /// Returns whether no callback is currently pending for a future drain.
    ///
    /// A batch already detached by the owner thread is not counted.
    ///
    /// @return whether the pending stack is empty at the time of the call
    public boolean isEmpty() {
        return head.get() == null;
    }

    /// Returns the number of callbacks currently pending for a future drain.
    ///
    /// A batch already detached by the owner thread is not counted.
    ///
    /// The result describes the immutable stack reachable from one head read. Concurrent enqueue or
    /// drain operations may complete before this traversal returns.
    ///
    /// @return the observed pending callback count
    public int size() {
        int count = 0;
        @Nullable Entry cursor = head.get();
        while (cursor != null) {
            count++;
            cursor = cursor.next;
        }
        return count;
    }

    /// Holds one queued callback and its older stack entry.
    @NotNullByDefault
    private static final class Entry {
        /// The deferred callback.
        private final Runnable update;

        /// The next older entry in this immutable stack.
        private final @Nullable Entry next;

        /// Creates a stack entry.
        ///
        /// @param update the deferred callback
        /// @param next the older entry, or `null`
        private Entry(Runnable update, @Nullable Entry next) {
            this.update = update;
            this.next = next;
        }
    }

    /// Accumulates successful callbacks and contained failures for one detached batch.
    @NotNullByDefault
    private static final class DrainAccumulator {
        /// The number of callbacks that returned normally.
        private int successfulCount;

        /// The failures accumulated in FIFO order.
        private final ArrayList<ExternalCommitFailure> failures = new ArrayList<>();

        /// Creates an empty accumulator.
        private DrainAccumulator() {
        }

        /// Executes every callback with an independent nested-transaction savepoint.
        ///
        /// @param domain the transaction domain
        /// @param first the first FIFO entry
        private void execute(StateDomain domain, Entry first) {
            int index = 0;
            @Nullable Entry cursor = first;
            while (cursor != null) {
                try {
                    StateTransaction.run(domain, cursor.update);
                    successfulCount++;
                } catch (RuntimeException | Error failure) {
                    failures.add(new ExternalCommitFailure(index, failure));
                }
                cursor = cursor.next;
                index++;
            }
        }
    }
}
