package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one pointer-free display-list command.
@NotNullByDefault
public sealed interface DisplayListOp {
    /// Fills an axis-aligned rectangle.
    ///
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the width
    /// @param height the height
    /// @param color the fill color
    record FillRect(float x, float y, float width, float height, Color color) implements DisplayListOp {
        /// Validates the rectangle.
        public FillRect {
            if (!Float.isFinite(x) || !Float.isFinite(y)
                    || !Float.isFinite(width) || !Float.isFinite(height)
                    || width < 0.0f || height < 0.0f) {
                throw new IllegalArgumentException("FillRect must be finite with nonnegative extents");
            }
            Objects.requireNonNull(color, "color");
        }
    }

    /// Fills a path.
    ///
    /// @param path the path
    /// @param color the fill color
    record FillPath(Path path, Color color) implements DisplayListOp {
        /// Validates the command.
        public FillPath {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(color, "color");
        }
    }

    /// Draws an 8-bit grayscale glyph coverage mask.
    ///
    /// @param x the destination x
    /// @param y the destination y
    /// @param width the mask width
    /// @param height the mask height
    /// @param coverage row-major coverage in `[0, 255]`
    /// @param color the glyph color
    record DrawGlyph(
            float x,
            float y,
            int width,
            int height,
            byte @org.jetbrains.annotations.Unmodifiable [] coverage,
            Color color
    ) implements DisplayListOp {
        /// Validates the glyph command.
        public DrawGlyph {
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                throw new IllegalArgumentException("Glyph origin must be finite");
            }
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("Glyph extents must be nonnegative");
            }
            Objects.requireNonNull(coverage, "coverage");
            if (coverage.length != Math.multiplyExact(width, height)) {
                throw new IllegalArgumentException("Glyph coverage length does not match extents");
            }
            coverage = coverage.clone();
            Objects.requireNonNull(color, "color");
        }
    }
}
