package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Drives the named first-stable font leftovers through shipped [`SfntFont`] entry points.
@NotNullByDefault
final class FirstStableLeftoversTest {
    /// COLR v0 layers come from [`SfntFont#colorLayers(int)`].
    @Test
    void colorLayersUseShippedColrEntry() {
        SfntFont font = ColrSampleFont.create();
        List<ColorLayer> layers = font.colorLayers(ColrSampleFont.GLYPH_BASE);
        assertEquals(2, layers.size());
        assertNotNull(layers.getFirst().color());
    }

    /// Variable instance access uses [`SfntFont#variationAxes()`] and [`SfntFont#outline`].
    @Test
    void variableInstanceMovesOutlineThroughShippedOutline() {
        SfntFont font = GvarSampleFont.create();
        assertEquals(1, font.variationAxes().size());
        CollectingPen peak = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, peak, new float[] {GvarSampleFont.MAX_WEIGHT});
        CollectingPen def = new CollectingPen();
        font.outline(GvarSampleFont.GLYPH_A, def);
        assertFalse(peak.commands().isEmpty());
        assertEquals(def.commands().getFirst().y0(), peak.commands().getFirst().y0(), 0.01f);
        assertEquals(
                def.commands().getFirst().x0() + GvarSampleFont.CONTOUR_X_DELTA,
                peak.commands().getFirst().x0(),
                0.01f
        );
        assertEquals(
                GvarSampleFont.DEFAULT_ADVANCE + GvarSampleFont.ADVANCE_PHANTOM_DELTA,
                font.metrics(GvarSampleFont.GLYPH_A, new float[] {GvarSampleFont.MAX_WEIGHT}).advanceWidth()
        );
        SfntFont avar = AvarSampleFont.create();
        CollectingPen remapped = new CollectingPen();
        avar.outline(GvarSampleFont.GLYPH_A, remapped, new float[] {AvarSampleFont.MID_WEIGHT});
        assertEquals(GvarSampleFont.CONTOUR_X_DELTA, remapped.commands().getFirst().x0(), 0.01f);
        SfntFont hvar = HvarSampleFont.create();
        assertEquals(
                HvarSampleFont.DEFAULT_ADVANCE + HvarSampleFont.ADVANCE_DELTA,
                hvar.metrics(HvarSampleFont.GLYPH_A, new float[] {HvarSampleFont.MAX_WEIGHT}).advanceWidth()
        );
    }

    /// CBLC/CBDT and EBLC/EBDT strikes use the shipped bitmap entries.
    @Test
    void embeddedBitmapsUseShippedColorAndGrayscaleEntries() {
        SfntFont color = CbdtSampleFont.create();
        @Nullable EmbeddedBitmap cbdt = color.colorBitmap(color.glyphId('A'));
        assertNotNull(cbdt);
        assertEquals(CbdtCblc.TAG_CBDT, cbdt.graphicType());
        SfntFont gray = EbdtSampleFont.create();
        @Nullable EmbeddedBitmap ebdt = gray.grayscaleBitmap(gray.glyphId('A'));
        assertNotNull(ebdt);
        assertEquals(CbdtCblc.TAG_EBDT, ebdt.graphicType());
        SfntFont sbix = SbixSampleFont.create();
        @Nullable EmbeddedBitmap png = sbix.embeddedBitmap(sbix.glyphId('A'));
        assertNotNull(png);
        assertEquals(SbixSampleFont.TAG_PNG, png.graphicType());
    }

    /// WOFF2 unwrap uses [`SfntFont`] on a [`Woff2File#wrap(byte[])`] image.
    @Test
    void woff2OpensThroughSfntFont() {
        byte[] woff2 = Woff2File.wrap(
                ColrV1SampleFont.bytes().toArray(java.lang.foreign.ValueLayout.JAVA_BYTE));
        SfntFont font = new SfntFont(woff2);
        assertEquals(2, font.colorLayers(ColrV1SampleFont.GLYPH_BASE).size());
    }
}
