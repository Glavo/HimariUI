package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the host-level role of a window presentation surface.
@NotNullByDefault
public enum SurfaceRole {
    /// An independently positioned top-level application window.
    TOPLEVEL,

    /// An owner-relative transient surface with host dismissal semantics where available.
    POPUP
}
