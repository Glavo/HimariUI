package org.glavo.himari.platform.wayland;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one Wayland connect attempt.
///
/// @param status `connected` or `environment-blocked`
/// @param detail a pointer-free diagnostic
/// @param fileDescriptor the display fd, or `-1` when blocked
/// @param hdrAssumed always `false`
@NotNullByDefault
public record WaylandProbe(String status, String detail, int fileDescriptor, boolean hdrAssumed) {
    /// Validates the observation.
    public WaylandProbe {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detail, "detail");
        if (hdrAssumed) {
            throw new IllegalArgumentException("Wayland first-stable presentation must not assume HDR");
        }
    }

    /// Attempts a real connect on Linux and records a block on every other host.
    ///
    /// @return the observation
    public static WaylandProbe run() {
        if (!WaylandLibraries.supportedHost()) {
            return new WaylandProbe(
                    "environment-blocked",
                    "Wayland requires a Linux compositor; host is " + System.getProperty("os.name", ""),
                    -1,
                    false
            );
        }
        try (WaylandDisplay display = WaylandDisplay.connect()) {
            return new WaylandProbe(
                    "connected",
                    "wl_display_connect succeeded",
                    display.fileDescriptor(),
                    false
            );
        } catch (RuntimeException failure) {
            return new WaylandProbe(
                    "environment-blocked",
                    "Wayland connect failed: " + failure.getMessage(),
                    -1,
                    false
            );
        }
    }
}
