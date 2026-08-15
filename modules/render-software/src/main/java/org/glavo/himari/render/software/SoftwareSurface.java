package org.glavo.himari.render.software;

import org.glavo.himari.graphics.Color;
import org.glavo.himari.graphics.DisplayList;
import org.glavo.himari.graphics.DisplayListOp;
import org.glavo.himari.graphics.Path;
import org.glavo.himari.graphics.PathVerb;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.MemorySegment;
import java.util.Objects;

/// Stores one extended-linear software surface and rasters display-list commands into it.
@NotNullByDefault
public final class SoftwareSurface {
    /// The surface width in pixels.
    private final int width;

    /// The surface height in pixels.
    private final int height;

    /// Premultiplied extended-linear RGBA stored as `r,g,b,a` per pixel.
    private final float[] pixels;

    /// Creates a transparent surface.
    ///
    /// @param width the positive width
    /// @param height the positive height
    public SoftwareSurface(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Surface extents must be positive");
        }
        this.width = width;
        this.height = height;
        this.pixels = new float[Math.multiplyExact(Math.multiplyExact(width, height), 4)];
    }

    /// Returns the width.
    ///
    /// @return the width
    public int width() {
        return width;
    }

    /// Returns the height.
    ///
    /// @return the height
    public int height() {
        return height;
    }

    /// Returns a copy of the extended-linear premultiplied pixel buffer.
    ///
    /// @return the pixels
    public float @Unmodifiable [] extendedLinearPremultiplied() {
        return pixels.clone();
    }

    /// Clears the surface to a solid unassociated color.
    ///
    /// @param color the clear color
    public void clear(Color color) {
        Color linear = Objects.requireNonNull(color, "color").toExtendedLinear();
        float alpha = linear.alpha();
        float red = linear.red() * alpha;
        float green = linear.green() * alpha;
        float blue = linear.blue() * alpha;
        for (int index = 0; index < pixels.length; index += 4) {
            pixels[index] = red;
            pixels[index + 1] = green;
            pixels[index + 2] = blue;
            pixels[index + 3] = alpha;
        }
    }

    /// Rasters one display list in painter order.
    ///
    /// @param list the display list
    public void replay(DisplayList list) {
        Objects.requireNonNull(list, "list");
        for (DisplayListOp op : list.ops()) {
            switch (op) {
                case DisplayListOp.FillRect rect -> fillRect(rect.x(), rect.y(), rect.width(), rect.height(), rect.color());
                case DisplayListOp.FillPath path -> fillPath(path.path(), path.color());
                case DisplayListOp.DrawGlyph glyph -> drawGlyph(glyph);
            }
        }
    }

    /// Fills an axis-aligned rectangle using coverage-weighted source-over.
    ///
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the width
    /// @param height the height
    /// @param color the unassociated fill
    public void fillRect(float x, float y, float width, float height, Color color) {
        fillCoverage(x, y, width, height, 1.0f, color);
    }

    /// Fills a polygonal path using even-odd scan conversion.
    ///
    /// @param path the path
    /// @param color the unassociated fill
    public void fillPath(Path path, Color color) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(color, "color");
        int pointCount = path.points().length / 2;
        if (pointCount < 3) {
            return;
        }
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float[] xs = new float[pointCount];
        float[] ys = new float[pointCount];
        int count = 0;
        int cursor = 0;
        float startX = 0.0f;
        float startY = 0.0f;
        for (PathVerb verb : path.verbs()) {
            switch (verb) {
                case MOVE -> {
                    startX = path.points()[cursor];
                    startY = path.points()[cursor + 1];
                    xs[count] = startX;
                    ys[count] = startY;
                    minX = Math.min(minX, startX);
                    minY = Math.min(minY, startY);
                    maxX = Math.max(maxX, startX);
                    maxY = Math.max(maxY, startY);
                    count++;
                    cursor += 2;
                }
                case LINE -> {
                    float x = path.points()[cursor];
                    float y = path.points()[cursor + 1];
                    xs[count] = x;
                    ys[count] = y;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    count++;
                    cursor += 2;
                }
                case CLOSE -> {
                    // Contour close is implied by even-odd wrapping.
                }
            }
        }
        int left = Math.max(0, (int) Math.floor(minX));
        int top = Math.max(0, (int) Math.floor(minY));
        int right = Math.min(width, (int) Math.ceil(maxX));
        int bottom = Math.min(height, (int) Math.ceil(maxY));
        for (int py = top; py < bottom; py++) {
            float sampleY = py + 0.5f;
            for (int px = left; px < right; px++) {
                if (evenOdd(xs, ys, count, px + 0.5f, sampleY)) {
                    blend(px, py, 1.0f, color);
                }
            }
        }
    }

    /// Draws one grayscale coverage glyph.
    ///
    /// @param glyph the glyph command
    public void drawGlyph(DisplayListOp.DrawGlyph glyph) {
        Objects.requireNonNull(glyph, "glyph");
        int originX = Math.round(glyph.x());
        int originY = Math.round(glyph.y());
        for (int row = 0; row < glyph.height(); row++) {
            int destY = originY + row;
            if (destY < 0 || destY >= height) {
                continue;
            }
            for (int column = 0; column < glyph.width(); column++) {
                int destX = originX + column;
                if (destX < 0 || destX >= width) {
                    continue;
                }
                int coverage = glyph.coverage()[row * glyph.width() + column] & 0xFF;
                if (coverage == 0) {
                    continue;
                }
                blend(destX, destY, coverage / 255.0f, glyph.color());
            }
        }
    }

    /// Encodes the surface as an 8-bit sRGB PNG.
    ///
    /// @return a read-only PNG file
    public MemorySegment toSdrPng() {
        return PngEncoder.encodeRgba(width, height, toSdrRgba());
    }

    /// Returns unassociated 8-bit sRGB pixels in row-major RGBA order.
    ///
    /// @return a read-only SDR pixel image
    public MemorySegment toSdrRgba() {
        byte[] rgba = new byte[width * height * 4];
        for (int pixel = 0, dest = 0; pixel < pixels.length; pixel += 4, dest += 4) {
            float alpha = pixels[pixel + 3];
            float red = alpha == 0.0f ? 0.0f : pixels[pixel] / alpha;
            float green = alpha == 0.0f ? 0.0f : pixels[pixel + 1] / alpha;
            float blue = alpha == 0.0f ? 0.0f : pixels[pixel + 2] / alpha;
            Color srgb = Color.extendedLinear(red, green, blue, Math.clamp(alpha, 0.0f, 1.0f)).toSrgb();
            rgba[dest] = (byte) Math.round(srgb.red() * 255.0f);
            rgba[dest + 1] = (byte) Math.round(srgb.green() * 255.0f);
            rgba[dest + 2] = (byte) Math.round(srgb.blue() * 255.0f);
            rgba[dest + 3] = (byte) Math.round(srgb.alpha() * 255.0f);
        }
        return MemorySegment.ofArray(rgba).asReadOnly();
    }

    /// Fills a rectangle with uniform coverage.
    ///
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the width
    /// @param height the height
    /// @param coverage the coverage
    /// @param color the fill
    private void fillCoverage(float x, float y, float width, float height, float coverage, Color color) {
        int left = Math.max(0, (int) Math.floor(x));
        int top = Math.max(0, (int) Math.floor(y));
        int right = Math.min(this.width, (int) Math.ceil(x + width));
        int bottom = Math.min(this.height, (int) Math.ceil(y + height));
        for (int py = top; py < bottom; py++) {
            for (int px = left; px < right; px++) {
                blend(px, py, coverage, color);
            }
        }
    }

    /// Source-over blends one pixel.
    ///
    /// @param x the x
    /// @param y the y
    /// @param coverage the coverage
    /// @param color the unassociated source
    private void blend(int x, int y, float coverage, Color color) {
        Color linear = color.toExtendedLinear();
        float sourceAlpha = linear.alpha() * coverage;
        float sourceRed = linear.red() * sourceAlpha;
        float sourceGreen = linear.green() * sourceAlpha;
        float sourceBlue = linear.blue() * sourceAlpha;
        int index = (y * width + x) * 4;
        float destAlpha = pixels[index + 3];
        float outAlpha = sourceAlpha + destAlpha * (1.0f - sourceAlpha);
        pixels[index] = sourceRed + pixels[index] * (1.0f - sourceAlpha);
        pixels[index + 1] = sourceGreen + pixels[index + 1] * (1.0f - sourceAlpha);
        pixels[index + 2] = sourceBlue + pixels[index + 2] * (1.0f - sourceAlpha);
        pixels[index + 3] = outAlpha;
    }

    /// Returns whether a point is inside an even-odd polygon.
    ///
    /// @param xs the x coordinates
    /// @param ys the y coordinates
    /// @param count the vertex count
    /// @param x the sample x
    /// @param y the sample y
    /// @return whether the point is inside
    private static boolean evenOdd(float[] xs, float[] ys, int count, float x, float y) {
        boolean inside = false;
        for (int index = 0, previous = count - 1; index < count; previous = index++) {
            float yi = ys[index];
            float yj = ys[previous];
            if ((yi > y) != (yj > y)) {
                float xj = xs[previous];
                float xi = xs[index];
                float intersect = (xj - xi) * (y - yi) / (yj - yi) + xi;
                if (x < intersect) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }
}
