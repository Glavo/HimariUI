package org.glavo.himari.ffi;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies callback failure publication and destructive retrieval.
@NotNullByDefault
final class CallbackFailureQueueTest {
    /// Verifies that draining preserves publication order and empties the queue.
    @Test
    void drainsFailuresInPublicationOrder() {
        CallbackFailureQueue queue = new CallbackFailureQueue();
        Throwable first = new IllegalStateException("first");
        Throwable second = new IllegalArgumentException("second");

        queue.publish(first);
        queue.publish(second);

        assertEquals(2, queue.size());
        assertEquals(List.of(first, second), queue.drain());
        assertTrue(queue.isEmpty());
        assertNull(queue.poll());
    }

    /// Verifies that polling removes only the earliest failure.
    @Test
    void pollsOneFailureAtATime() {
        CallbackFailureQueue queue = new CallbackFailureQueue();
        Throwable first = new IllegalStateException("first");
        Throwable second = new IllegalArgumentException("second");

        queue.publish(first);
        queue.publish(second);

        assertEquals(first, queue.poll());
        assertEquals(second, queue.poll());
        assertNull(queue.poll());
    }
}
