package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// Verifies type-1 GSUB `isol`/`init`/`medi`/`fina` on the constructed font.
@NotNullByDefault
final class GsubSubstitutionsTest {
    /// Maps Beh through each joining feature to a distinct glyph.
    @Test
    void substitutesJoiningForms() {
        SfntFont font = GsubSampleFont.create();
        int nominal = font.glyphId('\u0628');
        assertEquals(GsubSampleFont.GLYPH_BEH, nominal);
        assertEquals(GsubSampleFont.GLYPH_ISOL, font.substitute(nominal, GsubSampleFont.TAG_ISOL));
        assertEquals(GsubSampleFont.GLYPH_INIT, font.substitute(nominal, GsubSampleFont.TAG_INIT));
        assertEquals(GsubSampleFont.GLYPH_MEDI, font.substitute(nominal, GsubSampleFont.TAG_MEDI));
        assertEquals(GsubSampleFont.GLYPH_FINA, font.substitute(nominal, GsubSampleFont.TAG_FINA));
        assertNotEquals(font.metrics(GsubSampleFont.GLYPH_ISOL).advanceWidth(),
                font.metrics(GsubSampleFont.GLYPH_INIT).advanceWidth());
    }

    /// Leaves identities unchanged when GSUB is absent.
    @Test
    void missingGsubIsIdentity() {
        SfntFont font = OutlineSampleFont.create();
        int glyph = font.glyphId('A');
        assertEquals(glyph, font.substitute(glyph, GsubSampleFont.TAG_ISOL));
    }
}
