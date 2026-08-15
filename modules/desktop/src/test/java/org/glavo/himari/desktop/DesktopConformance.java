package org.glavo.himari.desktop;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;

/// Writes M11 desktop-entry evidence.
@NotNullByDefault
public final class DesktopConformance {
    /// Prevents instantiation.
    private DesktopConformance() {
    }

    /// Launches the shipped desktop entry in smoke mode.
    ///
    /// @param arguments one output directory
    /// @throws Exception if the profile fails
    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one output directory");
        }
        Path output = Path.of(arguments[0]);
        DesktopLaunchResult result = DesktopLaunch.run(true, output);
        if (result.host() == DesktopHost.WINDOWS && !result.windowCreated()) {
            throw new IllegalStateException("Windows desktop launch did not create a host window");
        }
        if (result.host() == DesktopHost.WINDOWS && result.presentedScanlines() <= 0) {
            throw new IllegalStateException("Windows desktop launch did not present software frames");
        }
        if (result.host() == DesktopHost.WINDOWS && !result.d3d12Presented()) {
            throw new IllegalStateException("Windows desktop launch did not present through D3D12");
        }
        if (result.host() == DesktopHost.WINDOWS && !result.popupHosted()) {
            throw new IllegalStateException("Windows desktop launch did not host a popup HWND");
        }
        if (result.activations() != 2 || !"Count: 2".equals(result.label())) {
            throw new IllegalStateException("Desktop CounterApp did not reach count 2");
        }
        if (result.pngBytes() <= 8 || result.extendedLinearBytes() <= 0 || result.inspectorNodes() <= 0) {
            throw new IllegalStateException("Desktop launch omitted PNG, extended-linear, or inspector artifacts");
        }
        System.out.println("DESKTOP-001 passed: host=" + result.host() + " windows=" + result.windowCount());
    }
}
