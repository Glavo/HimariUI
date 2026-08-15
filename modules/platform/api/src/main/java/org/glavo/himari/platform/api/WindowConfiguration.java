package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Describes application-controlled window properties.
///
/// For a top-level window, `frame` is its normal/restored frame in global logical desktop
/// coordinates; a host may publish a different effective frame while maximized or full-screen. For
/// a popup, the origin is relative to the effective origin of its owner and extents remain logical
/// pixels.
///
/// @param title the target-neutral title text
/// @param frame the requested logical frame
/// @param visible whether the window requests host presentation
/// @param state the requested top-level state; popups must use [WindowState#NORMAL]
@NotNullByDefault
public record WindowConfiguration(
        String title,
        LogicalRect frame,
        boolean visible,
        WindowState state
) {
    /// Creates a window configuration.
    public WindowConfiguration {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(frame, "frame");
        Objects.requireNonNull(state, "state");
    }
}
