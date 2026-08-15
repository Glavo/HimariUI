package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one path command.
@NotNullByDefault
public enum PathVerb {
    /// Move the current point.
    MOVE,

    /// Draw a line to the next point.
    LINE,

    /// Close the current contour.
    CLOSE
}
