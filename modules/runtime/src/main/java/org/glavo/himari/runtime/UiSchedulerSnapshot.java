package org.glavo.himari.runtime;

import org.jetbrains.annotations.NotNullByDefault;

/// Summarizes application scheduler queues, ownership, and lifetime counters.
///
/// @param closed whether the scheduler stopped accepting work
/// @param pendingStateUpdates callbacks waiting for the next state batch
/// @param stateDrainScheduled whether a host callback currently owns that pending batch
/// @param registeredWindowSchedulers active per-window frame schedulers
/// @param retainedFailures failures currently retained for diagnostics
/// @param droppedFailures failures not retained because the diagnostic buffer was full or its
/// sequence was exhausted
/// @param stateBatches state batches drained on the owner context
/// @param stateUpdates state callbacks attempted across drained batches
/// @param stateUpdateFailures state callbacks that failed and rolled back
/// @param stateBatchFailures outer state-batch publications that failed
/// @param frames frame callbacks attempted
/// @param coalescedFrameRequests explicit requests consumed by attempted frames
/// @param frameCallbackFailures frame callbacks whose failures were contained
/// @param redrawRequestFailures deferred follow-up redraw requests that failed
@NotNullByDefault
public record UiSchedulerSnapshot(
        boolean closed,
        int pendingStateUpdates,
        boolean stateDrainScheduled,
        int registeredWindowSchedulers,
        int retainedFailures,
        long droppedFailures,
        long stateBatches,
        long stateUpdates,
        long stateUpdateFailures,
        long stateBatchFailures,
        long frames,
        long coalescedFrameRequests,
        long frameCallbackFailures,
        long redrawRequestFailures
) {
    /// Validates every count.
    public UiSchedulerSnapshot {
        if (pendingStateUpdates < 0 || registeredWindowSchedulers < 0 || retainedFailures < 0
                || droppedFailures < 0L || stateBatches < 0L || stateUpdates < 0L
                || stateUpdateFailures < 0L || stateBatchFailures < 0L || frames < 0L
                || coalescedFrameRequests < 0L || frameCallbackFailures < 0L
                || redrawRequestFailures < 0L) {
            throw new IllegalArgumentException("Scheduler counts must be non-negative");
        }
        if (stateUpdateFailures > stateUpdates || frameCallbackFailures > frames) {
            throw new IllegalArgumentException("Failure counts must not exceed attempted work");
        }
    }
}
