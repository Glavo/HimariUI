package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one outline command recorded from an [`OutlinePen`].
///
/// @param verb the command
/// @param x0 move/line destination x, or quadratic control x
/// @param y0 move/line destination y, or quadratic control y
/// @param x1 quadratic destination x, or `0` when unused
/// @param y1 quadratic destination y, or `0` when unused
@NotNullByDefault
public record OutlineOp(OutlineVerb verb, float x0, float y0, float x1, float y1) {
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
        return new OutlineOp(OutlineVerb.MOVE, x, y, 0.0f, 0.0f);
    }

    /// Creates a line command.
    ///
    /// @param x the destination x
    /// @param y the destination y
    /// @return the command
    public static OutlineOp line(float x, float y) {
        return new OutlineOp(OutlineVerb.LINE, x, y, 0.0f, 0.0f);
    }

    /// Creates a quadratic command.
    ///
    /// @param cx the control x
    /// @param cy the control y
    /// @param x the destination x
    /// @param y the destination y
    /// @return the command
    public static OutlineOp quad(float cx, float cy, float x, float y) {
        return new OutlineOp(OutlineVerb.QUAD, cx, cy, x, y);
    }

    /// Creates a close command.
    ///
    /// @return the command
    public static OutlineOp close() {
        return new OutlineOp(OutlineVerb.CLOSE, 0.0f, 0.0f, 0.0f, 0.0f);
    }
}
