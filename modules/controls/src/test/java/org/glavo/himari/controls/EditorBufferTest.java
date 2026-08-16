package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies TextField and TextArea through their shipped buffer storage.
@NotNullByDefault
final class EditorBufferTest {
    /// Edits a field through [`TextField#replaceRange(int, int, String)`] and the gap buffer.
    @Test
    void textFieldReplaceMovesGapBuffer() {
        TextField field = new TextField();
        field.replaceRange(0, 0, "abcd");
        assertEquals("abcd", field.text());
        assertEquals("abcd", field.committed().toString());
        field.replaceRange(1, 3, "X");
        assertEquals("aXd", field.text());
        assertEquals(1 + 1, field.committed().gapStart());
        field.replaceRange(0, 1, "");
        assertEquals("Xd", field.text());
        assertEquals(0, field.committed().gapStart());
    }

    /// Commits area lines through the piece table.
    @Test
    void textAreaCommitAppendsPieces() {
        TextArea area = new TextArea();
        area.updateComposition("hello");
        area.commitComposition();
        assertEquals("hello", area.text());
        assertEquals(1, area.committed().pieceCount());
        area.updateComposition("world");
        area.commitComposition();
        assertEquals("hello\nworld", area.text());
        assertEquals(3, area.committed().pieceCount());
        area.replaceRange(0, 5, "hi");
        assertEquals("hi\nworld", area.text());
        assertTrue(area.committed().pieceCount() >= 2);
    }
}
