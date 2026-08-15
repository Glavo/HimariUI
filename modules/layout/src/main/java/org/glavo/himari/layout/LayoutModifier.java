package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

/// Adjusts incoming constraints or outgoing size for one layout node.
@NotNullByDefault
public sealed interface LayoutModifier
        permits LayoutModifier.Padding, LayoutModifier.ExactSize, LayoutModifier.MinSize {
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
