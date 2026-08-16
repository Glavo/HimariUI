package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Writes SFNT mapping and raster conformance evidence.
@NotNullByDefault
public final class FontConformance {
    /// Prevents instantiation.
    private FontConformance() {
    }

    /// Maps a Latin glyph and writes the report.
    ///
    /// @param arguments one output directory
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        SfntFont font = BitmapSfntFont.create();
        int glyph = font.glyphId('A');
        if (glyph <= 0) {
            throw new IllegalStateException("Latin glyph was not mapped");
        }
        GlyphMask mask = GlyphRasterizer.rasterize(font, glyph, 16);
        if (mask.width() <= 0 || mask.height() <= 0) {
            throw new IllegalStateException("Glyph raster was empty");
        }
        Path output = Path.of(arguments[0]);
        Files.createDirectories(output);
        LeftoverEvidence leftovers = leftoverEvidence();
        Files.writeString(output.resolve("leftovers.json"), leftovers.toJson(), StandardCharsets.UTF_8);
        Files.writeString(
                output.resolve("results.json"),
                """
                        {
                          "profile": "m4-font",
                          "workPackage": "FONT-001",
                          "status": "passed",
                          "glyphId": %d,
                          "maskWidth": %d,
                          "maskHeight": %d,
                          "colrLayers": %d,
                          "variationAxes": %d,
                          "gvarPeakMoved": %s,
                          "avarRemapped": %s,
                          "cbdtPresent": %s,
                          "ebdtPresent": %s,
                          "sbixPresent": %s
                        }
                        """.formatted(
                        glyph,
                        mask.width(),
                        mask.height(),
                        leftovers.colrLayers,
                        leftovers.variationAxes,
                        leftovers.gvarPeakMoved,
                        leftovers.avarRemapped,
                        leftovers.cbdtPresent,
                        leftovers.ebdtPresent,
                        leftovers.sbixPresent
                ),
                StandardCharsets.UTF_8
        );
    }

    /// Calls the named first-stable leftover entries on constructed faces.
    private static LeftoverEvidence leftoverEvidence() {
        SfntFont colr = ColrSampleFont.create();
        int layers = colr.colorLayers(ColrSampleFont.GLYPH_BASE).size();
        if (layers < 1) {
            throw new IllegalStateException("COLR leftover did not return layers");
        }
        SfntFont variable = GvarSampleFont.create();
        int axes = variable.variationAxes().size();
        CollectingPen def = new CollectingPen();
        variable.outline(GvarSampleFont.GLYPH_A, def);
        CollectingPen peak = new CollectingPen();
        variable.outline(GvarSampleFont.GLYPH_A, peak, new float[] {GvarSampleFont.MAX_WEIGHT});
        boolean moved = !peak.commands().isEmpty()
                && Math.abs(peak.commands().getFirst().x0() - def.commands().getFirst().x0()
                - GvarSampleFont.CONTOUR_X_DELTA) < 0.01f;
        if (axes < 1 || !moved) {
            throw new IllegalStateException("Variable leftover did not move the outline");
        }
        SfntFont avarFont = AvarSampleFont.create();
        CollectingPen remapped = new CollectingPen();
        avarFont.outline(GvarSampleFont.GLYPH_A, remapped, new float[] {AvarSampleFont.MID_WEIGHT});
        boolean avar = !remapped.commands().isEmpty()
                && Math.abs(remapped.commands().getFirst().x0() - GvarSampleFont.CONTOUR_X_DELTA) < 0.01f;
        if (!avar) {
            throw new IllegalStateException("avar leftover did not remap the mid instance");
        }
        SfntFont cbdtFont = CbdtSampleFont.create();
        boolean cbdt = cbdtFont.colorBitmap(cbdtFont.glyphId('A')) != null;
        SfntFont ebdtFont = EbdtSampleFont.create();
        boolean ebdt = ebdtFont.grayscaleBitmap(ebdtFont.glyphId('A')) != null;
        SfntFont sbixFont = SbixSampleFont.create();
        boolean sbix = sbixFont.embeddedBitmap(sbixFont.glyphId('A')) != null;
        if (!cbdt || !ebdt || !sbix) {
            throw new IllegalStateException("Embedded-bitmap leftover did not return a strike");
        }
        return new LeftoverEvidence(layers, axes, moved, avar, cbdt, ebdt, sbix);
    }

    /// Observations from the named font leftovers.
    ///
    /// @param colrLayers COLR layer count
    /// @param variationAxes `fvar` axis count
    /// @param gvarPeakMoved whether the peak instance moved the outline
    /// @param avarRemapped whether `avar` sent the mid instance to the peak
    /// @param cbdtPresent whether CBLC/CBDT returned a strike
    /// @param ebdtPresent whether EBLC/EBDT returned a strike
    /// @param sbixPresent whether `sbix` returned a strike
    private record LeftoverEvidence(
            int colrLayers,
            int variationAxes,
            boolean gvarPeakMoved,
            boolean avarRemapped,
            boolean cbdtPresent,
            boolean ebdtPresent,
            boolean sbixPresent
    ) {
        /// Encodes the leftover observation.
        ///
        /// @return JSON
        String toJson() {
            return """
                    {
                      "colrLayers": %d,
                      "variationAxes": %d,
                      "gvarPeakMoved": %s,
                      "avarRemapped": %s,
                      "cbdtPresent": %s,
                      "ebdtPresent": %s,
                      "sbixPresent": %s
                    }
                    """.formatted(
                    colrLayers,
                    variationAxes,
                    gvarPeakMoved,
                    avarRemapped,
                    cbdtPresent,
                    ebdtPresent,
                    sbixPresent
            );
        }
    }
}
