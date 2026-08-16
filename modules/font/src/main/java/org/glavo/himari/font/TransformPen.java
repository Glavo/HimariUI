package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Applies an affine transform, then forwards commands to another pen.
@NotNullByDefault
final class TransformPen implements OutlinePen {
    /// Destination pen.
    private final OutlinePen inner;

    /// `x` coefficient of the source `x`.
    private final float xx;

    /// `x` coefficient of the source `y`.
    private final float xy;

    /// `y` coefficient of the source `x`.
    private final float yx;

    /// `y` coefficient of the source `y`.
    private final float yy;

    /// Translation `x`.
    private final float dx;

    /// Translation `y`.
    private final float dy;

    /// Reused mapped `x`.
    private float mappedX;

    /// Reused mapped `y`.
    private float mappedY;

    /// Creates a transforming wrapper.
    ///
    /// @param inner the destination
    /// @param xx the `x` scale
    /// @param xy the `x` shear
    /// @param yx the `y` shear
    /// @param yy the `y` scale
    /// @param dx the translation x
    /// @param dy the translation y
    TransformPen(OutlinePen inner, float xx, float xy, float yx, float yy, float dx, float dy) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.xx = xx;
        this.xy = xy;
        this.yx = yx;
        this.yy = yy;
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public void moveTo(float x, float y) {
        map(x, y);
        inner.moveTo(mappedX, mappedY);
    }

    @Override
    public void lineTo(float x, float y) {
        map(x, y);
        inner.lineTo(mappedX, mappedY);
    }

    @Override
    public void quadTo(float cx, float cy, float x, float y) {
        map(cx, cy);
        float controlX = mappedX;
        float controlY = mappedY;
        map(x, y);
        inner.quadTo(controlX, controlY, mappedX, mappedY);
    }

    @Override
    public void cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
        map(c1x, c1y);
        float firstX = mappedX;
        float firstY = mappedY;
        map(c2x, c2y);
        float secondX = mappedX;
        float secondY = mappedY;
        map(x, y);
        inner.cubicTo(firstX, firstY, secondX, secondY, mappedX, mappedY);
    }

    @Override
    public void close() {
        inner.close();
    }

    /// Maps `(x, y)` into [`#mappedX`] and [`#mappedY`].
    private void map(float x, float y) {
        mappedX = xx * x + xy * y + dx;
        mappedY = yx * x + yy * y + dy;
    }
}
