package org.glavo.himari.desktop;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies host detection and the Windows desktop smoke launch.
@NotNullByDefault
final class DesktopLaunchTest {
    /// Detects Windows on this development host.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void detectsWindowsHost() {
        assertEquals(DesktopHost.WINDOWS, DesktopHost.detect());
    }

    /// Opens two HWND windows, increments the CounterApp tree, and closes in smoke mode.
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void smokeLaunchCreatesWindowsAndIncrements() throws Exception {
        DesktopLaunchResult result = DesktopLaunch.run(true, null);
        assertEquals(DesktopHost.WINDOWS, result.host());
        assertTrue(result.windowCreated());
        assertEquals(2, result.windowCount());
        assertEquals(2, result.activations());
        assertEquals("Count: 2", result.label());
        assertTrue(result.inspectorNodes() >= 2);
        assertTrue(result.pngBytes() > 8);
        assertTrue(result.extendedLinearBytes() > 0);
        assertTrue(result.presentedScanlines() >= 120);
        assertTrue(result.d3d12Presented());
        assertTrue(result.popupHosted());
        assertFalse(result.messageLoopRan());
        assertEquals(0, result.deviceRemovedReason());
        assertEquals(2, result.sleepEvents());
        assertEquals(2, result.wakeEvents());
        assertTrue(result.presentedAfterWake() >= 120);
        assertEquals("environment-blocked", result.waylandStatus());
        assertTrue(result.macosStatus().equals("environment-blocked") || result.macosStatus().equals("resolved"));
        assertEquals("environment-blocked", result.metalStatus());
        assertTrue(result.objcStatus().equals("environment-blocked") || result.objcStatus().equals("layout-verified"));
    }
}
