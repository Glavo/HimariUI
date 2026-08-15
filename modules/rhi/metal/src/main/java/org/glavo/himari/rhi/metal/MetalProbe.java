package org.glavo.himari.rhi.metal;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one Metal device-create attempt.
///
/// @param status `created` or `environment-blocked`
/// @param detail a pointer-free diagnostic
/// @param hdrAssumed always `false`
@NotNullByDefault
public record MetalProbe(String status, String detail, boolean hdrAssumed) {
    /// Validates the observation.
    public MetalProbe {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(detail, "detail");
        if (hdrAssumed) {
            throw new IllegalArgumentException("Metal first-stable presentation must not assume HDR");
        }
    }

    /// Creates a device on macOS and records a block on every other host.
    ///
    /// @return the observation
    public static MetalProbe run() {
        if (!MetalLibraries.supportedHost()) {
            return new MetalProbe(
                    "environment-blocked",
                    "Metal requires macOS; host is " + System.getProperty("os.name", ""),
                    false
            );
        }
        try (MetalDevice device = MetalDevice.open()) {
            if (device.nativeHandle().address() == 0L) {
                throw new IllegalStateException("Metal device handle was NULL");
            }
            return new MetalProbe("created", "MTLCreateSystemDefaultDevice succeeded", false);
        } catch (RuntimeException failure) {
            return new MetalProbe(
                    "environment-blocked",
                    "Metal device creation failed: " + failure.getMessage(),
                    false
            );
        }
    }
}
