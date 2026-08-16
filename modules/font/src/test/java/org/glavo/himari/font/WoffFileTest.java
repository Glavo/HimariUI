package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies WOFF1 unwrap through the shipped [`SfntFont`] constructor.
@NotNullByDefault
final class WoffFileTest {
    /// Opens an uncompressed WOFF1 wrap of the COLR v1 sample.
    @Test
    void uncompressedWoffOpensThroughSfntFont() {
        byte[] sfnt = ColrV1SampleFont.bytes().toArray(ValueLayout.JAVA_BYTE);
        byte[] woff = WoffFile.wrapUncompressed(sfnt);
        assertTrue(WoffFile.isWoff(MemorySegment.ofArray(woff)));
        SfntFont font = new SfntFont(woff);
        List<ColorLayer> layers = font.colorLayers(ColrV1SampleFont.GLYPH_BASE);
        assertEquals(2, layers.size());
        assertEquals(ColrV1SampleFont.GLYPH_BACK, layers.getFirst().glyphId());
    }

    /// Inflates a zlib-compressed WOFF1 wrap through [`SfntFont`].
    @Test
    void compressedWoffInflatesThroughSfntFont() {
        byte[] sfnt = MvarSampleFont.bytes().toArray(ValueLayout.JAVA_BYTE);
        byte[] woff = WoffFile.wrapCompressed(sfnt);
        assertTrue(WoffFile.isWoff(MemorySegment.ofArray(woff)));
        SfntFont font = new SfntFont(woff);
        assertEquals(MvarSampleFont.DEFAULT_ASCENDER, font.ascender());
        assertEquals(
                MvarSampleFont.DEFAULT_ASCENDER + MvarSampleFont.ASCENDER_DELTA,
                font.ascender(new float[] {MvarSampleFont.MAX_WEIGHT})
        );
    }
}
