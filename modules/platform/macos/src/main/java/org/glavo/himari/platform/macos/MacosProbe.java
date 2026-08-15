package org.glavo.himari.platform.macos;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one Objective-C runtime probe.
///
/// @param status `resolved` or `environment-blocked`
/// @param detail a pointer-free diagnostic
/// @param hdrAssumed always `false`
@NotNullByDefault
public record MacosProbe(String status, String detail, boolean hdrAssumed) {
    /// Validates the observation.
    public MacosProbe {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detail, "detail");
        if (hdrAssumed) {
            throw new IllegalArgumentException("macOS first-stable presentation must not assume HDR");
        }
    }

    /// Resolves `NSObject` on macOS and records a block on every other host.
    ///
    /// @return the observation
    public static MacosProbe run() {
        if (!MacosLibraries.supportedHost()) {
            return new MacosProbe(
                    "environment-blocked",
                    "NSWindow requires macOS; host is " + System.getProperty("os.name", ""),
                    false
            );
        }
        try (MacosRuntime runtime = MacosRuntime.open()) {
            if (runtime.nsObjectClass().address() == 0L || runtime.allocSelector().address() == 0L) {
                throw new IllegalStateException("Objective-C runtime returned a NULL class or selector");
            }
            return new MacosProbe("resolved", "objc_getClass(NSObject) and sel_registerName(alloc) succeeded", false);
        } catch (RuntimeException failure) {
            return new MacosProbe(
                    "environment-blocked",
                    "Objective-C runtime failed: " + failure.getMessage(),
                    false
            );
        }
    }
}
