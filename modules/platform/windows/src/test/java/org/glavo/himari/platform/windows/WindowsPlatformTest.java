package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.api.CapabilityReport;
import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerDeviceKind;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsGrid;
import org.glavo.himari.layout.semantics.SemanticsGridItem;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.layout.semantics.SemanticsScroll;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.SurfaceRole;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowLifecycle;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.rhi.vulkan.VulkanDevice;
import org.glavo.himari.rhi.vulkan.VulkanPresentation;
import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the shipped Windows platform session, WndProc input, IME session, and UIA projection.
@NotNullByDefault
@EnabledOnOs(OS.WINDOWS)
final class WindowsPlatformTest {
    /// Opens a real HWND, reconfigures it, and closes it through the production backend.
    @Test
    void createsConfiguresAndClosesWindow() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            List<WindowEventType> types = new ArrayList<>();
            WindowConfiguration initial = new WindowConfiguration(
                    "HimariUI Windows",
                    new LogicalRect(32.0, 32.0, 320.0, 240.0),
                    false,
                    WindowState.NORMAL
            );
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(initial),
                    event -> types.add(event.type())
            ).toCompletableFuture().get();
            platform.pump();
            assertEquals(WindowLifecycle.OPEN, window.snapshot().lifecycle());
            assertTrue(window.nativeHandle().address() != 0L);
            assertTrue(window.dpi() >= 96);
            window.applyDpiChange(144, 32, 32, 200, 160);
            assertEquals(144, window.dpi());
            assertEquals(1.5, window.scaleFactor(), 0.001);
            assertTrue(window.snapshot().surfaceSize().width() > 0);
            WindowConfiguration next = new WindowConfiguration(
                    "HimariUI Windows",
                    new LogicalRect(40.0, 40.0, 400.0, 300.0),
                    true,
                    WindowState.NORMAL
            );
            WindowSnapshot configured = window.configure(next).toCompletableFuture().get();
            platform.pump();
            assertTrue(configured.effectivelyVisible());
            assertEquals(400.0, configured.effectiveFrame().width());
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertTrue(window.isClosed());
            assertTrue(types.contains(WindowEventType.CREATED));
            assertTrue(types.contains(WindowEventType.CONFIGURATION_CHANGED));
            assertTrue(types.contains(WindowEventType.CLOSED));
            assertFalse(platform.displayTopology().displays().isEmpty());
            assertTrue(platform.displayTopology().displays().getFirst()
                    .colorCapabilities().description().presentationModes()
                    .contains(org.glavo.himari.platform.api.PresentationMode.SDR));
            assertTrue(platform.displayTopology().displays().getFirst().physicalSize().width() > 0);
            CapabilityReport report = CapabilityReport.from(platform);
            assertEquals(org.glavo.himari.platform.api.PresentationMode.SDR, report.requested());
            assertEquals(org.glavo.himari.platform.api.PresentationMode.SDR, report.effective());
            assertEquals("application", report.mappingOwner());
            assertEquals("host advertised only SDR", report.disabledHdrReason());
        } finally {
            platform.close();
        }
    }

    /// Delivers sleep and wake power broadcasts through the production WndProc.
    @Test
    void deliversSleepAndWakeThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow first = openToplevel(platform, "Sleep", 16.0, 16.0);
            WindowsWindow second = openToplevel(platform, "Wake", 80.0, 80.0);
            platform.pump();
            first.sendMessage(WindowsWindow.WM_POWERBROADCAST, WindowsWindow.PBT_APMSUSPEND, 0L);
            second.sendMessage(WindowsWindow.WM_POWERBROADCAST, WindowsWindow.PBT_APMSUSPEND, 0L);
            first.sendMessage(WindowsWindow.WM_POWERBROADCAST, WindowsWindow.PBT_APMRESUMESUSPEND, 0L);
            second.sendMessage(WindowsWindow.WM_POWERBROADCAST, WindowsWindow.PBT_APMRESUMESUSPEND, 0L);
            assertEquals(1, first.sleepEvents());
            assertEquals(1, first.wakeEvents());
            assertEquals(1, second.sleepEvents());
            assertEquals(1, second.wakeEvents());
            first.closeAsync().toCompletableFuture().get();
            second.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
    }

    /// Keeps a second HWND alive after the first is destroyed.
    @Test
    void closesOneWindowWithoutQuittingTheSession() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow first = openToplevel(platform, "First", 16.0, 16.0);
            WindowsWindow second = openToplevel(platform, "Second", 80.0, 80.0);
            platform.pump();
            assertEquals(2, platform.openWindowCount());
            first.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertTrue(first.isClosed());
            assertFalse(second.isClosed());
            assertEquals(1, platform.openWindowCount());
            assertEquals(WindowLifecycle.OPEN, second.snapshot().lifecycle());
            second.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertEquals(0, platform.openWindowCount());
        } finally {
            platform.close();
        }
    }

    /// Creates an owner-relative popup and closes it when the owner closes.
    @Test
    void closesOwnedPopupWithParent() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow owner = openToplevel(platform, "Owner", 24.0, 24.0);
            WindowsWindow popup = platform.createWindow(
                    WindowRequest.popup(owner.id(), new WindowConfiguration(
                            "Popup",
                            new LogicalRect(40.0, 40.0, 120.0, 80.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> { }
            ).toCompletableFuture().get();
            platform.pump();
            assertEquals(SurfaceRole.POPUP, popup.snapshot().role());
            assertEquals(owner.id(), popup.snapshot().ownerId());
            owner.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertTrue(owner.isClosed());
            assertTrue(popup.isClosed());
        } finally {
            platform.close();
        }
    }

    /// Blits a software RGBA frame into a live HWND through generated GDI bindings.
    @Test
    void presentsSoftwareRgbaThroughGdi() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Present", 40.0, 40.0);
            platform.pump();
            int width = 16;
            int height = 8;
            byte[] rgba = new byte[width * height * 4];
            for (int pixel = 0; pixel < rgba.length; pixel += 4) {
                rgba[pixel] = (byte) 0x20;
                rgba[pixel + 1] = (byte) 0x40;
                rgba[pixel + 2] = (byte) 0x80;
                rgba[pixel + 3] = (byte) 0xFF;
            }
            int scanlines = window.presentSdrRgba(java.lang.foreign.MemorySegment.ofArray(rgba), width, height);
            platform.pump();
            assertEquals(height, scanlines);
        } finally {
            platform.close();
        }
    }

    /// Delivers pointer, key, and character input through the production WndProc.
    @Test
    void deliversPostedInputThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Input", 32.0, 32.0);
            platform.pump();
            window.postPointer(PointerEventType.DOWN, 12, 18);
            window.postPointer(PointerEventType.UP, 12, 18);
            window.postVirtualKey(true, 0x0D, 0x1C, false);
            window.postVirtualKey(true, 0x1B, 0x01, true, true);
            window.postXButton(true, 8, 9, 1);
            window.postChar('n');
            window.postChar('i');
            platform.pump();
            List<PointerEvent> pointers = window.takePointerEvents();
            assertEquals(3, pointers.size());
            assertEquals(PointerEventType.DOWN, pointers.getFirst().type());
            assertEquals(12.0f, pointers.getFirst().x());
            assertEquals(18.0f, pointers.getFirst().y());
            assertEquals(PointerDeviceKind.MOUSE, pointers.getFirst().device());
            assertEquals(PointerEvent.BUTTON_PRIMARY, pointers.getFirst().buttons());
            assertTrue(pointers.getFirst().timestampMillis() > 0L);
            assertEquals(1, pointers.getFirst().sequenceId());
            assertFalse(pointers.getFirst().synthetic());
            assertEquals(PointerEventType.UP, pointers.get(1).type());
            assertEquals(0, pointers.get(1).buttons());
            assertEquals(2, pointers.get(1).sequenceId());
            assertTrue(pointers.get(1).timestampMillis() >= pointers.getFirst().timestampMillis());
            List<org.glavo.himari.layout.input.KeyEvent> keys = window.takeKeyEvents();
            assertEquals(2, keys.size());
            assertEquals(LogicalKey.ENTER, keys.get(0).key());
            assertEquals(0x1C, keys.get(0).scanCode());
            assertFalse(keys.get(0).repeat());
            assertEquals(LogicalKey.ESCAPE, keys.get(1).key());
            assertEquals(0x01, keys.get(1).scanCode());
            assertTrue(keys.get(1).repeat());
            assertTrue(keys.get(1).extended());
            assertFalse(keys.get(0).extended());
            assertFalse(keys.get(0).meta());
            window.postVirtualKey(true, 0x5B);
            window.postVirtualKey(true, 0x09);
            platform.pump();
            List<org.glavo.himari.layout.input.KeyEvent> metaKeys = window.takeKeyEvents();
            assertEquals(2, metaKeys.size());
            assertEquals(LogicalKey.META, metaKeys.get(0).key());
            assertTrue(metaKeys.get(0).meta());
            assertEquals(LogicalKey.TAB, metaKeys.get(1).key());
            assertTrue(metaKeys.get(1).meta());
            assertEquals(PointerEventType.DOWN, pointers.get(2).type());
            assertEquals(PointerEvent.BUTTON_X1, pointers.get(2).buttons());
            assertEquals(8.0f, pointers.get(2).x());
            assertEquals(9.0f, pointers.get(2).y());
            assertEquals("ni", window.ime().surroundingText());
            assertTrue(window.ime().committed());
        } finally {
            platform.close();
        }
    }

    /// Delivers `WM_MOUSEWHEEL` through the production WndProc as one wheel notch.
    @Test
    void deliversPostedWheelThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Wheel", 32.0, 32.0);
            platform.pump();
            window.postWheel(10, 14, 1);
            window.postPointer(PointerEventType.WHEEL, 10, 14);
            platform.pump();
            List<PointerEvent> pointers = window.takePointerEvents();
            assertEquals(2, pointers.size());
            assertEquals(PointerEventType.WHEEL, pointers.getFirst().type());
            assertEquals(10.0f, pointers.getFirst().x());
            assertEquals(14.0f, pointers.getFirst().y());
            assertEquals(1.0f, pointers.getFirst().wheelDelta());
            assertEquals(PointerEventType.WHEEL, pointers.get(1).type());
            assertEquals(1.0f, pointers.get(1).wheelDelta());
            window.postPointer(PointerEventType.WHEEL_HORIZONTAL, 10, 14);
            platform.pump();
            List<PointerEvent> horizontal = window.takePointerEvents();
            assertEquals(1, horizontal.size());
            assertEquals(PointerEventType.WHEEL_HORIZONTAL, horizontal.getFirst().type());
            assertEquals(1.0f, horizontal.getFirst().wheelDelta());
        } finally {
            platform.close();
        }
    }

    /// Captures the HWND on `WM_LBUTTONDOWN` and releases it on `WM_LBUTTONUP`.
    @Test
    void capturesMouseOnLeftButtonDown() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Capture", 32.0, 32.0);
            platform.pump();
            window.postPointer(PointerEventType.DOWN, 6, 6);
            platform.pump();
            assertTrue(window.captured());
            window.postPointer(PointerEventType.UP, 6, 6);
            platform.pump();
            assertFalse(window.captured());
        } finally {
            platform.close();
        }
    }

    /// Loads `IDC_ARROW` through generated `LoadCursorW` and installs it with `SetCursor`.
    @Test
    void loadsArrowCursorThroughUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Cursor", 32.0, 32.0);
            platform.pump();
            assertTrue(window.setSystemCursor(WindowsNativeWindow.IDC_ARROW));
        } finally {
            platform.close();
        }
    }

    /// Delivers `WM_RBUTTON*` as secondary pointer events.
    @Test
    void deliversPostedSecondaryButtonThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "RightButton", 32.0, 32.0);
            platform.pump();
            window.postPointer(PointerEventType.SECONDARY_DOWN, 4, 5);
            window.postPointer(PointerEventType.SECONDARY_UP, 4, 5);
            platform.pump();
            List<PointerEvent> pointers = window.takePointerEvents();
            assertEquals(2, pointers.size());
            assertEquals(PointerEventType.SECONDARY_DOWN, pointers.getFirst().type());
            assertEquals(PointerEventType.SECONDARY_UP, pointers.get(1).type());
            assertEquals(4.0f, pointers.getFirst().x());
            assertEquals(5.0f, pointers.getFirst().y());
        } finally {
            platform.close();
        }
    }

    /// Delivers `WM_MBUTTON*` as middle pointer events.
    @Test
    void deliversPostedMiddleButtonThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MiddleButton", 32.0, 32.0);
            platform.pump();
            window.postPointer(PointerEventType.MIDDLE_DOWN, 7, 8);
            window.postPointer(PointerEventType.MIDDLE_UP, 7, 8);
            platform.pump();
            List<PointerEvent> pointers = window.takePointerEvents();
            assertEquals(2, pointers.size());
            assertEquals(PointerEventType.MIDDLE_DOWN, pointers.getFirst().type());
            assertEquals(PointerEventType.MIDDLE_UP, pointers.get(1).type());
            assertEquals(7.0f, pointers.getFirst().x());
            assertEquals(8.0f, pointers.getFirst().y());
        } finally {
            platform.close();
        }
    }

    /// Delivers Home/End/Backspace/Delete and latches Control so a following Home has `ctrl`.
    @Test
    void deliversHomeEndAndCtrlThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "EditorKeys", 32.0, 32.0);
            platform.pump();
            window.postVirtualKey(true, 0x24);
            window.postVirtualKey(false, 0x24);
            window.postVirtualKey(true, 0x23);
            window.postVirtualKey(false, 0x23);
            window.postVirtualKey(true, 0x08);
            window.postVirtualKey(false, 0x08);
            window.postVirtualKey(true, 0x2E);
            window.postVirtualKey(false, 0x2E);
            window.postVirtualKey(true, 0x11);
            window.postVirtualKey(true, 0x24);
            window.postVirtualKey(false, 0x24);
            window.postVirtualKey(false, 0x11);
            window.postVirtualKey(true, 0x12);
            window.postVirtualKey(true, 0x21);
            window.postVirtualKey(false, 0x21);
            window.postVirtualKey(false, 0x12);
            window.postVirtualKey(true, 0x22);
            window.postVirtualKey(false, 0x22);
            platform.pump();
            List<org.glavo.himari.layout.input.KeyEvent> keys = window.takeKeyEvents();
            assertEquals(14, keys.size());
            assertEquals(LogicalKey.HOME, keys.get(0).key());
            assertEquals(LogicalKey.END, keys.get(2).key());
            assertEquals(LogicalKey.BACKSPACE, keys.get(4).key());
            assertEquals(LogicalKey.DELETE, keys.get(6).key());
            assertEquals(LogicalKey.HOME, keys.get(8).key());
            assertTrue(keys.get(8).ctrl());
            assertFalse(keys.get(0).ctrl());
            assertEquals(LogicalKey.PAGE_UP, keys.get(10).key());
            assertTrue(keys.get(10).alt());
            assertEquals(LogicalKey.PAGE_DOWN, keys.get(12).key());
            assertFalse(keys.get(12).alt());
        } finally {
            platform.close();
        }
    }

    /// Latches `VK_SHIFT` so a following Tab is delivered with `shift`.
    @Test
    void deliversShiftTabThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ShiftTab", 32.0, 32.0);
            platform.pump();
            window.postVirtualKey(true, 0x10);
            window.postVirtualKey(true, 0x09);
            window.postVirtualKey(false, 0x09);
            window.postVirtualKey(false, 0x10);
            platform.pump();
            List<org.glavo.himari.layout.input.KeyEvent> keys = window.takeKeyEvents();
            assertEquals(2, keys.size());
            assertEquals(LogicalKey.TAB, keys.get(0).key());
            assertTrue(keys.get(0).shift());
            assertEquals(KeyEventType.UP, keys.get(1).type());
            assertEquals(LogicalKey.TAB, keys.get(1).key());
        } finally {
            platform.close();
        }
    }

    /// Queries generated `GetPointerPenInfo` and decodes a packed `POINTER_PEN_INFO`.
    @Test
    void queriesPenAxesThroughGetPointerPenInfo() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "PenInfo", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.PenAxes missing = window.queryPenInfo(0x00FFFFFF);
            assertEquals(0.0f, missing.pressure());
            assertEquals(0.0f, missing.tiltX());
            assertEquals(0.0f, missing.tiltY());
            assertEquals(0.0f, missing.rotation());
            java.lang.foreign.MemorySegment packed = java.lang.foreign.Arena.ofAuto().allocate(
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_PEN_INFO
            );
            packed.fill((byte) 0);
            packed.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_PEN_INFO_PENMASK_OFFSET,
                    WindowsNativeWindow.PEN_MASK_PRESSURE
                            | WindowsNativeWindow.PEN_MASK_ROTATION
                            | WindowsNativeWindow.PEN_MASK_TILT_X
                            | WindowsNativeWindow.PEN_MASK_TILT_Y
            );
            packed.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_PEN_INFO_PRESSURE_OFFSET,
                    512
            );
            packed.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_PEN_INFO_TILTX_OFFSET,
                    30
            );
            packed.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_PEN_INFO_TILTY_OFFSET,
                    -15
            );
            packed.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_PEN_INFO_ROTATION_OFFSET,
                    90
            );
            packed.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_PEN_INFO_PENFLAGS_OFFSET,
                    WindowsNativeWindow.PEN_FLAG_INVERTED
                            | WindowsNativeWindow.PEN_FLAG_ERASER
            );
            WindowsNativeWindow.PenAxes axes = WindowsNativeWindow.decodePenInfo(packed);
            assertEquals(0.5f, axes.pressure(), 0.001f);
            assertEquals(30.0f, axes.tiltX(), 0.001f);
            assertEquals(-15.0f, axes.tiltY(), 0.001f);
            assertEquals(90.0f, axes.rotation(), 0.001f);
            assertTrue(axes.inverted());
            assertTrue(axes.eraser());
            PointerEvent event = new PointerEvent(
                    PointerEventType.DOWN,
                    4.0f,
                    5.0f,
                    PointerDeviceKind.PEN,
                    0.0f,
                    7,
                    0.5f,
                    30.0f,
                    -15.0f,
                    90.0f
            );
            assertEquals(0.5f, event.pressure());
            assertEquals(30.0f, event.tiltX());
            assertEquals(-15.0f, event.tiltY());
            assertEquals(90.0f, event.rotation());
            assertEquals(7, event.pointerId());
            window.postPen(
                    PointerEventType.DOWN,
                    4,
                    5,
                    7,
                    axes
            );
            platform.pump();
            List<PointerEvent> posted = window.takePointerEvents();
            assertFalse(posted.isEmpty());
            PointerEvent delivered = posted.getLast();
            assertEquals(PointerDeviceKind.PEN, delivered.device());
            assertEquals(7, delivered.pointerId());
            assertEquals(0.5f, delivered.pressure(), 0.001f);
            assertEquals(30.0f, delivered.tiltX(), 0.001f);
            assertEquals(-15.0f, delivered.tiltY(), 0.001f);
            assertEquals(90.0f, delivered.rotation(), 0.001f);
            assertTrue(delivered.inverted());
            assertTrue(delivered.eraser());
            assertEquals(PointerEvent.BUTTON_PRIMARY, delivered.buttons());
            assertTrue(delivered.sequenceId() > 0);
            assertTrue(delivered.timestampMillis() > 0L);
            assertFalse(delivered.synthetic());
        } finally {
            platform.close();
        }
    }

    /// Classifies `GetPointerType` results and queries the generated binding.
    @Test
    void classifiesPenAndQueriesGetPointerType() throws Exception {
        assertEquals(PointerDeviceKind.PEN, WindowsNativeWindow.deviceKindFromPointerType(WindowsNativeWindow.PT_PEN));
        assertEquals(PointerDeviceKind.MOUSE, WindowsNativeWindow.deviceKindFromPointerType(WindowsNativeWindow.PT_MOUSE));
        assertEquals(PointerDeviceKind.TOUCH, WindowsNativeWindow.deviceKindFromPointerType(WindowsNativeWindow.PT_TOUCH));
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "PointerType", 32.0, 32.0);
            platform.pump();
            assertEquals(0, window.queryPointerType(0x00FFFFFF));
        } finally {
            platform.close();
        }
    }

    /// Delivers `WM_POINTER*` through the production WndProc as touch events.
    @Test
    void deliversPostedTouchThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Touch", 32.0, 32.0);
            platform.pump();
            window.postPointer(PointerEventType.DOWN, 20, 24, PointerDeviceKind.TOUCH, 3);
            window.postPointer(PointerEventType.UP, 20, 24, PointerDeviceKind.TOUCH, 3);
            platform.pump();
            List<PointerEvent> pointers = window.takePointerEvents();
            assertEquals(2, pointers.size());
            assertEquals(PointerEventType.DOWN, pointers.getFirst().type());
            assertEquals(PointerDeviceKind.TOUCH, pointers.getFirst().device());
            assertEquals(20.0f, pointers.getFirst().x());
            assertEquals(24.0f, pointers.getFirst().y());
            assertEquals(3, pointers.getFirst().pointerId());
            assertEquals(PointerDeviceKind.TOUCH, pointers.get(1).device());
            assertEquals(3, pointers.get(1).pointerId());
        } finally {
            platform.close();
        }
    }

    /// Hosts an owner-relative popup HWND and treats `WM_CLOSE` as a dismiss.
    @Test
    void popupHostReportsCloseAsDismiss() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow owner = openToplevel(platform, "PopupOwner", 24.0, 24.0);
            platform.pump();
            AtomicBoolean dismissed = new AtomicBoolean();
            WindowsWindow popup = WindowsPopupHost.show(
                    platform,
                    owner,
                    "MenuHost",
                    new LogicalRect(40.0, 40.0, 120.0, 80.0),
                    () -> dismissed.set(true)
            );
            platform.pump();
            assertEquals(SurfaceRole.POPUP, popup.snapshot().role());
            popup.nativeWindow().postMessage(0x0010, 0L, 0L);
            platform.pump();
            assertTrue(dismissed.get());
            assertFalse(popup.isClosed());
            popup.closeAsync().toCompletableFuture().get();
            platform.pump();
        } finally {
            platform.close();
        }
    }

    /// Emits `CLOSE_REQUESTED` from `WM_CLOSE` without destroying the HWND.
    @Test
    void closeMessageRequestsWithoutDestroying() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            List<WindowEventType> types = new ArrayList<>();
            WindowsWindow window = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "Close",
                            new LogicalRect(16.0, 16.0, 200.0, 120.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> types.add(event.type())
            ).toCompletableFuture().get();
            platform.pump();
            window.nativeWindow().postMessage(0x0010, 0L, 0L);
            platform.pump();
            assertTrue(types.contains(WindowEventType.CLOSE_REQUESTED));
            assertFalse(window.isClosed());
            window.closeAsync().toCompletableFuture().get();
            platform.pump();
            assertTrue(window.isClosed());
        } finally {
            platform.close();
        }
    }

    /// Returns from the stay-open pump after queued close requests destroy the last HWND.
    @Test
    void pumpUntilClosedReturnsAfterLastCloseRequest() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow[] hosted = new WindowsWindow[2];
            hosted[0] = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "Loop-1",
                            new LogicalRect(16.0, 16.0, 200.0, 120.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> closeOnRequest(hosted, event)
            ).toCompletableFuture().get();
            hosted[1] = platform.createWindow(
                    WindowRequest.toplevel(new WindowConfiguration(
                            "Loop-2",
                            new LogicalRect(80.0, 80.0, 200.0, 120.0),
                            true,
                            WindowState.NORMAL
                    )),
                    event -> closeOnRequest(hosted, event)
            ).toCompletableFuture().get();
            platform.pump();
            assertEquals(2, platform.openWindowCount());
            hosted[0].nativeWindow().postMessage(0x0010, 0L, 0L);
            hosted[1].nativeWindow().postMessage(0x0010, 0L, 0L);
            platform.pumpUntilClosed();
            assertEquals(0, platform.openWindowCount());
            assertTrue(hosted[0].isClosed());
            assertTrue(hosted[1].isClosed());
        } finally {
            platform.close();
        }
    }

    /// Drives the shipped IME session and UIA projection.
    @Test
    void imeAndAutomationUseShippedContracts() {
        WindowsImeSession ime = new WindowsImeSession();
        ime.setSurroundingText("", 0);
        ime.setCandidateRectangle(8.0f, 16.0f, 24.0f, 12.0f);
        ime.updateComposition("ni");
        assertEquals("ni", ime.composition());
        assertEquals(0, ime.compositionStart());
        assertEquals(2, ime.compositionEnd());
        assertEquals(8.0f, ime.candidateX());
        assertEquals("ni", ime.commit());
        assertTrue(ime.committed());
        assertEquals("ni", ime.surroundingText());
        assertEquals("ni", ime.reconvert());
        ime.cancel();
        assertEquals(null, ime.composition());
        ime.updateComposition("hao");
        assertEquals("hao", ime.reject());
        assertEquals("hao", ime.lastRejected());
        assertEquals(null, ime.composition());
        assertEquals("ni", ime.surroundingText());
        LayoutTree tree = new LayoutTree();
        tree.setRoot(BootstrapCounterPane.create(tree, new AtomicInteger()));
        tree.measure(Constraints.loose(200.0f, 200.0f));
        tree.place();
        List<WindowsAutomationNode> nodes = WindowsAutomationBridge.inspect(tree.semantics());
        assertTrue(nodes.stream().anyMatch(WindowsAutomationNode::invokeSupported));
        assertTrue(nodes.stream().anyMatch(node -> node.name().equals("Increment")));
        WindowsAutomationNode increment = nodes.stream()
                .filter(WindowsAutomationNode::invokeSupported)
                .findFirst()
                .orElseThrow();
        assertEquals("Button", increment.controlType());
        assertTrue(increment.width() > 0.0f && increment.height() > 0.0f);
        assertEquals(null, increment.textRange());
        assertEquals(tree.semantics().nodeWith(SemanticsAction.ACTIVATE).id(), increment.id());
        LayoutTree editorTree = new LayoutTree();
        LayoutFactory editorFactory = new LayoutFactory(editorTree);
        LayoutNode field = editorFactory.leaf(
                "field",
                new Size(160.0f, 24.0f),
                List.of(),
                true,
                SemanticsRole.TEXT_FIELD,
                "hello",
                java.util.Set.of(SemanticsAction.ACTIVATE),
                () -> { }
        );
        field.setTextRange(new SemanticsTextRange(1, 4, 4));
        editorTree.setRoot(editorFactory.column("root", Alignment.START, List.of(), field));
        editorTree.measure(Constraints.loose(200.0f, 200.0f));
        editorTree.place();
        WindowsAutomationNode edit = WindowsAutomationBridge.inspect(editorTree.semantics()).stream()
                .filter(node -> node.controlType().equals("Edit"))
                .findFirst()
                .orElseThrow();
        assertEquals(new SemanticsTextRange(1, 4, 4), edit.textRange());
        LayoutTree barTree = new LayoutTree();
        LayoutFactory barFactory = new LayoutFactory(barTree);
        LayoutNode bar = barFactory.leaf(
                "bar",
                new Size(160.0f, 16.0f),
                List.of(),
                true,
                SemanticsRole.SCROLLBAR,
                "Thumb",
                java.util.Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                null
        );
        bar.setRangeValue(20.0);
        barTree.setRoot(barFactory.column("root", Alignment.START, List.of(), bar));
        barTree.measure(Constraints.loose(200.0f, 200.0f));
        barTree.place();
        WindowsAutomationNode scrollBar = WindowsAutomationBridge.inspect(barTree.semantics()).stream()
                .filter(node -> node.controlType().equals("ScrollBar"))
                .findFirst()
                .orElseThrow();
        assertEquals(20.0, scrollBar.rangeValue());
    }

    /// Writes and reads Unicode clipboard text through generated User32/Kernel32 bindings.
    @Test
    void clipboardRoundTripsUnicodeThroughWin32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Clipboard", 48.0, 48.0);
            platform.pump();
            String marker = "HimariUI-clipboard-" + Long.toUnsignedString(System.nanoTime());
            try {
                window.writeClipboard(marker);
            } catch (WindowsClipboard.ClipboardUnavailableException unavailable) {
                Assumptions.assumeFalse(
                        unavailable.accessDenied(),
                        "host denied OpenClipboard: " + unavailable.getMessage()
                );
                throw unavailable;
            }
            assertEquals(marker, window.readClipboard());
            WindowsWindow other = openToplevel(platform, "ClipboardPeer", 48.0, 48.0);
            platform.pump();
            assertEquals(marker, other.readClipboard());
            String ansi = "HimariUI-ansi-" + Long.toUnsignedString(System.nanoTime());
            window.writeAnsiClipboard(ansi);
            assertEquals(ansi, window.readAnsiClipboard());
            String html = "<div>HimariUI-html-" + Long.toUnsignedString(System.nanoTime()) + "</div>";
            window.writeHtmlClipboard(html);
            assertEquals(html, window.readHtmlClipboard());
            assertTrue(window.htmlClipboardFormat() > 0);
            String rtf = "{\\rtf1 HimariUI-rtf-" + Long.toUnsignedString(System.nanoTime()) + "}";
            window.writeRtfClipboard(rtf);
            assertEquals(rtf, window.readRtfClipboard());
            assertTrue(window.rtfClipboardFormat() > 0);
            byte[] dib = new byte[44];
            dib[0] = 40;
            dib[4] = 1;
            dib[8] = 1;
            dib[12] = 1;
            dib[14] = 32;
            dib[20] = 4;
            dib[40] = (byte) 30;
            dib[41] = (byte) 20;
            dib[42] = (byte) 10;
            dib[43] = (byte) 40;
            window.writeDibClipboard(dib);
            byte[] read = window.readDibClipboard();
            assertEquals(44, read.length);
            assertEquals(40, read[0]);
            assertEquals(30, read[40] & 0xFF);
            assertEquals(20, read[41] & 0xFF);
            assertEquals(10, read[42] & 0xFF);
            assertEquals(40, read[43] & 0xFF);
            String drop = "C:\\HimariUI\\clip-" + Long.toUnsignedString(System.nanoTime()) + ".txt";
            window.writeDropFilesClipboard(List.of(drop));
            List<String> dropped = window.readDropFilesClipboard();
            assertEquals(List.of(drop), dropped);
            window.clearClipboard();
            assertEquals(null, window.readClipboard());
            assertEquals(null, window.readAnsiClipboard());
            assertEquals(null, window.readHtmlClipboard());
            assertEquals(null, window.readRtfClipboard());
            assertEquals(null, window.readDibClipboard());
            assertEquals(null, window.readDropFilesClipboard());
        } finally {
            platform.close();
        }
    }

    /// Continues scheduled UI work from a move/resize modal-loop timer.
    @Test
    void modalLoopTimerDrainsScheduledWork() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Modal", 56.0, 56.0);
            platform.pump();
            window.nativeWindow().postMessage(0x0231, 0L, 0L);
            platform.pump();
            assertTrue(window.modalLoopActive());
            AtomicBoolean ran = new AtomicBoolean();
            platform.eventLoop().post(() -> ran.set(true));
            window.nativeWindow().sendMessage(0x0113, WindowsNativeWindow.MODAL_TIMER_ID, 0L);
            assertTrue(ran.get());
            assertTrue(window.modalTimerTicks() >= 1);
            window.nativeWindow().postMessage(0x0232, 0L, 0L);
            platform.pump();
            assertFalse(window.modalLoopActive());
        } finally {
            platform.close();
        }
    }

    /// Registers an OLE IDropTarget and invokes Drop through the COM vtable.
    @Test
    void oleDropTargetDispatchesThroughVtable() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Drop", 64.0, 64.0);
            platform.pump();
            try (WindowsDropTarget target = window.registerDropTarget()) {
                target.invokeDrop(40, 50);
                assertEquals(1, target.dropCount());
                assertEquals(40, target.lastDropX());
                assertEquals(50, target.lastDropY());
            }
        } finally {
            platform.close();
        }
    }

    /// Extracts Unicode text through `IDataObject::GetData` during `IDropTarget::Drop`.
    @Test
    void oleDropExtractsUnicodeThroughGetData() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "GetData", 72.0, 72.0);
            platform.pump();
            try (
                    WindowsDropTarget target = window.registerDropTarget();
                    WindowsDataObject data = window.createUnicodeDataObject("HimariUI-drop")
            ) {
                target.invokeDrop(data.nativeObject(), 24, 32);
                assertEquals(1, target.dropCount());
                assertEquals("HimariUI-drop", target.lastDroppedText());
            }
            try (
                    WindowsDropTarget files = window.registerDropTarget();
                    WindowsDataObject drop = window.createDropFilesDataObject(
                            List.of("C:\\HimariUI\\dropped.txt")
                    )
            ) {
                files.invokeDrop(drop.nativeObject(), 8, 12);
                assertEquals(List.of("C:\\HimariUI\\dropped.txt"), files.lastDroppedFiles());
            }
        } finally {
            platform.close();
        }
    }

    /// Creates the TSF thread manager, applies IMM32 placement, and reads a UIA control type.
    @Test
    void tsfImm32AndUiaUseNativeCom() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "TsfUia", 88.0, 88.0);
            platform.pump();
            window.ime().setCandidateRectangle(6.0f, 10.0f, 20.0f, 14.0f);
            window.applyImeCandidate();
            window.ime().setSurroundingText("hello", 5);
            try (
                    WindowsTsfSession tsf = window.openTsf();
                    WindowsTextStore store = window.createTextStore()
            ) {
                assertTrue(tsf.available(), "CoCreateInstance(ITfThreadMgr) HRESULT=" + tsf.createResult());
                assertTrue(tsf.activate(), "ITfThreadMgr::Activate HRESULT=" + tsf.activateResult());
                assertTrue(tsf.clientId() != 0);
                assertEquals(0, store.invokeRequestLock(WindowsTextStore.TS_LF_READWRITE));
                assertTrue(store.lockCount() >= 1);
                store.invokeSetText(0, 5, "nihao");
                assertEquals("nihao", store.invokeGetText(0, -1));
                assertEquals("nihao", window.ime().surroundingText());
                store.invokeSetSelection(2);
                WindowsTextStore.Selection selection = store.invokeGetSelection();
                assertEquals(2, selection.start());
                assertEquals(2, selection.end());
                assertEquals(2, window.ime().compositionStart());
                assertEquals(0, store.invokeGetAcpFromPoint(6, 10));
                assertEquals(5, store.invokeGetAcpFromPoint(26, 10));
                WindowsTextStore.ScreenExtent extent = store.invokeGetScreenExt();
                assertEquals(6, extent.left());
                assertEquals(10, extent.top());
                assertEquals(26, extent.right());
                assertEquals(24, extent.bottom());
                assertFalse(store.invokeQueryInsertEmbedded());
                assertTrue(store.invokeGetFormattedText() < 0);
                assertEquals(0, store.invokeRetrieveRequestedAttrs());
                assertFalse(store.invokeFindNextAttrTransition());
                assertTrue(tsf.attach(store), "CreateDocumentMgr/CreateContext/Push failed");
                assertTrue(tsf.documentAttached());
            }
            LayoutTree tree = new LayoutTree();
            tree.setRoot(BootstrapCounterPane.create(tree, new AtomicInteger()));
            tree.measure(Constraints.loose(200.0f, 200.0f));
            tree.place();
            SemanticsNode increment = tree.semantics().nodeWith(SemanticsAction.ACTIVATE);
            try (WindowsAutomationProvider provider = window.automationProvider(increment)) {
                assertEquals(
                        WindowsAutomationProvider.UIA_BUTTON_CONTROL_TYPE_ID,
                        provider.invokePropertyValue(WindowsAutomationProvider.UIA_CONTROL_TYPE_PROPERTY_ID)
                );
                assertEquals(
                        increment.focused() ? 1 : 0,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_HAS_KEYBOARD_FOCUS_PROPERTY_ID
                        )
                );
                assertEquals(
                        increment.focusable() && !increment.disabled() ? 1 : 0,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_KEYBOARD_FOCUSABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        increment.role().name(),
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_CLASS_NAME_PROPERTY_ID
                        )
                );
                assertEquals(
                        increment.role().name(),
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_LOCALIZED_CONTROL_TYPE_PROPERTY_ID
                        )
                );
                assertTrue(provider.raiseTextChanged() >= 0);
                assertTrue(provider.raiseTextSelectionChanged() >= 0);
                assertEquals(
                        increment.password() ? 1 : 0,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_PASSWORD_PROPERTY_ID
                        )
                );
                assertEquals(
                        increment.bounds().width() <= 0.0f || increment.bounds().height() <= 0.0f ? 1 : 0,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_OFFSCREEN_PROPERTY_ID
                        )
                );
                assertEquals(
                        "HimariUI",
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_FRAMEWORK_ID_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        provider.invokePropertyValue(WindowsAutomationProvider.UIA_IS_ENABLED_PROPERTY_ID)
                );
                LayoutNode incrementLive = findActivate(tree.root());
                incrementLive.setDisabled(true);
                try (WindowsAutomationProvider disabled = window.automationProvider(incrementLive)) {
                    assertEquals(
                            0,
                            disabled.invokePropertyValue(WindowsAutomationProvider.UIA_IS_ENABLED_PROPERTY_ID)
                    );
                    assertEquals(
                            0,
                            disabled.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_KEYBOARD_FOCUSABLE_PROPERTY_ID
                            )
                    );
                }
                incrementLive.setDisabled(false);
                incrementLive.setReadOnly(true);
                try (WindowsAutomationProvider readOnly = window.automationProvider(incrementLive)) {
                    assertEquals(
                            1,
                            readOnly.invokePropertyValue(WindowsAutomationProvider.UIA_IS_READ_ONLY_PROPERTY_ID)
                    );
                }
                incrementLive.setReadOnly(false);
                incrementLive.setHint("Increases the counter");
                try (WindowsAutomationProvider named = window.automationProvider(incrementLive)) {
                    assertEquals(
                            incrementLive.label(),
                            named.invokePropertyValueString(WindowsAutomationProvider.UIA_NAME_PROPERTY_ID)
                    );
                    assertEquals(
                            incrementLive.name(),
                            named.invokePropertyValueString(WindowsAutomationProvider.UIA_AUTOMATION_ID_PROPERTY_ID)
                    );
                    assertEquals(
                            "Increases the counter",
                            named.invokePropertyValueString(WindowsAutomationProvider.UIA_HELP_TEXT_PROPERTY_ID)
                    );
                }
                incrementLive.setHint("");
                incrementLive.setPassword(true);
                try (WindowsAutomationProvider secret = window.automationProvider(incrementLive)) {
                    assertEquals(
                            1,
                            secret.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_PASSWORD_PROPERTY_ID
                            )
                    );
                }
                incrementLive.setPassword(false);
                incrementLive.setAccessKey("I");
                incrementLive.setAcceleratorKey("Ctrl+I");
                incrementLive.setRequired(true);
                incrementLive.setItemStatus("busy");
                incrementLive.setLocale("en-US");
                incrementLive.setLevel(2);
                incrementLive.setPositionInSet(1);
                incrementLive.setSizeOfSet(3);
                incrementLive.setDescription("Increases the counter");
                incrementLive.setError(true);
                try (WindowsAutomationProvider keys = window.automationProvider(incrementLive)) {
                    assertEquals(
                            "I",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_ACCESS_KEY_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "Ctrl+I",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_ACCELERATOR_KEY_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            incrementLive.label(),
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_VALUE_VALUE_PROPERTY_ID
                            )
                    );
                    float[] box = keys.invokeBoundingRectangle();
                    assertEquals(increment.bounds().x(), box[0], 0.01f);
                    assertEquals(increment.bounds().y(), box[1], 0.01f);
                    assertEquals(increment.bounds().width(), box[2], 0.01f);
                    assertEquals(increment.bounds().height(), box[3], 0.01f);
                    assertEquals(
                            1,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_CONTROL_ELEMENT_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            1,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_CONTENT_ELEMENT_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            WindowsAutomationProvider.ORIENTATION_NONE,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_ORIENTATION_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            1,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_REQUIRED_FOR_FORM_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "busy",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_ITEM_STATUS_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            WindowsAutomationProvider.LCID_EN_US,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_CULTURE_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            2,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_LEVEL_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            1,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_POSITION_IN_SET_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            3,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_SIZE_OF_SET_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "Increases the counter",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_FULL_DESCRIPTION_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            0,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_DATA_VALID_FOR_FORM_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "HimariUI.BUTTON",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_PROVIDER_DESCRIPTION_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            0,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_DIALOG_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            (int) ProcessHandle.current().pid(),
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_PROCESS_ID_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            (int) window.nativeHandle().address(),
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_NATIVE_WINDOW_HANDLE_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            WindowsAutomationProvider.HEADING_LEVEL_NONE + 2,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_HEADING_LEVEL_PROPERTY_ID
                            )
                    );
                    float[] click = keys.invokeClickablePoint();
                    assertEquals(
                            increment.bounds().x() + increment.bounds().width() * 0.5f,
                            click[0],
                            0.01f
                    );
                    assertEquals(
                            increment.bounds().y() + increment.bounds().height() * 0.5f,
                            click[1],
                            0.01f
                    );
                }
                LayoutFactory factory = new LayoutFactory(tree);
                LayoutNode slider = factory.leaf(
                        "slider",
                        new Size(160.0f, 24.0f),
                        List.of(),
                        true,
                        SemanticsRole.SLIDER,
                        "Volume",
                        Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                        null
                );
                LayoutNode bar = factory.leaf(
                        "bar",
                        new Size(16.0f, 80.0f),
                        List.of(),
                        true,
                        SemanticsRole.SCROLLBAR,
                        "Scroll",
                        Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                        null
                );
                tree.setRoot(factory.column("oriented", Alignment.START, List.of(), slider, bar));
                tree.measure(Constraints.loose(200.0f, 200.0f));
                tree.place();
                try (WindowsAutomationProvider slide = window.automationProvider(slider)) {
                    assertEquals(
                            WindowsAutomationProvider.ORIENTATION_HORIZONTAL,
                            slide.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_ORIENTATION_PROPERTY_ID
                            )
                    );
                }
                try (WindowsAutomationProvider scroll = window.automationProvider(bar)) {
                    assertEquals(
                            WindowsAutomationProvider.ORIENTATION_VERTICAL,
                            scroll.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_ORIENTATION_PROPERTY_ID
                            )
                    );
                }
                LayoutNode dialog = factory.leaf(
                        "dialog",
                        new Size(80.0f, 40.0f),
                        List.of(),
                        false,
                        SemanticsRole.DIALOG,
                        "Confirm",
                        Set.of(),
                        null
                );
                try (WindowsAutomationProvider modal = window.automationProvider(dialog)) {
                    assertEquals(
                            1,
                            modal.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_DIALOG_PROPERTY_ID
                            )
                    );
                }
                assertTrue(provider.invokePatternProvider(WindowsAutomationProvider.UIA_INVOKE_PATTERN_ID));
                assertEquals(1, provider.invoke());
                assertTrue(provider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SYNCHRONIZED_INPUT_PATTERN_ID
                ));
                assertEquals(
                        1,
                        provider.startListening(WindowsAutomationProvider.SYNCHRONIZED_INPUT_KEY_DOWN)
                );
                assertEquals(1, provider.cancelSynchronizedInput());
                assertTrue(provider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_PATTERN_ID
                ));
                assertEquals(0, provider.legacyChildId());
                assertEquals(increment.label(), provider.legacyName());
                assertEquals(WindowsAutomationProvider.ROLE_SYSTEM_PUSHBUTTON, provider.legacyRole());
                assertEquals(2, provider.invokeLegacyDefaultAction());
                assertEquals(increment.label(), provider.legacyValue());
                assertEquals(WindowsAutomationProvider.STATE_SYSTEM_FOCUSABLE, provider.legacyState());
                assertEquals(increment.label(), provider.legacyDescription());
                assertEquals("Press", provider.legacyDefaultAction());
                assertEquals("", provider.legacyKeyboardShortcut());
                assertEquals(increment.label(), provider.legacyHelp());
                assertTrue(provider.invokeFragmentNavigate(WindowsAutomationProvider.NAVIGATE_DIRECTION_PARENT));
                assertFalse(provider.invokeFragmentNavigate(1));
                assertEquals(1, provider.invokeFragmentSetFocus());
                assertTrue(provider.invokeFragmentRoot());
                double[] fragmentBounds = provider.invokeFragmentBoundingRectangle();
                assertEquals(increment.bounds().x(), fragmentBounds[0], 0.001);
                assertEquals(increment.bounds().y(), fragmentBounds[1], 0.001);
                assertEquals(increment.bounds().width(), fragmentBounds[2], 0.001);
                assertEquals(increment.bounds().height(), fragmentBounds[3], 0.001);
                assertTrue(provider.invokeFragmentRootFromPoint(increment.bounds().x(), increment.bounds().y()));
                assertTrue(provider.invokeFragmentRootFocus());
                assertEquals(1, provider.invokeLegacySetValue(increment.label()));
                assertEquals(increment.label(), provider.lastLegacyValue());
                int[] runtimeId = provider.invokeFragmentRuntimeId();
                assertEquals(1, runtimeId[0]);
                assertEquals((int) increment.id(), runtimeId[1]);
                assertEquals(0, provider.invokeEmbeddedFragmentRoots());
                assertEquals(WindowsAutomationProvider.PROVIDER_OPTIONS_SERVER_SIDE, provider.invokeProviderOptions());
                assertTrue(provider.invokeHostRawElementProvider());
            }
            LayoutTree valueTree = new LayoutTree();
            LayoutFactory factory = new LayoutFactory(valueTree);
            LayoutNode toggle = factory.leaf(
                    "toggle",
                    new Size(48.0f, 24.0f),
                    List.of(),
                    true,
                    SemanticsRole.TOGGLE,
                    "Muted",
                    java.util.Set.of(SemanticsAction.ACTIVATE),
                    () -> { }
            );
            toggle.setSelected(false);
            LayoutNode slider = factory.leaf(
                    "slider",
                    new Size(160.0f, 24.0f),
                    List.of(),
                    true,
                    SemanticsRole.SLIDER,
                    "Volume",
                    java.util.Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                    null
            );
            slider.setRangeValue(3.0);
            LayoutNode option = factory.leaf(
                    "option",
                    new Size(120.0f, 20.0f),
                    List.of(),
                    true,
                    SemanticsRole.RADIO,
                    "Left",
                    java.util.Set.of(SemanticsAction.ACTIVATE),
                    () -> { }
            );
            option.setSelected(false);
            LayoutNode item = factory.leaf(
                    "item",
                    new Size(160.0f, 20.0f),
                    List.of(),
                    true,
                    SemanticsRole.TREE_ITEM,
                    "Folder",
                    java.util.Set.of(SemanticsAction.ACTIVATE, SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                    () -> { }
            );
            LayoutNode status = factory.leaf(
                    "status",
                    new Size(120.0f, 20.0f),
                    List.of(),
                    false,
                    SemanticsRole.STATUS,
                    "Saved",
                    java.util.Set.of(),
                    null
            );
            status.setLiveRegion(SemanticsLiveRegion.POLITE);
            LayoutNode alert = factory.leaf(
                    "alert",
                    new Size(120.0f, 20.0f),
                    List.of(),
                    false,
                    SemanticsRole.STATUS,
                    "Alert",
                    java.util.Set.of(),
                    null
            );
            alert.setLiveRegion(SemanticsLiveRegion.ASSERTIVE);
            LayoutNode field = factory.leaf(
                    "field",
                    new Size(160.0f, 24.0f),
                    List.of(),
                    true,
                    SemanticsRole.TEXT_FIELD,
                    "hello",
                    java.util.Set.of(SemanticsAction.ACTIVATE),
                    () -> { }
            );
            field.setTextRange(new SemanticsTextRange(1, 4, 4));
            LayoutNode table = factory.leaf(
                    "table",
                    new Size(160.0f, 40.0f),
                    List.of(),
                    false,
                    SemanticsRole.TABLE,
                    "People",
                    java.util.Set.of(),
                    null
            );
            table.setGrid(new SemanticsGrid(2, 3));
            LayoutNode cell = factory.leaf(
                    "cell",
                    new Size(80.0f, 20.0f),
                    List.of(),
                    false,
                    SemanticsRole.TABLE_CELL,
                    "r0c1",
                    java.util.Set.of(),
                    null
            );
            cell.setGridItem(new SemanticsGridItem(0, 1));
            LayoutNode list = factory.leaf(
                    "list",
                    new Size(160.0f, 40.0f),
                    List.of(),
                    true,
                    SemanticsRole.LIST,
                    "Items",
                    java.util.Set.of(
                            SemanticsAction.INCREMENT,
                            SemanticsAction.DECREMENT,
                            SemanticsAction.SCROLL_INTO_VIEW,
                            SemanticsAction.REALIZE),
                    null
            );
            list.setScroll(new SemanticsScroll(25.0, 20.0, true, 10.0, 30.0, true));
            LayoutNode dialog = factory.leaf(
                    "dialog",
                    new Size(80.0f, 40.0f),
                    List.of(),
                    true,
                    SemanticsRole.DIALOG,
                    "Confirm",
                    java.util.Set.of(),
                    null
            );
            valueTree.setRoot(factory.column(
                    "root",
                    Alignment.START,
                    List.of(),
                    toggle,
                    slider,
                    option,
                    item,
                    table,
                    cell,
                    list,
                    status,
                    alert,
                    field,
                    dialog
            ));
            valueTree.measure(Constraints.loose(400.0f, 400.0f));
            valueTree.place();
            SemanticsNode toggleNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TOGGLE)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode sliderNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.SLIDER)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode optionNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.RADIO)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode itemNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TREE_ITEM)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode statusNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.STATUS
                            && node.liveRegion() == SemanticsLiveRegion.POLITE)
                    .findFirst()
                    .orElseThrow();
            SemanticsNode alertNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.STATUS
                            && node.liveRegion() == SemanticsLiveRegion.ASSERTIVE)
                    .findFirst()
                    .orElseThrow();
            assertEquals(SemanticsLiveRegion.POLITE, statusNode.liveRegion());
            assertEquals(SemanticsLiveRegion.ASSERTIVE, alertNode.liveRegion());
            try (WindowsAutomationProvider toggleProvider = window.automationProvider(toggleNode)) {
                assertTrue(toggleProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TOGGLE_PATTERN_ID));
                assertEquals(WindowsAutomationProvider.TOGGLE_STATE_OFF, toggleProvider.toggleState());
                assertEquals(WindowsAutomationProvider.TOGGLE_STATE_ON, toggleProvider.toggle());
            }
            try (WindowsAutomationProvider rangeProvider = window.automationProvider(sliderNode)) {
                assertTrue(rangeProvider.invokePatternProvider(WindowsAutomationProvider.UIA_RANGE_VALUE_PATTERN_ID));
                assertEquals(3.0, rangeProvider.rangeValue());
                assertEquals(7.5, rangeProvider.setRangeValue(7.5));
                assertEquals(7.5, rangeProvider.rangeValue());
            }
            try (WindowsAutomationProvider selectionProvider = window.automationProvider(optionNode)) {
                assertTrue(selectionProvider.invokePatternProvider(WindowsAutomationProvider.UIA_SELECTION_ITEM_PATTERN_ID));
                assertFalse(selectionProvider.itemSelected());
                assertTrue(selectionProvider.selectItem());
                assertFalse(selectionProvider.removeItemFromSelection());
            }
            try (WindowsAutomationProvider expandProvider = window.automationProvider(itemNode)) {
                assertTrue(expandProvider.invokePatternProvider(WindowsAutomationProvider.UIA_EXPAND_COLLAPSE_PATTERN_ID));
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_EXPANDED, expandProvider.expandState());
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_COLLAPSED, expandProvider.collapse());
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_EXPANDED, expandProvider.expand());
            }
            SemanticsNode tableNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TABLE)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider gridProvider = window.automationProvider(tableNode)) {
                assertTrue(gridProvider.invokePatternProvider(WindowsAutomationProvider.UIA_GRID_PATTERN_ID));
                assertTrue(gridProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TABLE_PATTERN_ID));
                assertEquals(2, gridProvider.gridRowCount());
                assertEquals(3, gridProvider.gridColumnCount());
                assertTrue(gridProvider.invokeGetItem(1, 2));
                assertEquals(1, gridProvider.invokeFetchedItemRow());
                assertEquals(2, gridProvider.invokeFetchedItemColumn());
                assertTrue(gridProvider.invokeFetchedContainingGrid());
                assertFalse(gridProvider.invokeGetItem(2, 0));
                assertEquals(0, gridProvider.invokeRowHeaders());
                assertEquals(0, gridProvider.invokeColumnHeaders());
                assertEquals(WindowsAutomationProvider.ROW_OR_COLUMN_MAJOR_ROW, gridProvider.rowOrColumnMajor());
                assertTrue(gridProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SPREADSHEET_PATTERN_ID
                ));
                assertTrue(gridProvider.invokeSpreadsheetItem("People"));
                assertFalse(gridProvider.invokeSpreadsheetItem("missing"));
            }
            SemanticsNode cellNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TABLE_CELL)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider cellProvider = window.automationProvider(cellNode)) {
                assertTrue(cellProvider.invokePatternProvider(WindowsAutomationProvider.UIA_GRID_ITEM_PATTERN_ID));
                assertTrue(cellProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TABLE_ITEM_PATTERN_ID));
                assertEquals(0, cellProvider.gridItemRow());
                assertEquals(1, cellProvider.gridItemColumn());
                assertEquals(0, cellProvider.invokeRowHeaderItems());
                assertTrue(cellProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SPREADSHEET_ITEM_PATTERN_ID
                ));
                assertEquals("=r0c1", cellProvider.spreadsheetFormula());
            }
            SemanticsNode listNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.LIST)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider scrollProvider = window.automationProvider(listNode)) {
                assertTrue(scrollProvider.invokePatternProvider(WindowsAutomationProvider.UIA_SCROLL_PATTERN_ID));
                assertTrue(scrollProvider.verticallyScrollable());
                assertTrue(scrollProvider.horizontallyScrollable());
                assertEquals(25.0, scrollProvider.verticalScrollPercent());
                assertEquals(10.0, scrollProvider.horizontalScrollPercent());
                assertEquals(40.0, scrollProvider.setVerticalScrollPercent(40.0));
                assertEquals(50.0, scrollProvider.scrollVertical(WindowsAutomationProvider.SCROLL_AMOUNT_SMALL_INCREMENT));
                assertEquals(20.0, scrollProvider.setHorizontalScrollPercent(20.0));
                assertEquals(30.0, scrollProvider.scrollHorizontal(WindowsAutomationProvider.SCROLL_AMOUNT_SMALL_INCREMENT));
                assertTrue(scrollProvider.invokePatternProvider(WindowsAutomationProvider.UIA_SCROLL_ITEM_PATTERN_ID));
                assertEquals(1, scrollProvider.invokeScrollItem());
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_VIRTUALIZED_ITEM_PATTERN_ID
                ));
                assertEquals(1, scrollProvider.invokeVirtualizedItem());
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_ITEM_CONTAINER_PATTERN_ID
                ));
                assertTrue(scrollProvider.invokeFindItemByProperty("Items"));
                assertFalse(scrollProvider.invokeFindItemByProperty("missing"));
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_MULTIPLE_VIEW_PATTERN_ID
                ));
                assertEquals(1, scrollProvider.currentView());
                assertEquals("List", scrollProvider.viewName(1));
                assertEquals(2, scrollProvider.setCurrentView(2));
                assertEquals(2, scrollProvider.currentView());
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_DROP_TARGET_PATTERN_ID
                ));
                assertEquals("move", scrollProvider.dropTargetEffect());
                assertTrue(scrollProvider.invokePatternProvider(WindowsAutomationProvider.UIA_DRAG_PATTERN_ID));
                assertFalse(scrollProvider.isGrabbed());
                assertEquals("copy", scrollProvider.dropEffect());
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SELECTION_PATTERN_ID
                ));
                assertTrue(scrollProvider.canSelectMultiple());
                assertFalse(scrollProvider.isSelectionRequired());
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SELECTION_PATTERN2_ID
                ));
                assertEquals(1, scrollProvider.selectionItemCount());
                assertTrue(scrollProvider.invokeCurrentSelectedItem());
                assertTrue(scrollProvider.invokeFirstSelectedItem());
                assertTrue(scrollProvider.invokeLastSelectedItem());
            }
            SemanticsNode dialogNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.DIALOG)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider windowProvider = window.automationProvider(dialogNode)) {
                assertTrue(windowProvider.invokePatternProvider(WindowsAutomationProvider.UIA_WINDOW_PATTERN_ID));
                assertTrue(windowProvider.canMaximize());
                assertTrue(windowProvider.canMinimize());
                assertTrue(windowProvider.isModal());
                assertFalse(windowProvider.isTopmost());
                assertTrue(windowProvider.waitForInputIdle(0));
                assertEquals(
                        WindowsAutomationProvider.WINDOW_INTERACTION_READY,
                        windowProvider.windowInteractionState()
                );
                assertEquals(
                        WindowsAutomationProvider.WINDOW_VISUAL_STATE_MAXIMIZED,
                        windowProvider.setWindowVisualState(WindowsAutomationProvider.WINDOW_VISUAL_STATE_MAXIMIZED)
                );
                assertEquals(1, windowProvider.closeWindow());
                assertTrue(windowProvider.invokePatternProvider(WindowsAutomationProvider.UIA_DOCK_PATTERN_ID));
                assertEquals(
                        WindowsAutomationProvider.DOCK_POSITION_NONE,
                        windowProvider.dockPosition()
                );
                assertEquals(
                        WindowsAutomationProvider.DOCK_POSITION_TOP,
                        windowProvider.setDockPosition(WindowsAutomationProvider.DOCK_POSITION_TOP)
                );
                assertTrue(windowProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TRANSFORM_PATTERN_ID));
                assertTrue(windowProvider.canMove());
                assertTrue(windowProvider.canResize());
                assertTrue(windowProvider.canRotate());
                assertEquals(12.0, windowProvider.moveTransform(12.0, 24.0));
                assertEquals(80.0, windowProvider.resizeTransform(80.0, 40.0));
                assertEquals(15.0, windowProvider.rotateTransform(15.0));
                assertTrue(windowProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_TRANSFORM_PATTERN2_ID
                ));
                assertTrue(windowProvider.canZoom());
                assertEquals(1.0, windowProvider.zoomLevel());
                assertEquals(2.5, windowProvider.zoomTransform(2.5));
                assertEquals(2.5, windowProvider.zoomLevel());
                assertEquals(3.5, windowProvider.zoomByUnit(WindowsAutomationProvider.ZOOM_UNIT_LARGE_INCREMENT));
                assertEquals(3.5, windowProvider.zoomLevel());
                assertEquals(0.5, windowProvider.zoomMinimum());
                assertEquals(4.0, windowProvider.zoomMaximum());
                assertTrue(windowProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_CUSTOM_NAVIGATION_PATTERN_ID
                ));
                assertTrue(windowProvider.invokeNavigate(WindowsAutomationProvider.NAVIGATE_DIRECTION_PARENT));
                assertFalse(windowProvider.invokeNavigate(1));
                assertTrue(windowProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_OBJECT_MODEL_PATTERN_ID
                ));
                assertTrue(windowProvider.invokeObjectModel());
            }
            try (WindowsAutomationProvider statusProvider = window.automationProvider(status);
                    WindowsAutomationProvider alertProvider = window.automationProvider(alert)) {
                assertEquals(
                        WindowsAutomationProvider.UIA_STATUS_BAR_CONTROL_TYPE_ID,
                        statusProvider.invokePropertyValue(WindowsAutomationProvider.UIA_CONTROL_TYPE_PROPERTY_ID)
                );
                assertEquals(
                        WindowsAutomationProvider.LIVE_SETTING_POLITE,
                        statusProvider.invokePropertyValue(WindowsAutomationProvider.UIA_LIVE_SETTING_PROPERTY_ID)
                );
                assertEquals(
                        WindowsAutomationProvider.LIVE_SETTING_ASSERTIVE,
                        alertProvider.invokePropertyValue(WindowsAutomationProvider.UIA_LIVE_SETTING_PROPERTY_ID)
                );
                status.setLabel("Updated");
                assertEquals("Updated", status.label());
                assertEquals(1, statusProvider.liveRegionChangedCount());
                assertTrue(
                        statusProvider.lastLiveRegionEventResult() >= 0,
                        "UiaRaiseAutomationEvent HRESULT=" + statusProvider.lastLiveRegionEventResult()
                );
                statusProvider.clientsAreListening();
                assertTrue(statusProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_ANNOTATION_PATTERN_ID
                ));
                assertEquals(
                        WindowsAutomationProvider.ANNOTATION_TYPE_COMMENT,
                        statusProvider.annotationTypeId()
                );
                assertEquals("Comment", statusProvider.annotationTypeName());
                assertEquals("Himari", statusProvider.annotationAuthor());
                assertEquals("2026-08-17", statusProvider.annotationDateTime());
                assertTrue(statusProvider.invokeAnnotationTarget());
                assertTrue(statusProvider.invokePatternProvider(WindowsAutomationProvider.UIA_STYLES_PATTERN_ID));
                assertEquals(WindowsAutomationProvider.STYLE_ID_NORMAL, statusProvider.styleId());
                assertEquals("Normal", statusProvider.styleName());
            }
            SemanticsNode fieldNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider textProvider = window.automationProvider(fieldNode)) {
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_VALUE_PATTERN_ID));
                assertEquals("hello", textProvider.value());
                assertEquals("world", textProvider.setValue("world"));
                assertEquals("world", textProvider.value());
                assertFalse(textProvider.valueReadOnly());
                field.setReadOnly(true);
                try (WindowsAutomationProvider readOnly = window.automationProvider(field)) {
                    assertTrue(readOnly.valueReadOnly());
                    assertEquals(
                            1,
                            readOnly.invokePropertyValue(WindowsAutomationProvider.UIA_IS_READ_ONLY_PROPERTY_ID)
                    );
                }
                field.setReadOnly(false);
                field.setHint("Type a greeting");
                try (WindowsAutomationProvider hinted = window.automationProvider(field)) {
                    assertEquals(
                            field.label(),
                            hinted.invokePropertyValueString(WindowsAutomationProvider.UIA_NAME_PROPERTY_ID)
                    );
                    assertEquals(
                            "Type a greeting",
                            hinted.invokePropertyValueString(WindowsAutomationProvider.UIA_HELP_TEXT_PROPERTY_ID)
                    );
                }
                field.setHint("");
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_CHILD_PATTERN_ID));
                assertTrue(textProvider.invokeTextContainer());
                assertTrue(textProvider.invokeTextChildRange());
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_EDIT_PATTERN_ID));
                assertTrue(textProvider.invokeActiveComposition());
                assertTrue(textProvider.invokeConversionTarget());
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_PATTERN2_ID));
                assertTrue(textProvider.invokeCaretRange());
                assertTrue(textProvider.invokeRangeFromAnnotation());
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_PATTERN_ID));
                assertTrue(textProvider.invokeDocumentRange());
                assertTrue(textProvider.invokeGetVisibleRanges());
                assertTrue(textProvider.invokeRangeFromChild());
                assertTrue(textProvider.invokeRangeFromPoint(
                        fieldNode.bounds().x() + 1.0,
                        fieldNode.bounds().y() + 1.0
                ));
                assertFalse(textProvider.invokeRangeFromPoint(-1000.0, -1000.0));
                assertEquals(
                        WindowsAutomationProvider.SUPPORTED_TEXT_SELECTION_SINGLE,
                        textProvider.invokeSupportedTextSelection()
                );
                assertEquals("hello", textProvider.invokeGetText(-1));
                assertEquals("he", textProvider.invokeGetText(2));
                assertTrue(textProvider.invokeClone());
                assertTrue(textProvider.invokeCompareSelf());
                assertTrue(textProvider.invokeEnclosingElement());
                textProvider.invokeExpandToEnclosingUnit(WindowsAutomationProvider.TEXT_UNIT_DOCUMENT);
                assertEquals("hello", textProvider.invokeGetText(-1));
                assertEquals(-2, textProvider.invokeMove(WindowsAutomationProvider.TEXT_UNIT_CHARACTER, -2));
                assertEquals(
                        4,
                        textProvider.invokeExpandToEnclosingUnit(WindowsAutomationProvider.TEXT_UNIT_CHARACTER).end()
                );
                assertEquals("l", textProvider.invokeGetText(-1));
                double[] rects = textProvider.invokeGetBoundingRectangles();
                assertEquals(4, rects.length);
                assertEquals(fieldNode.bounds().width(), rects[2], 0.01);
                assertEquals(fieldNode.bounds().height(), rects[3], 0.01);
                textProvider.invokeExpandToEnclosingUnit(WindowsAutomationProvider.TEXT_UNIT_DOCUMENT);
                assertTrue(textProvider.invokeCompareEndpoints(
                        WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_START,
                        WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_END
                ) < 0);
                assertTrue(textProvider.invokeFindText("ell", false));
                assertEquals("ell", textProvider.invokeGetText(-1));
                assertEquals(
                        1,
                        textProvider.invokeMoveEndpointByUnit(
                                WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_END,
                                WindowsAutomationProvider.TEXT_UNIT_CHARACTER,
                                1
                        )
                );
                assertEquals("ello", textProvider.invokeGetText(-1));
                textProvider.invokeMoveEndpointByRange(
                        WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_END,
                        WindowsAutomationProvider.TEXT_PATTERN_RANGE_ENDPOINT_START
                );
                assertEquals("", textProvider.invokeGetText(-1));
                assertFalse(textProvider.invokeGetSelection());
                textProvider.invokeSelect();
                assertTrue(textProvider.invokeGetSelection());
                assertFalse(textProvider.invokeFindAttribute(40013));
                assertEquals(0, textProvider.invokeGetAttributeValue(40013));
                textProvider.invokeRemoveFromSelection();
                assertFalse(textProvider.invokeGetSelection());
                textProvider.invokeAddToSelection();
                assertTrue(textProvider.invokeGetSelection());
                assertTrue(textProvider.invokeScrollIntoView(true));
                assertEquals(0, textProvider.invokeGetChildren());
                assertEquals(1, textProvider.invokeShowContextMenu());
                assertEquals(1, textProvider.invokeSimpleShowContextMenu());
            }
            try (WindowsAutomationProvider liveField = window.automationProvider(field)) {
                assertEquals("hello", liveField.invokeGetText(-1));
                field.setLabel("updated-hello");
                assertEquals("updated-hello", liveField.invokeGetText(-1));
            }
        } finally {
            platform.close();
        }
    }

    /// Creates a Vulkan logical device and a `VkSurfaceKHR` for the production HWND.
    @Test
    void vulkanCreatesWin32SurfaceForHwnd() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "VulkanSurface", 96.0, 96.0);
            platform.pump();
            try (VulkanDevice device = VulkanDevice.open()) {
                assertTrue(device.capabilities().logicalDeviceCreated());
                assertTrue(device.capabilities().graphicsQueueFamily() >= 0);
                VulkanPresentation presentation = device.presentSdr(
                        window.moduleHandle(),
                        window.nativeHandle(),
                        window.nativeWindow().clientWidth(),
                        window.nativeWindow().clientHeight()
                );
                assertTrue(presentation.swapchainCreated());
                assertTrue(presentation.cleared());
                assertTrue(presentation.presented());
                assertTrue(device.capabilities().win32SurfaceCreated());
                assertFalse(presentation.hdrMetadataApplied());
            }
        } finally {
            platform.close();
        }
    }

    /// Closes the matching hosted HWND when the host asks the application to decide.
    ///
    /// @param hosted the windows that may honor the request
    /// @param event the host event
    private static void closeOnRequest(WindowsWindow[] hosted, WindowEvent event) {
        if (event.type() != WindowEventType.CLOSE_REQUESTED) {
            return;
        }
        for (WindowsWindow candidate : hosted) {
            if (candidate != null
                    && !candidate.isClosed()
                    && candidate.id().equals(event.snapshot().id())) {
                candidate.closeAsync();
            }
        }
    }

    /// Finds the first activatable leaf.
    private static LayoutNode findActivate(LayoutNode node) {
        if (node.actions().contains(SemanticsAction.ACTIVATE)) {
            return node;
        }
        for (LayoutNode child : node.children()) {
            try {
                return findActivate(child);
            } catch (IllegalStateException ignored) {
                // try the next sibling
            }
        }
        throw new IllegalStateException("no activatable node");
    }

    /// Opens one visible top-level window.
    private static WindowsWindow openToplevel(WindowsPlatform platform, String title, double x, double y)
            throws Exception {
        return platform.createWindow(
                WindowRequest.toplevel(new WindowConfiguration(
                        title,
                        new LogicalRect(x, y, 240.0, 160.0),
                        true,
                        WindowState.NORMAL
                )),
                event -> { }
        ).toCompletableFuture().get();
    }
}
