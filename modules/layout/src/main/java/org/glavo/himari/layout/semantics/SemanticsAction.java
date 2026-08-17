package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one semantic action exposed without a public M9 control type.
@NotNullByDefault
public enum SemanticsAction {
    /// Activates the node.
    ACTIVATE,

    /// Increases a bounded value or advances a collection window.
    INCREMENT,

    /// Decreases a bounded value or rewinds a collection window.
    DECREMENT,

    /// Scrolls this item into the nearest scrollable ancestor viewport.
    SCROLL_INTO_VIEW,

    /// Realizes a virtualized item so it can participate in the accessibility tree.
    REALIZE
}
