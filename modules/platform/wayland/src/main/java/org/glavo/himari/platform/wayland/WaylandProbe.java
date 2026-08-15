package org.glavo.himari.platform.wayland;

import org.glavo.himari.platform.wayland.linux.WaylandLinuxHost;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one Wayland connect and xdg-shell bind attempt.
///
/// @param status `connected` or `environment-blocked`
/// @param detail a pointer-free diagnostic
/// @param fileDescriptor the display fd, or `-1` when blocked
/// @param xdgWmBaseAdvertised whether `xdg_wm_base` was advertised
/// @param toplevelCreated whether an `xdg_toplevel` was created
/// @param shmAdvertised whether `wl_shm` was advertised
/// @param seatAdvertised whether `wl_seat` was advertised
/// @param decorationManagerAdvertised whether `zxdg_decoration_manager_v1` was advertised
/// @param hdrAssumed always `false`
@NotNullByDefault
public record WaylandProbe(
        String status,
        String detail,
        int fileDescriptor,
        boolean xdgWmBaseAdvertised,
        boolean toplevelCreated,
        boolean shmAdvertised,
        boolean seatAdvertised,
        boolean decorationManagerAdvertised,
        boolean hdrAssumed
) {
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
        if (!WaylandLinuxHost.supported()) {
            return new WaylandProbe(
                    "environment-blocked",
                    "Wayland requires a Linux compositor; host is " + System.getProperty("os.name", ""),
                    -1,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }
        try (WaylandDisplay display = WaylandDisplay.connect();
             WaylandXdgSession xdg = WaylandXdgSession.bind(display)) {
            return new WaylandProbe(
                    "connected",
                    xdg.toplevelCreated()
                            ? "xdg_toplevel created"
                            : xdg.xdgWmBaseAdvertised()
                            ? "xdg_wm_base advertised"
                            : "wl_display_connect succeeded; xdg_wm_base not advertised",
                    display.fileDescriptor(),
                    xdg.xdgWmBaseAdvertised(),
                    xdg.toplevelCreated(),
                    xdg.shmAdvertised(),
                    xdg.seatAdvertised(),
                    xdg.decorationManagerAdvertised(),
                    false
            );
        } catch (RuntimeException failure) {
            return new WaylandProbe(
                    "environment-blocked",
                    "Wayland connect or xdg-shell bind failed: " + failure.getMessage(),
                    -1,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }
    }
}
