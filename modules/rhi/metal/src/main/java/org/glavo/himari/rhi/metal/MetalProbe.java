package org.glavo.himari.rhi.metal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Records one Metal device and command-queue create attempt.
///
/// @param status `created` or `environment-blocked`
/// @param detail a pointer-free diagnostic
/// @param capabilities the snapshot, or `null` when blocked
/// @param hdrAssumed always `false`
@NotNullByDefault
public record MetalProbe(
        String status,
        String detail,
        @Nullable MetalCapabilities capabilities,
        boolean hdrAssumed
) {
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
                    null,
                    false
            );
        }
        try (MetalDevice device = MetalDevice.open()) {
            if (device.nativeHandle().address() == 0L || device.commandQueue().address() == 0L) {
                throw new IllegalStateException("Metal device or command queue handle was NULL");
            }
            return new MetalProbe(
                    "created",
                    "MTLCreateSystemDefaultDevice created a command queue and committed a buffer",
                    device.capabilities(),
                    false
            );
        } catch (RuntimeException failure) {
            return new MetalProbe(
                    "environment-blocked",
                    "Metal device creation failed: " + failure.getMessage(),
                    null,
                    false
            );
        }
    }
}
