package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies GSUB context format 2/3 through [`SfntFont#contextSubstitute(int, int, int)`].
@NotNullByDefault
final class GsubCoverageTest {
    /// Substitutes `A` before `B` from a format-3 coverage pair.
    @Test
    void format3ContextSubstitutesAb() {
        SfntFont font = GsubCoverageSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int x = font.glyphId('X');
        assertEquals(x, font.contextSubstitute(a, b, SfntFont.TAG_CALT));
        assertEquals(a, font.contextSubstitute(a, a, SfntFont.TAG_CALT));
    }

    /// Substitutes `A` before `BC` from a format-3 chain coverage triple.
    @Test
    void format3ChainSubstitutesAbc() {
        SfntFont font = GsubChainCoverageSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int y = font.glyphId('Y');
        assertEquals(y, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(a, b, a, SfntFont.TAG_CALT));
    }

    /// Substitutes `A` before `BC` from a format-2 class chain.
    @Test
    void format2ClassChainSubstitutesAbc() {
        SfntFont font = GsubChainClassSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int c = font.glyphId('C');
        int y = font.glyphId('Y');
        assertEquals(y, font.chainSubstitute(a, b, c, SfntFont.TAG_CALT));
        assertEquals(a, font.chainSubstitute(a, b, a, SfntFont.TAG_CALT));
    }

    /// Substitutes `A` before `B` from a format-2 class rule.
    @Test
    void format2ClassContextSubstitutesAb() {
        SfntFont font = GsubContextClassSampleFont.create();
        int a = font.glyphId('A');
        int b = font.glyphId('B');
        int x = font.glyphId('X');
        assertEquals(x, font.contextSubstitute(a, b, SfntFont.TAG_CALT));
        assertEquals(a, font.contextSubstitute(a, a, SfntFont.TAG_CALT));
    }
}
