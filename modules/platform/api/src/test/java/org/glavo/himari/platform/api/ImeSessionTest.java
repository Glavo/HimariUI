package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// Verifies portable IME composition, rejection, and surrounding-text updates.
@NotNullByDefault
final class ImeSessionTest {
    /// Commits injected composition into surrounding text.
    @Test
    void commitsCompositionIntoSurroundingText() {
        ImeSession session = new ImeSession();
        session.setSurroundingText("", 0);
        session.updateComposition("ni");
        assertEquals("ni", session.composition());
        assertEquals("ni", session.commit());
        assertEquals("ni", session.surroundingText());
        assertEquals(2, session.caret());
        assertNull(session.composition());
    }

    /// Rejects live composition without rewriting surrounding text.
    @Test
    void rejectsCompositionWithoutChangingSurroundingText() {
        ImeSession session = new ImeSession();
        session.setSurroundingText("ab", 2);
        session.updateComposition("hao");
        assertEquals("hao", session.reject());
        assertEquals("hao", session.lastRejected());
        assertEquals("ab", session.surroundingText());
        assertEquals(2, session.caret());
        assertNull(session.composition());
    }
}
