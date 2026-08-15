package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.FrameClock;
import org.glavo.himari.platform.api.PlatformEventLoop;
import org.glavo.himari.platform.api.ScheduledTask;
import org.glavo.himari.platform.api.ScheduledTaskState;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

/// Implements a deterministic, manually driven, nonblocking platform event loop.
///
/// Any thread may submit or cancel work. The creating thread advances time and dispatches callbacks.
/// Ready tasks are ordered by absolute deadline and then submission sequence. If a callback throws,
/// it is marked completed, dispatch stops, and the exception is propagated; other queued tasks
/// remain available to a later [#runUntilIdle()] call.
@NotNullByDefault
public final class HeadlessEventLoop implements PlatformEventLoop, AutoCloseable {
    /// Orders tasks by deadline and stable submission sequence.
    private static final Comparator<HeadlessScheduledTask> TASK_ORDER = Comparator
            .comparingLong(HeadlessScheduledTask::deadlineNanos)
            .thenComparingLong(HeadlessScheduledTask::sequence);

    /// The thread permitted to drive callback dispatch.
    private final Thread ownerThread;

    /// The manually advanced clock used by task deadlines.
    private final ManualFrameClock clock;

    /// The monitor protecting the queue, closure, task states, and submission sequence.
    private final Object lock = new Object();

    /// Pending tasks ordered by deadline and submission sequence.
    private final PriorityQueue<HeadlessScheduledTask> queue = new PriorityQueue<>(TASK_ORDER);

    /// The next positive task submission sequence, guarded by [#lock].
    private long nextSequence = 1L;

    /// Whether the loop permanently stopped accepting tasks, guarded by [#lock] and published
    /// through synchronized access.
    private boolean closed;

    /// Whether owner-thread dispatch is active; accessed only by the owner thread.
    private boolean dispatching;

    /// Creates a loop with a new clock at timestamp zero.
    public HeadlessEventLoop() {
        this(new ManualFrameClock());
    }

    /// Creates a loop driven by an existing manual clock owned by the current thread.
    ///
    /// @param clock the clock to drive
    /// @throws IllegalStateException if the current thread does not own `clock`
    public HeadlessEventLoop(ManualFrameClock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        clock.checkOwnerThread();
        this.ownerThread = Thread.currentThread();
    }

    /// Returns the manually advanced loop clock.
    ///
    /// @return the loop clock
    @Override
    public ManualFrameClock clock() {
        return clock;
    }

    /// Returns whether the calling thread owns dispatch.
    ///
    /// @return whether the caller is the owner thread
    @Override
    public boolean isOwnerThread() {
        return Thread.currentThread() == ownerThread;
    }

    /// Verifies that the calling thread owns dispatch.
    ///
    /// @throws IllegalStateException if called from another thread
    @Override
    public void checkOwnerThread() {
        if (!isOwnerThread()) {
            throw new IllegalStateException(
                    "Headless event loop is owned by thread '" + ownerThread.getName()
                            + "' but was accessed from '" + Thread.currentThread().getName() + "'"
            );
        }
    }

    /// Schedules a callback at the clock's current timestamp.
    ///
    /// @param callback the callback to execute
    /// @return the scheduled task handle
    /// @throws IllegalStateException if the loop is closed or task identifiers are exhausted
    @Override
    public ScheduledTask post(Runnable callback) {
        return schedule(clock.nowNanos(), callback);
    }

