package org.glavo.himari.layout.input;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one normalized logical key.
@NotNullByDefault
public enum LogicalKey {
    /// Tab traversal.
    TAB,

    /// Enter activation.
    ENTER,

    /// Dismisses a modal popup, menu, dialog, or tooltip.
    ESCAPE,

    /// Space activation.
    SPACE,

    /// Decrease a focused range or rewind a collection.
    ARROW_LEFT,

    /// Increase a focused range or advance a collection.
    ARROW_RIGHT,

    /// Decrease a focused vertical range.
    ARROW_UP,

    /// Increase a focused vertical range.
    ARROW_DOWN,

    /// Move the caret to the start of the current line.
    HOME,

    /// Move the caret to the end of the current line.
    END,

    /// Delete the cluster before the caret.
    BACKSPACE,

    /// Delete the cluster after the caret.
    DELETE,

    /// Page the focused collection backward.
    PAGE_UP,

    /// Page the focused collection forward.
    PAGE_DOWN,

    /// Windows / Super modifier.
    META
}
