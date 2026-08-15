package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.ByteBuffer;
import java.util.Objects;

/// Rasters a simple TrueType glyph to an 8-bit grayscale coverage mask.
@NotNullByDefault
public final class GlyphRasterizer {
    /// Prevents instantiation.
    private GlyphRasterizer() {
    }

    /// Rasters one glyph into a tightly packed coverage mask.
    ///
    /// @param font the font
    /// @param glyphId the glyph identity
    /// @param pixelHeight the positive destination em height
    /// @return the coverage mask
    public static GlyphMask rasterize(SfntFont font, int glyphId, int pixelHeight) {
        Objects.requireNonNull(font, "font");
        if (pixelHeight <= 0) {
            throw new IllegalArgumentException("pixelHeight must be positive");
        }
        ByteBuffer glyf = font.glyf(glyphId);
        if (glyf.remaining() == 0) {
            return new GlyphMask(0, 0, new byte[0]);
        }
        short contours = glyf.getShort();
        if (contours <= 0) {
            return new GlyphMask(0, 0, new byte[0]);
        }
        short xMin = glyf.getShort();
        short yMin = glyf.getShort();
        short xMax = glyf.getShort();
        short yMax = glyf.getShort();
        int lastPoint = Short.toUnsignedInt(glyf.getShort());
        int instructionLength = Short.toUnsignedInt(glyf.getShort());
        glyf.position(glyf.position() + instructionLength);
        int pointCount = lastPoint + 1;
        int[] xs = new int[pointCount];
        int[] ys = new int[pointCount];
        for (int index = 0; index < pointCount; index++) {
            glyf.get();
        }
        int x = 0;
        int y = 0;
        for (int index = 0; index < pointCount; index++) {
            x += glyf.getShort();
            xs[index] = x;
        }
        for (int index = 0; index < pointCount; index++) {
            y += glyf.getShort();
            ys[index] = y;
        }
        float scale = pixelHeight / (float) font.unitsPerEm();
        int width = Math.max(1, Math.round((xMax - xMin) * scale));
        int height = Math.max(1, Math.round((yMax - yMin) * scale));
        byte[] coverage = new byte[width * height];
        for (int row = 0; row < height; row++) {
            float sampleY = yMin + (row + 0.5f) / scale;
            for (int column = 0; column < width; column++) {
                float sampleX = xMin + (column + 0.5f) / scale;
                if (evenOdd(xs, ys, sampleX, sampleY)) {
                    coverage[row * width + column] = (byte) 255;
                }
            }
        }
        return new GlyphMask(width, height, coverage);
    }

    /// Even-odd fill test.
    ///
    /// @param xs the x coordinates
    /// @param ys the y coordinates
    /// @param x the sample x
    /// @param y the sample y
    /// @return whether the sample is inside
    private static boolean evenOdd(int[] xs, int[] ys, float x, float y) {
        boolean inside = false;
        for (int index = 0, previous = xs.length - 1; index < xs.length; previous = index++) {
            float yi = ys[index];
            float yj = ys[previous];
            if ((yi > y) != (yj > y)) {
                float intersect = (xs[previous] - xs[index]) * (y - yi) / (yj - yi) + xs[index];
                if (x < intersect) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }
}
