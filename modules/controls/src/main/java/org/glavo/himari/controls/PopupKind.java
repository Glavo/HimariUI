package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the first-stable popup surface kind.
@NotNullByDefault
public enum PopupKind {
    /// A generic in-window overlay.
    OVERLAY,

    /// A menu that hosts activatable items.
    MENU,

    /// A modal dialog.
    DIALOG,

    /// A non-activating tooltip.
    TOOLTIP,

    /// A context menu opened from a secondary pointer press.
    CONTEXT_MENU
}
