package org.glavo.himari.layout;

import org.jetbrains.annotations.NotNullByDefault;

/// Places a child inside leftover space on one axis.
@NotNullByDefault
public enum Alignment {
    /// Align to the start of the axis.
    START,

    /// Center on the axis.
    CENTER,

    /// Align to the end of the axis.
    END;

    /// Returns the origin that places `child` inside `parent`.
    ///
    /// @param parent the parent extent
    /// @param child the child extent
    /// @return the origin
    public float place(float parent, float child) {
        return switch (this) {
            case START -> 0.0f;
            case CENTER -> (parent - child) * 0.5f;
            case END -> parent - child;
        };
    }
}
