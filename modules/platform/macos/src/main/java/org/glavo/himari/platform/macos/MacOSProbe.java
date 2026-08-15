package org.glavo.himari.platform.macos;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one Objective-C runtime and `NSWindow` probe.
///
/// @param status `resolved` or `environment-blocked`
/// @param detail a pointer-free diagnostic
/// @param nsWindowCreated whether an `NSWindow` was created
/// @param metalLayerAttached whether a `CAMetalLayer` was attached
/// @param hdrAssumed always `false`
@NotNullByDefault
public record MacOSProbe(
        String status,
        String detail,
        boolean nsWindowCreated,
        boolean metalLayerAttached,
        boolean hdrAssumed
) {
    /// Validates the observation.
    public MacOSProbe {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detail, "detail");
        if (hdrAssumed) {
            throw new IllegalArgumentException("macOS first-stable presentation must not assume HDR");
        }
    }

    /// Creates an `NSWindow` on macOS and records a block on every other host.
    ///
    /// @return the observation
    public static MacOSProbe run() {
        if (!MacOSLibraries.supportedHost()) {
            return new MacOSProbe(
                    "environment-blocked",
                    "NSWindow requires macOS; host is " + System.getProperty("os.name", ""),
                    false,
                    false,
                    false
            );
        }
        try (MacOSWindow window = MacOSWindow.open()) {
            if (window.nativeHandle().address() == 0L || window.metalLayer().address() == 0L) {
                throw new IllegalStateException("NSWindow or CAMetalLayer handle was NULL");
            }
            return new MacOSProbe(
                    "resolved",
                    "NSWindow created with an attached CAMetalLayer",
                    true,
                    true,
                    false
            );
        } catch (RuntimeException failure) {
            return new MacOSProbe(
                    "environment-blocked",
                    "NSWindow creation failed: " + failure.getMessage(),
                    false,
                    false,
                    false
            );
        }
    }
}
