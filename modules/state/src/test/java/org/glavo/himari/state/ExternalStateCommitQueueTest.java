package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies deferred execution, batching, failure isolation, FIFO ordering, and concurrent enqueue.
@NotNullByDefault
final class ExternalStateCommitQueueTest {
    /// Verifies that enqueue never invokes user code on the producer thread.
    ///
    /// @throws InterruptedException if the test thread is interrupted while joining the producer
    @Test
    void defersProducerWorkToTheOwnerThread() throws InterruptedException {
        StateDomain domain = new StateDomain();
        IntState state = domain.intState(0);
        ExternalStateCommitQueue queue = domain.externalCommits();
        Thread ownerThread = Thread.currentThread();
        AtomicReference<@Nullable Thread> executionThread = new AtomicReference<>();

        Thread producer = Thread.ofPlatform().name("external-producer").start(() ->
                queue.enqueue(() -> {
                    executionThread.set(Thread.currentThread());
                    state.set(1);
                })
        );
        producer.join();

        assertNull(executionThread.get());
        assertEquals(0, state.get());
        assertEquals(1, queue.size());

        ExternalCommitResult result = queue.drain();
        assertSame(ownerThread, executionThread.get());
        assertEquals(1, state.get());
        assertEquals(1, result.attemptedCount());
        assertEquals(1, result.successfulCount());
        assertEquals(0L, result.epochBefore());
        assertEquals(1L, result.epochAfter());
        assertTrue(result.changed());
        assertTrue(result.allSucceeded());
        assertTrue(queue.isEmpty());
    }

    /// Verifies FIFO execution and one-epoch publication for a captured batch.
    @Test
    void coalescesSuccessfulCallbacksIntoOneEpoch() {
        StateDomain domain = new StateDomain();
        IntState state = domain.intState(0);
        ExternalStateCommitQueue queue = domain.externalCommits();
        List<Integer> executionOrder = new ArrayList<>();

        for (int value = 1; value <= 5; value++) {
            int capturedValue = value;
            queue.enqueue(() -> {
                executionOrder.add(capturedValue);
                state.set(capturedValue);
            });
        }

        ExternalCommitResult result = queue.drain();
        assertEquals(List.of(1, 2, 3, 4, 5), executionOrder);
        assertEquals(5, state.get());
        assertEquals(1L, state.version());
        assertEquals(1L, domain.epoch());
        assertEquals(5, result.attemptedCount());
        assertEquals(5, result.successfulCount());
        assertTrue(result.failures().isEmpty());
    }

    /// Verifies per-callback rollback and continued execution after a failure.
    @Test
    void isolatesFailedCallbacksWithNestedSavepoints() {
        StateDomain domain = new StateDomain();
        IntState state = domain.intState(0);
        ExternalStateCommitQueue queue = domain.externalCommits();

        queue.enqueue(() -> state.set(1));
        queue.enqueue(() -> {
            state.set(100);
            throw new IllegalArgumentException("reject this update");
        });
        queue.enqueue(() -> state.set(state.get() + 1));

        ExternalCommitResult result = queue.drain();
        assertEquals(2, state.get());
        assertEquals(1L, state.version());
        assertEquals(1L, domain.epoch());
        assertEquals(3, result.attemptedCount());
        assertEquals(2, result.successfulCount());
        assertFalse(result.allSucceeded());
        assertEquals(1, result.failures().size());
        assertEquals(1, result.failures().getFirst().batchIndex());
        assertInstanceOf(IllegalArgumentException.class, result.failures().getFirst().cause());
    }

    /// Verifies that callbacks enqueued after batch detachment wait for the next drain.
    @Test
    void leavesReentrantEnqueueForTheNextDrain() {
        StateDomain domain = new StateDomain();
        IntState state = domain.intState(0);
        ExternalStateCommitQueue queue = domain.externalCommits();
        queue.enqueue(() -> {
            state.set(1);
            queue.enqueue(() -> state.set(2));
        });

        ExternalCommitResult first = queue.drain();
        assertEquals(1, first.attemptedCount());
        assertEquals(1, state.get());
        assertEquals(1, queue.size());

        ExternalCommitResult second = queue.drain();
        assertEquals(1, second.attemptedCount());
        assertEquals(2, state.get());
        assertEquals(2L, domain.epoch());
        assertTrue(queue.isEmpty());
    }

    /// Verifies loss-free enqueue from several concurrent producers and one-epoch owner drain.
    ///
    /// @throws InterruptedException if the test thread is interrupted while joining a producer
    @Test
    void acceptsConcurrentProducersWithoutLostUpdates() throws InterruptedException {
        int producerCount = 8;
        int updatesPerProducer = 125;
        StateDomain domain = new StateDomain();
        IntState state = domain.intState(0);
        ExternalStateCommitQueue queue = domain.externalCommits();
        List<Thread> producers = new ArrayList<>(producerCount);

        for (int producerIndex = 0; producerIndex < producerCount; producerIndex++) {
            producers.add(Thread.ofPlatform().start(() -> {
                for (int updateIndex = 0; updateIndex < updatesPerProducer; updateIndex++) {
                    queue.enqueue(() -> state.set(state.get() + 1));
                }
            }));
        }
        for (Thread producer : producers) {
            producer.join();
        }

        assertEquals(producerCount * updatesPerProducer, queue.size());
        ExternalCommitResult result = queue.drain();
        assertEquals(producerCount * updatesPerProducer, result.attemptedCount());
        assertEquals(producerCount * updatesPerProducer, result.successfulCount());
        assertEquals(producerCount * updatesPerProducer, state.get());
        assertEquals(1L, state.version());
        assertEquals(1L, domain.epoch());
        assertTrue(queue.isEmpty());
    }

    /// Verifies the empty-drain result without publishing a new epoch.
    @Test
    void returnsAnEmptyUnchangedResult() {
        StateDomain domain = new StateDomain();

        ExternalCommitResult result = domain.externalCommits().drain();
        assertEquals(0, result.attemptedCount());
        assertEquals(0, result.successfulCount());
        assertEquals(0L, result.epochBefore());
        assertEquals(0L, result.epochAfter());
        assertFalse(result.changed());
        assertTrue(result.allSucceeded());
    }
}
