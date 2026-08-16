package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one outline command recorded from an [`OutlinePen`].
///
/// @param verb the command
/// @param x0 move/line destination x, quadratic control x, or first cubic control x
/// @param y0 move/line destination y, quadratic control y, or first cubic control y
/// @param x1 quadratic destination x, second cubic control x, or `0` when unused
/// @param y1 quadratic destination y, second cubic control y, or `0` when unused
/// @param x2 cubic destination x, or `0` when unused
/// @param y2 cubic destination y, or `0` when unused
@NotNullByDefault
public record OutlineOp(OutlineVerb verb, float x0, float y0, float x1, float y1, float x2, float y2) {
    /// Validates the command.
    public OutlineOp {
        Objects.requireNonNull(verb, "verb");
    }

    /// Creates a move command.
    ///
    /// @param x the destination x
    /// @param y the destination y
    /// @return the command
    public static OutlineOp move(float x, float y) {
        return new OutlineOp(OutlineVerb.MOVE, x, y, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /// Creates a line command.
    ///
    /// @param x the destination x
    /// @param y the destination y
    /// @return the command
    public static OutlineOp line(float x, float y) {
        return new OutlineOp(OutlineVerb.LINE, x, y, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    /// Creates a quadratic command.
    ///
    /// @param cx the control x
    /// @param cy the control y
    /// @param x the destination x
    /// @param y the destination y
    /// @return the command
    public static OutlineOp quad(float cx, float cy, float x, float y) {
        return new OutlineOp(OutlineVerb.QUAD, cx, cy, x, y, 0.0f, 0.0f);
    }

    /// Creates a cubic command.
    ///
    /// @param c1x the first control x
    /// @param c1y the first control y
    /// @param c2x the second control x
    /// @param c2y the second control y
    /// @param x the destination x
    /// @param y the destination y
    /// @return the command
    public static OutlineOp cubic(float c1x, float c1y, float c2x, float c2y, float x, float y) {
        return new OutlineOp(OutlineVerb.CUBIC, c1x, c1y, c2x, c2y, x, y);
    }

    /// Creates a close command.
    ///
    /// @return the command
    public static OutlineOp close() {
        return new OutlineOp(OutlineVerb.CLOSE, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }
}
