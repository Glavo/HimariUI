package org.glavo.himari.desktop;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the host family used to choose a desktop backend.
@NotNullByDefault
public enum DesktopHost {
    /// Windows Win32 session.
    WINDOWS,

    /// macOS AppKit session.
    MACOS,

    /// Linux Wayland session.
    LINUX,

    /// No recognized desktop operating system.
    UNKNOWN;

    /// Detects the current host from `os.name`.
    ///
    /// @return the host family
    public static DesktopHost detect() {
        String name = System.getProperty("os.name", "");
        if (name.startsWith("Windows")) {
            return WINDOWS;
        }
        if (name.startsWith("Mac")) {
            return MACOS;
        }
        if (name.startsWith("Linux")) {
            return LINUX;
        }
        return UNKNOWN;
    }
}
