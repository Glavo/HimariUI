package org.glavo.himari.runtime;

import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.platform.headless.HeadlessBackend;
import org.glavo.himari.platform.headless.HeadlessPlatform;
import org.glavo.himari.platform.headless.HeadlessWindow;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.MutableState;
import org.glavo.himari.state.StateDomain;
import org.glavo.himari.state.StateTransaction;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies owner-context state scheduling, per-window frame coalescing, and failure containment.
@NotNullByDefault
final class UiSchedulerTest {
    /// Verifies cross-thread FIFO state batching, nested rollback, and owner-thread execution.
    ///
    /// @throws InterruptedException if the test thread is interrupted while joining its worker
    @Test
    void batchesCrossThreadStateUpdatesAndContainsFailures() throws InterruptedException {
        HeadlessPlatform platform = openPlatform();
        StateDomain domain = new StateDomain();
        IntState value = domain.intState(0);
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), domain);
        Thread ownerThread = Thread.currentThread();
        ArrayList<String> order = new ArrayList<>();
        AtomicReference<@Nullable Thread> callbackThread = new AtomicReference<>();

        Thread worker = Thread.ofPlatform().name("ui-state-ingress").start(() -> {
            scheduler.enqueueStateUpdate(() -> {
                callbackThread.set(Thread.currentThread());
                order.add("first");
                value.set(1);
            });
            scheduler.enqueueStateUpdate(() -> {
                order.add("failed");
                value.set(99);
                throw new IllegalStateException("planned state failure");
            });
            scheduler.enqueueStateUpdate(() -> {
                order.add("third");
                value.set(value.get() + 1);
            });
        });
        worker.join();

        assertEquals(0, value.get());
        assertEquals(0L, domain.epoch());
        assertEquals(3, scheduler.snapshot().pendingStateUpdates());
        assertEquals(1, platform.eventLoop().pendingTaskCount());

        platform.eventLoop().runUntilIdle();

        assertEquals(List.of("first", "failed", "third"), order);
        assertSame(ownerThread, callbackThread.get());
        assertEquals(2, value.get());
        assertEquals(1L, domain.epoch());
        UiSchedulerSnapshot snapshot = scheduler.snapshot();
        assertEquals(1L, snapshot.stateBatches());
        assertEquals(3L, snapshot.stateUpdates());
        assertEquals(1L, snapshot.stateUpdateFailures());
        assertEquals(0L, snapshot.stateBatchFailures());
        UiSchedulerFailure failure = scheduler.failures().getFirst();
        assertEquals(UiSchedulerFailureKind.STATE_UPDATE, failure.kind());
        assertEquals(1L, failure.contextSequence());
        assertEquals("planned state failure", failure.cause().getMessage());
    }

    /// Verifies that work submitted from a running batch publishes in a later transaction.
    @Test
    void defersUpdatesSubmittedDuringDrain() {
        HeadlessPlatform platform = openPlatform();
        StateDomain domain = new StateDomain();
        IntState value = domain.intState(0);
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), domain);

        scheduler.enqueueStateUpdate(() -> {
            value.set(1);
            scheduler.enqueueStateUpdate(() -> value.set(2));
        });
        platform.eventLoop().runUntilIdle();

        assertEquals(2, value.get());
        assertEquals(2L, domain.epoch());
        assertEquals(2L, scheduler.snapshot().stateBatches());
        assertEquals(2L, scheduler.snapshot().stateUpdates());
    }

    /// Verifies bounded scheduler-owned state ingress without executing rejected work.
    @Test
    void rejectsStateIngressBeyondConfiguredCapacity() {
        HeadlessPlatform platform = openPlatform();
        StateDomain domain = new StateDomain();
        IntState value = domain.intState(0);
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), domain, 2, 8);

        scheduler.enqueueStateUpdate(() -> value.set(value.get() + 1));
        scheduler.enqueueStateUpdate(() -> value.set(value.get() + 1));
        assertThrows(
                RejectedExecutionException.class,
                () -> scheduler.enqueueStateUpdate(() -> value.set(100))
        );
        assertEquals(2, scheduler.snapshot().pendingStateUpdates());

        platform.eventLoop().runUntilIdle();

        assertEquals(2, value.get());
        assertEquals(2L, scheduler.snapshot().stateUpdates());
    }

    /// Verifies that failure during outer publication is diagnosed without escaping host dispatch.
    @Test
    void containsOuterStatePublicationFailure() {
        HeadlessPlatform platform = openPlatform();
        StateDomain domain = new StateDomain();
        CommitFailureValue initial = new CommitFailureValue(true);
        MutableState<CommitFailureValue> value = domain.mutableState(initial);
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), domain);

        scheduler.enqueueStateUpdate(() -> value.set(new CommitFailureValue(false)));
        platform.eventLoop().runUntilIdle();

        assertSame(initial, value.get());
        assertEquals(0L, domain.epoch());
        assertEquals(1L, scheduler.snapshot().stateBatches());
        assertEquals(1L, scheduler.snapshot().stateUpdates());
        assertEquals(0L, scheduler.snapshot().stateUpdateFailures());
        assertEquals(1L, scheduler.snapshot().stateBatchFailures());
        assertEquals(UiSchedulerFailureKind.STATE_BATCH, scheduler.failures().getFirst().kind());
        assertEquals("planned publication failure", scheduler.failures().getFirst().cause().getMessage());
    }

    /// Verifies exact request coalescing, clock sampling, and independent window routing.
    @Test
    void coalescesFramesIndependentlyPerWindow() {
        HeadlessPlatform platform = openPlatform();
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), new StateDomain());
        HeadlessWindow firstWindow = createWindow(platform, scheduler, "First");
        HeadlessWindow secondWindow = createWindow(platform, scheduler, "Second");
        ArrayList<FrameTick> ticks = new ArrayList<>();
        WindowFrameScheduler first = scheduler.createFrameScheduler(firstWindow, ticks::add);
        WindowFrameScheduler second = scheduler.createFrameScheduler(secondWindow, ticks::add);

        platform.clock().advanceTo(50L);
        assertEquals(1L, first.requestFrame());
        assertEquals(2L, first.requestFrame());
        assertEquals(3L, first.requestFrame());
        assertEquals(1L, second.requestFrame());
        assertEquals(2L, second.requestFrame());
        assertEquals(2, platform.eventLoop().pendingTaskCount());

        platform.eventLoop().runUntilIdle();

        assertEquals(2, ticks.size());
        assertEquals(firstWindow.id(), ticks.get(0).windowId());
        assertEquals(3L, ticks.get(0).requestGeneration());
        assertEquals(3L, ticks.get(0).coalescedRequestCount());
        assertEquals(50L, ticks.get(0).timestampNanos());
        assertEquals(secondWindow.id(), ticks.get(1).windowId());
        assertEquals(2L, ticks.get(1).coalescedRequestCount());
        assertEquals(0L, first.snapshot().pendingRequestCount());
        assertEquals(0L, second.snapshot().pendingRequestCount());
        assertEquals(2L, scheduler.snapshot().frames());
        assertEquals(5L, scheduler.snapshot().coalescedFrameRequests());

        first.requestFrame();
        first.close();
        assertThrows(
                IllegalStateException.class,
                () -> scheduler.createFrameScheduler(firstWindow, ticks::add)
        );

        firstWindow.closeAsync();
        platform.eventLoop().runUntilIdle();
        assertTrue(first.snapshot().closed());
        assertFalse(second.snapshot().closed());
        assertEquals(2, ticks.size());
        assertEquals(1, scheduler.snapshot().registeredWindowSchedulers());
    }

    /// Verifies that requests made during a frame obtain one follow-up redraw and one later tick.
    @Test
    void schedulesOneFollowUpForRequestsMadeDuringFrame() {
        HeadlessPlatform platform = openPlatform();
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), new StateDomain());
        HeadlessWindow window = createWindow(platform, scheduler, "Follow-up");
        ArrayList<FrameTick> ticks = new ArrayList<>();
        AtomicReference<@Nullable WindowFrameScheduler> frameReference = new AtomicReference<>();
        WindowFrameScheduler frame = scheduler.createFrameScheduler(window, tick -> {
            ticks.add(tick);
            if (ticks.size() == 1) {
                require(frameReference.get()).requestFrame();
                require(frameReference.get()).requestFrame();
            }
        });
        frameReference.set(frame);

        frame.requestFrame();
        platform.eventLoop().runUntilIdle();

        assertEquals(2, ticks.size());
        assertEquals(1L, ticks.get(0).coalescedRequestCount());
        assertEquals(1L, ticks.get(0).requestGeneration());
        assertEquals(2L, ticks.get(1).coalescedRequestCount());
        assertEquals(3L, ticks.get(1).requestGeneration());
        assertEquals(3L, frame.snapshot().deliveredGeneration());
        assertEquals(0, platform.eventLoop().pendingTaskCount());
    }

    /// Verifies frame failure containment and oldest-first bounded diagnostic eviction.
    @Test
    void containsFrameFailuresAndBoundsDiagnostics() {
        HeadlessPlatform platform = openPlatform();
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), new StateDomain(), 8, 2);
        HeadlessWindow window = createWindow(platform, scheduler, "Failures");
        AtomicInteger attempts = new AtomicInteger();
        WindowFrameScheduler frame = scheduler.createFrameScheduler(window, tick -> {
            int attempt = attempts.incrementAndGet();
            throw new IllegalStateException("frame failure " + attempt);
        });

        for (int index = 0; index < 3; index++) {
            frame.requestFrame();
            platform.eventLoop().runUntilIdle();
        }

        UiSchedulerSnapshot snapshot = scheduler.snapshot();
        assertEquals(3L, snapshot.frames());
        assertEquals(3L, snapshot.frameCallbackFailures());
        assertEquals(2, snapshot.retainedFailures());
        assertEquals(1L, snapshot.droppedFailures());
        List<UiSchedulerFailure> failures = scheduler.failures();
        assertEquals(List.of(2L, 3L), failures.stream().map(UiSchedulerFailure::sequence).toList());
        assertEquals("frame failure 2", failures.getFirst().cause().getMessage());
        assertEquals(2, scheduler.drainFailures().size());
        assertTrue(scheduler.failures().isEmpty());
    }

    /// Verifies thread-safe request admission and one exact coalesced frame under contention.
    ///
    /// @throws InterruptedException if the test thread is interrupted while coordinating workers
    @Test
    void coalescesConcurrentFrameRequests() throws InterruptedException {
        HeadlessPlatform platform = openPlatform();
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), new StateDomain());
        HeadlessWindow window = createWindow(platform, scheduler, "Concurrent");
        ArrayList<FrameTick> ticks = new ArrayList<>();
        WindowFrameScheduler frame = scheduler.createFrameScheduler(window, ticks::add);
        CountDownLatch ready = new CountDownLatch(4);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<@Nullable Throwable> workerFailure = new AtomicReference<>();
        ArrayList<Thread> workers = new ArrayList<>();

        for (int workerIndex = 0; workerIndex < 4; workerIndex++) {
            Thread worker = Thread.ofPlatform().name("frame-request-" + workerIndex).start(() -> {
                ready.countDown();
                try {
                    start.await();
                    for (int requestIndex = 0; requestIndex < 250; requestIndex++) {
                        frame.requestFrame();
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    workerFailure.compareAndSet(null, failure);
                } catch (RuntimeException | Error failure) {
                    workerFailure.compareAndSet(null, failure);
                }
            });
            workers.add(worker);
        }
        ready.await();
        start.countDown();
        for (Thread worker : workers) {
            worker.join();
        }

        assertNull(workerFailure.get());
        assertEquals(1, platform.eventLoop().pendingTaskCount());
        assertEquals(1_000L, frame.snapshot().requestedGeneration());
        platform.eventLoop().runUntilIdle();
        assertEquals(1, ticks.size());
        assertEquals(1_000L, ticks.getFirst().coalescedRequestCount());
        assertEquals(1_000L, ticks.getFirst().requestGeneration());
    }

    /// Verifies that a failed deferred redraw is contained and clears stranded pending requests.
    @Test
    void containsDeferredRedrawFailure() {
        HeadlessPlatform platform = openPlatform();
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), new StateDomain());
        HeadlessWindow window = createWindow(platform, scheduler, "Redraw failure");
        AtomicReference<@Nullable WindowFrameScheduler> frameReference = new AtomicReference<>();
        WindowFrameScheduler frame = scheduler.createFrameScheduler(window, tick -> {
            require(frameReference.get()).requestFrame();
            platform.eventLoop().close();
        });
        frameReference.set(frame);

        frame.requestFrame();
        platform.eventLoop().runUntilIdle();

        assertTrue(platform.eventLoop().isClosed());
        assertEquals(0L, frame.snapshot().pendingRequestCount());
        assertFalse(frame.snapshot().hostRedrawOutstanding());
        assertEquals(1L, scheduler.snapshot().redrawRequestFailures());
        assertEquals(UiSchedulerFailureKind.REDRAW_REQUEST, scheduler.failures().getFirst().kind());
        scheduler.close();
    }

    /// Verifies deterministic scheduler closure without closing borrowed host resources.
    @Test
    void closesPendingWorkWithoutOwningHostResources() {
        HeadlessPlatform platform = openPlatform();
        StateDomain domain = new StateDomain();
        IntState value = domain.intState(0);
        UiScheduler scheduler = new UiScheduler(platform.eventLoop(), domain);
        HeadlessWindow window = createWindow(platform, scheduler, "Close");
        AtomicInteger frames = new AtomicInteger();
        WindowFrameScheduler frame = scheduler.createFrameScheduler(window, tick ->
                frames.incrementAndGet());
        scheduler.enqueueStateUpdate(() -> value.set(7));
        frame.requestFrame();

        assertThrows(
                IllegalStateException.class,
                () -> StateTransaction.run(domain, scheduler::close)
        );
        assertFalse(scheduler.snapshot().closed());

        scheduler.close();

        assertEquals(7, value.get());
        assertEquals(1L, domain.epoch());
        assertTrue(scheduler.snapshot().closed());
        assertEquals(0, scheduler.snapshot().pendingStateUpdates());
        assertFalse(scheduler.snapshot().stateDrainScheduled());
        assertEquals(0, scheduler.snapshot().registeredWindowSchedulers());
        assertTrue(frame.snapshot().closed());
        assertFalse(platform.eventLoop().isClosed());
        assertFalse(window.isClosed());
        assertThrows(IllegalStateException.class, () -> scheduler.enqueueStateUpdate(() -> {
        }));
        assertThrows(IllegalStateException.class, frame::requestFrame);

        platform.eventLoop().runUntilIdle();
        assertEquals(0, frames.get());
        scheduler.close();
    }

    /// Creates a routed Headless window and dispatches its creation event before returning.
    ///
    /// @param platform the owning platform
    /// @param scheduler the event router
    /// @param title the window title
    /// @return the created window
    private static HeadlessWindow createWindow(
            HeadlessPlatform platform,
            UiScheduler scheduler,
            String title
    ) {
        WindowConfiguration configuration = new WindowConfiguration(
                title,
                new LogicalRect(0.0, 0.0, 320.0, 200.0),
                true,
                WindowState.NORMAL
        );
        CompletableFuture<HeadlessWindow> completion = platform.createWindow(
                WindowRequest.toplevel(configuration),
                scheduler::handleWindowEvent
        ).toCompletableFuture();
        platform.eventLoop().runUntilIdle();
        return completion.join();
    }

    /// Opens one default deterministic Headless session on the calling thread.
    ///
    /// @return the open session
    private static HeadlessPlatform openPlatform() {
        return new HeadlessBackend().open().toCompletableFuture().join();
    }

    /// Requires a non-null test reference.
    ///
    /// @param value the candidate value
    /// @param <T> the reference type
    /// @return the non-null value
    private static <T> T require(@Nullable T value) {
        if (value == null) {
            throw new AssertionError("Expected a non-null test reference");
        }
        return value;
    }

    /// Supplies an initial value whose semantic comparison fails during outer publication.
    @NotNullByDefault
    private static final class CommitFailureValue {
        /// Whether this value rejects comparison.
        private final boolean failComparison;

        /// Creates one comparison test value.
        ///
        /// @param failComparison whether [#equals(Object)] throws
        private CommitFailureValue(boolean failComparison) {
            this.failComparison = failComparison;
        }

        /// Compares by identity unless this value represents the planned publication failure.
        ///
        /// @param other the candidate value, or `null`
        /// @return whether both references are identical
        @Override
        public boolean equals(@Nullable Object other) {
            if (failComparison) {
                throw new IllegalStateException("planned publication failure");
            }
            return this == other;
        }

        /// Returns the identity hash code used by this test-only identity equality.
        ///
        /// @return this object's identity hash code
        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
