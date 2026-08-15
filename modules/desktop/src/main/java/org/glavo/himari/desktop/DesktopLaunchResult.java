package org.glavo.himari.desktop;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one `himari-desktop` launch.
///
/// @param host the detected host family
/// @param windowCreated whether a host window was created
/// @param windowCount the number of windows opened before smoke-close
/// @param smoke whether the launch closed itself
/// @param activations the CounterApp increment count
/// @param label the mounted counter label
/// @param inspectorNodes the inspector node count
/// @param pngBytes the SDR PNG size
/// @param extendedLinearBytes the extended-linear capture size
/// @param waylandStatus the Wayland probe status
/// @param macosStatus the macOS probe status
/// @param metalStatus the Metal probe status
/// @param objcStatus the Objective-C block probe status
@NotNullByDefault
public record DesktopLaunchResult(
        DesktopHost host,
        boolean windowCreated,
        int windowCount,
        boolean smoke,
        int activations,
        String label,
        int inspectorNodes,
        int pngBytes,
        int extendedLinearBytes,
        String waylandStatus,
        String macosStatus,
        String metalStatus,
        String objcStatus
) {
    /// Validates the result.
    public DesktopLaunchResult {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(waylandStatus, "waylandStatus");
        Objects.requireNonNull(macosStatus, "macosStatus");
        Objects.requireNonNull(metalStatus, "metalStatus");
        Objects.requireNonNull(objcStatus, "objcStatus");
        if (windowCount < 0 || activations < 0 || inspectorNodes < 0 || pngBytes < 0 || extendedLinearBytes < 0) {
            throw new IllegalArgumentException("Desktop launch counters must be nonnegative");
        }
    }

    /// Encodes the launch as JSON.
    ///
    /// @return the document
    public String toJson() {
        return """
                {
                  "profile": "m11-desktop",
                  "workPackage": "DESKTOP-001",
                  "status": "passed",
                  "host": "%s",
                  "windowCreated": %s,
                  "windowCount": %d,
                  "smoke": %s,
                  "activations": %d,
                  "label": "%s",
                  "inspectorNodes": %d,
                  "pngBytes": %d,
                  "extendedLinearBytes": %d,
                  "waylandStatus": "%s",
                  "macosStatus": "%s",
                  "metalStatus": "%s",
                  "objcStatus": "%s"
                }
                """.formatted(
                host.name(),
                windowCreated,
                windowCount,
                smoke,
                activations,
                label,
                inspectorNodes,
                pngBytes,
                extendedLinearBytes,
                waylandStatus,
                macosStatus,
                metalStatus,
                objcStatus
        );
    }
}
