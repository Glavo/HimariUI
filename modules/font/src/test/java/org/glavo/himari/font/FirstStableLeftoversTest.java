package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        SfntFont stat = FvarSampleFont.create();
        assertEquals(2, stat.statAxes().size());
        assertEquals(FvarSampleFont.TAG_WGHT, stat.statAxes().getFirst().tag());
        assertEquals(FvarSampleFont.STAT_AXIS_NAME_ID, stat.statAxes().getFirst().nameId());
        assertEquals(FvarSampleFont.TAG_WDTH, stat.statAxes().get(1).tag());
        assertEquals(4, stat.statNamedInstances().size());
        assertEquals(FvarSampleFont.STAT_INSTANCE_NAME_ID, stat.statNamedInstances().getFirst().nameId());
        assertEquals(FvarSampleFont.DEFAULT_WEIGHT, stat.statNamedInstances().getFirst().value(), 0.01f);
        assertTrue(stat.statNamedInstances().getFirst().elidableAxisValueName());
        assertEquals(2, stat.statNamedInstances().get(1).format());
        assertEquals(FvarSampleFont.STAT_LIGHT_MIN, stat.statNamedInstances().get(1).rangeMin(), 0.01f);
        assertEquals(3, stat.statNamedInstances().get(2).format());
        assertTrue(stat.statNamedInstances().get(2).olderSiblingFontAttribute());
        assertEquals(FvarSampleFont.STAT_BOLD_LINKED, stat.statNamedInstances().get(2).linkedValue(), 0.01f);
        assertEquals(4, stat.statNamedInstances().get(3).format());
        assertEquals(FvarSampleFont.STAT_BLACK_VALUE, stat.statNamedInstances().get(3).value(), 0.01f);
        assertEquals(1, stat.statNamedInstances().get(3).extraAxisIndices().length);
        assertEquals(FvarSampleFont.STAT_BLACK_WIDTH, stat.statNamedInstances().get(3).extraValues()[0], 0.01f);
        assertEquals(FvarSampleFont.STAT_ELIDED_FALLBACK_NAME_ID, stat.statElidedFallbackNameId());
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

    /// SVG documents, including gzip payloads, come from [`SfntFont#svgDocument(int)`].
    @Test
    void svgDocumentUsesShippedEntry() {
        SfntFont font = SvgSampleFont.create();
        assertEquals(SvgSampleFont.DOCUMENT, font.svgDocument(SvgSampleFont.GLYPH_A));
        assertEquals(null, font.svgDocument(0));
        assertEquals(
                SvgSampleFont.DOCUMENT,
                new SfntFont(SvgSampleFont.compressedBytes()).svgDocument(SvgSampleFont.GLYPH_A)
        );
        assertThrows(IllegalArgumentException.class, () -> new SfntFont(SvgSampleFont.truncatedGzipBytes()));
    }

    /// `OS/2` and `name` family come from [`SfntFont#weightClass()`] and [`SfntFont#familyName()`].
    @Test
    void os2AndFamilyNameUseShippedEntries() {
        SfntFont font = Os2SampleFont.create();
        assertEquals(Os2SampleFont.WEIGHT_CLASS, font.weightClass());
        assertEquals(Os2SampleFont.WIDTH_CLASS, font.widthClass());
        assertEquals(Os2SampleFont.AVG_CHAR_WIDTH, font.avgCharWidth());
        assertEquals(Os2SampleFont.FS_TYPE, font.fsType());
        assertEquals(java.util.Arrays.compare(Os2SampleFont.PANOSE, font.panose()), 0);
        assertEquals(Os2SampleFont.VENDOR, font.vendorId());
        assertEquals(Os2SampleFont.FS_SELECTION, font.fsSelection());
        assertEquals(Os2SampleFont.TYPO_ASCENDER, font.typoAscender());
        assertEquals(Os2SampleFont.TYPO_DESCENDER, font.typoDescender());
        assertEquals(Os2SampleFont.TYPO_LINE_GAP, font.typoLineGap());
        assertEquals(Os2SampleFont.WIN_ASCENT, font.winAscent());
        assertEquals(Os2SampleFont.WIN_DESCENT, font.winDescent());
        assertEquals(Os2SampleFont.COPYRIGHT, font.copyright());
        assertEquals(Os2SampleFont.UNIQUE, font.uniqueId());
        assertEquals(Os2SampleFont.FAMILY, font.familyName());
        assertEquals(Os2SampleFont.STYLE, font.styleName());
        assertEquals(Os2SampleFont.FULL, font.fullName());
        assertEquals(Os2SampleFont.VERSION, font.versionString());
        assertEquals(Os2SampleFont.POSTSCRIPT, font.postScriptName());
        assertEquals(Os2SampleFont.TRADEMARK, font.trademark());
        assertEquals(Os2SampleFont.MANUFACTURER, font.manufacturer());
        assertEquals(Os2SampleFont.DESIGNER, font.designer());
        assertEquals(Os2SampleFont.DESCRIPTION, font.description());
        assertEquals(Os2SampleFont.TYPOGRAPHIC_FAMILY, font.typographicFamily());
        assertEquals(Os2SampleFont.TYPOGRAPHIC_SUBFAMILY, font.typographicSubfamily());
        assertEquals(Os2SampleFont.VENDOR_URL, font.vendorUrl());
        assertEquals(Os2SampleFont.LICENSE, font.license());
        assertEquals(Os2SampleFont.DESIGNER_URL, font.designerUrl());
        assertEquals(Os2SampleFont.LICENSE_URL, font.licenseUrl());
        assertEquals(Os2SampleFont.WWS_FAMILY, font.wwsFamily());
        assertEquals(Os2SampleFont.WWS_SUBFAMILY, font.wwsSubfamily());
        assertEquals(Os2SampleFont.SAMPLE_TEXT, font.sampleText());
        assertEquals(Os2SampleFont.COMPATIBLE_FULL, font.compatibleFull());
        assertEquals(Os2SampleFont.POST_SCRIPT_CID, font.postScriptCid());
        assertEquals(Os2SampleFont.VARIATIONS_POST_SCRIPT_PREFIX, font.variationsPostScriptPrefix());
        assertEquals(Os2SampleFont.LIGHT_BACKGROUND_PALETTE, font.lightBackgroundPalette());
        assertEquals(Os2SampleFont.DARK_BACKGROUND_PALETTE, font.darkBackgroundPalette());
        assertEquals(Os2SampleFont.SUBSCRIPT_X_SIZE, font.subscriptXSize());
        assertEquals(Os2SampleFont.SUBSCRIPT_Y_SIZE, font.subscriptYSize());
        assertEquals(Os2SampleFont.SUBSCRIPT_X_OFFSET, font.subscriptXOffset());
        assertEquals(Os2SampleFont.SUBSCRIPT_Y_OFFSET, font.subscriptYOffset());
        assertEquals(Os2SampleFont.SUPERSCRIPT_X_SIZE, font.superscriptXSize());
        assertEquals(Os2SampleFont.SUPERSCRIPT_Y_SIZE, font.superscriptYSize());
        assertEquals(Os2SampleFont.SUPERSCRIPT_X_OFFSET, font.superscriptXOffset());
        assertEquals(Os2SampleFont.SUPERSCRIPT_Y_OFFSET, font.superscriptYOffset());
        assertEquals(Os2SampleFont.STRIKEOUT_SIZE, font.strikeoutSize());
        assertEquals(Os2SampleFont.STRIKEOUT_POSITION, font.strikeoutPosition());
        assertEquals(Os2SampleFont.FAMILY_CLASS, font.familyClass());
        assertEquals(Os2SampleFont.UNICODE_RANGE1, font.unicodeRange1());
        assertEquals(Os2SampleFont.UNICODE_RANGE2, font.unicodeRange2());
        assertEquals(Os2SampleFont.UNICODE_RANGE3, font.unicodeRange3());
        assertEquals(Os2SampleFont.UNICODE_RANGE4, font.unicodeRange4());
        assertEquals(Os2SampleFont.CODE_PAGE_RANGE1, font.codePageRange1());
        assertEquals(Os2SampleFont.CODE_PAGE_RANGE2, font.codePageRange2());
        assertEquals(Os2SampleFont.FIRST_CHAR_INDEX, font.firstCharIndex());
        assertEquals(Os2SampleFont.LAST_CHAR_INDEX, font.lastCharIndex());
        assertEquals(Os2SampleFont.X_HEIGHT, font.xHeight());
        assertEquals(Os2SampleFont.CAP_HEIGHT, font.capHeight());
        assertEquals(Os2SampleFont.DEFAULT_CHAR, font.defaultChar());
        assertEquals(Os2SampleFont.BREAK_CHAR, font.breakChar());
        assertEquals(Os2SampleFont.MAX_CONTEXT, font.maxContext());
        assertEquals(Os2SampleFont.ITALIC_ANGLE, font.italicAngle(), 0.001f);
        assertEquals(Os2SampleFont.UNDERLINE_POSITION, font.underlinePosition());
        assertEquals(Os2SampleFont.UNDERLINE_THICKNESS, font.underlineThickness());
        assertTrue(font.fixedPitch());
        assertEquals(Os2Table.WEIGHT_REGULAR, BitmapSfntFont.create().weightClass());
        assertEquals(Os2Table.WIDTH_MEDIUM, BitmapSfntFont.create().widthClass());
        assertEquals(0, BitmapSfntFont.create().fsSelection());
        assertFalse(BitmapSfntFont.create().fixedPitch());
    }

    /// WOFF2 unwrap uses [`SfntFont`] on a [`Woff2File#wrap(byte[])`] image.
    @Test
    void woff2OpensThroughSfntFont() {
        byte[] woff2 = Woff2File.wrap(
                ColrV1SampleFont.bytes().toArray(java.lang.foreign.ValueLayout.JAVA_BYTE));
        SfntFont font = new SfntFont(woff2);
        assertEquals(2, font.colorLayers(ColrV1SampleFont.GLYPH_BASE).size());
    }

    /// WOFF2 unwrap inflates a Brotli static-dictionary distance through [`Woff2File#unwrap`].
    @Test
    void woff2StaticDictionaryInflatesThroughUnwrap() {
        byte[] sfnt = SvgSampleFont.bytes().toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
        byte[] woff2 = Woff2File.wrapWithStaticDictionary(sfnt);
        SfntFont font = new SfntFont(woff2);
        assertEquals(SvgSampleFont.DOCUMENT, font.svgDocument(SvgSampleFont.GLYPH_A));
    }
}
