package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies type-5 `IgnoreMarks` through [`SfntFont#contextSubstitute(int, int, int, int)`].
@NotNullByDefault
final class GsubContextMarkTest {
    /// Substitutes `A` before `C` when `B` is a GDEF mark.
    @Test
    void contextRuleSkipsGdefMark() {
        SfntFont font = GsubContextMarkSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int x = font.glyphId('X');
        assertEquals(GdefTable.CLASS_MARK, font.glyphClass(b));
        assertEquals(x, font.contextSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(a, font.contextSubstitute(a, b, SfntFont.TAG_CALT));
        assertEquals(x, font.contextSubstitute(a, c, SfntFont.TAG_CALT));
    }
}
