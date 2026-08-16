package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one first-stable reloadable resource family.
@NotNullByDefault
public enum ResourceKind {
    /// Theme token documents.
    THEME,

    /// Style sheets or style tables.
    STYLE,

    /// Image payloads.
    IMAGE,

    /// Font file payloads.
    FONT
}
