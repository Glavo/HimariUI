package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.ArrayList;

/// Builds an immutable [Path].
@NotNullByDefault
public final class PathBuilder {
    /// Recorded verbs.
    private final ArrayList<PathVerb> verbs = new ArrayList<>();

    /// Packed coordinates.
    private final ArrayList<Float> points = new ArrayList<>();

    /// Creates an empty builder.
    public PathBuilder() {
    }

    /// Moves the current point.
    ///
    /// @param x the x coordinate
    /// @param y the y coordinate
    /// @return this builder
    public PathBuilder moveTo(float x, float y) {
        verbs.add(PathVerb.MOVE);
        points.add(x);
        points.add(y);
        return this;
    }

    /// Appends a line.
    ///
    /// @param x the x coordinate
    /// @param y the y coordinate
    /// @return this builder
    public PathBuilder lineTo(float x, float y) {
        verbs.add(PathVerb.LINE);
        points.add(x);
        points.add(y);
        return this;
    }

    /// Closes the current contour.
    ///
    /// @return this builder
    public PathBuilder close() {
        verbs.add(PathVerb.CLOSE);
        return this;
    }

    /// Builds the immutable path.
    ///
    /// @return the path
    public Path build() {
        float[] packed = new float[points.size()];
        for (int index = 0; index < points.size(); index++) {
            packed[index] = points.get(index);
        }
        return new Path(verbs, packed);
    }
}
