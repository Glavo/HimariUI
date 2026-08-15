package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Stores an immutable path of move/line/close commands.
///
/// @param verbs the commands
/// @param points packed `x,y` coordinates referenced by move and line verbs
@NotNullByDefault
public record Path(@Unmodifiable List<PathVerb> verbs, float @Unmodifiable [] points) {
    /// Validates the path.
    public Path {
        verbs = List.copyOf(verbs);
        Objects.requireNonNull(points, "points");
        points = points.clone();
        int required = 0;
        for (PathVerb verb : verbs) {
            required += switch (verb) {
                case MOVE, LINE -> 2;
                case CLOSE -> 0;
            };
        }
        if (points.length != required) {
            throw new IllegalArgumentException("Path point count does not match verbs");
        }
        for (float point : points) {
            if (!Float.isFinite(point)) {
                throw new IllegalArgumentException("Path points must be finite");
            }
        }
    }

    /// Creates an axis-aligned rectangle path.
    ///
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the width
    /// @param height the height
    /// @return the closed rectangle
    public static Path rectangle(float x, float y, float width, float height) {
        if (!Float.isFinite(x) || !Float.isFinite(y)
                || !Float.isFinite(width) || !Float.isFinite(height)
                || width < 0.0f || height < 0.0f) {
            throw new IllegalArgumentException("Rectangle must be finite with nonnegative extents");
        }
        return new Path(
                List.of(PathVerb.MOVE, PathVerb.LINE, PathVerb.LINE, PathVerb.LINE, PathVerb.CLOSE),
                new float[] {x, y, x + width, y, x + width, y + height, x, y + height}
        );
    }
}
