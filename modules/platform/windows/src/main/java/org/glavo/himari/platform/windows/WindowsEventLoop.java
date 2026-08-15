package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.api.PlatformEventLoop;
import org.glavo.himari.platform.api.ScheduledTask;
import org.glavo.himari.platform.api.ScheduledTaskState;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;

/// Host-driven Windows event loop combining posted Java work with `PeekMessage` dispatch.
@NotNullByDefault
public final class WindowsEventLoop implements PlatformEventLoop, AutoCloseable {
    /// Orders tasks by deadline then sequence.
    private static final Comparator<WindowsScheduledTask> TASK_ORDER = Comparator
            .comparingLong(WindowsScheduledTask::deadlineNanos)
            .thenComparingLong(WindowsScheduledTask::sequence);

    /// The owner thread.
    private final Thread ownerThread;

    /// The monotonic clock.
    private final WindowsFrameClock clock;

    /// Protects the queue and closure.
    private final Object lock = new Object();

    /// Pending tasks.
    private final PriorityQueue<WindowsScheduledTask> queue = new PriorityQueue<>(TASK_ORDER);

    /// Next sequence.
    private long nextSequence = 1L;

    /// Whether the loop is closed.
    private boolean closed;

    /// Whether dispatch is active.
    private boolean dispatching;

    /// Creates a loop owned by the current thread.
    public WindowsEventLoop() {
        this.ownerThread = Thread.currentThread();
        this.clock = new WindowsFrameClock();
    }

    /// {@inheritDoc}
    @Override
    public WindowsFrameClock clock() {
        return clock;
    }

    /// {@inheritDoc}
    @Override
    public boolean isOwnerThread() {
        return Thread.currentThread() == ownerThread;
    }

    /// {@inheritDoc}
    @Override
    public void checkOwnerThread() {
        if (!isOwnerThread()) {
            throw new IllegalStateException("Windows event loop accessed from a non-owner thread");
        }
    }

    /// {@inheritDoc}
    @Override
    public ScheduledTask post(Runnable callback) {
        return schedule(clock.nowNanos(), callback);
    }

    /// {@inheritDoc}
    @Override
    public ScheduledTask schedule(long deadlineNanos, Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        if (deadlineNanos < 0L) {
            throw new IllegalArgumentException("Task deadline must be nonnegative");
        }
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("Windows event loop is closed");
            }
            WindowsScheduledTask task = new WindowsScheduledTask(this, deadlineNanos, nextSequence, callback);
            nextSequence++;
            queue.add(task);
            return task;
        }
    }

    /// Drains ready Java callbacks when the owner loop is not already dispatching.
    ///
    /// Used by move/resize modal-loop timer reentry. A nested dispatch is skipped rather than
    /// throwing so `WndProc` can keep pumping.
    ///
    /// @return the number of callbacks begun, or `0` when already dispatching
    public long drainReadyIfIdle() {
        checkOwnerThread();
        if (dispatching) {
            return 0L;
        }
        return runUntilIdle();
    }

    /// Drains every currently ready Java callback.
    ///
    /// @return the number of callbacks begun
    public long runUntilIdle() {
        checkOwnerThread();
        if (dispatching) {
            throw new IllegalStateException("Windows event-loop dispatch cannot be reentered");
        }
        dispatching = true;
        long executed = 0L;
        try {
            while (true) {
                WindowsScheduledTask task = pollReady();
                if (task == null) {
                    return executed;
                }
                executed++;
                try {
                    task.setStateUnderLock(ScheduledTaskState.RUNNING);
                    task.callback().run();
                } finally {
                    task.complete();
                }
            }
        } finally {
            dispatching = false;
        }
    }

    /// {@inheritDoc}
    @Override
    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    /// Closes the loop and cancels pending work.
    @Override
    public void close() {
        checkOwnerThread();
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            for (WindowsScheduledTask task : queue) {
                task.cancelUnderLock();
            }
            queue.clear();
        }
    }

    /// Cancels one task.
    ///
    /// @param task the task
    /// @return whether the task was pending
    boolean cancel(WindowsScheduledTask task) {
        synchronized (lock) {
            if (task.stateUnderLock() != ScheduledTaskState.PENDING) {
                return false;
            }
            task.cancelUnderLock();
            queue.remove(task);
            return true;
        }
    }

    /// Returns the current state of one task.
    ///
    /// @param task the task
    /// @return the state
    ScheduledTaskState stateOf(WindowsScheduledTask task) {
        synchronized (lock) {
            return task.stateUnderLock();
        }
    }

    /// Polls one ready task.
    ///
    /// @return the task, or `null`
    private @Nullable WindowsScheduledTask pollReady() {
        synchronized (lock) {
            WindowsScheduledTask head = queue.peek();
            if (head == null || head.deadlineNanos() > clock.nowNanos()) {
                return null;
            }
            queue.poll();
            if (head.stateUnderLock() != ScheduledTaskState.PENDING) {
                return pollReady();
            }
            return head;
        }
    }
}
