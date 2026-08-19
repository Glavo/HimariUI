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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
            assertTrue(keys.get(0).timestampMillis() > 0L);
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
            assertEquals(org.glavo.himari.layout.input.KeyLocation.LEFT, metaKeys.get(0).location());
            assertEquals(LogicalKey.TAB, metaKeys.get(1).key());
            assertTrue(metaKeys.get(1).meta());
            window.postVirtualKey(true, 0x5C);
            platform.pump();
            List<org.glavo.himari.layout.input.KeyEvent> rightMeta = window.takeKeyEvents();
            assertEquals(LogicalKey.META, rightMeta.getFirst().key());
            assertEquals(org.glavo.himari.layout.input.KeyLocation.RIGHT, rightMeta.getFirst().location());
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
            java.lang.foreign.MemorySegment touchPacked = java.lang.foreign.Arena.ofAuto().allocate(
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_TOUCH_INFO
            );
            touchPacked.fill((byte) 0);
            touchPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_TOUCH_INFO_TOUCHMASK_OFFSET,
                    WindowsNativeWindow.TOUCH_MASK_CONTACTAREA
                            | WindowsNativeWindow.TOUCH_MASK_ORIENTATION
            );
            touchPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTLEFT_OFFSET,
                    10
            );
            touchPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTTOP_OFFSET,
                    20
            );
            touchPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTRIGHT_OFFSET,
                    18
            );
            touchPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTBOTTOM_OFFSET,
                    32
            );
            touchPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_TOUCH_INFO_ORIENTATION_OFFSET,
                    45
            );
            WindowsNativeWindow.ContactArea decoded = WindowsNativeWindow.decodeTouchInfo(touchPacked);
            assertEquals(8.0f, decoded.width(), 0.001f);
            assertEquals(12.0f, decoded.height(), 0.001f);
            assertEquals(45.0f, decoded.orientation(), 0.001f);
            WindowsNativeWindow.ContactArea missingTouch = window.queryTouchInfo(0x00FFFFFF);
            assertEquals(0.0f, missingTouch.width());
            assertEquals(0.0f, missingTouch.height());
            window.postTouch(
                    PointerEventType.DOWN,
                    6,
                    7,
                    9,
                    new WindowsNativeWindow.ContactArea(8.0f, 12.0f, 45.0f)
            );
            platform.pump();
            PointerEvent touch = window.takePointerEvents().getLast();
            assertEquals(PointerDeviceKind.TOUCH, touch.device());
            assertEquals(9, touch.pointerId());
            assertEquals(8.0f, touch.contactWidth(), 0.001f);
            assertEquals(12.0f, touch.contactHeight(), 0.001f);
            assertEquals(45.0f, touch.orientation(), 0.001f);
            assertFalse(touch.synthetic());
            java.lang.foreign.MemorySegment infoPacked = java.lang.foreign.Arena.ofAuto().allocate(
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO
            );
            infoPacked.fill((byte) 0);
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_FRAMEID_OFFSET,
                    42
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_POINTERFLAGS_OFFSET,
                    WindowsNativeWindow.POINTER_FLAG_INRANGE
                            | WindowsNativeWindow.POINTER_FLAG_INCONTACT
                            | WindowsNativeWindow.POINTER_FLAG_CANCELED
                            | WindowsNativeWindow.POINTER_FLAG_PRIMARY
                            | WindowsNativeWindow.POINTER_FLAG_FIRSTBUTTON
                            | WindowsNativeWindow.POINTER_FLAG_SECONDBUTTON
                            | WindowsNativeWindow.POINTER_FLAG_THIRDBUTTON
                            | WindowsNativeWindow.POINTER_FLAG_FOURTHBUTTON
                            | WindowsNativeWindow.POINTER_FLAG_FIFTHBUTTON
                            | WindowsNativeWindow.POINTER_FLAG_NEW
                            | WindowsNativeWindow.POINTER_FLAG_CONFIDENCE
                            | WindowsNativeWindow.POINTER_FLAG_DOWN
                            | WindowsNativeWindow.POINTER_FLAG_UPDATE
                            | WindowsNativeWindow.POINTER_FLAG_WHEEL
                            | WindowsNativeWindow.POINTER_FLAG_HWHEEL
                            | WindowsNativeWindow.POINTER_FLAG_CAPTURECHANGED
                            | WindowsNativeWindow.POINTER_FLAG_HASTRANSFORM
                            | WindowsNativeWindow.POINTER_FLAG_UP
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_HISTORYCOUNT_OFFSET,
                    3
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_KEYSTATES_OFFSET,
                    WindowsNativeWindow.POINTER_MOD_SHIFT | WindowsNativeWindow.POINTER_MOD_CTRL
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_BUTTONCHANGETYPE_OFFSET,
                    WindowsNativeWindow.POINTER_CHANGE_FIRSTBUTTON_DOWN
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_INPUTDATA_OFFSET,
                    7
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_LONG,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_PERFORMANCECOUNT_OFFSET,
                    1234567890123L
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_PIXELLOCATIONRAWX_OFFSET,
                    100
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_PIXELLOCATIONRAWY_OFFSET,
                    200
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_HIMETRICLOCATIONX_OFFSET,
                    2540
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_HIMETRICLOCATIONY_OFFSET,
                    5080
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_HIMETRICLOCATIONRAWX_OFFSET,
                    3810
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_HIMETRICLOCATIONRAWY_OFFSET,
                    7620
            );
            infoPacked.set(
                    java.lang.foreign.ValueLayout.JAVA_INT,
                    org.glavo.himari.platform.windows.generated.Win32Layouts.POINTER_INFO_DWTIME_OFFSET,
                    12345
            );
            WindowsNativeWindow.PointerFlags decodedFlags = WindowsNativeWindow.decodePointerInfo(infoPacked);
            assertEquals(42, decodedFlags.frameId());
            assertEquals(3, decodedFlags.historyCount());
            assertEquals(7, decodedFlags.inputData());
            assertEquals(1234567890123L, decodedFlags.performanceCount());
            assertEquals(100, decodedFlags.rawX());
            assertEquals(200, decodedFlags.rawY());
            assertEquals(2540, decodedFlags.himetricX());
            assertEquals(5080, decodedFlags.himetricY());
            assertEquals(3810, decodedFlags.himetricRawX());
            assertEquals(7620, decodedFlags.himetricRawY());
            assertEquals(12345, decodedFlags.pointerTime());
            assertEquals(
                    WindowsNativeWindow.POINTER_MOD_SHIFT | WindowsNativeWindow.POINTER_MOD_CTRL,
                    decodedFlags.keyStates()
            );
            assertEquals(WindowsNativeWindow.POINTER_CHANGE_FIRSTBUTTON_DOWN, decodedFlags.buttonChangeType());
            assertTrue(decodedFlags.inRange());
            assertTrue(decodedFlags.inContact());
            assertTrue(decodedFlags.canceled());
            assertTrue(decodedFlags.primary());
            assertTrue(decodedFlags.firstButton());
            assertTrue(decodedFlags.secondButton());
            assertTrue(decodedFlags.thirdButton());
            assertTrue(decodedFlags.fourthButton());
            assertTrue(decodedFlags.fifthButton());
            assertTrue(decodedFlags.newPointer());
            assertTrue(decodedFlags.confidence());
            assertTrue(decodedFlags.down());
            assertTrue(decodedFlags.update());
            assertTrue(decodedFlags.wheel());
            assertTrue(decodedFlags.horizontalWheel());
            assertTrue(decodedFlags.captureChanged());
            assertTrue(decodedFlags.hasTransform());
            assertTrue(decodedFlags.up());
            WindowsNativeWindow.PointerFlags missingFlags = window.queryPointerInfo(0x00FFFFFF);
            assertEquals(0, missingFlags.frameId());
            assertEquals(0, missingFlags.historyCount());
            assertEquals(0, missingFlags.keyStates());
            assertEquals(0, missingFlags.buttonChangeType());
            assertEquals(0, missingFlags.inputData());
            assertEquals(0L, missingFlags.performanceCount());
            assertEquals(0, missingFlags.rawX());
            assertEquals(0, missingFlags.rawY());
            assertEquals(0, missingFlags.himetricX());
            assertEquals(0, missingFlags.himetricY());
            assertEquals(0, missingFlags.himetricRawX());
            assertEquals(0, missingFlags.himetricRawY());
            assertEquals(0, missingFlags.pointerTime());
            assertFalse(missingFlags.inRange());
            assertFalse(missingFlags.inContact());
            assertFalse(missingFlags.canceled());
            assertFalse(missingFlags.primary());
            assertFalse(missingFlags.firstButton());
            assertFalse(missingFlags.secondButton());
            assertFalse(missingFlags.thirdButton());
            assertFalse(missingFlags.fourthButton());
            assertFalse(missingFlags.fifthButton());
            assertFalse(missingFlags.newPointer());
            assertFalse(missingFlags.confidence());
            assertFalse(missingFlags.down());
            assertFalse(missingFlags.update());
            assertFalse(missingFlags.wheel());
            assertFalse(missingFlags.horizontalWheel());
            assertFalse(missingFlags.captureChanged());
            assertFalse(missingFlags.hasTransform());
            assertFalse(missingFlags.up());
            window.installPointerFlags(
                    11,
                    new WindowsNativeWindow.PointerFlags(
                            42, true, true, true, true, true, true, true, true, true, true, true,
                            true, true, true, true, true, true, true, 3,
                            WindowsNativeWindow.POINTER_MOD_SHIFT | WindowsNativeWindow.POINTER_MOD_CTRL,
                            WindowsNativeWindow.POINTER_CHANGE_FIRSTBUTTON_DOWN,
                            7,
                            1234567890123L,
                            100,
                            200,
                            2540,
                            5080,
                            3810,
                            7620,
                            12345
                    )
            );
            window.postTouch(
                    PointerEventType.DOWN,
                    8,
                    9,
                    11,
                    new WindowsNativeWindow.ContactArea(4.0f, 6.0f, 15.0f)
            );
            platform.pump();
            PointerEvent flagged = window.takePointerEvents().getLast();
            assertEquals(PointerDeviceKind.TOUCH, flagged.device());
            assertEquals(11, flagged.pointerId());
            assertTrue(flagged.inRange());
            assertTrue(flagged.inContact());
            assertEquals(42, flagged.frameId());
            assertTrue(flagged.canceled());
            assertTrue(flagged.primary());
            assertTrue(flagged.firstButton());
            assertTrue(flagged.secondButton());
            assertTrue(flagged.thirdButton());
            assertTrue(flagged.fourthButton());
            assertTrue(flagged.fifthButton());
            assertTrue(flagged.newPointer());
            assertTrue(flagged.confidence());
            assertTrue(flagged.down());
            assertTrue(flagged.update());
            assertTrue(flagged.wheel());
            assertTrue(flagged.horizontalWheel());
            assertTrue(flagged.captureChanged());
            assertTrue(flagged.hasTransform());
            assertTrue(flagged.up());
            assertEquals(3, flagged.historyCount());
            assertEquals(
                    WindowsNativeWindow.POINTER_MOD_SHIFT | WindowsNativeWindow.POINTER_MOD_CTRL,
                    flagged.keyStates()
            );
            assertEquals(WindowsNativeWindow.POINTER_CHANGE_FIRSTBUTTON_DOWN, flagged.buttonChangeType());
            assertEquals(7, flagged.inputData());
            assertEquals(1234567890123L, flagged.performanceCount());
            assertEquals(100, flagged.rawX());
            assertEquals(200, flagged.rawY());
            assertEquals(2540, flagged.himetricX());
            assertEquals(5080, flagged.himetricY());
            assertEquals(3810, flagged.himetricRawX());
            assertEquals(7620, flagged.himetricRawY());
            assertEquals(12345, flagged.pointerTime());
            assertFalse(flagged.synthetic());
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
                    assertEquals(
                            1,
                            readOnly.invokePropertyValue(WindowsAutomationProvider.UIA_VALUE_IS_READ_ONLY_PROPERTY_ID)
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
                incrementLive.setItemType("button");
                incrementLive.setLandmarkType(WindowsAutomationProvider.LANDMARK_TYPE_MAIN);
                incrementLive.setLocalizedLandmarkType("main");
                incrementLive.setAriaRole("button");
                incrementLive.setAriaProperties("pressed=false");
                incrementLive.setControllerFor("counter");
                incrementLive.setDescribedBy("hint");
                incrementLive.setFlowsTo("status");
                incrementLive.setLabeledBy("title");
                incrementLive.setFlowsFrom("header");
                incrementLive.setOptimizeForVisualContent(true);
                incrementLive.setFillColor(0xFF1565C0);
                incrementLive.setOutlineColor(0xFFE0E0E0);
                incrementLive.setFillType(WindowsAutomationProvider.FILL_TYPE_COLOR);
                incrementLive.setVisualEffects(WindowsAutomationProvider.VISUAL_EFFECTS_SHADOW);
                incrementLive.setOutlineThickness(2);
                incrementLive.setRotation(90);
                incrementLive.setPeripheral(true);
                incrementLive.setAnnotationType(60000);
                incrementLive.setAnnotationObjects("note");
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
                            "button",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_ITEM_TYPE_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            WindowsAutomationProvider.LANDMARK_TYPE_MAIN,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_LANDMARK_TYPE_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "main",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_LOCALIZED_LANDMARK_TYPE_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "button",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_ARIA_ROLE_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "pressed=false",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_ARIA_PROPERTIES_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "counter",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_CONTROLLER_FOR_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "hint",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_DESCRIBED_BY_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "status",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_FLOWS_TO_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "title",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_LABELED_BY_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "header",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_FLOWS_FROM_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            1,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_OPTIMIZE_FOR_VISUAL_CONTENT_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            0xFF1565C0,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_FILL_COLOR_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            0xFFE0E0E0,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_OUTLINE_COLOR_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            WindowsAutomationProvider.FILL_TYPE_COLOR,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_FILL_TYPE_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            WindowsAutomationProvider.VISUAL_EFFECTS_SHADOW,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_VISUAL_EFFECTS_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            2,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_OUTLINE_THICKNESS_PROPERTY_ID
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
                    float[] center = keys.invokeCenterPoint();
                    assertEquals(click[0], center[0], 0.01f);
                    assertEquals(click[1], center[1], 0.01f);
                    assertEquals(
                            90,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_ROTATION_PROPERTY_ID
                            )
                    );
                    float[] size = keys.invokeSize();
                    assertEquals(increment.bounds().width(), size[0], 0.01f);
                    assertEquals(increment.bounds().height(), size[1], 0.01f);
                    assertEquals(
                            (int) incrementLive.id(),
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_RUNTIME_ID_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            1,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_IS_PERIPHERAL_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            60000,
                            keys.invokePropertyValue(
                                    WindowsAutomationProvider.UIA_ANNOTATION_TYPES_PROPERTY_ID
                            )
                    );
                    assertEquals(
                            "note",
                            keys.invokePropertyValueString(
                                    WindowsAutomationProvider.UIA_ANNOTATION_OBJECTS_PROPERTY_ID
                            )
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
                assertEquals(
                        1,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_INVOKE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_VALUE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(1, provider.invoke());
                assertTrue(provider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SYNCHRONIZED_INPUT_PATTERN_ID
                ));
                assertEquals(
                        1,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SYNCHRONIZED_INPUT_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_OBJECT_MODEL_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_ANNOTATION_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        provider.startListening(WindowsAutomationProvider.SYNCHRONIZED_INPUT_KEY_DOWN)
                );
                assertEquals(1, provider.cancelSynchronizedInput());
                assertTrue(provider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_PATTERN_ID
                ));
                assertEquals(0, provider.legacyChildId());
                assertEquals(
                        0,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_CHILD_ID_PROPERTY_ID
                        )
                );
                assertEquals(increment.label(), provider.legacyName());
                assertEquals(
                        increment.label(),
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_NAME_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.ROLE_SYSTEM_PUSHBUTTON, provider.legacyRole());
                assertEquals(
                        WindowsAutomationProvider.ROLE_SYSTEM_PUSHBUTTON,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_ROLE_PROPERTY_ID
                        )
                );
                assertEquals(2, provider.invokeLegacyDefaultAction());
                assertEquals(increment.label(), provider.legacyValue());
                assertEquals(
                        increment.label(),
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_VALUE_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.STATE_SYSTEM_FOCUSABLE, provider.legacyState());
                assertEquals(
                        WindowsAutomationProvider.STATE_SYSTEM_FOCUSABLE,
                        provider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_STATE_PROPERTY_ID
                        )
                );
                assertEquals(increment.label(), provider.legacyDescription());
                assertEquals(
                        increment.label(),
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_DESCRIPTION_PROPERTY_ID
                        )
                );
                assertEquals("Press", provider.legacyDefaultAction());
                assertEquals(
                        "Press",
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_DEFAULT_ACTION_PROPERTY_ID
                        )
                );
                assertEquals("", provider.legacyKeyboardShortcut());
                assertEquals(
                        "",
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_KEYBOARD_SHORTCUT_PROPERTY_ID
                        )
                );
                assertEquals(increment.label(), provider.legacyHelp());
                assertTrue(
                        provider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_SELECTION_PROPERTY_ID
                        )
                );
                assertEquals(
                        increment.label(),
                        provider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_LEGACY_IACCESSIBLE_HELP_PROPERTY_ID
                        )
                );
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
                assertEquals(
                        1,
                        toggleProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TOGGLE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        toggleProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SCROLL_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        toggleProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SELECTION_CAN_SELECT_MULTIPLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        toggleProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SCROLL_HORIZONTALLY_SCROLLABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        toggleProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SCROLL_VERTICALLY_SCROLLABLE_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.TOGGLE_STATE_OFF, toggleProvider.toggleState());
                assertEquals(
                        WindowsAutomationProvider.TOGGLE_STATE_OFF,
                        toggleProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TOGGLE_TOGGLE_STATE_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.TOGGLE_STATE_ON, toggleProvider.toggle());
                assertEquals(
                        WindowsAutomationProvider.TOGGLE_STATE_ON,
                        toggleProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TOGGLE_TOGGLE_STATE_PROPERTY_ID
                        )
                );
            }
            try (WindowsAutomationProvider rangeProvider = window.automationProvider(sliderNode)) {
                assertTrue(rangeProvider.invokePatternProvider(WindowsAutomationProvider.UIA_RANGE_VALUE_PATTERN_ID));
                assertEquals(
                        1,
                        rangeProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_RANGE_VALUE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(3.0, rangeProvider.rangeValue());
                assertEquals(
                        0.0,
                        rangeProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_RANGE_VALUE_MINIMUM_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(
                        100.0,
                        rangeProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_RANGE_VALUE_MAXIMUM_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(
                        0,
                        rangeProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_RANGE_VALUE_IS_READ_ONLY_PROPERTY_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.RANGE_LARGE_CHANGE,
                        rangeProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_RANGE_VALUE_LARGE_CHANGE_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(
                        WindowsAutomationProvider.RANGE_SMALL_CHANGE,
                        rangeProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_RANGE_VALUE_SMALL_CHANGE_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(7.5, rangeProvider.setRangeValue(7.5));
                assertEquals(7.5, rangeProvider.rangeValue());
            }
            try (WindowsAutomationProvider selectionProvider = window.automationProvider(optionNode)) {
                assertTrue(selectionProvider.invokePatternProvider(WindowsAutomationProvider.UIA_SELECTION_ITEM_PATTERN_ID));
                assertEquals(
                        1,
                        selectionProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SELECTION_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        selectionProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_EXPAND_COLLAPSE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertFalse(selectionProvider.itemSelected());
                assertEquals(
                        0,
                        selectionProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SELECTION_ITEM_IS_SELECTED_PROPERTY_ID
                        )
                );
                assertTrue(selectionProvider.selectItem());
                assertEquals(
                        1,
                        selectionProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SELECTION_ITEM_IS_SELECTED_PROPERTY_ID
                        )
                );
                assertFalse(selectionProvider.removeItemFromSelection());
                assertEquals(
                        0,
                        selectionProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SELECTION_ITEM_IS_SELECTED_PROPERTY_ID
                        )
                );
                assertTrue(
                        selectionProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_SELECTION_ITEM_SELECTION_CONTAINER_PROPERTY_ID
                        )
                );
            }
            try (WindowsAutomationProvider expandProvider = window.automationProvider(itemNode)) {
                assertTrue(expandProvider.invokePatternProvider(WindowsAutomationProvider.UIA_EXPAND_COLLAPSE_PATTERN_ID));
                assertEquals(
                        1,
                        expandProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_EXPAND_COLLAPSE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        expandProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SELECTION_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_EXPANDED, expandProvider.expandState());
                assertEquals(
                        WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_EXPANDED,
                        expandProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_EXPAND_COLLAPSE_EXPAND_COLLAPSE_STATE_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_COLLAPSED, expandProvider.collapse());
                assertEquals(
                        WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_COLLAPSED,
                        expandProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_EXPAND_COLLAPSE_EXPAND_COLLAPSE_STATE_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_EXPANDED, expandProvider.expand());
                assertEquals(
                        WindowsAutomationProvider.EXPAND_COLLAPSE_STATE_EXPANDED,
                        expandProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_EXPAND_COLLAPSE_EXPAND_COLLAPSE_STATE_PROPERTY_ID
                        )
                );
            }
            SemanticsNode tableNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TABLE)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider gridProvider = window.automationProvider(tableNode)) {
                assertTrue(gridProvider.invokePatternProvider(WindowsAutomationProvider.UIA_GRID_PATTERN_ID));
                assertEquals(
                        1,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_GRID_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_EXPAND_COLLAPSE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(gridProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TABLE_PATTERN_ID));
                assertEquals(
                        1,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TABLE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_GRID_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(2, gridProvider.gridRowCount());
                assertEquals(
                        2,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_GRID_ROW_COUNT_PROPERTY_ID
                        )
                );
                assertEquals(3, gridProvider.gridColumnCount());
                assertEquals(
                        3,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_GRID_COLUMN_COUNT_PROPERTY_ID
                        )
                );
                assertTrue(gridProvider.invokeGetItem(1, 2));
                assertEquals(1, gridProvider.invokeFetchedItemRow());
                assertEquals(2, gridProvider.invokeFetchedItemColumn());
                assertTrue(gridProvider.invokeFetchedContainingGrid());
                assertFalse(gridProvider.invokeGetItem(2, 0));
                assertEquals(0, gridProvider.invokeRowHeaders());
                assertEquals(0, gridProvider.invokeColumnHeaders());
                assertArrayEquals(
                        new int[] {0, 0},
                        gridProvider.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_TABLE_ROW_HEADERS_PROPERTY_ID
                        )
                );
                assertArrayEquals(
                        new int[] {0, 0},
                        gridProvider.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_TABLE_COLUMN_HEADERS_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.ROW_OR_COLUMN_MAJOR_ROW, gridProvider.rowOrColumnMajor());
                assertEquals(
                        WindowsAutomationProvider.ROW_OR_COLUMN_MAJOR_ROW,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TABLE_ROW_OR_COLUMN_MAJOR_PROPERTY_ID
                        )
                );
                assertTrue(gridProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SPREADSHEET_PATTERN_ID
                ));
                assertEquals(
                        1,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SPREADSHEET_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SPREADSHEET_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        gridProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_STYLES_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(gridProvider.invokeSpreadsheetItem("People"));
                assertFalse(gridProvider.invokeSpreadsheetItem("missing"));
            }
            SemanticsNode cellNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TABLE_CELL)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider cellProvider = window.automationProvider(cellNode)) {
                assertTrue(cellProvider.invokePatternProvider(WindowsAutomationProvider.UIA_GRID_ITEM_PATTERN_ID));
                assertEquals(
                        1,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_GRID_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TABLE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(cellProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TABLE_ITEM_PATTERN_ID));
                assertEquals(
                        1,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TABLE_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SELECTION_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(0, cellProvider.gridItemRow());
                assertEquals(
                        0,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_GRID_ITEM_ROW_PROPERTY_ID
                        )
                );
                assertEquals(1, cellProvider.gridItemColumn());
                assertEquals(
                        1,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_GRID_ITEM_COLUMN_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_GRID_ITEM_ROW_SPAN_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_GRID_ITEM_COLUMN_SPAN_PROPERTY_ID
                        )
                );
                assertTrue(
                        cellProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_GRID_ITEM_CONTAINING_GRID_PROPERTY_ID
                        )
                );
                assertEquals(0, cellProvider.invokeRowHeaderItems());
                assertArrayEquals(
                        new int[] {0, 0},
                        cellProvider.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_TABLE_ITEM_ROW_HEADER_ITEMS_PROPERTY_ID
                        )
                );
                assertArrayEquals(
                        new int[] {0, 0},
                        cellProvider.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_TABLE_ITEM_COLUMN_HEADER_ITEMS_PROPERTY_ID
                        )
                );
                assertTrue(cellProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SPREADSHEET_ITEM_PATTERN_ID
                ));
                assertEquals(
                        1,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SPREADSHEET_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SPREADSHEET_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals("=r0c1", cellProvider.spreadsheetFormula());
                assertEquals(
                        "=r0c1",
                        cellProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_SPREADSHEET_ITEM_FORMULA_PROPERTY_ID
                        )
                );
                assertTrue(
                        cellProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_SPREADSHEET_ITEM_ANNOTATION_OBJECTS_PROPERTY_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ANNOTATION_TYPE_COMMENT,
                        cellProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SPREADSHEET_ITEM_ANNOTATION_TYPES_PROPERTY_ID
                        )
                );
            }
            SemanticsNode listNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.LIST)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider scrollProvider = window.automationProvider(listNode)) {
                assertTrue(scrollProvider.invokePatternProvider(WindowsAutomationProvider.UIA_SCROLL_PATTERN_ID));
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SCROLL_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TOGGLE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(scrollProvider.verticallyScrollable());
                assertTrue(scrollProvider.horizontallyScrollable());
                assertEquals(25.0, scrollProvider.verticalScrollPercent());
                assertEquals(10.0, scrollProvider.horizontalScrollPercent());
                assertEquals(
                        10.0,
                        scrollProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_SCROLL_HORIZONTAL_SCROLL_PERCENT_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(
                        25.0,
                        scrollProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_SCROLL_VERTICAL_SCROLL_PERCENT_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(
                        30.0,
                        scrollProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_SCROLL_HORIZONTAL_VIEW_SIZE_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SCROLL_HORIZONTALLY_SCROLLABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        20.0,
                        scrollProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_SCROLL_VERTICAL_VIEW_SIZE_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SCROLL_VERTICALLY_SCROLLABLE_PROPERTY_ID
                        )
                );
                assertEquals(40.0, scrollProvider.setVerticalScrollPercent(40.0));
                assertEquals(
                        40.0,
                        scrollProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_SCROLL_VERTICAL_SCROLL_PERCENT_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(50.0, scrollProvider.scrollVertical(WindowsAutomationProvider.SCROLL_AMOUNT_SMALL_INCREMENT));
                assertEquals(20.0, scrollProvider.setHorizontalScrollPercent(20.0));
                assertEquals(
                        20.0,
                        scrollProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_SCROLL_HORIZONTAL_SCROLL_PERCENT_PROPERTY_ID
                        ),
                        0.001
                );
                assertEquals(30.0, scrollProvider.scrollHorizontal(WindowsAutomationProvider.SCROLL_AMOUNT_SMALL_INCREMENT));
                assertEquals(
                        30.0,
                        scrollProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_SCROLL_HORIZONTAL_SCROLL_PERCENT_PROPERTY_ID
                        ),
                        0.001
                );
                assertTrue(scrollProvider.invokePatternProvider(WindowsAutomationProvider.UIA_SCROLL_ITEM_PATTERN_ID));
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SCROLL_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_GRID_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(1, scrollProvider.invokeScrollItem());
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_VIRTUALIZED_ITEM_PATTERN_ID
                ));
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_VIRTUALIZED_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_ITEM_CONTAINER_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TEXT_PATTERN2_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(1, scrollProvider.invokeVirtualizedItem());
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_ITEM_CONTAINER_PATTERN_ID
                ));
                assertTrue(scrollProvider.invokeFindItemByProperty("Items"));
                assertFalse(scrollProvider.invokeFindItemByProperty("missing"));
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_MULTIPLE_VIEW_PATTERN_ID
                ));
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_MULTIPLE_VIEW_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SELECTION_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TABLE_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(1, scrollProvider.currentView());
                assertArrayEquals(
                        new int[] {1, 1},
                        scrollProvider.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_MULTIPLE_VIEW_SUPPORTED_VIEWS_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_MULTIPLE_VIEW_CURRENT_VIEW_PROPERTY_ID
                        )
                );
                assertEquals("List", scrollProvider.viewName(1));
                assertEquals(2, scrollProvider.setCurrentView(2));
                assertEquals(2, scrollProvider.currentView());
                assertEquals(
                        2,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_MULTIPLE_VIEW_CURRENT_VIEW_PROPERTY_ID
                        )
                );
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_DROP_TARGET_PATTERN_ID
                ));
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_DROP_TARGET_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_CUSTOM_NAVIGATION_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals("move", scrollProvider.dropTargetEffect());
                assertTrue(scrollProvider.invokePatternProvider(WindowsAutomationProvider.UIA_DRAG_PATTERN_ID));
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_DRAG_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TRANSFORM_PATTERN2_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TRANSFORM2_CAN_ZOOM_PROPERTY_ID
                        )
                );
                assertFalse(scrollProvider.isGrabbed());
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_DRAG_IS_GRABBED_PROPERTY_ID
                        )
                );
                assertTrue(
                        scrollProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_DRAG_GRABBED_ITEMS_PROPERTY_ID
                        )
                );
                assertEquals("copy", scrollProvider.dropEffect());
                assertEquals(
                        "copy",
                        scrollProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_DRAG_DROP_EFFECT_PROPERTY_ID
                        )
                );
                assertEquals(
                        "copy",
                        scrollProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_DRAG_DROP_EFFECTS_PROPERTY_ID
                        )
                );
                assertEquals(
                        "move",
                        scrollProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_DROP_TARGET_DROP_TARGET_EFFECT_PROPERTY_ID
                        )
                );
                assertEquals(
                        "move",
                        scrollProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_DROP_TARGET_DROP_TARGET_EFFECTS_PROPERTY_ID
                        )
                );
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SELECTION_PATTERN_ID
                ));
                assertTrue(scrollProvider.canSelectMultiple());
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SELECTION_CAN_SELECT_MULTIPLE_PROPERTY_ID
                        )
                );
                assertTrue(
                        scrollProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_SELECTION_SELECTION_PROPERTY_ID
                        )
                );
                assertFalse(scrollProvider.isSelectionRequired());
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SELECTION_IS_SELECTION_REQUIRED_PROPERTY_ID
                        )
                );
                assertTrue(scrollProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_SELECTION_PATTERN2_ID
                ));
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SELECTION_PATTERN2_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_DOCK_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TRANSFORM_CAN_MOVE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TRANSFORM_CAN_RESIZE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TRANSFORM_CAN_ROTATE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_CAN_MAXIMIZE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_CAN_MINIMIZE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_IS_MODAL_PROPERTY_ID
                        )
                );
                assertEquals(1, scrollProvider.selectionItemCount());
                assertEquals(
                        1,
                        scrollProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_SELECTION2_ITEM_COUNT_PROPERTY_ID
                        )
                );
                assertTrue(scrollProvider.invokeCurrentSelectedItem());
                assertTrue(scrollProvider.invokeFirstSelectedItem());
                assertTrue(scrollProvider.invokeLastSelectedItem());
                assertTrue(
                        scrollProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_SELECTION2_CURRENT_SELECTED_ITEM_PROPERTY_ID
                        )
                );
                assertTrue(
                        scrollProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_SELECTION2_FIRST_SELECTED_ITEM_PROPERTY_ID
                        )
                );
                assertTrue(
                        scrollProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_SELECTION2_LAST_SELECTED_ITEM_PROPERTY_ID
                        )
                );
            }
            SemanticsNode dialogNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.DIALOG)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider windowProvider = window.automationProvider(dialogNode)) {
                assertTrue(windowProvider.invokePatternProvider(WindowsAutomationProvider.UIA_WINDOW_PATTERN_ID));
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_WINDOW_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.canMaximize());
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_CAN_MAXIMIZE_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.canMinimize());
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_CAN_MINIMIZE_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.isModal());
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_IS_MODAL_PROPERTY_ID
                        )
                );
                assertFalse(windowProvider.isTopmost());
                assertEquals(
                        0,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_IS_TOPMOST_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.waitForInputIdle(0));
                assertEquals(
                        WindowsAutomationProvider.WINDOW_INTERACTION_READY,
                        windowProvider.windowInteractionState()
                );
                assertEquals(
                        WindowsAutomationProvider.WINDOW_INTERACTION_READY,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_WINDOW_INTERACTION_STATE_PROPERTY_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.WINDOW_VISUAL_STATE_NORMAL,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_WINDOW_VISUAL_STATE_PROPERTY_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.WINDOW_VISUAL_STATE_MAXIMIZED,
                        windowProvider.setWindowVisualState(WindowsAutomationProvider.WINDOW_VISUAL_STATE_MAXIMIZED)
                );
                assertEquals(
                        WindowsAutomationProvider.WINDOW_VISUAL_STATE_MAXIMIZED,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_WINDOW_WINDOW_VISUAL_STATE_PROPERTY_ID
                        )
                );
                assertEquals(1, windowProvider.closeWindow());
                assertTrue(windowProvider.invokePatternProvider(WindowsAutomationProvider.UIA_DOCK_PATTERN_ID));
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_DOCK_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SELECTION_PATTERN2_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.DOCK_POSITION_NONE,
                        windowProvider.dockPosition()
                );
                assertEquals(
                        WindowsAutomationProvider.DOCK_POSITION_NONE,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_DOCK_DOCK_POSITION_PROPERTY_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.DOCK_POSITION_TOP,
                        windowProvider.setDockPosition(WindowsAutomationProvider.DOCK_POSITION_TOP)
                );
                assertEquals(
                        WindowsAutomationProvider.DOCK_POSITION_TOP,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_DOCK_DOCK_POSITION_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TRANSFORM_PATTERN_ID));
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TRANSFORM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TEXT_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_LEGACY_IACCESSIBLE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.canMove());
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TRANSFORM_CAN_MOVE_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.canResize());
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TRANSFORM_CAN_RESIZE_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.canRotate());
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TRANSFORM_CAN_ROTATE_PROPERTY_ID
                        )
                );
                assertEquals(12.0, windowProvider.moveTransform(12.0, 24.0));
                assertEquals(80.0, windowProvider.resizeTransform(80.0, 40.0));
                assertEquals(15.0, windowProvider.rotateTransform(15.0));
                assertTrue(windowProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_TRANSFORM_PATTERN2_ID
                ));
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TRANSFORM_PATTERN2_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_DRAG_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.canZoom());
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_TRANSFORM2_CAN_ZOOM_PROPERTY_ID
                        )
                );
                assertEquals(1.0, windowProvider.zoomLevel());
                assertEquals(2.5, windowProvider.zoomTransform(2.5));
                assertEquals(2.5, windowProvider.zoomLevel());
                assertEquals(3.5, windowProvider.zoomByUnit(WindowsAutomationProvider.ZOOM_UNIT_LARGE_INCREMENT));
                assertEquals(3.5, windowProvider.zoomLevel());
                assertEquals(0.5, windowProvider.zoomMinimum());
                assertEquals(4.0, windowProvider.zoomMaximum());
                assertEquals(
                        3.5,
                        windowProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_TRANSFORM2_ZOOM_LEVEL_PROPERTY_ID
                        ),
                        0.0001
                );
                assertEquals(
                        0.5,
                        windowProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_TRANSFORM2_ZOOM_MINIMUM_PROPERTY_ID
                        ),
                        0.0001
                );
                assertEquals(
                        4.0,
                        windowProvider.invokePropertyValueDouble(
                                WindowsAutomationProvider.UIA_TRANSFORM2_ZOOM_MAXIMUM_PROPERTY_ID
                        ),
                        0.0001
                );
                assertTrue(windowProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_CUSTOM_NAVIGATION_PATTERN_ID
                ));
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_CUSTOM_NAVIGATION_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_DROP_TARGET_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(windowProvider.invokeNavigate(WindowsAutomationProvider.NAVIGATE_DIRECTION_PARENT));
                assertFalse(windowProvider.invokeNavigate(1));
                assertTrue(windowProvider.invokePatternProvider(
                        WindowsAutomationProvider.UIA_OBJECT_MODEL_PATTERN_ID
                ));
                assertEquals(
                        1,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_OBJECT_MODEL_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        windowProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_ANNOTATION_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
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
                        1,
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_ANNOTATION_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_OBJECT_MODEL_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ANNOTATION_TYPE_COMMENT,
                        statusProvider.annotationTypeId()
                );
                assertEquals(
                        WindowsAutomationProvider.ANNOTATION_TYPE_COMMENT,
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_ANNOTATION_ANNOTATION_TYPE_ID_PROPERTY_ID
                        )
                );
                assertEquals("Comment", statusProvider.annotationTypeName());
                assertEquals(
                        "Comment",
                        statusProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_ANNOTATION_ANNOTATION_TYPE_NAME_PROPERTY_ID
                        )
                );
                assertEquals("Himari", statusProvider.annotationAuthor());
                assertEquals(
                        "Himari",
                        statusProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_ANNOTATION_AUTHOR_PROPERTY_ID
                        )
                );
                assertEquals("2026-08-17", statusProvider.annotationDateTime());
                assertEquals(
                        "2026-08-17",
                        statusProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_ANNOTATION_DATE_TIME_PROPERTY_ID
                        )
                );
                assertTrue(statusProvider.invokeAnnotationTarget());
                assertTrue(
                        statusProvider.invokePropertyValueUnknown(
                                WindowsAutomationProvider.UIA_ANNOTATION_TARGET_PROPERTY_ID
                        )
                );
                assertTrue(statusProvider.invokePatternProvider(WindowsAutomationProvider.UIA_STYLES_PATTERN_ID));
                assertEquals(
                        1,
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_STYLES_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_SPREADSHEET_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(WindowsAutomationProvider.STYLE_ID_NORMAL, statusProvider.styleId());
                assertEquals(
                        WindowsAutomationProvider.STYLE_ID_NORMAL,
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_STYLES_STYLE_ID_PROPERTY_ID
                        )
                );
                assertEquals("Normal", statusProvider.styleName());
                assertEquals(
                        "Normal",
                        statusProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_STYLES_STYLE_NAME_PROPERTY_ID
                        )
                );
                assertEquals(
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_FILL_COLOR_PROPERTY_ID
                        ),
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_STYLES_FILL_COLOR_PROPERTY_ID
                        )
                );
                assertEquals(
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_FILL_TYPE_PROPERTY_ID
                        ),
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_STYLES_FILL_PATTERN_STYLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.STYLE_SHAPE_RECTANGLE,
                        statusProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_STYLES_SHAPE_PROPERTY_ID
                        )
                );
                assertEquals(
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_OUTLINE_COLOR_PROPERTY_ID
                        ),
                        statusProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_STYLES_FILL_PATTERN_COLOR_PROPERTY_ID
                        )
                );
                assertEquals(
                        statusProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_ARIA_PROPERTIES_PROPERTY_ID
                        ),
                        statusProvider.invokePropertyValueString(
                                WindowsAutomationProvider.UIA_STYLES_EXTENDED_PROPERTIES_PROPERTY_ID
                        )
                );
            }
            SemanticsNode fieldNode = valueTree.semantics().nodes().stream()
                    .filter(node -> node.role() == SemanticsRole.TEXT_FIELD)
                    .findFirst()
                    .orElseThrow();
            try (WindowsAutomationProvider textProvider = window.automationProvider(fieldNode)) {
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_VALUE_PATTERN_ID));
                assertEquals(
                        1,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_VALUE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_INVOKE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
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
                    assertEquals(
                            1,
                            readOnly.invokePropertyValue(WindowsAutomationProvider.UIA_VALUE_IS_READ_ONLY_PROPERTY_ID)
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
                assertEquals(
                        1,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TEXT_CHILD_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_DRAG_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(textProvider.invokeTextContainer());
                assertTrue(textProvider.invokeTextChildRange());
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_EDIT_PATTERN_ID));
                assertEquals(
                        1,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TEXT_EDIT_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_CUSTOM_NAVIGATION_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(textProvider.invokeActiveComposition());
                assertTrue(textProvider.invokeConversionTarget());
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_PATTERN2_ID));
                assertEquals(
                        1,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TEXT_PATTERN2_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_ITEM_CONTAINER_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_VIRTUALIZED_ITEM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertTrue(textProvider.invokeCaretRange());
                assertTrue(textProvider.invokeRangeFromAnnotation());
                assertTrue(textProvider.invokePatternProvider(WindowsAutomationProvider.UIA_TEXT_PATTERN_ID));
                assertEquals(
                        1,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TEXT_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_TRANSFORM_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
                assertEquals(
                        1,
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_IS_LEGACY_IACCESSIBLE_PATTERN_AVAILABLE_PROPERTY_ID
                        )
                );
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
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_IS_HIDDEN_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_ANIMATION_STYLE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_BACKGROUND_COLOR,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_BACKGROUND_COLOR_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_FONT_NAME,
                        textProvider.invokeGetAttributeValueString(
                                WindowsAutomationProvider.UIA_FONT_NAME_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_FONT_SIZE,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_FONT_SIZE_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_FONT_WEIGHT,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_FONT_WEIGHT_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_FOREGROUND_COLOR,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_FOREGROUND_COLOR_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_BULLET_STYLE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_CAP_STYLE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        textProvider.invokePropertyValue(
                                WindowsAutomationProvider.UIA_CULTURE_PROPERTY_ID
                        ),
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_CULTURE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_HORIZONTAL_TEXT_ALIGNMENT_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_IS_ITALIC_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_IS_READ_ONLY_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_INDENT_FIRST_LINE,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_INDENTATION_FIRST_LINE_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_INDENT_LEADING,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_INDENTATION_LEADING_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_INDENT_TRAILING,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_INDENTATION_TRAILING_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_IS_SUBSCRIPT_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_IS_SUPERSCRIPT_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_MARGIN,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_MARGIN_BOTTOM_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_MARGIN,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_MARGIN_LEADING_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_MARGIN,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_MARGIN_TOP_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_MARGIN,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_MARGIN_TRAILING_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_OUTLINE_STYLES_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_FOREGROUND_COLOR,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_OVERLINE_COLOR_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_OVERLINE_STYLE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_FOREGROUND_COLOR,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_STRIKETHROUGH_COLOR_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_STRIKETHROUGH_STYLE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_TABS_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_TEXT_FLOW_DIRECTIONS_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_FOREGROUND_COLOR,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_UNDERLINE_COLOR_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_UNDERLINE_STYLE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ANNOTATION_TYPE_COMMENT,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_ANNOTATION_TYPES_ATTRIBUTE_ID
                        )
                );
                assertTrue(
                        textProvider.invokeGetAttributeValueUnknown(
                                WindowsAutomationProvider.UIA_ANNOTATION_OBJECTS_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        "Normal",
                        textProvider.invokeGetAttributeValueString(
                                WindowsAutomationProvider.UIA_STYLE_NAME_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.STYLE_ID_NORMAL,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_STYLE_ID_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        "",
                        textProvider.invokeGetAttributeValueString(
                                WindowsAutomationProvider.UIA_LINK_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_IS_ACTIVE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_SELECTION_ACTIVE_END_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_CARET_POSITION_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        0,
                        textProvider.invokeGetAttributeValueInt(
                                WindowsAutomationProvider.UIA_CARET_BIDI_MODE_ATTRIBUTE_ID
                        )
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_LINE_SPACING,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_LINE_SPACING_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_PARAGRAPH_SPACING,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_BEFORE_PARAGRAPH_SPACING_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        WindowsAutomationProvider.ATTRIBUTE_PARAGRAPH_SPACING,
                        textProvider.invokeGetAttributeValueDouble(
                                WindowsAutomationProvider.UIA_AFTER_PARAGRAPH_SPACING_ATTRIBUTE_ID
                        ),
                        0.0001
                );
                assertEquals(
                        "",
                        textProvider.invokeGetAttributeValueString(
                                WindowsAutomationProvider.UIA_SAY_AS_INTERPRET_AS_ATTRIBUTE_ID
                        )
                );
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
