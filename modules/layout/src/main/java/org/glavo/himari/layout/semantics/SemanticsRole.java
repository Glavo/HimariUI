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
    TEXT_FIELD,

    /// A multiline editable text area.
    TEXT_AREA,

    /// A status or live-region announcement.
    STATUS,

    /// A generic in-window overlay popup.
    POPUP,

    /// A menu surface that hosts activatable items.
    MENU,

    /// One activatable item inside a menu.
    MENU_ITEM,

    /// A modal dialog surface.
    DIALOG,

    /// A non-activating tooltip surface.
    TOOLTIP
}
