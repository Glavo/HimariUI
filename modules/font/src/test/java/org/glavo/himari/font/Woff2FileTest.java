package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies WOFF2 unwrap through the shipped [`SfntFont`] constructor.
@NotNullByDefault
final class Woff2FileTest {
    /// Opens a trivial-Brotli WOFF2 wrap of the COLR v1 sample.
    @Test
    void trivialWoff2OpensThroughSfntFont() {
        byte[] sfnt = ColrV1SampleFont.bytes().toArray(ValueLayout.JAVA_BYTE);
        byte[] woff2 = Woff2File.wrap(sfnt);
        assertTrue(Woff2File.isWoff2(MemorySegment.ofArray(woff2)));
        SfntFont font = new SfntFont(woff2);
        List<ColorLayer> layers = font.colorLayers(ColrV1SampleFont.GLYPH_BASE);
        assertEquals(2, layers.size());
        assertEquals(ColrV1SampleFont.GLYPH_BACK, layers.getFirst().glyphId());
    }

    /// Inflates a command-coded Brotli WOFF2 wrap through [`SfntFont`].
    @Test
    void commandWoff2InflatesThroughSfntFont() {
        byte[] sfnt = MvarSampleFont.bytes().toArray(ValueLayout.JAVA_BYTE);
        byte[] woff2 = Woff2File.wrap(sfnt, true);
        assertTrue(Woff2File.isWoff2(MemorySegment.ofArray(woff2)));
        SfntFont font = new SfntFont(woff2);
        assertEquals(MvarSampleFont.DEFAULT_ASCENDER, font.ascender());
        assertEquals(
                MvarSampleFont.DEFAULT_ASCENDER + MvarSampleFont.ASCENDER_DELTA,
                font.ascender(new float[] {MvarSampleFont.MAX_WEIGHT})
        );
    }

    /// Inflates a WOFF2 whose Brotli stream uses a static-dictionary distance.
    @Test
    void dictionaryWoff2InflatesThroughSfntFont() {
        byte[] sfnt = SvgSampleFont.bytes().toArray(ValueLayout.JAVA_BYTE);
        byte[] woff2 = Woff2File.wrapWithStaticDictionary(sfnt);
        assertTrue(Woff2File.isWoff2(MemorySegment.ofArray(woff2)));
        SfntFont font = new SfntFont(woff2);
        assertEquals(SvgSampleFont.DOCUMENT, font.svgDocument(SvgSampleFont.GLYPH_A));
    }

    /// Reconstructs WOFF2 transformed `glyf`/`loca`/`hmtx` through [`SfntFont`].
    @Test
    void transformedGlyfOpensThroughSfntFont() {
        byte[] sfnt = OutlineSampleFont.bytes().toArray(ValueLayout.JAVA_BYTE);
        byte[] woff2 = Woff2File.wrapTransformed(sfnt);
        assertTrue(Woff2File.isWoff2(MemorySegment.ofArray(woff2)));
        SfntFont original = OutlineSampleFont.create();
        SfntFont decoded = new SfntFont(woff2);
        CollectingPen originalBump = new CollectingPen();
        CollectingPen decodedBump = new CollectingPen();
        original.outline(OutlineSampleFont.GLYPH_BUMP, originalBump);
        decoded.outline(OutlineSampleFont.GLYPH_BUMP, decodedBump);
        assertEquals(originalBump.commands(), decodedBump.commands());
        CollectingPen originalComposite = new CollectingPen();
        CollectingPen decodedComposite = new CollectingPen();
        original.outline(OutlineSampleFont.GLYPH_COMPOSITE, originalComposite);
        decoded.outline(OutlineSampleFont.GLYPH_COMPOSITE, decodedComposite);
        assertEquals(originalComposite.commands(), decodedComposite.commands());
        CollectingPen originalImplied = new CollectingPen();
        CollectingPen decodedImplied = new CollectingPen();
        original.outline(OutlineSampleFont.GLYPH_IMPLIED, originalImplied);
        decoded.outline(OutlineSampleFont.GLYPH_IMPLIED, decodedImplied);
        assertEquals(originalImplied.commands(), decodedImplied.commands());
        assertEquals(
                original.metrics(OutlineSampleFont.GLYPH_COMPOSITE).advanceWidth(),
                decoded.metrics(OutlineSampleFont.GLYPH_COMPOSITE).advanceWidth()
        );
        assertEquals(
                original.metrics(OutlineSampleFont.GLYPH_COMPOSITE).leftSideBearing(),
                decoded.metrics(OutlineSampleFont.GLYPH_COMPOSITE).leftSideBearing()
        );
    }

    /// Opens a WOFF2 collection wrap and keeps the first TTC face.
    @Test
    void collectionWoff2OpensFirstFaceThroughSfntFont() {
        byte[] sfnt = ColrV1SampleFont.bytes().toArray(ValueLayout.JAVA_BYTE);
        byte[] ttc = TtcFile.wrap(sfnt);
        byte[] woff2 = Woff2File.wrap(ttc);
        assertTrue(Woff2File.isWoff2(MemorySegment.ofArray(woff2)));
        SfntFont font = new SfntFont(woff2);
        assertEquals(2, font.colorLayers(ColrV1SampleFont.GLYPH_BASE).size());
    }
}
