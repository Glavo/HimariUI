package org.glavo.himari.platform.headless;

import org.glavo.himari.platform.api.ScheduledTask;
import org.glavo.himari.platform.api.ScheduledTaskState;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies host-driven ordering, cancellation, reentrancy, failure, and randomized scheduling.
@NotNullByDefault
final class HeadlessEventLoopTest {
    /// Verifies deadline order, FIFO ties, nested submission, and missed-deadline sampling.
    @Test
    void dispatchesReadyTasksInStableOrder() {
        HeadlessEventLoop loop = new HeadlessEventLoop();
        ArrayList<String> events = new ArrayList<>();
        loop.schedule(10L, () -> events.add("ten-first"));
        loop.post(() -> {
            events.add("now");
            loop.post(() -> events.add("nested"));
        });
        loop.schedule(10L, () -> events.add("ten-second"));

        assertEquals(2L, loop.runUntilIdle());
        assertEquals(List.of("now", "nested"), events);
        assertEquals(2, loop.pendingTaskCount());

        assertEquals(2L, loop.advanceTo(25L));
        assertEquals(List.of("now", "nested", "ten-first", "ten-second"), events);
        assertEquals(25L, loop.clock().nowNanos());
        assertEquals(0, loop.pendingTaskCount());
    }

    /// Verifies cancellation state transitions and close cancellation.
    @Test
    void cancelsOnlyPendingTasks() {
        HeadlessEventLoop loop = new HeadlessEventLoop();
        ScheduledTask cancelled = loop.schedule(10L, () -> {
            throw new AssertionError("cancelled task ran");
        });
        ScheduledTask completed = loop.post(() -> {
        });

        assertTrue(cancelled.cancel());
        assertFalse(cancelled.cancel());
        assertEquals(ScheduledTaskState.CANCELLED, cancelled.state());
        assertEquals(1L, loop.runUntilIdle());
        assertEquals(ScheduledTaskState.COMPLETED, completed.state());
        assertFalse(completed.cancel());

        ScheduledTask pendingAtClose = loop.schedule(20L, () -> {
        });
        loop.close();
        assertTrue(loop.isClosed());
        assertEquals(ScheduledTaskState.CANCELLED, pendingAtClose.state());
        assertThrows(IllegalStateException.class, () -> loop.post(() -> {
        }));
    }

    /// Verifies that callbacks cannot recursively dispatch or advance manual time.
    @Test
    void rejectsReentrantDispatchAndTimeAdvancement() {
        HeadlessEventLoop loop = new HeadlessEventLoop();
        loop.post(() -> {
            assertThrows(IllegalStateException.class, loop::runUntilIdle);
            assertThrows(IllegalStateException.class, () -> loop.advanceBy(1L));
            assertEquals(0L, loop.clock().nowNanos());
        });

        assertEquals(1L, loop.runUntilIdle());
    }

    /// Verifies that a failed callback does not discard later work and itself becomes completed.
    @Test
    void preservesQueueAfterCallbackFailure() {
        HeadlessEventLoop loop = new HeadlessEventLoop();
        ScheduledTask failed = loop.post(() -> {
            throw new IllegalStateException("planned failure");
        });
        ArrayList<String> events = new ArrayList<>();
        loop.post(() -> events.add("survived"));

        IllegalStateException failure = assertThrows(IllegalStateException.class, loop::runUntilIdle);
        assertEquals("planned failure", failure.getMessage());
        assertEquals(ScheduledTaskState.COMPLETED, failed.state());
        assertEquals(1, loop.pendingTaskCount());
        assertEquals(1L, loop.runUntilIdle());
        assertEquals(List.of("survived"), events);
    }

    /// Compares 5,000 randomized schedule, cancellation, and clock operations with a naive model.
    @Test
    void matchesNaiveSchedulerModel() {
        HeadlessEventLoop loop = new HeadlessEventLoop();
        ArrayList<Long> deadlines = new ArrayList<>();
        ArrayList<Boolean> cancelled = new ArrayList<>();
        ArrayList<Boolean> executed = new ArrayList<>();
        ArrayList<ScheduledTask> handles = new ArrayList<>();
        ArrayList<Integer> actualOrder = new ArrayList<>();
        ArrayList<Integer> expectedOrder = new ArrayList<>();
        Random random = new Random(0x484541444c455353L);
        long now = 0L;

        for (int operation = 0; operation < 5_000; operation++) {
            int action = random.nextInt(10);
            if (action < 6 || handles.isEmpty()) {
                int id = handles.size();
                long deadline = now + random.nextLong(0L, 41L);
                deadlines.add(deadline);
                cancelled.add(false);
                executed.add(false);
                handles.add(loop.schedule(deadline, () -> actualOrder.add(id)));
            } else if (action < 8) {
                int id = random.nextInt(handles.size());
                boolean expectedCancellation = !cancelled.get(id) && !executed.get(id);
                assertEquals(expectedCancellation, handles.get(id).cancel());
                if (expectedCancellation) {
                    cancelled.set(id, true);
                }
            } else {
                now += random.nextLong(0L, 21L);
                appendReadyModelTasks(deadlines, cancelled, executed, expectedOrder, now);
                loop.advanceTo(now);
                assertEquals(expectedOrder, actualOrder, "execution order at operation " + operation);
            }
        }

        now += 100L;
        appendReadyModelTasks(deadlines, cancelled, executed, expectedOrder, now);
        loop.advanceTo(now);
        assertEquals(expectedOrder, actualOrder);
        assertEquals(0, loop.pendingTaskCount());
    }

    /// Appends every ready model task in deadline and submission order.
    ///
    /// @param deadlines model deadlines by submission identifier
    /// @param cancelled model cancellation flags
    /// @param executed model execution flags, updated for appended tasks
    /// @param expectedOrder accumulated expected callback identifiers
    /// @param now the current model timestamp
    private static void appendReadyModelTasks(
            List<Long> deadlines,
            List<Boolean> cancelled,
            List<Boolean> executed,
            List<Integer> expectedOrder,
            long now
    ) {
        ArrayList<Integer> ready = new ArrayList<>();
        for (int id = 0; id < deadlines.size(); id++) {
            if (!cancelled.get(id) && !executed.get(id) && deadlines.get(id) <= now) {
                ready.add(id);
            }
        }
        ready.sort(Comparator.comparingLong(deadlines::get).thenComparingInt(Integer::intValue));
        for (int id : ready) {
            executed.set(id, true);
            expectedOrder.add(id);
        }
    }
}
