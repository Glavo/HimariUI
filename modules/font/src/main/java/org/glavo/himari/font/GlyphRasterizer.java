package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.ByteBuffer;
import java.util.Objects;

/// Rasters an unhinted TrueType outline to an 8-bit grayscale coverage mask.
///
/// Coverage is 4×4 supersampled non-zero winding of flattened quadratic contours. The shipped path
/// does not fill the raw `glyf` point list as a polyline.
@NotNullByDefault
public final class GlyphRasterizer {
    /// Subsamples per pixel axis.
    private static final int SUBSAMPLES = 4;

    /// Maximum quad subdivision depth.
    private static final int MAX_FLATTEN_DEPTH = 8;

    /// Squared flatness in font units at which a quad becomes a line.
    private static final float FLATNESS_SQUARED = 0.25f;

    /// Prevents instantiation.
    private GlyphRasterizer() {
    }

    /// Rasters one glyph into a tightly packed coverage mask.
    ///
    /// The mask origin is the glyph's `glyf` bounding-box minimum. Row 0 is the minimum y. Empty
    /// glyphs return a zero-by-zero mask.
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
        if (glyf.remaining() < 10) {
            return new GlyphMask(0, 0, new byte[0]);
        }
        glyf.getShort();
        short xMin = glyf.getShort();
        short yMin = glyf.getShort();
        short xMax = glyf.getShort();
        short yMax = glyf.getShort();
        FlatteningPen flatten = new FlatteningPen();
        font.outline(glyphId, flatten);
        if (flatten.pointCount == 0) {
            return new GlyphMask(0, 0, new byte[0]);
        }
        float scale = pixelHeight / (float) font.unitsPerEm();
        int width = Math.max(1, Math.round((xMax - xMin) * scale));
        int height = Math.max(1, Math.round((yMax - yMin) * scale));
        byte[] coverage = new byte[width * height];
        int sampleMax = SUBSAMPLES * SUBSAMPLES;
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                int hits = 0;
                for (int sampleY = 0; sampleY < SUBSAMPLES; sampleY++) {
                    float y = yMin + (row + (sampleY + 0.5f) / SUBSAMPLES) / scale;
                    for (int sampleX = 0; sampleX < SUBSAMPLES; sampleX++) {
                        float x = xMin + (column + (sampleX + 0.5f) / SUBSAMPLES) / scale;
                        if (flatten.contains(x, y)) {
                            hits++;
                        }
                    }
                }
                coverage[row * width + column] = (byte) ((hits * 255) / sampleMax);
            }
        }
        return new GlyphMask(width, height, coverage);
    }

    /// Collects flattened closed contours in font units.
    private static final class FlatteningPen implements OutlinePen {
        /// Contour x coordinates.
        private float[] xs = new float[32];

        /// Contour y coordinates.
        private float[] ys = new float[32];

        /// Exclusive end index of each closed contour.
        private int[] ends = new int[4];

        /// Number of stored points.
        private int pointCount;

        /// Number of closed contours.
        private int contourCount;

        /// Index of the current contour's first point.
        private int contourStart;

        /// Whether a contour is open.
        private boolean open;

        /// Current point x.
        private float currentX;

        /// Current point y.
        private float currentY;

        @Override
        public void moveTo(float x, float y) {
            if (open) {
                close();
            }
            contourStart = pointCount;
            add(x, y);
            currentX = x;
            currentY = y;
            open = true;
        }

        @Override
        public void lineTo(float x, float y) {
            if (!open) {
                moveTo(x, y);
                return;
            }
            add(x, y);
            currentX = x;
            currentY = y;
        }

        @Override
        public void quadTo(float cx, float cy, float x, float y) {
            if (!open) {
                moveTo(cx, cy);
            }
            flattenQuad(currentX, currentY, cx, cy, x, y, 0);
            currentX = x;
            currentY = y;
        }

        @Override
        public void close() {
            if (!open || pointCount <= contourStart) {
                open = false;
                return;
            }
            float startX = xs[contourStart];
            float startY = ys[contourStart];
            if (currentX != startX || currentY != startY) {
                add(startX, startY);
            }
            if (ends.length == contourCount) {
                ends = growInts(ends);
            }
            ends[contourCount++] = pointCount;
            open = false;
        }

        /// Returns whether `(x, y)` is inside by the non-zero winding rule.
        ///
        /// @param x the sample x
        /// @param y the sample y
        /// @return whether the sample is covered
        private boolean contains(float x, float y) {
            int winding = 0;
            int start = 0;
            for (int contour = 0; contour < contourCount; contour++) {
                int end = ends[contour];
                for (int index = start; index < end - 1; index++) {
                    winding += crossing(xs[index], ys[index], xs[index + 1], ys[index + 1], x, y);
                }
                start = end;
            }
            return winding != 0;
        }

        /// Subdivides a quadratic until it is flat.
        private void flattenQuad(float x0, float y0, float cx, float cy, float x1, float y1, int depth) {
            float dx = x0 - 2.0f * cx + x1;
            float dy = y0 - 2.0f * cy + y1;
            if (depth >= MAX_FLATTEN_DEPTH || dx * dx + dy * dy <= FLATNESS_SQUARED) {
                add(x1, y1);
                return;
            }
            float ax = (x0 + cx) * 0.5f;
            float ay = (y0 + cy) * 0.5f;
            float bx = (cx + x1) * 0.5f;
            float by = (cy + y1) * 0.5f;
            float mx = (ax + bx) * 0.5f;
            float my = (ay + by) * 0.5f;
            flattenQuad(x0, y0, ax, ay, mx, my, depth + 1);
            flattenQuad(mx, my, bx, by, x1, y1, depth + 1);
        }

        /// Appends one point.
        private void add(float x, float y) {
            if (pointCount == xs.length) {
                xs = grow(xs);
                ys = grow(ys);
            }
            xs[pointCount] = x;
            ys[pointCount] = y;
            pointCount++;
        }
    }

    /// Returns the winding contribution of one directed edge.
    private static int crossing(float x0, float y0, float x1, float y1, float x, float y) {
        if (y0 <= y) {
            if (y1 > y && isLeft(x0, y0, x1, y1, x, y) > 0.0f) {
                return 1;
            }
        } else if (y1 <= y && isLeft(x0, y0, x1, y1, x, y) < 0.0f) {
            return -1;
        }
        return 0;
    }

    /// Returns the signed cross product of `(x1,y1)-(x0,y0)` and `(x,y)-(x0,y0)`.
    private static float isLeft(float x0, float y0, float x1, float y1, float x, float y) {
        return (x1 - x0) * (y - y0) - (x - x0) * (y1 - y0);
    }

    /// Grows a coordinate buffer.
    private static float[] grow(float[] values) {
        float[] grown = new float[values.length * 2];
        System.arraycopy(values, 0, grown, 0, values.length);
        return grown;
    }

    /// Grows an end-index buffer.
    private static int[] growInts(int[] values) {
        int[] grown = new int[values.length * 2];
        System.arraycopy(values, 0, grown, 0, values.length);
        return grown;
    }
}
