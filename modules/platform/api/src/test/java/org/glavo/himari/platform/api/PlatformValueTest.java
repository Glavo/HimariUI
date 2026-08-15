package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies validation and extensibility of target-neutral platform values.
@NotNullByDefault
final class PlatformValueTest {
    /// Verifies that color capability values preserve extensible modes and wide-gamut coordinates.
    @Test
    void preservesStructuredColorCapabilities() {
        PresentationMode futureMode = new PresentationMode("future-display-mode");
        ArrayList<PresentationMode> modes = new ArrayList<>(List.of(
                futureMode,
                PresentationMode.EXTENDED_LINEAR,
                PresentationMode.SDR
        ));
        DisplayColorDescription description = new DisplayColorDescription(
                new DisplayPrimaries(
                        new Chromaticity(0.7347, 0.2653),
                        new Chromaticity(0.0, 1.0),
                        new Chromaticity(0.0001, -0.0770),
                        new Chromaticity(0.32168, 0.33767)
                ),
                0.0001,
                2_000.0,
                203.0,
                8.0,
                modes
        );
        modes.clear();

        assertEquals(List.of(
                futureMode,
                PresentationMode.EXTENDED_LINEAR,
                PresentationMode.SDR
        ), description.presentationModes());
        assertThrows(
                UnsupportedOperationException.class,
                () -> description.presentationModes().add(PresentationMode.PQ)
        );
        assertThrows(IllegalArgumentException.class, () -> new DisplayColorDescription(
                DisplayPrimaries.SRGB,
                0.0,
                100.0,
                80.0,
                1.0,
                List.of(PresentationMode.PQ)
        ));
    }

    /// Verifies geometry, role, and event-shape invariants at construction boundaries.
    @Test
    void rejectsInconsistentPlatformValues() {
        WindowConfiguration normal = new WindowConfiguration(
                "Window",
                new LogicalRect(0.0, 0.0, 100.0, 50.0),
                true,
                WindowState.NORMAL
        );
        WindowConfiguration maximized = new WindowConfiguration(
                "Popup",
                new LogicalRect(0.0, 0.0, 100.0, 50.0),
                true,
                WindowState.MAXIMIZED
        );

        assertThrows(IllegalArgumentException.class, () -> new LogicalRect(0.0, 0.0, -1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new PhysicalSize(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new WindowRequest(
                SurfaceRole.TOPLEVEL,
                new WindowId(1L),
                normal
        ));
        assertThrows(IllegalArgumentException.class, () -> WindowRequest.popup(
                new WindowId(1L),
                maximized
        ));
        assertThrows(IllegalArgumentException.class, () -> new WindowEvent(
                1L,
                0L,
                WindowEventType.CREATED,
                snapshot(normal),
                snapshot(normal)
        ));
    }

    /// Creates one valid open top-level snapshot for event validation.
    ///
    /// @param configuration the application configuration
    /// @return the snapshot
    private static WindowSnapshot snapshot(WindowConfiguration configuration) {
        SurfaceDescriptor surface = new SurfaceDescriptor(
                new SurfaceId(1L),
                SurfaceRole.TOPLEVEL,
                SurfaceKind.SOFTWARE
        );
        return new WindowSnapshot(
                new WindowId(1L),
                SurfaceRole.TOPLEVEL,
                null,
                configuration,
                configuration.frame(),
                configuration.visible(),
                new PhysicalSize(100, 50),
                1.0,
                new DisplayId("display"),
                surface,
                0L,
                WindowLifecycle.OPEN
        );
    }
}
