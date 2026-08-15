package org.glavo.himari.layout.semantics;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the semantic role of one layout node.
@NotNullByDefault
public enum SemanticsRole {
    /// A generic container.
    NONE,

    /// Static text.
    TEXT,

    /// An activatable control.
    BUTTON,

    /// A two-state switch.
    TOGGLE,

    /// A bounded numeric value.
    SLIDER,

    /// A scrollable or lazy collection.
    LIST,

    /// An editable text field.
    TEXT_FIELD
}