    /// Schedules a callback at an absolute manual-clock deadline.
    ///
    /// @param deadlineNanos the nonnegative absolute deadline
    /// @param callback the callback to execute
    /// @return the scheduled task handle
    /// @throws IllegalArgumentException if `deadlineNanos` is negative
    /// @throws IllegalStateException if the loop is closed or task identifiers are exhausted
    @Override
    public ScheduledTask schedule(long deadlineNanos, Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        if (deadlineNanos < 0L) {
            throw new IllegalArgumentException("Task deadline must be nonnegative");
        }
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("Headless event loop is closed");
            }
            if (nextSequence == Long.MAX_VALUE) {
                throw new IllegalStateException("Headless event-loop task identifiers are exhausted");
            }
            HeadlessScheduledTask task = new HeadlessScheduledTask(
                    this,
                    deadlineNanos,
                    nextSequence,
                    callback
            );
            nextSequence++;
            queue.add(task);
            return task;
        }
    }

    /// Executes every task ready at the current timestamp until none remain.
    ///
    /// Tasks posted by a callback for the current or an earlier timestamp execute in the same drain.
    /// A callback failure stops this drain after marking that task completed; later calls continue
    /// with the remaining queue.
    ///
    /// @return the number of callbacks that began execution
    /// @throws IllegalStateException if called from another thread or recursively during dispatch
    public long runUntilIdle() {
        checkOwnerThread();
        if (dispatching) {
            throw new IllegalStateException("Headless event-loop dispatch cannot be reentered");
        }

        dispatching = true;
        long executed = 0L;
        try {
            while (true) {
                HeadlessScheduledTask task = pollReadyTask();
                if (task == null) {
                    return executed;
                }
                executed = Math.incrementExact(executed);
                try {
                    task.callback().run();
                } finally {
                    task.complete();
                }
            }
        } finally {
            dispatching = false;
        }
    }

    /// Advances to an absolute timestamp and drains all work then ready.
    ///
    /// Delayed callbacks observe the target timestamp rather than replaying every missed interval.
    ///
    /// @param timestampNanos the new timestamp, no earlier than the current time
    /// @return the number of callbacks that began execution
    /// @throws IllegalArgumentException if the clock would move backwards
    /// @throws IllegalStateException if called from another thread or during dispatch
    public long advanceTo(long timestampNanos) {
        checkCanAdvance();
        clock.advanceTo(timestampNanos);
        return runUntilIdle();
    }

    /// Advances by a duration and drains all work then ready.
    ///
    /// @param deltaNanos the nonnegative duration
    /// @return the number of callbacks that began execution
    /// @throws IllegalArgumentException if `deltaNanos` is negative
    /// @throws ArithmeticException if the resulting timestamp exceeds `long` range
    /// @throws IllegalStateException if called from another thread or during dispatch
    public long advanceBy(long deltaNanos) {
        checkCanAdvance();
        clock.advanceBy(deltaNanos);
        return runUntilIdle();
    }

    /// Returns the number of callbacks still eligible to begin execution.
    ///
    /// @return the pending task count
    public int pendingTaskCount() {
        synchronized (lock) {
            int count = 0;
            for (HeadlessScheduledTask task : queue) {
                if (task.stateUnderLock() == ScheduledTaskState.PENDING) {
                    count++;
                }
            }
            return count;
        }
    }

    /// Returns whether this loop permanently stopped accepting work.
    ///
    /// @return whether the loop is closed
    @Override
    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    /// Closes the loop and cancels every callback that has not begun.
    ///
    /// The owner thread may close the loop from within the currently running callback. Closure is
    /// idempotent and never interrupts that callback.
    ///
    /// @throws IllegalStateException if called from another thread
    @Override
    public void close() {
        checkOwnerThread();
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            for (HeadlessScheduledTask task : queue) {
                task.cancelUnderLock();
            }
            queue.clear();
        }
    }

    /// Verifies that owner-thread time advancement is not reentrant.
    ///
    /// @throws IllegalStateException if called from another thread or during callback dispatch
    private void checkCanAdvance() {
        checkOwnerThread();
        if (dispatching) {
            throw new IllegalStateException("Manual time cannot advance during event-loop dispatch");
        }
    }

    /// Removes and starts the next ready noncancelled task.
    ///
    /// @return the task, or `null` if no task is ready
    private HeadlessScheduledTask pollReadyTask() {
        synchronized (lock) {
            while (true) {
                HeadlessScheduledTask task = queue.peek();
                if (task == null || task.deadlineNanos() > clock.nowNanos()) {
                    return null;
                }
                queue.remove();
                if (task.startUnderLock()) {
                    return task;
                }
            }
        }
    }

    /// Cancels one pending task while holding the event-loop monitor.
    ///
    /// @param task the task to cancel
    /// @return whether cancellation changed its state
    private boolean cancel(HeadlessScheduledTask task) {
        synchronized (lock) {
            if (!task.cancelUnderLock()) {
                return false;
            }
            queue.remove(task);
            return true;
        }
    }

    /// Marks one running task completed while holding the event-loop monitor.
    ///
    /// @param task the task to complete
    private void complete(HeadlessScheduledTask task) {
        synchronized (lock) {
            task.completeUnderLock();
        }
    }

    /// Stores one ordered task and its thread-safe lifecycle state.
    @NotNullByDefault
    private static final class HeadlessScheduledTask implements ScheduledTask {
        /// The loop whose monitor protects this task's state, released at a terminal state.
        private volatile @Nullable HeadlessEventLoop loop;

        /// The absolute manual-clock deadline.
        private final long deadlineNanos;

        /// The stable submission sequence used to break deadline ties.
        private final long sequence;

        /// The callback retained only while pending or running.
        private @Nullable Runnable callback;

        /// The current lifecycle state, guarded by the loop monitor and published for lock-free reads.
        private volatile ScheduledTaskState state = ScheduledTaskState.PENDING;

        /// Creates a pending scheduled task.
        ///
        /// @param loop the owning loop
        /// @param deadlineNanos the absolute deadline
        /// @param sequence the stable submission sequence
        /// @param callback the callback
        private HeadlessScheduledTask(
                HeadlessEventLoop loop,
                long deadlineNanos,
                long sequence,
                Runnable callback
        ) {
            this.loop = loop;
            this.deadlineNanos = deadlineNanos;
            this.sequence = sequence;
            this.callback = callback;
        }

        /// Returns the absolute deadline for queue ordering.
        ///
        /// @return the deadline
        private long deadlineNanos() {
            return deadlineNanos;
        }

        /// Returns the submission sequence for queue ordering.
        ///
        /// @return the sequence
        private long sequence() {
            return sequence;
        }

        /// Returns the callback to execute.
        ///
        /// @return the callback
        private Runnable callback() {
            return Objects.requireNonNull(callback, "callback");
        }

        /// Cancels this task if pending.
        ///
        /// @return whether this call changed the state
        @Override
        public boolean cancel() {
            @Nullable HeadlessEventLoop currentLoop = loop;
            return currentLoop != null && currentLoop.cancel(this);
        }

        /// Returns the published task state.
        ///
        /// @return the task state
        @Override
        public ScheduledTaskState state() {
            return state;
        }

        /// Returns the task state while the caller holds the loop monitor.
        ///
        /// @return the task state
        private ScheduledTaskState stateUnderLock() {
            return state;
        }

        /// Changes a pending task to cancelled while the caller holds the loop monitor.
        ///
        /// @return whether cancellation changed the state
        private boolean cancelUnderLock() {
            if (state != ScheduledTaskState.PENDING) {
                return false;
            }
            state = ScheduledTaskState.CANCELLED;
            callback = null;
            loop = null;
            return true;
        }

        /// Changes a pending task to running while the caller holds the loop monitor.
        ///
        /// @return whether the task began execution
        private boolean startUnderLock() {
            if (state != ScheduledTaskState.PENDING) {
                return false;
            }
            state = ScheduledTaskState.RUNNING;
            return true;
        }

        /// Marks this running task completed through the owning loop.
        private void complete() {
            Objects.requireNonNull(loop, "loop").complete(this);
        }

        /// Changes a running task to completed while the caller holds the loop monitor.
        private void completeUnderLock() {
            if (state != ScheduledTaskState.RUNNING) {
                throw new IllegalStateException("Only a running task may complete");
            }
            state = ScheduledTaskState.COMPLETED;
            callback = null;
            loop = null;
        }
    }
}
