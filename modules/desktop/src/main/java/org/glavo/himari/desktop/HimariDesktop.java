package org.glavo.himari.desktop;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/// Production `himari-desktop` entry that launches the first-stable CounterApp.
@NotNullByDefault
public final class HimariDesktop {
    /// Prevents instantiation.
    private HimariDesktop() {
    }

    /// Launches the desktop CounterApp.
    ///
    /// Recognized arguments are `--smoke` and an optional output directory. Smoke mode creates the
    /// host window, exercises the CounterApp tree, writes artifacts, and closes the session.
    ///
    /// @param arguments the command-line arguments
    /// @throws Exception if the launch fails
    public static void main(String[] arguments) throws Exception {
        boolean smoke = false;
        @Nullable Path output = null;
        for (String argument : arguments) {
            if ("--smoke".equals(argument)) {
                smoke = true;
            } else if (!argument.startsWith("-")) {
                output = Path.of(argument);
            }
        }
        if (output == null) {
            output = Path.of("build/conformance/m11-desktop");
        }
        DesktopLaunchResult result = DesktopLaunch.run(smoke, output);
        System.out.println(
                "HimariDesktop host=" + result.host()
                        + " windowCreated=" + result.windowCreated()
                        + " activations=" + result.activations()
                        + " presented=" + result.presentedScanlines()
                        + " d3d12=" + result.d3d12Presented()
                        + " png=" + result.pngBytes()
        );
    }
}
