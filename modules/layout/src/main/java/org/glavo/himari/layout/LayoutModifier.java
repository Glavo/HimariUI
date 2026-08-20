package org.glavo.himari.layout;

import org.glavo.himari.layout.semantics.TextDirection;
import org.jetbrains.annotations.NotNullByDefault;

import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Adjusts incoming constraints or outgoing size for one layout node.
@NotNullByDefault
public sealed interface LayoutModifier
        permits LayoutModifier.Padding,
        LayoutModifier.ExactSize,
        LayoutModifier.MinSize,
        LayoutModifier.MaxSize,
        LayoutModifier.FlexGrow,
        LayoutModifier.GridColumns,
        LayoutModifier.OverlayOffset,
        LayoutModifier.AspectRatio,
        LayoutModifier.Scale,
        LayoutModifier.Rotate,
        LayoutModifier.Translate,
        LayoutModifier.Skew,
        LayoutModifier.Clip,
        LayoutModifier.ClipRRect,
        LayoutModifier.ClipOval,
        LayoutModifier.ClipPath,
        LayoutModifier.IgnorePointer,
        LayoutModifier.AbsorbPointer,
        LayoutModifier.ReadingDirection {
    /// Deflates incoming constraints by padding on every side.
    ///
    /// @param all the nonnegative padding
    record Padding(float all) implements LayoutModifier {
        /// Validates the padding.
        public Padding {
            if (!Float.isFinite(all) || all < 0.0f) {
                throw new IllegalArgumentException("Padding must be finite and nonnegative");
            }
        }

        /// {@inheritDoc}
        @Override
        public Constraints apply(Constraints incoming) {
            return incoming.deflate(all * 2.0f, all * 2.0f);
        }

        /// {@inheritDoc}
        @Override
        public Size wrap(Size child) {
            return new Size(child.width() + all * 2.0f, child.height() + all * 2.0f);
        }

        /// {@inheritDoc}
        @Override
        public Offset childOrigin() {
            return new Offset(all, all);
        }
    }

    /// Forces an exact measured size after child measurement.
    ///
    /// @param width the exact width
    /// @param height the exact height
    record ExactSize(float width, float height) implements LayoutModifier {
        /// Validates the size.
        public ExactSize {
            if (!Float.isFinite(width) || !Float.isFinite(height) || width < 0.0f || height < 0.0f) {
                throw new IllegalArgumentException("Exact size must be finite and nonnegative");
            }
        }

        /// {@inheritDoc}
        @Override
        public Constraints apply(Constraints incoming) {
            Size size = incoming.constrain(width, height);
            return Constraints.tight(size.width(), size.height());
        }

        /// {@inheritDoc}
        @Override
        public Size wrap(Size child) {
            return new Size(width, height);
        }
    }

    /// Raises incoming minima.
    ///
    /// @param minWidth the additional minimum width
    /// @param minHeight the additional minimum height
    record MinSize(float minWidth, float minHeight) implements LayoutModifier {
        /// Validates the minima.
        public MinSize {
            if (!Float.isFinite(minWidth) || !Float.isFinite(minHeight)
                    || minWidth < 0.0f || minHeight < 0.0f) {
                throw new IllegalArgumentException("Minimum size must be finite and nonnegative");
            }
        }

        /// {@inheritDoc}
        @Override
        public Constraints apply(Constraints incoming) {
            return new Constraints(
                    Math.min(incoming.maxWidth(), Math.max(incoming.minWidth(), minWidth)),
                    incoming.maxWidth(),
                    Math.min(incoming.maxHeight(), Math.max(incoming.minHeight(), minHeight)),
                    incoming.maxHeight()
            );
        }
    }

    /// Lowers incoming maxima and clamps the published size.
    ///
    /// @param maxWidth the finite nonnegative maximum width
    /// @param maxHeight the finite nonnegative maximum height
    record MaxSize(float maxWidth, float maxHeight) implements LayoutModifier {
        /// Validates the maxima.
        public MaxSize {
            if (!Float.isFinite(maxWidth) || !Float.isFinite(maxHeight)
                    || maxWidth < 0.0f || maxHeight < 0.0f) {
                throw new IllegalArgumentException("Maximum size must be finite and nonnegative");
            }
        }

        /// {@inheritDoc}
        @Override
        public Constraints apply(Constraints incoming) {
            float nextMaxWidth = Math.min(incoming.maxWidth(), maxWidth);
            float nextMaxHeight = Math.min(incoming.maxHeight(), maxHeight);
            return new Constraints(
                    Math.min(incoming.minWidth(), nextMaxWidth),
                    nextMaxWidth,
                    Math.min(incoming.minHeight(), nextMaxHeight),
                    nextMaxHeight
            );
        }

        /// {@inheritDoc}
        @Override
        public Size wrap(Size child) {
            return new Size(Math.min(child.width(), maxWidth), Math.min(child.height(), maxHeight));
        }
    }

    /// Requests a share of leftover main-axis space inside a [LayoutKind#FLEX] parent.
    ///
    /// @param weight the nonnegative grow weight; `0` keeps the child's intrinsic width
    record FlexGrow(float weight) implements LayoutModifier {
        /// Validates the weight.
        public FlexGrow {
            if (!Float.isFinite(weight) || weight < 0.0f) {
                throw new IllegalArgumentException("Flex grow must be finite and nonnegative");
            }
        }
    }

    /// Declares the column count of a [LayoutKind#GRID] parent.
    ///
    /// @param columns the positive column count
    record GridColumns(int columns) implements LayoutModifier {
        /// Validates the column count.
        public GridColumns {
            if (columns <= 0) {
                throw new IllegalArgumentException("Grid column count must be positive");
            }
        }
    }

    /// Forces a width-over-height aspect ratio that fits the incoming maxima.
    ///
    /// The largest size with this ratio that fits inside the incoming maxima is
    /// published as tight constraints. When the height implied by the maximum
    /// width exceeds the maximum height, the height axis wins.
    ///
    /// @param ratio the positive width divided by height
    record AspectRatio(float ratio) implements LayoutModifier {
        /// Validates the ratio.
        public AspectRatio {
            if (!Float.isFinite(ratio) || ratio <= 0.0f) {
                throw new IllegalArgumentException("Aspect ratio must be finite and positive");
            }
        }

        /// {@inheritDoc}
        @Override
        public Constraints apply(Constraints incoming) {
            float width = incoming.maxWidth();
            float height = width / ratio;
            if (height > incoming.maxHeight()) {
                height = incoming.maxHeight();
                width = height * ratio;
            }
            Size size = incoming.constrain(width, height);
            return Constraints.tight(size.width(), size.height());
        }

        /// {@inheritDoc}
        @Override
        public Size wrap(Size child) {
            if (child.height() <= 0.0f) {
                return new Size(child.width(), child.width() / ratio);
            }
            float childRatio = child.width() / child.height();
            if (Math.abs(childRatio - ratio) <= 1.0e-5f) {
                return child;
            }
            return new Size(child.height() * ratio, child.height());
        }
    }

    /// Scales incoming constraints down and the published size up by `factor`.
    ///
    /// @param factor the positive scale
    record Scale(float factor) implements LayoutModifier {
        /// Validates the factor.
        public Scale {
            if (!Float.isFinite(factor) || factor <= 0.0f) {
                throw new IllegalArgumentException("Scale factor must be finite and positive");
            }
        }

        /// {@inheritDoc}
        @Override
        public Constraints apply(Constraints incoming) {
            return new Constraints(
                    incoming.minWidth() / factor,
                    incoming.maxWidth() / factor,
                    incoming.minHeight() / factor,
                    incoming.maxHeight() / factor
            );
        }

        /// {@inheritDoc}
        @Override
        public Size wrap(Size child) {
            return new Size(child.width() * factor, child.height() * factor);
        }
    }

    /// Rotates the child's axis-aligned box; the published size is the AABB.
    ///
    /// @param degrees the finite rotation in degrees, clockwise
    record Rotate(float degrees) implements LayoutModifier {
        /// Validates the rotation.
        public Rotate {
            if (!Float.isFinite(degrees)) {
                throw new IllegalArgumentException("Rotation must be finite");
            }
        }

        /// {@inheritDoc}
        @Override
        public Size wrap(Size child) {
            double radians = Math.toRadians(degrees);
            float cos = (float) Math.abs(Math.cos(radians));
            float sin = (float) Math.abs(Math.sin(radians));
            return new Size(
                    Math.fma(child.width(), cos, child.height() * sin),
                    Math.fma(child.width(), sin, child.height() * cos)
            );
        }
    }

    /// Translates the child inside an expanded axis-aligned box.
    ///
    /// @param x the finite x translation
    /// @param y the finite y translation
    record Translate(float x, float y) implements LayoutModifier {
        /// Validates the translation.
        public Translate {
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                throw new IllegalArgumentException("Translation must be finite");
            }
        }

        /// {@inheritDoc}
        @Override
        public Size wrap(Size child) {
            float minX = Math.min(0.0f, x);
            float minY = Math.min(0.0f, y);
            float maxX = Math.max(child.width(), child.width() + x);
            float maxY = Math.max(child.height(), child.height() + y);
            return new Size(maxX - minX, maxY - minY);
        }

        /// {@inheritDoc}
        @Override
        public Offset childOrigin() {
            return new Offset(x - Math.min(0.0f, x), y - Math.min(0.0f, y));
        }
    }

    /// Shears the child's box; the published size is the AABB.
    ///
    /// `x` is the horizontal shear factor (`x' = x + xFactor * y`). `y` is the
    /// vertical shear factor (`y' = y + yFactor * x`).
    ///
    /// @param x the finite horizontal shear factor
    /// @param y the finite vertical shear factor
    record Skew(float x, float y) implements LayoutModifier {
        /// Validates the shear.
        public Skew {
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                throw new IllegalArgumentException("Shear must be finite");
            }
        }

        /// {@inheritDoc}
        @Override
        public Size wrap(Size child) {
            float width = child.width();
            float height = child.height();
            float minX = Math.min(0.0f, Math.min(width, Math.min(x * height, width + x * height)));
            float maxX = Math.max(0.0f, Math.max(width, Math.max(x * height, width + x * height)));
            float minY = Math.min(0.0f, Math.min(y * width, Math.min(height, height + y * width)));
            float maxY = Math.max(0.0f, Math.max(y * width, Math.max(height, height + y * width)));
            return new Size(maxX - minX, maxY - minY);
        }
    }

    /// Clips descendant hit testing to this node's bounds.
    ///
    /// Overflowing children remain painted in layout coordinates but are not
    /// hittable outside this node.
    record Clip() implements LayoutModifier {
        /// Creates a clip-to-bounds modifier.
        public Clip {
        }
    }

    /// Clips descendant hit testing to a rounded rectangle of this node's bounds.
    ///
    /// @param radius the nonnegative corner radius
    record ClipRRect(float radius) implements LayoutModifier {
        /// Validates the radius.
        public ClipRRect {
            if (!Float.isFinite(radius) || radius < 0.0f) {
                throw new IllegalArgumentException("Corner radius must be finite and nonnegative");
            }
        }
    }

    /// Clips descendant hit testing to the oval inscribed in this node's bounds.
    record ClipOval() implements LayoutModifier {
        /// Creates an oval clip.
        public ClipOval {
        }
    }

    /// Clips descendant hit testing to a polygon in this node's local coordinates.
    ///
    /// @param points even-length `x,y` pairs, at least three vertices
    record ClipPath(float @Unmodifiable [] points) implements LayoutModifier {
        /// Validates and copies the vertices.
        public ClipPath {
            Objects.requireNonNull(points, "points");
            if (points.length < 6 || (points.length & 1) != 0) {
                throw new IllegalArgumentException("Path clip requires at least three x,y vertices");
            }
            for (float value : points) {
                if (!Float.isFinite(value)) {
                    throw new IllegalArgumentException("Path clip vertices must be finite");
                }
            }
            points = Arrays.copyOf(points, points.length);
        }
    }

    /// Excludes this node and its descendants from hit testing.
    record IgnorePointer() implements LayoutModifier {
        /// Creates a pointer-ignoring modifier.
        public IgnorePointer {
        }
    }

    /// Hits this node and hides descendants and siblings behind it.
    record AbsorbPointer() implements LayoutModifier {
        /// Creates a pointer-absorbing modifier.
        public AbsorbPointer {
        }
    }

    /// Selects LTR or RTL placement for row, flex, flow, and grid parents.
    ///
    /// @param direction the reading direction
    record ReadingDirection(TextDirection direction) implements LayoutModifier {
        /// Validates the direction.
        public ReadingDirection {
            Objects.requireNonNull(direction, "direction");
        }
    }

    /// Places a child at an explicit overlay or portal offset.
    ///
    /// @param x the finite x offset
    /// @param y the finite y offset
    record OverlayOffset(float x, float y) implements LayoutModifier {
        /// Validates the offset.
        public OverlayOffset {
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                throw new IllegalArgumentException("Overlay offset must be finite");
            }
        }
    }

    /// Transforms incoming constraints before child measurement.
    ///
    /// @param incoming the parent constraints
    /// @return the child constraints
    default Constraints apply(Constraints incoming) {
        return incoming;
    }

    /// Transforms the child size into the parent size.
    ///
    /// @param child the measured child size
    /// @return the parent size
    default Size wrap(Size child) {
        return child;
    }

    /// Returns the origin of the child inside this modifier.
    ///
    /// @return the child origin
    default Offset childOrigin() {
        return Offset.ZERO;
    }
}
