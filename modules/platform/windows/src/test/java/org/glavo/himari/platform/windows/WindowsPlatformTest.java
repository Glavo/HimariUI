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

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    /// Queries generated `GetKeyState` / `GetAsyncKeyState` on the production key and pointer path.
    @Test
    void queriesLiveModifierStateThroughGeneratedUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "KeyState", 32.0, 32.0);
            platform.pump();
            short shift = window.keyState(0x10);
            short asyncShift = window.asyncKeyState(0x10);
            short asyncControl = window.asyncKeyState(0x11);
            assertEquals(shift, window.keyState(0x10));
            assertEquals(asyncShift, window.asyncKeyState(0x10));
            byte[] snapshot = new byte[256];
            assertTrue(window.copyKeyboardState(snapshot));
            assertEquals((shift & 0x8000) != 0, (snapshot[0x10] & 0x80) != 0);
            window.postVirtualKey(true, 0x10);
            window.postVirtualKey(true, 0x0D);
            platform.pump();
            List<org.glavo.himari.layout.input.KeyEvent> keys = window.takeKeyEvents();
            assertEquals(1, keys.size());
            assertEquals(LogicalKey.ENTER, keys.getFirst().key());
            assertTrue(keys.getFirst().shift(), "latched VK_SHIFT plus GetKeyState must mark shift");
            WindowsNativeWindow.PenAxes axes = new WindowsNativeWindow.PenAxes(0.25f, 0.0f, 0.0f, 0.0f);
            window.postPen(PointerEventType.DOWN, 4, 5, 7, axes);
            platform.pump();
            PointerEvent pointer = window.takePointerEvents().getLast();
            int expectedAsync = 0;
            if ((asyncShift & 0x8000) != 0 || (window.asyncKeyState(0x10) & 0x8000) != 0) {
                expectedAsync |= WindowsNativeWindow.POINTER_MOD_SHIFT;
            }
            if ((asyncControl & 0x8000) != 0 || (window.asyncKeyState(0x11) & 0x8000) != 0) {
                expectedAsync |= WindowsNativeWindow.POINTER_MOD_CTRL;
            }
            assertEquals(expectedAsync, pointer.keyStates() & (WindowsNativeWindow.POINTER_MOD_SHIFT
                    | WindowsNativeWindow.POINTER_MOD_CTRL));
        } finally {
            platform.close();
        }
    }

    /// Delivers `WM_UNICHAR` and `WM_DEADCHAR` through the production WndProc.
    @Test
    void deliversUnicharAndDeadCharThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Unichar", 32.0, 32.0);
            platform.pump();
            assertTrue(window.supportsUnichar());
            window.postDeadChar('^');
            platform.pump();
            assertEquals("^", window.ime().composition());
            assertFalse(window.ime().committed());
            window.postUnichar('e');
            platform.pump();
            assertEquals("e", window.ime().surroundingText());
            assertTrue(window.ime().committed());
            window.postUnichar(0x1F600);
            platform.pump();
            assertTrue(window.ime().surroundingText().contains(new String(Character.toChars(0x1F600))));
            window.postSysDeadChar('`');
            platform.pump();
            assertEquals("`", window.ime().composition());
            window.postSysChar('a');
            platform.pump();
            assertTrue(window.ime().surroundingText().contains("a"));
            window.postVirtualKey(true, 0x0D);
            platform.pump();
            List<org.glavo.himari.layout.input.KeyEvent> mapped = window.takeKeyEvents();
            assertEquals(1, mapped.size());
            assertEquals(LogicalKey.ENTER, mapped.getFirst().key());
            assertTrue(mapped.getFirst().scanCode() > 0, "MapVirtualKeyW must fill a missing scan code");
        } finally {
            platform.close();
        }
    }

    /// Translates virtual keys through generated `ToUnicodeW`, `VkKeyScanW`, `GetKeyNameTextW`, and `GetKeyboardLayout`.
    @Test
    void translatesVirtualKeysThroughGeneratedUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ToUnicode", 32.0, 32.0);
            platform.pump();
            window.postVirtualKey(true, 0x20);
            platform.pump();
            List<org.glavo.himari.layout.input.KeyEvent> keys = window.takeKeyEvents();
            assertEquals(1, keys.size());
            assertEquals(LogicalKey.SPACE, keys.getFirst().key());
            assertEquals(" ", keys.getFirst().text());
            assertEquals(" ", window.takeTranslatedCharacters());
            assertTrue(window.rawInputRegistered(), "HWND creation must call RegisterRawInputDevices");
            assertTrue(
                    window.lastRegisteredRawInputDevices() != Integer.MIN_VALUE,
                    "HWND creation must call GetRegisteredRawInputDevices"
            );
            window.sendMessage(0x00FF, 0L, 0L);
            assertTrue(
                    window.lastRawInputBytes() != Integer.MIN_VALUE,
                    "WM_INPUT must call GetRawInputData"
            );
            assertTrue(
                    window.lastRawInputBufferBytes() != Integer.MIN_VALUE,
                    "WM_INPUT must call GetRawInputBuffer"
            );
            assertTrue(
                    window.lastRawInputBufferPackets() != Integer.MIN_VALUE,
                    "WM_INPUT must record GetRawInputBuffer packets"
            );
            assertTrue(
                    window.lastRawInputDeviceInfoBytes() != Integer.MIN_VALUE,
                    "WM_INPUT must call GetRawInputDeviceInfoW"
            );
            assertTrue(
                    window.lastRawInputDeviceListCount() != Integer.MIN_VALUE,
                    "HWND creation must call GetRawInputDeviceList"
            );
            window.postInputDeviceChange(1);
            platform.pump();
            assertEquals(1, window.lastInputDeviceChange());
            assertTrue(
                    window.lastMouseInPointerEnabled() != Integer.MIN_VALUE,
                    "HWND creation must call EnableMouseInPointer/IsMouseInPointerEnabled"
            );
            assertTrue(
                    window.lastPointerDeviceCount() != Integer.MIN_VALUE,
                    "HWND creation must call GetPointerDevices"
            );
            assertTrue(window.lastKeyboardLayout() != 0L, "GetKeyboardLayout must return a non-NULL HKL");
            assertFalse(window.lastKeyName().isEmpty(), "GetKeyNameTextW must name VK_SPACE");
            if ((window.lastCharVirtualKeyScan() & 0xFF) != 0x20) {
                window.postChar(' ');
                platform.pump();
            }
            assertEquals(0x20, window.lastCharVirtualKeyScan() & 0xFF);
            assertEquals(0x20, window.scanVirtualKey(' ') & 0xFF);
        } finally {
            platform.close();
        }
    }

    /// Reads IMM32 composition through generated `ImmGetCompositionStringW` on `WM_IME_COMPOSITION`.
    @Test
    void readsImmCompositionStringThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ImmComposition", 32.0, 32.0);
            platform.pump();
            assertTrue(
                    window.lastAssociateContext() != -1L,
                    "HWND create must call ImmAssociateContext"
            );
            assertTrue(
                    window.lastAssociateContextExResult() != Integer.MIN_VALUE,
                    "HWND create must call ImmAssociateContextEx"
            );
            assertTrue(
                    window.lastCreateContext() != -1L,
                    "HWND create must call ImmCreateContext"
            );
            assertTrue(
                    window.lastClipboardFormatCount() != Integer.MIN_VALUE,
                    "HWND create must call CountClipboardFormats"
            );
            assertTrue(
                    window.lastClipboardSequence() != Integer.MIN_VALUE,
                    "HWND create must call GetClipboardSequenceNumber"
            );
            assertTrue(
                    window.lastClipboardOwner() != -1L,
                    "HWND create must call GetClipboardOwner"
            );
            assertTrue(
                    window.lastOpenClipboardWindow() != -1L,
                    "HWND create must call GetOpenClipboardWindow"
            );
            assertTrue(
                    window.lastClipboardUnicodeAvailable() != Integer.MIN_VALUE,
                    "HWND create must call IsClipboardFormatAvailable"
            );
            assertTrue(
                    window.lastPriorityClipboardFormat() != Integer.MIN_VALUE,
                    "HWND create must call GetPriorityClipboardFormat"
            );
            assertTrue(
                    window.lastEnumClipboardFormat() != Integer.MIN_VALUE,
                    "HWND create must call EnumClipboardFormats"
            );
            assertTrue(
                    window.lastEnumClipboardFormatCount() != Integer.MIN_VALUE,
                    "HWND create must walk EnumClipboardFormats"
            );
            assertTrue(
                    window.lastClipboardFormatNameChars() != Integer.MIN_VALUE,
                    "HWND create must call GetClipboardFormatNameW"
            );
            assertTrue(
                    window.lastUpdatedClipboardFormatsResult() != Integer.MIN_VALUE,
                    "HWND create must call GetUpdatedClipboardFormats"
            );
            assertTrue(
                    window.lastUpdatedClipboardFormatCount() != Integer.MIN_VALUE,
                    "HWND create must read GetUpdatedClipboardFormats count"
            );
            assertTrue(
                    window.lastAddClipboardFormatListenerResult() != Integer.MIN_VALUE,
                    "HWND create must call AddClipboardFormatListener"
            );
            window.sendMessage(0x0281, 1L, 0L);
            assertEquals(1, window.lastImeSetContext());
            window.sendMessage(0x0285, 1L, 0L);
            assertEquals(1, window.lastImeSelect());
            window.postImeStart();
            platform.pump();
            assertTrue(window.imeActive());
            window.ime().setConversionStatus(0, 0);
            window.ime().setOpenStatus(true);
            platform.pump();
            assertTrue(
                    window.lastSetConversionStatusResult() != Integer.MIN_VALUE,
                    "ImeSession conversion write-back must call ImmSetConversionStatus"
            );
            assertTrue(
                    window.lastSetOpenStatusResult() != Integer.MIN_VALUE,
                    "ImeSession open write-back must call ImmSetOpenStatus"
            );
            window.ime().setCompositionFontFace("Segoe UI");
            platform.pump();
            assertTrue(
                    window.lastSetCompositionFontResult() != Integer.MIN_VALUE,
                    "ImeSession font write-back must call ImmSetCompositionFontW"
            );
            assertTrue(
                    window.lastCompositionFontResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetCompositionFontW"
            );
            assertTrue(
                    window.lastConversionStatus() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetConversionStatus"
            );
            assertTrue(
                    window.lastImeOpenStatus() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetOpenStatus"
            );
            assertTrue(
                    window.lastCompositionWindowResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetCompositionWindow"
            );
            assertTrue(
                    window.lastImeVirtualKey() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetVirtualKey"
            );
            assertTrue(
                    window.lastImeIsIme() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmIsIME"
            );
            assertTrue(
                    window.lastImeMenuItemCount() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetImeMenuItemsW"
            );
            assertTrue(
                    window.lastImeEscapeResult() != Long.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmEscapeW"
            );
            assertTrue(
                    window.lastImeDescriptionChars() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetDescriptionW"
            );
            assertTrue(
                    window.lastImeProperty() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetProperty"
            );
            assertTrue(
                    window.lastDefaultImeWnd() != -1L,
                    "WM_IME_STARTCOMPOSITION must call ImmGetDefaultIMEWnd"
            );
            Objects.requireNonNull(window.lastImeDescription(), "ImmGetDescriptionW");
            assertTrue(
                    window.lastStatusWindowPosResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetStatusWindowPos"
            );
            assertTrue(
                    window.lastImeHotKeyResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetHotKey"
            );
            assertTrue(
                    window.lastSetHotKeyResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmSetHotKey"
            );
            assertTrue(
                    window.lastConversionReverseBytes() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must fill ImmGetConversionListW GCL_REVERSECONVERSION"
            );
            assertTrue(
                    window.lastImeIsUiMessage() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmIsUIMessageW"
            );
            assertTrue(
                    window.lastRegisterWordStyleCount() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetRegisterWordStyleW"
            );
            assertTrue(
                    window.lastEnumInputContextResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmEnumInputContext"
            );
            assertTrue(
                    window.lastEnumRegisterWordCount() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmEnumRegisterWordW"
            );
            assertTrue(
                    window.lastImmRequestMessageResult() != Long.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmRequestMessageW"
            );
            assertTrue(
                    window.lastRegisterWordResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmRegisterWordW"
            );
            assertTrue(
                    window.lastUnregisterWordResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmUnregisterWordW"
            );
            assertTrue(
                    window.lastConversionListBytes() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetConversionListW"
            );
            assertTrue(
                    window.lastConversionReverseLength() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmGetConversionListW GCL_REVERSE_LENGTH"
            );
            assertTrue(
                    window.lastSimulateHotKeyResult() != Integer.MIN_VALUE,
                    "WM_IME_STARTCOMPOSITION must call ImmSimulateHotKey"
            );
            window.sendMessage(0x0283, WindowsNativeWindow.IMC_GETSTATUSWINDOWPOS, 0L);
            assertEquals(WindowsNativeWindow.IMC_GETSTATUSWINDOWPOS, window.lastImeControl());
            Objects.requireNonNull(window.lastImeFileName(), "ImmGetIMEFileNameW");
            window.postImeComposition(WindowsNativeWindow.GCS_COMPSTR);
            platform.pump();
            assertTrue(
                    window.lastCompositionStringBytes() != Integer.MIN_VALUE,
                    "WndProc must call ImmGetCompositionStringW"
            );
            assertTrue(
                    window.lastCompositionCursor() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_CURSORPOS"
            );
            assertTrue(
                    window.lastCompositionAttrBytes() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_COMPATTR"
            );
            assertEquals(
                    window.lastCompositionCursor() < 0 ? -1 : window.lastCompositionCursor(),
                    window.ime().compositionCursor()
            );
            if (window.lastCompositionAttrBytes() <= 0) {
                assertEquals(0, window.ime().compositionAttributes().length);
            } else {
                assertEquals(window.lastCompositionAttrBytes(), window.ime().compositionAttributes().length);
            }
            assertTrue(
                    window.lastCompositionReadingBytes() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_COMPREADSTR"
            );
            assertTrue(
                    window.lastCompositionClauseBytes() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_COMPCLAUSE"
            );
            assertTrue(
                    window.lastResultReadingBytes() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_RESULTREADSTR"
            );
            assertTrue(
                    window.lastResultClauseBytes() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_RESULTCLAUSE"
            );
            assertTrue(
                    window.lastCompositionDeltaStart() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_DELTASTART"
            );
            assertEquals(
                    window.lastCompositionDeltaStart() < 0 ? -1 : window.lastCompositionDeltaStart(),
                    window.ime().compositionDeltaStart()
            );
            if (window.lastCompositionClauseBytes() <= 0) {
                assertEquals(0, window.ime().compositionClause().length);
            } else {
                assertEquals(window.lastCompositionClauseBytes() / 4, window.ime().compositionClause().length);
            }
            assertTrue(
                    window.lastCompositionReadingAttrBytes() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_COMPREADATTR"
            );
            assertTrue(
                    window.lastCompositionReadingClauseBytes() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_COMPREADCLAUSE"
            );
            assertTrue(
                    window.lastResultReadingClauseBytes() != Integer.MIN_VALUE,
                    "WM_IME_COMPOSITION must call ImmGetCompositionStringW GCS_RESULTREADCLAUSE"
            );
            boolean written = window.setCompositionString("ni");
            window.postImeComposition(WindowsNativeWindow.GCS_COMPSTR);
            platform.pump();
            if (written) {
                assertEquals("ni", window.ime().composition());
            }
            window.postImeChar('z');
            platform.pump();
            assertTrue(window.ime().surroundingText().contains("z"));
            window.postImeNotify(WindowsNativeWindow.IMN_OPENCANDIDATE, 0);
            platform.pump();
            assertTrue(
                    window.lastCandidateCount() != Integer.MIN_VALUE,
                    "WM_IME_NOTIFY must call ImmGetCandidateListW"
            );
            assertTrue(
                    window.lastCandidateListCount() != Integer.MIN_VALUE,
                    "WM_IME_NOTIFY must call ImmGetCandidateListCountW"
            );
            assertTrue(
                    window.lastCandidateWindowResult() != Integer.MIN_VALUE,
                    "WM_IME_NOTIFY must call ImmGetCandidateWindow"
            );
            window.postImeNotify(WindowsNativeWindow.IMN_GUIDELINE, 0);
            platform.pump();
            assertTrue(
                    window.lastGuideLineBytes() != Integer.MIN_VALUE,
                    "WM_IME_NOTIFY IMN_GUIDELINE must call ImmGetGuideLineW"
            );
            assertEquals("", window.ime().guideline());
            window.ime().setSurroundingText("hello", 5);
            window.ime().setCandidateRectangle(4.0f, 6.0f, 8.0f, 12.0f);
            platform.pump();
            assertTrue(
                    window.lastSetStatusWindowPosResult() != Integer.MIN_VALUE,
                    "ImeSession candidate rectangle write-back must call ImmSetStatusWindowPos"
            );
            long documentBytes = window.sendImeRequest(WindowsNativeWindow.IMR_DOCUMENTFEED);
            assertEquals(WindowsNativeWindow.IMR_DOCUMENTFEED, window.lastImeRequest());
            assertTrue(
                    documentBytes > 32L && window.lastImeRequestBytes() > 32,
                    "IMR_DOCUMENTFEED must return a RECONVERTSTRING plus document bytes"
            );
            assertTrue(window.sendImeRequest(WindowsNativeWindow.IMR_RECONVERTSTRING) > 32L);
            assertEquals(WindowsNativeWindow.IMR_RECONVERTSTRING, window.lastImeRequest());
            window.sendImeRequest(WindowsNativeWindow.IMR_CONFIRMRECONVERTSTRING);
            assertEquals(WindowsNativeWindow.IMR_CONFIRMRECONVERTSTRING, window.lastImeRequest());
            assertTrue(window.sendImeRequest(WindowsNativeWindow.IMR_QUERYCHARPOSITION) >= 36L);
            assertEquals(WindowsNativeWindow.IMR_QUERYCHARPOSITION, window.lastImeRequest());
            assertTrue(window.lastImeRequestBytes() >= 36);
            assertTrue(window.sendImeRequest(WindowsNativeWindow.IMR_COMPOSITIONWINDOW) >= 28L);
            assertEquals(WindowsNativeWindow.IMR_COMPOSITIONWINDOW, window.lastImeRequest());
            assertTrue(window.sendImeRequest(WindowsNativeWindow.IMR_CANDIDATEWINDOW) >= 32L);
            assertTrue(window.sendImeRequest(WindowsNativeWindow.IMR_COMPOSITIONFONT) >= 92L);
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment position = arena.allocate(36);
                position.fill((byte) 0);
                position.set(ValueLayout.JAVA_INT, 4L, 2);
                long answered = window.sendMessage(0x0288, WindowsNativeWindow.IMR_QUERYCHARPOSITION, position.address());
                assertTrue(answered >= 36L);
                assertEquals(2, window.lastImeCharPos());
                assertEquals(4, position.get(ValueLayout.JAVA_INT, 8L));
                assertEquals(6, position.get(ValueLayout.JAVA_INT, 12L));
                MemorySegment reconvert = arena.allocate(window.lastImeRequestBytes() < 64 ? 64 : window.lastImeRequestBytes());
                reconvert.fill((byte) 0);
                long filled = window.sendMessage(0x0288, WindowsNativeWindow.IMR_DOCUMENTFEED, reconvert.address());
                assertTrue(filled > 32L);
                assertEquals((int) filled, reconvert.get(ValueLayout.JAVA_INT, 0L));
                assertEquals(5, reconvert.get(ValueLayout.JAVA_INT, 8L));
            }
            window.postImeEnd();
            platform.pump();
            assertFalse(window.imeActive());
            assertTrue(
                    window.lastImmNotifyResult() != Integer.MIN_VALUE,
                    "WM_IME_ENDCOMPOSITION must call ImmNotifyIME"
            );
        } finally {
            platform.close();
        }
    }

    /// Delivers `WM_MOUSELEAVE` after generated `TrackMouseEvent(TME_LEAVE)` on `WM_MOUSEMOVE`.
    @Test
    void deliversMouseLeaveThroughTrackMouseEvent() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MouseLeave", 32.0, 32.0);
            platform.pump();
            window.postPointer(PointerEventType.MOVE, 11, 13);
            platform.pump();
            List<PointerEvent> events = window.takePointerEvents();
            assertEquals(PointerEventType.MOVE, events.getFirst().type());
            assertEquals(11.0f, events.getFirst().x());
            assertEquals(13.0f, events.getFirst().y());
            assertTrue(window.lastTrackMouseEventSucceeded(), "WM_MOUSEMOVE must call TrackMouseEvent");
            boolean sawLeave = events.stream().anyMatch(event -> event.type() == PointerEventType.LEAVE);
            if (!sawLeave) {
                window.postMouseLeave();
                platform.pump();
                events = window.takePointerEvents();
                sawLeave = events.stream().anyMatch(event -> event.type() == PointerEventType.LEAVE);
            }
            assertTrue(sawLeave, "WM_MOUSELEAVE must be delivered after TrackMouseEvent");
            PointerEvent leave = events.stream()
                    .filter(event -> event.type() == PointerEventType.LEAVE)
                    .findFirst()
                    .orElseThrow();
            assertEquals(PointerDeviceKind.MOUSE, leave.device());
            assertEquals(11.0f, leave.x());
            assertEquals(13.0f, leave.y());
            assertFalse(window.mouseLeaveTracked());
            WindowsNativeWindow.PenAxes axes = new WindowsNativeWindow.PenAxes(0.25f, 0.0f, 0.0f, 0.0f);
            window.postPen(PointerEventType.ROUTED_TO, 4, 5, 6, axes);
            window.postPen(PointerEventType.ROUTED_AWAY, 4, 5, 6, axes);
            window.postPen(PointerEventType.ROUTED_RELEASED, 4, 5, 6, axes);
            platform.pump();
            List<PointerEvent> routed = window.takePointerEvents();
            assertEquals(3, routed.size());
            assertEquals(PointerEventType.ROUTED_TO, routed.get(0).type());
            assertEquals(PointerEventType.ROUTED_AWAY, routed.get(1).type());
            assertEquals(PointerEventType.ROUTED_RELEASED, routed.get(2).type());
            assertEquals(PointerDeviceKind.PEN, routed.get(0).device());
            assertEquals(6, routed.get(0).pointerId());
            assertTrue(
                    window.lastPointerFrameCount() != Integer.MIN_VALUE,
                    "WM_POINTER* must call GetPointerFrameInfo"
            );
            assertTrue(
                    window.lastPointerFrameHistoryEntries() != Integer.MIN_VALUE,
                    "WM_POINTER* must call GetPointerFrameInfoHistory"
            );
            assertTrue(
                    window.lastPointerSourceDevice() != -1L,
                    "WM_POINTER* must read POINTER_INFO.sourceDevice"
            );
            assertTrue(
                    window.lastPointerHwndTarget() != -1L,
                    "WM_POINTER* must read POINTER_INFO.hwndTarget"
            );
            assertEquals(window.lastPointerSourceDevice(), routed.get(2).sourceDevice());
            assertEquals(window.lastPointerHwndTarget(), routed.get(2).hwndTarget());
            assertTrue(
                    window.lastPointerFramePenCount() != Integer.MIN_VALUE,
                    "pen WM_POINTER* must call GetPointerFramePenInfo"
            );
            assertTrue(
                    window.lastPointerHistoryCount() != Integer.MIN_VALUE,
                    "WM_POINTER* must call GetPointerInfoHistory"
            );
            assertTrue(
                    window.lastPointerPenHistoryCount() != Integer.MIN_VALUE,
                    "pen WM_POINTER* must call GetPointerPenInfoHistory"
            );
            assertTrue(
                    window.lastPointerFramePenHistoryEntries() != Integer.MIN_VALUE,
                    "pen WM_POINTER* must call GetPointerFramePenInfoHistory"
            );
            assertTrue(
                    window.lastPointerCursorId() != Integer.MIN_VALUE,
                    "WM_POINTER* must call GetPointerCursorId"
            );
            assertTrue(
                    window.lastPointerDevice() != -1L,
                    "WM_POINTER* must call GetPointerDevice"
            );
            assertTrue(
                    window.lastPointerDeviceRectsResult() != Integer.MIN_VALUE,
                    "WM_POINTER* must call GetPointerDeviceRects"
            );
            assertTrue(
                    window.lastPointerDevicePropertyCount() != Integer.MIN_VALUE,
                    "WM_POINTER* must call GetPointerDeviceProperties"
            );
            assertTrue(
                    window.lastPointerDeviceCursorCount() != Integer.MIN_VALUE,
                    "WM_POINTER* must call GetPointerDeviceCursors"
            );
            assertTrue(
                    window.lastSkipPointerFrameResult() != Integer.MIN_VALUE,
                    "WM_POINTER* must call SkipPointerFrameMessages"
            );
            window.postPointer(PointerEventType.DOWN, 4, 5, PointerDeviceKind.TOUCH, 7);
            platform.pump();
            assertTrue(
                    window.lastPointerFrameTouchCount() != Integer.MIN_VALUE,
                    "touch WM_POINTER* must call GetPointerFrameTouchInfo"
            );
            assertTrue(
                    window.lastPointerTouchHistoryCount() != Integer.MIN_VALUE,
                    "touch WM_POINTER* must call GetPointerTouchInfoHistory"
            );
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

    /// Delivers `WM_POINTERENTER` and `WM_POINTERLEAVE` through the production WndProc.
    @Test
    void deliversPointerEnterAndLeaveThroughWndProc() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "PointerEnter", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.PenAxes axes = new WindowsNativeWindow.PenAxes(0.25f, 0.0f, 0.0f, 40.0f);
            window.postPen(PointerEventType.ENTER, 6, 7, 4, axes);
            window.postPen(PointerEventType.LEAVE, 6, 7, 4, axes);
            platform.pump();
            List<PointerEvent> events = window.takePointerEvents();
            assertEquals(2, events.size());
            assertEquals(PointerEventType.ENTER, events.get(0).type());
            assertEquals(PointerDeviceKind.PEN, events.get(0).device());
            assertEquals(4, events.get(0).pointerId());
            assertEquals(6.0f, events.get(0).x());
            assertEquals(7.0f, events.get(0).y());
            assertEquals(40.0f, events.get(0).rotation());
            assertEquals(PointerEventType.LEAVE, events.get(1).type());
            assertEquals(4, events.get(1).pointerId());
            window.postPen(PointerEventType.CAPTURE_CHANGED, 6, 7, 4, axes);
            platform.pump();
            List<PointerEvent> capture = window.takePointerEvents();
            assertEquals(1, capture.size());
            assertEquals(PointerEventType.CAPTURE_CHANGED, capture.getFirst().type());
            window.postPen(PointerEventType.WHEEL, 8, 9, 5, axes);
            window.postPen(PointerEventType.WHEEL_HORIZONTAL, 8, 9, 5, axes);
            platform.pump();
            List<PointerEvent> wheels = window.takePointerEvents();
            assertEquals(2, wheels.size());
            assertEquals(PointerEventType.WHEEL, wheels.get(0).type());
            assertEquals(PointerEventType.WHEEL_HORIZONTAL, wheels.get(1).type());
            window.postPen(PointerEventType.ACTIVATE, 8, 9, 5, axes);
            platform.pump();
            List<PointerEvent> activate = window.takePointerEvents();
            assertEquals(1, activate.size());
            assertEquals(PointerEventType.ACTIVATE, activate.getFirst().type());
            assertEquals(PointerDeviceKind.PEN, activate.getFirst().device());
            assertEquals(5, activate.getFirst().pointerId());
            window.postPen(PointerEventType.NON_CLIENT_DOWN, 2, 3, 5, axes);
            window.postPen(PointerEventType.NON_CLIENT_MOVE, 3, 4, 5, axes);
            window.postPen(PointerEventType.NON_CLIENT_UP, 3, 4, 5, axes);
            platform.pump();
            List<PointerEvent> nonClient = window.takePointerEvents();
            assertEquals(3, nonClient.size());
            assertEquals(PointerEventType.NON_CLIENT_DOWN, nonClient.get(0).type());
            assertEquals(PointerEventType.NON_CLIENT_MOVE, nonClient.get(1).type());
            assertEquals(PointerEventType.NON_CLIENT_UP, nonClient.get(2).type());
            assertEquals(2.0f, nonClient.get(0).x());
            assertEquals(3.0f, nonClient.get(0).y());
            window.postSysVirtualKey(true, 0x12);
            window.postSysVirtualKey(true, 0x0D);
            window.postSysVirtualKey(false, 0x0D);
            window.postSysVirtualKey(false, 0x12);
            platform.pump();
            List<org.glavo.himari.layout.input.KeyEvent> sysKeys = window.takeKeyEvents();
            assertEquals(2, sysKeys.size());
            assertEquals(LogicalKey.ENTER, sysKeys.get(0).key());
            assertTrue(sysKeys.get(0).alt());
            assertEquals(KeyEventType.DOWN, sysKeys.get(0).type());
            assertEquals(LogicalKey.ENTER, sysKeys.get(1).key());
            assertEquals(KeyEventType.UP, sysKeys.get(1).type());
        } finally {
            platform.close();
        }
    }

    /// Publishes table headers through generated `ITableProvider` and `ITableItemProvider`.
    @Test
    void tableHeadersRoundTripThroughGeneratedUia() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "TableHeaders", 64.0, 64.0);
            platform.pump();
            LayoutTree tree = new LayoutTree();
            LayoutFactory factory = new LayoutFactory(tree);
            LayoutNode table = factory.leaf(
                    "people",
                    new Size(160.0f, 40.0f),
                    List.of(),
                    false,
                    SemanticsRole.TABLE,
                    "People",
                    java.util.Set.of(),
                    null
            );
            table.setGrid(new SemanticsGrid(
                    1,
                    2,
                    new String[] {"Name", "Value"},
                    new String[] {"A"}
            ));
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
            cell.setGridItem(new SemanticsGridItem(0, 1, 1, 1, "Value", "A"));
            tree.setRoot(table);
            tree.measure(Constraints.loose(400.0f, 400.0f));
            tree.place();
            try (WindowsAutomationProvider grid = window.automationProvider(table)) {
                assertEquals(2, grid.invokeColumnHeaders());
                assertEquals(1, grid.invokeRowHeaders());
                assertArrayEquals(
                        new int[] {2, 0},
                        grid.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_TABLE_COLUMN_HEADERS_PROPERTY_ID
                        )
                );
                assertArrayEquals(
                        new int[] {1, 0},
                        grid.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_TABLE_ROW_HEADERS_PROPERTY_ID
                        )
                );
                assertEquals("Name", grid.invokeColumnHeaderName(0));
                assertEquals("Value", grid.invokeColumnHeaderName(1));
                assertEquals(
                        WindowsAutomationProvider.UIA_HEADER_ITEM_CONTROL_TYPE_ID,
                        grid.invokeColumnHeaderControlType(0)
                );
                assertEquals("A", grid.invokeRowHeaderName(0));
                assertEquals(
                        WindowsAutomationProvider.UIA_HEADER_ITEM_CONTROL_TYPE_ID,
                        grid.invokeRowHeaderControlType(0)
                );
            }
            try (WindowsAutomationProvider item = window.automationProvider(cell)) {
                assertEquals(1, item.invokeColumnHeaderItems());
                assertEquals(1, item.invokeRowHeaderItems());
                assertArrayEquals(
                        new int[] {1, 0},
                        item.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_TABLE_ITEM_COLUMN_HEADER_ITEMS_PROPERTY_ID
                        )
                );
                assertArrayEquals(
                        new int[] {1, 0},
                        item.invokePropertyValueInts(
                                WindowsAutomationProvider.UIA_TABLE_ITEM_ROW_HEADER_ITEMS_PROPERTY_ID
                        )
                );
                assertEquals("Value", item.invokeColumnHeaderItemName());
            }
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

    /// Loads additional system cursors and reads `GetCursor` / `GetCursorPos` / `ShowCursor`.
    @Test
    void loadsSystemCursorsAndReadsCursorState() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "Cursors", 32.0, 32.0);
            platform.pump();
            int[] ids = {
                    WindowsNativeWindow.IDC_IBEAM,
                    WindowsNativeWindow.IDC_WAIT,
                    WindowsNativeWindow.IDC_CROSS,
                    WindowsNativeWindow.IDC_SIZEWE,
                    WindowsNativeWindow.IDC_SIZENS,
                    WindowsNativeWindow.IDC_SIZEALL,
                    WindowsNativeWindow.IDC_NO,
                    WindowsNativeWindow.IDC_HAND,
                    WindowsNativeWindow.IDC_APPSTARTING,
                    WindowsNativeWindow.IDC_HELP
            };
            for (int cursorId : ids) {
                assertTrue(window.setSystemCursor(cursorId), "cursor " + cursorId);
            }
            assertTrue(window.currentCursor().address() != 0L);
            WindowsNativeWindow.ScreenPoint position = window.cursorPosition();
            assertTrue(Integer.MIN_VALUE < position.x() && position.x() < Integer.MAX_VALUE);
            int shown = window.showCursor(true);
            int hidden = window.showCursor(false);
            assertEquals(shown - 1, hidden);
            assertTrue(window.setSystemCursor(WindowsNativeWindow.IDC_HAND));
            long handled = window.nativeWindow().sendMessage(0x0020, window.nativeHandle().address(), 1L);
            assertEquals(1L, handled);
            assertTrue(window.currentCursor().address() != 0L);
            WindowsNativeWindow.CursorInfo info = window.cursorInfo();
            assertTrue(info.showing());
            WindowsNativeWindow.ScreenPoint current = window.cursorPosition();
            assertTrue(window.setCursorPosition(current.x(), current.y()));
            WindowsNativeWindow.ClipRect clip = window.clipCursorRect();
            assertTrue(clip.right() > clip.left());
            assertTrue(clip.bottom() > clip.top());
            assertTrue(window.releaseCursorClip());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETWHEELSCROLLLINES` and `SPI_GETWHEELSCROLLCHARS`.
    @Test
    void readsWheelScrollMetricsThroughSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "WheelMetrics", 32.0, 32.0);
            platform.pump();
            assertTrue(Integer.toUnsignedLong(window.wheelScrollLines()) >= 1L);
            assertTrue(Integer.toUnsignedLong(window.wheelScrollChars()) >= 1L);
        } finally {
            platform.close();
        }
    }

    /// Reads effective monitor DPI through generated `MonitorFromWindow` and `GetDpiForMonitor`.
    @Test
    void monitorDpiMatchesWindowDpiOnTheSameDisplay() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MonitorDpi", 64.0, 64.0);
            platform.pump();
            WindowsNativeWindow.MonitorDpi monitor = window.monitorDpi();
            assertTrue(monitor.x() >= 96);
            assertTrue(monitor.y() >= 96);
            assertEquals(window.dpi(), monitor.x());
            assertEquals(monitor.x(), window.nativeWindow().monitorDpi().x());
        } finally {
            platform.close();
        }
    }

    /// Reads monitor and work rectangles through generated `GetMonitorInfoW`.
    @Test
    void monitorInfoReportsDisplayAndWorkArea() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MonitorInfo", 64.0, 64.0);
            platform.pump();
            WindowsNativeWindow.MonitorInfo info = window.monitorInfo();
            assertTrue(info.monitor().right() > info.monitor().left());
            assertTrue(info.monitor().bottom() > info.monitor().top());
            assertTrue(info.work().left() >= info.monitor().left());
            assertTrue(info.work().top() >= info.monitor().top());
            assertTrue(info.work().right() <= info.monitor().right());
            assertTrue(info.work().bottom() <= info.monitor().bottom());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_SWAPBUTTON` through generated `GetSystemMetrics`.
    @Test
    void swapButtonsReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SwapButtons", 32.0, 32.0);
            platform.pump();
            boolean swapped = window.swapButtons();
            assertEquals(swapped, window.nativeWindow().swapButtons());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXDRAG` / `SM_CYDRAG` through generated `GetSystemMetrics`.
    @Test
    void dragThresholdReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "DragThreshold", 32.0, 32.0);
            platform.pump();
            assertTrue(window.dragThresholdX() >= 1);
            assertTrue(window.dragThresholdY() >= 1);
            assertEquals(window.dragThresholdX(), window.nativeWindow().dragThresholdX());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETMOUSESPEED` through generated `SystemParametersInfoW`.
    @Test
    void mouseSpeedReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MouseSpeed", 32.0, 32.0);
            platform.pump();
            int speed = window.mouseSpeed();
            assertTrue(speed >= 1 && speed <= 20);
            assertEquals(speed, window.nativeWindow().mouseSpeed());
        } finally {
            platform.close();
        }
    }

    /// Reads `GetDoubleClickTime` through the generated User32 binding.
    @Test
    void doubleClickTimeReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "DoubleClickTime", 32.0, 32.0);
            platform.pump();
            int time = window.doubleClickTime();
            assertTrue(time > 0);
            assertEquals(time, window.nativeWindow().doubleClickTime());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXDOUBLECLK` / `SM_CYDOUBLECLK` through generated `GetSystemMetrics`.
    @Test
    void doubleClickThresholdReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "DoubleClickThreshold", 32.0, 32.0);
            platform.pump();
            assertTrue(window.doubleClickThresholdX() >= 1);
            assertTrue(window.doubleClickThresholdY() >= 1);
            assertEquals(window.doubleClickThresholdX(), window.nativeWindow().doubleClickThresholdX());
        } finally {
            platform.close();
        }
    }

    /// Reads `GetCaretBlinkTime` through the generated User32 binding.
    @Test
    void caretBlinkTimeReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "CaretBlinkTime", 32.0, 32.0);
            platform.pump();
            int time = window.caretBlinkTime();
            assertEquals(time, window.nativeWindow().caretBlinkTime());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETKEYBOARDDELAY` through generated `SystemParametersInfoW`.
    @Test
    void keyboardDelayReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "KeyboardDelay", 32.0, 32.0);
            platform.pump();
            int delay = window.keyboardDelay();
            assertTrue(delay >= 0 && delay <= 3);
            assertEquals(delay, window.nativeWindow().keyboardDelay());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETKEYBOARDSPEED` through generated `SystemParametersInfoW`.
    @Test
    void keyboardSpeedReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "KeyboardSpeed", 32.0, 32.0);
            platform.pump();
            int speed = window.keyboardSpeed();
            assertTrue(speed >= 0 && speed <= 31);
            assertEquals(speed, window.nativeWindow().keyboardSpeed());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETCARETWIDTH` through generated `SystemParametersInfoW`.
    @Test
    void caretWidthReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "CaretWidth", 32.0, 32.0);
            platform.pump();
            int width = window.caretWidth();
            assertTrue(width >= 1);
            assertEquals(width, window.nativeWindow().caretWidth());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETMOUSEHOVERTIME` through generated `SystemParametersInfoW`.
    @Test
    void mouseHoverTimeReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MouseHoverTime", 32.0, 32.0);
            platform.pump();
            int time = window.mouseHoverTime();
            assertTrue(time > 0);
            assertEquals(time, window.nativeWindow().mouseHoverTime());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETMOUSEHOVERWIDTH` / `SPI_GETMOUSEHOVERHEIGHT` through generated `SystemParametersInfoW`.
    @Test
    void mouseHoverSizeReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MouseHoverSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.mouseHoverWidth() >= 1);
            assertTrue(window.mouseHoverHeight() >= 1);
            assertEquals(window.mouseHoverWidth(), window.nativeWindow().mouseHoverWidth());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXICON` / `SM_CYICON` through generated `GetSystemMetrics`.
    @Test
    void iconSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "IconSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.iconWidth() >= 1);
            assertTrue(window.iconHeight() >= 1);
            assertEquals(window.iconWidth(), window.nativeWindow().iconWidth());
        } finally {
            platform.close();
        }
    }

    /// Reads loaded keyboard layouts through generated `GetKeyboardLayoutList`.
    @Test
    void keyboardLayoutListReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "KeyboardLayoutList", 32.0, 32.0);
            platform.pump();
            long[] layouts = window.keyboardLayouts();
            assertTrue(layouts.length >= 1);
            assertTrue(layouts[0] != 0L);
            assertEquals(layouts.length, window.nativeWindow().keyboardLayouts().length);
        } finally {
            platform.close();
        }
    }

    /// Reads `GetKeyboardLayoutNameW` through the generated User32 binding.
    @Test
    void keyboardLayoutNameReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "KeyboardLayoutName", 32.0, 32.0);
            platform.pump();
            String name = window.keyboardLayoutName();
            assertTrue(name.length() >= 8);
            assertEquals(name, window.nativeWindow().keyboardLayoutName());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETFONTSMOOTHING` through generated `SystemParametersInfoW`.
    @Test
    void fontSmoothingReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "FontSmoothing", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.fontSmoothingEnabled();
            assertEquals(enabled, window.nativeWindow().fontSmoothingEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETKEYBOARDPREF` through generated `SystemParametersInfoW`.
    @Test
    void keyboardPrefReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "KeyboardPref", 32.0, 32.0);
            platform.pump();
            boolean preferred = window.keyboardPreferred();
            assertEquals(preferred, window.nativeWindow().keyboardPreferred());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXSMICON` / `SM_CYSMICON` through generated `GetSystemMetrics`.
    @Test
    void smallIconSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SmallIconSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.smallIconWidth() >= 1);
            assertTrue(window.smallIconHeight() >= 1);
            assertEquals(window.smallIconWidth(), window.nativeWindow().smallIconWidth());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXCURSOR` / `SM_CYCURSOR` through generated `GetSystemMetrics`.
    @Test
    void cursorSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "CursorSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.cursorWidth() >= 1);
            assertTrue(window.cursorHeight() >= 1);
            assertEquals(window.cursorWidth(), window.nativeWindow().cursorWidth());
        } finally {
            platform.close();
        }
    }

    /// Reads `GetSysColor` `COLOR_WINDOW` / `COLOR_WINDOWTEXT` through the generated User32 binding.
    @Test
    void sysColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SysColor", 32.0, 32.0);
            platform.pump();
            int background = window.windowColor();
            int text = window.windowTextColor();
            assertTrue(background != text);
            assertEquals(background, window.nativeWindow().windowColor());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETDROPSHADOW` through generated `SystemParametersInfoW`.
    @Test
    void dropShadowReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "DropShadow", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.dropShadowEnabled();
            assertEquals(enabled, window.nativeWindow().dropShadowEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CYCAPTION` through generated `GetSystemMetrics`.
    @Test
    void captionHeightReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "CaptionHeight", 32.0, 32.0);
            platform.pump();
            assertTrue(window.captionHeight() >= 1);
            assertEquals(window.captionHeight(), window.nativeWindow().captionHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETMENUANIMATION` through generated `SystemParametersInfoW`.
    @Test
    void menuAnimationReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MenuAnimation", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.menuAnimationEnabled();
            assertEquals(enabled, window.nativeWindow().menuAnimationEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETFLATMENU` through generated `SystemParametersInfoW`.
    @Test
    void flatMenuReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "FlatMenu", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.flatMenuEnabled();
            assertEquals(enabled, window.nativeWindow().flatMenuEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CYMENU` through generated `GetSystemMetrics`.
    @Test
    void menuHeightReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MenuHeight", 32.0, 32.0);
            platform.pump();
            assertTrue(window.menuHeight() >= 1);
            assertEquals(window.menuHeight(), window.nativeWindow().menuHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXBORDER` / `SM_CYBORDER` through generated `GetSystemMetrics`.
    @Test
    void borderSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "BorderSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.borderWidth() >= 1);
            assertTrue(window.borderHeight() >= 1);
            assertEquals(window.borderWidth(), window.nativeWindow().borderWidth());
            assertEquals(window.borderHeight(), window.nativeWindow().borderHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETMENUDROPALIGNMENT` through generated `SystemParametersInfoW`.
    @Test
    void menuDropAlignmentReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MenuDropAlignment", 32.0, 32.0);
            platform.pump();
            boolean left = window.menuDropAlignsLeft();
            assertEquals(left, window.nativeWindow().menuDropAlignsLeft());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETMENUFADE` through generated `SystemParametersInfoW`.
    @Test
    void menuFadeReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MenuFade", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.menuFadeEnabled();
            assertEquals(enabled, window.nativeWindow().menuFadeEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETCOMBOBOXANIMATION` through generated `SystemParametersInfoW`.
    @Test
    void comboBoxAnimationReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ComboBoxAnimation", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.comboBoxAnimationEnabled();
            assertEquals(enabled, window.nativeWindow().comboBoxAnimationEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_HIGHLIGHT` / `COLOR_HIGHLIGHTTEXT` through generated `GetSysColor`.
    @Test
    void highlightColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "HighlightColor", 32.0, 32.0);
            platform.pump();
            int background = window.highlightColor();
            int foreground = window.highlightTextColor();
            assertEquals(background, window.nativeWindow().highlightColor());
            assertEquals(foreground, window.nativeWindow().highlightTextColor());
            assertTrue((background & 0xFF000000) == 0);
            assertTrue((foreground & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXFRAME` / `SM_CYFRAME` through generated `GetSystemMetrics`.
    @Test
    void frameSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "FrameSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.frameWidth() >= 1);
            assertTrue(window.frameHeight() >= 1);
            assertEquals(window.frameWidth(), window.nativeWindow().frameWidth());
            assertEquals(window.frameHeight(), window.nativeWindow().frameHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETTOOLTIPANIMATION` through generated `SystemParametersInfoW`.
    @Test
    void tooltipAnimationReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "TooltipAnimation", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.tooltipAnimationEnabled();
            assertEquals(enabled, window.nativeWindow().tooltipAnimationEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_GRAYTEXT` through generated `GetSysColor`.
    @Test
    void grayTextColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "GrayText", 32.0, 32.0);
            platform.pump();
            int color = window.grayTextColor();
            assertEquals(color, window.nativeWindow().grayTextColor());
            assertTrue((color & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXFULLSCREEN` / `SM_CYFULLSCREEN` through generated `GetSystemMetrics`.
    @Test
    void fullscreenSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "FullscreenSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.fullscreenWidth() >= 1);
            assertTrue(window.fullscreenHeight() >= 1);
            assertEquals(window.fullscreenWidth(), window.nativeWindow().fullscreenWidth());
            assertEquals(window.fullscreenHeight(), window.nativeWindow().fullscreenHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETSELECTIONFADE` through generated `SystemParametersInfoW`.
    @Test
    void selectionFadeReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SelectionFade", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.selectionFadeEnabled();
            assertEquals(enabled, window.nativeWindow().selectionFadeEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETLISTBOXSMOOTHSCROLLING` through generated `SystemParametersInfoW`.
    @Test
    void listBoxSmoothScrollingReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ListBoxSmoothScrolling", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.listBoxSmoothScrollingEnabled();
            assertEquals(enabled, window.nativeWindow().listBoxSmoothScrollingEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETSNAPTODEFBUTTON` through generated `SystemParametersInfoW`.
    @Test
    void snapToDefaultButtonReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SnapToDefaultButton", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.snapToDefaultButtonEnabled();
            assertEquals(enabled, window.nativeWindow().snapToDefaultButtonEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_BTNFACE` through generated `GetSysColor`.
    @Test
    void buttonFaceColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ButtonFace", 32.0, 32.0);
            platform.pump();
            int color = window.buttonFaceColor();
            assertEquals(color, window.nativeWindow().buttonFaceColor());
            assertTrue((color & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXHSCROLL` / `SM_CYHSCROLL` / `SM_CXVSCROLL` / `SM_CYVSCROLL` through generated `GetSystemMetrics`.
    @Test
    void scrollBarMetricsReadSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ScrollBarMetrics", 32.0, 32.0);
            platform.pump();
            assertTrue(window.horizontalScrollArrowWidth() >= 1);
            assertTrue(window.horizontalScrollBarHeight() >= 1);
            assertTrue(window.verticalScrollBarWidth() >= 1);
            assertTrue(window.verticalScrollArrowHeight() >= 1);
            assertEquals(window.horizontalScrollArrowWidth(), window.nativeWindow().horizontalScrollArrowWidth());
            assertEquals(window.horizontalScrollBarHeight(), window.nativeWindow().horizontalScrollBarHeight());
            assertEquals(window.verticalScrollBarWidth(), window.nativeWindow().verticalScrollBarWidth());
            assertEquals(window.verticalScrollArrowHeight(), window.nativeWindow().verticalScrollArrowHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETMENUUNDERLINES` through generated `SystemParametersInfoW`.
    @Test
    void menuUnderlinesReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MenuUnderlines", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.menuUnderlinesEnabled();
            assertEquals(enabled, window.nativeWindow().menuUnderlinesEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETHOTTRACKING` through generated `SystemParametersInfoW`.
    @Test
    void hotTrackingReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "HotTracking", 32.0, 32.0);
            platform.pump();
            boolean enabled = window.hotTrackingEnabled();
            assertEquals(enabled, window.nativeWindow().hotTrackingEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_BTNTEXT` through generated `GetSysColor`.
    @Test
    void buttonTextColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ButtonText", 32.0, 32.0);
            platform.pump();
            int color = window.buttonTextColor();
            assertEquals(color, window.nativeWindow().buttonTextColor());
            assertTrue((color & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_INACTIVEBORDER` through generated `GetSysColor`.
    @Test
    void inactiveBorderColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "InactiveBorder", 32.0, 32.0);
            platform.pump();
            int color = window.inactiveBorderColor();
            assertEquals(color, window.nativeWindow().inactiveBorderColor());
            assertTrue((color & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CMONITORS` through generated `GetSystemMetrics`.
    @Test
    void monitorCountReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MonitorCount", 32.0, 32.0);
            platform.pump();
            assertTrue(window.monitorCount() >= 1);
            assertEquals(window.monitorCount(), window.nativeWindow().monitorCount());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXMIN` / `SM_CYMIN` through generated `GetSystemMetrics`.
    @Test
    void minWindowSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MinWindowSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.minWindowWidth() >= 1);
            assertTrue(window.minWindowHeight() >= 1);
            assertEquals(window.minWindowWidth(), window.nativeWindow().minWindowWidth());
            assertEquals(window.minWindowHeight(), window.nativeWindow().minWindowHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXMAXIMIZED` / `SM_CYMAXIMIZED` through generated `GetSystemMetrics`.
    @Test
    void maximizedWindowSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MaximizedWindowSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.maximizedWindowWidth() >= window.minWindowWidth());
            assertTrue(window.maximizedWindowHeight() >= window.minWindowHeight());
            assertEquals(window.maximizedWindowWidth(), window.nativeWindow().maximizedWindowWidth());
            assertEquals(window.maximizedWindowHeight(), window.nativeWindow().maximizedWindowHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXSCREEN` / `SM_CYSCREEN` through generated `GetSystemMetrics`.
    @Test
    void screenSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ScreenSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.screenWidth() > 0);
            assertTrue(window.screenHeight() > 0);
            assertEquals(window.screenWidth(), window.nativeWindow().screenWidth());
            assertEquals(window.screenHeight(), window.nativeWindow().screenHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXVIRTUALSCREEN` / `SM_CYVIRTUALSCREEN` through generated `GetSystemMetrics`.
    @Test
    void virtualScreenSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "VirtualScreenSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.virtualScreenWidth() >= window.screenWidth());
            assertTrue(window.virtualScreenHeight() >= window.screenHeight());
            assertEquals(window.virtualScreenWidth(), window.nativeWindow().virtualScreenWidth());
            assertEquals(window.virtualScreenHeight(), window.nativeWindow().virtualScreenHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETWORKAREA` through generated `SystemParametersInfoW`.
    @Test
    void workAreaReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "WorkArea", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.ClipRect area = window.workArea();
            assertTrue(area.right() > area.left());
            assertTrue(area.bottom() > area.top());
            assertEquals(area, window.nativeWindow().workArea());
            assertTrue((area.right() - area.left()) <= window.virtualScreenWidth());
            assertTrue((area.bottom() - area.top()) <= window.virtualScreenHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CMOUSEBUTTONS` / `SM_MOUSEPRESENT` / `SM_MOUSEWHEELPRESENT` through generated `GetSystemMetrics`.
    @Test
    void mousePresenceReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MousePresence", 32.0, 32.0);
            platform.pump();
            assertTrue(window.mouseButtonCount() >= 0);
            assertEquals(window.mouseButtonCount(), window.nativeWindow().mouseButtonCount());
            assertEquals(window.mousePresent(), window.nativeWindow().mousePresent());
            assertEquals(window.mouseWheelPresent(), window.nativeWindow().mouseWheelPresent());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXEDGE` / `SM_CYEDGE` through generated `GetSystemMetrics`.
    @Test
    void edgeSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "EdgeSize", 32.0, 32.0);
            platform.pump();
            assertTrue(window.edgeWidth() >= 0);
            assertTrue(window.edgeHeight() >= 0);
            assertEquals(window.edgeWidth(), window.nativeWindow().edgeWidth());
            assertEquals(window.edgeHeight(), window.nativeWindow().edgeHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CYSMCAPTION` through generated `GetSystemMetrics`.
    @Test
    void smallCaptionHeightReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SmallCaption", 32.0, 32.0);
            platform.pump();
            assertTrue(window.smallCaptionHeight() > 0);
            assertEquals(window.smallCaptionHeight(), window.nativeWindow().smallCaptionHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_MENU` / `COLOR_MENUTEXT` through generated `GetSysColor`.
    @Test
    void menuColorsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MenuColors", 32.0, 32.0);
            platform.pump();
            int menu = window.menuColor();
            int text = window.menuTextColor();
            assertEquals(menu, window.nativeWindow().menuColor());
            assertEquals(text, window.nativeWindow().menuTextColor());
            assertTrue((menu & 0xFF000000) == 0);
            assertTrue((text & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_MENUBAR` through generated `GetSysColor`.
    @Test
    void menuBarColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MenuBarColor", 32.0, 32.0);
            platform.pump();
            int color = window.menuBarColor();
            assertEquals(color, window.nativeWindow().menuBarColor());
            assertTrue((color & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_INFOBK` / `COLOR_INFOTEXT` through generated `GetSysColor`.
    @Test
    void infoColorsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "InfoColors", 32.0, 32.0);
            platform.pump();
            int background = window.infoBackgroundColor();
            int text = window.infoTextColor();
            assertEquals(background, window.nativeWindow().infoBackgroundColor());
            assertEquals(text, window.nativeWindow().infoTextColor());
            assertTrue((background & 0xFF000000) == 0);
            assertTrue((text & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_HOTLIGHT` through generated `GetSysColor`.
    @Test
    void hotLightColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "HotLight", 32.0, 32.0);
            platform.pump();
            int color = window.hotLightColor();
            assertEquals(color, window.nativeWindow().hotLightColor());
            assertTrue((color & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_SCROLLBAR` / `COLOR_APPWORKSPACE` / `COLOR_WINDOWFRAME` through generated `GetSysColor`.
    @Test
    void frameWorkspaceColorsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "FrameWorkspaceColors", 32.0, 32.0);
            platform.pump();
            int scroll = window.scrollBarColor();
            int workspace = window.appWorkspaceColor();
            int frame = window.windowFrameColor();
            assertEquals(scroll, window.nativeWindow().scrollBarColor());
            assertEquals(workspace, window.nativeWindow().appWorkspaceColor());
            assertEquals(frame, window.nativeWindow().windowFrameColor());
            assertTrue((scroll & 0xFF000000) == 0);
            assertTrue((workspace & 0xFF000000) == 0);
            assertTrue((frame & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXSIZE` / `SM_CYSIZE` / `SM_CXMENUSIZE` / `SM_CYMENUSIZE` through generated `GetSystemMetrics`.
    @Test
    void captionAndMenuButtonSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "CaptionMenuButtons", 32.0, 32.0);
            platform.pump();
            assertTrue(window.captionButtonWidth() > 0);
            assertTrue(window.captionButtonHeight() > 0);
            assertTrue(window.menuButtonWidth() > 0);
            assertTrue(window.menuButtonHeight() > 0);
            assertEquals(window.captionButtonWidth(), window.nativeWindow().captionButtonWidth());
            assertEquals(window.captionButtonHeight(), window.nativeWindow().captionButtonHeight());
            assertEquals(window.menuButtonWidth(), window.nativeWindow().menuButtonWidth());
            assertEquals(window.menuButtonHeight(), window.nativeWindow().menuButtonHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_XVIRTUALSCREEN` / `SM_YVIRTUALSCREEN` through generated `GetSystemMetrics`.
    @Test
    void virtualScreenOriginReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "VirtualScreenOrigin", 32.0, 32.0);
            platform.pump();
            assertEquals(window.virtualScreenX(), window.nativeWindow().virtualScreenX());
            assertEquals(window.virtualScreenY(), window.nativeWindow().virtualScreenY());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETUIEFFECTS` / `SPI_GETDRAGFULLWINDOWS` through generated `SystemParametersInfoW`.
    @Test
    void uiEffectsAndFullWindowDragReadSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "UiEffectsDrag", 32.0, 32.0);
            platform.pump();
            assertEquals(window.uiEffectsEnabled(), window.nativeWindow().uiEffectsEnabled());
            assertEquals(window.dragFullWindowsEnabled(), window.nativeWindow().dragFullWindowsEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_ACTIVECAPTION` / `COLOR_INACTIVECAPTION` through generated `GetSysColor`.
    @Test
    void captionColorsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "CaptionColors", 32.0, 32.0);
            platform.pump();
            int active = window.activeCaptionColor();
            int inactive = window.inactiveCaptionColor();
            assertEquals(active, window.nativeWindow().activeCaptionColor());
            assertEquals(inactive, window.nativeWindow().inactiveCaptionColor());
            assertTrue((active & 0xFF000000) == 0);
            assertTrue((inactive & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `GetForegroundWindow` / `GetFocus` / `GetActiveWindow` through generated User32 bindings.
    @Test
    void foregroundFocusAndActiveWindowsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ForegroundFocus", 32.0, 32.0);
            platform.pump();
            MemorySegment foreground = window.foregroundWindow();
            MemorySegment focus = window.focusWindow();
            MemorySegment active = window.activeWindow();
            assertEquals(foreground.address(), window.nativeWindow().foregroundWindow().address());
            assertEquals(focus.address(), window.nativeWindow().focusWindow().address());
            assertEquals(active.address(), window.nativeWindow().activeWindow().address());
        } finally {
            platform.close();
        }
    }

    /// Reads `GetCaretPos` through the generated User32 binding.
    @Test
    void caretPositionReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "CaretPos", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.ScreenPoint caret = window.caretPosition();
            WindowsNativeWindow.ScreenPoint again = window.nativeWindow().caretPosition();
            assertEquals(caret.x(), again.x());
            assertEquals(caret.y(), again.y());
        } finally {
            platform.close();
        }
    }

    /// Reads restored bounds through generated `GetWindowPlacement`.
    @Test
    void windowPlacementReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "WindowPlacement", 120.0, 80.0);
            platform.pump();
            WindowsNativeWindow.WindowPlacement placement = window.windowPlacement();
            WindowsNativeWindow.WindowPlacement again = window.nativeWindow().windowPlacement();
            assertEquals(placement.showCmd(), again.showCmd());
            assertTrue(placement.normal().right() > placement.normal().left());
            assertTrue(placement.normal().bottom() > placement.normal().top());
            assertEquals(placement.normal().left(), again.normal().left());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_CAPTIONTEXT` / `COLOR_INACTIVECAPTIONTEXT` through generated `GetSysColor`.
    @Test
    void captionTextColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "CaptionText", 32.0, 32.0);
            platform.pump();
            int color = window.captionTextColor();
            int inactive = window.inactiveCaptionTextColor();
            assertEquals(color, window.nativeWindow().captionTextColor());
            assertEquals(inactive, window.nativeWindow().inactiveCaptionTextColor());
            assertTrue((color & 0xFF000000) == 0);
            assertTrue((inactive & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `IsIconic` / `IsZoomed` / `IsWindowVisible` through generated User32 bindings.
    @Test
    void windowStateQueriesReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "WindowStateQueries", 32.0, 32.0);
            platform.pump();
            assertFalse(window.iconic());
            assertFalse(window.zoomed());
            assertEquals(window.iconic(), window.nativeWindow().iconic());
            assertEquals(window.zoomed(), window.nativeWindow().zoomed());
            assertEquals(window.windowVisible(), window.nativeWindow().windowVisible());
        } finally {
            platform.close();
        }
    }

    /// Disables and re-enables the HWND through generated `EnableWindow` / `IsWindowEnabled`.
    @Test
    void enableWindowTogglesInputThroughUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "EnableWindow", 32.0, 32.0);
            platform.pump();
            assertTrue(window.windowEnabled());
            window.setWindowEnabled(false);
            assertFalse(window.windowEnabled());
            assertEquals(window.windowEnabled(), window.nativeWindow().windowEnabled());
            window.setWindowEnabled(true);
            assertTrue(window.windowEnabled());
            assertTrue(window.nativeWindow().windowEnabled());
        } finally {
            platform.close();
        }
    }

    /// Reads the HWND title through generated `GetWindowTextLengthW` / `GetWindowTextW`.
    @Test
    void windowTextReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "HimariTitle", 32.0, 32.0);
            platform.pump();
            assertEquals("HimariTitle", window.windowText());
            assertEquals(window.windowText(), window.nativeWindow().windowText());
            window.setWindowText("HimariRenamed");
            assertEquals("HimariRenamed", window.windowText());
            assertEquals("HimariRenamed", window.nativeWindow().windowText());
        } finally {
            platform.close();
        }
    }

    /// Reads the window class through generated `GetClassNameW`.
    @Test
    void classNameReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ClassName", 32.0, 32.0);
            platform.pump();
            String name = window.className();
            assertTrue(name.startsWith("HimariUIWindow"));
            assertEquals(name, window.nativeWindow().className());
        } finally {
            platform.close();
        }
    }

    /// Round-trips the client origin through generated `ClientToScreen` / `ScreenToClient`.
    @Test
    void clientToScreenRoundTripsThroughUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ClientToScreen", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.ScreenPoint screen = window.clientToScreen(0, 0);
            WindowsNativeWindow.ScreenPoint client = window.screenToClient(screen.x(), screen.y());
            assertEquals(0, client.x());
            assertEquals(0, client.y());
            assertEquals(screen, window.nativeWindow().clientToScreen(0, 0));
            assertEquals(client, window.nativeWindow().screenToClient(screen.x(), screen.y()));
        } finally {
            platform.close();
        }
    }

    /// Assigns keyboard focus through generated `SetFocus` and reads it back with `GetFocus`.
    @Test
    void setFocusRoundTripsThroughUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SetFocus", 32.0, 32.0);
            platform.pump();
            window.setFocus();
            assertEquals(window.nativeHandle().address(), window.focusWindow().address());
            window.nativeWindow().setFocus();
            assertEquals(window.nativeHandle().address(), window.nativeWindow().focusWindow().address());
        } finally {
            platform.close();
        }
    }

    /// Invokes generated `SetForegroundWindow` on a live HWND.
    @Test
    void setForegroundWindowInvokesUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SetForegroundWindow", 32.0, 32.0);
            platform.pump();
            window.setFocus();
            boolean accepted = window.setForegroundWindow();
            boolean again = window.nativeWindow().setForegroundWindow();
            if (accepted || again) {
                assertEquals(window.nativeHandle().address(), window.foregroundWindow().address());
                assertEquals(window.nativeHandle().address(), window.nativeWindow().foregroundWindow().address());
            }
        } finally {
            platform.close();
        }
    }

    /// Assigns the active window through generated `SetActiveWindow`.
    @Test
    void setActiveWindowInvokesUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "SetActiveWindow", 32.0, 32.0);
            platform.pump();
            window.setFocus();
            window.setActiveWindow();
            window.nativeWindow().setActiveWindow();
            MemorySegment active = window.activeWindow();
            assertEquals(active.address(), window.nativeWindow().activeWindow().address());
        } finally {
            platform.close();
        }
    }

    /// Reads `GetWindowThreadProcessId` through the generated User32 binding.
    @Test
    void windowThreadProcessIdsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ThreadProcessIds", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.ThreadProcessIds ids = window.threadProcessIds();
            assertTrue(ids.threadId() != 0);
            assertTrue(ids.processId() != 0);
            assertEquals(ids, window.nativeWindow().threadProcessIds());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETANIMATION` through generated `SystemParametersInfoW` into `ANIMATIONINFO`.
    @Test
    void minimizeMaximizeAnimationReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "AnimationInfo", 32.0, 32.0);
            platform.pump();
            int minAnimate = window.animationMinAnimate();
            assertEquals(minAnimate, window.nativeWindow().animationMinAnimate());
            assertEquals(minAnimate != 0, window.animationMinAnimate() != 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETACTIVEWINDOWTRACKING` / `SPI_GETFOREGROUNDLOCKTIMEOUT` through generated `SystemParametersInfoW`.
    @Test
    void foregroundTrackingReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ForegroundTracking", 32.0, 32.0);
            platform.pump();
            assertEquals(window.activeWindowTrackingEnabled(), window.nativeWindow().activeWindowTrackingEnabled());
            assertTrue(window.foregroundLockTimeoutMillis() >= 0);
            assertEquals(window.foregroundLockTimeoutMillis(), window.nativeWindow().foregroundLockTimeoutMillis());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXMINTRACK` / `SM_CYMINTRACK` through generated `GetSystemMetrics`.
    @Test
    void minTrackSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MinTrack", 32.0, 32.0);
            platform.pump();
            assertTrue(window.minTrackWidth() >= window.minWindowWidth());
            assertTrue(window.minTrackHeight() >= window.minWindowHeight());
            assertEquals(window.minTrackWidth(), window.nativeWindow().minTrackWidth());
            assertEquals(window.minTrackHeight(), window.nativeWindow().minTrackHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_GRADIENTACTIVECAPTION` / `COLOR_GRADIENTINACTIVECAPTION` through generated `GetSysColor`.
    @Test
    void gradientCaptionColorsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "GradientCaption", 32.0, 32.0);
            platform.pump();
            int active = window.gradientActiveCaptionColor();
            int inactive = window.gradientInactiveCaptionColor();
            assertEquals(active, window.nativeWindow().gradientActiveCaptionColor());
            assertEquals(inactive, window.nativeWindow().gradientInactiveCaptionColor());
            assertTrue((active & 0xFF000000) == 0);
            assertTrue((inactive & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `IsWindow` / `GetParent` / `GetDesktopWindow` / `GetAncestor` through generated User32 bindings.
    @Test
    void windowAncestryReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "WindowAncestry", 32.0, 32.0);
            platform.pump();
            assertTrue(window.isWindow());
            assertTrue(window.nativeWindow().isWindow());
            assertEquals(0L, window.parentWindow().address());
            assertEquals(0L, window.nativeWindow().parentWindow().address());
            assertTrue(window.desktopWindow().address() != 0L);
            assertEquals(window.desktopWindow().address(), window.nativeWindow().desktopWindow().address());
            assertEquals(window.nativeHandle().address(), window.rootAncestor().address());
            assertEquals(window.rootAncestor().address(), window.nativeWindow().rootAncestor().address());
        } finally {
            platform.close();
        }
    }

    /// Raises the HWND through generated `BringWindowToTop`.
    @Test
    void bringWindowToTopInvokesUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "BringWindowToTop", 32.0, 32.0);
            platform.pump();
            window.bringToTop();
            window.nativeWindow().bringToTop();
            assertTrue(window.isWindow());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXMAXTRACK` / `SM_CYMAXTRACK` through generated `GetSystemMetrics`.
    @Test
    void maxTrackSizeReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MaxTrack", 32.0, 32.0);
            platform.pump();
            assertTrue(window.maxTrackWidth() >= window.minTrackWidth());
            assertTrue(window.maxTrackHeight() >= window.minTrackHeight());
            assertEquals(window.maxTrackWidth(), window.nativeWindow().maxTrackWidth());
            assertEquals(window.maxTrackHeight(), window.nativeWindow().maxTrackHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_BTNSHADOW` / `COLOR_BTNHIGHLIGHT` through generated `GetSysColor`.
    @Test
    void buttonEdgeColorsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ButtonEdgeColors", 32.0, 32.0);
            platform.pump();
            int shadow = window.buttonShadowColor();
            int highlight = window.buttonHighlightColor();
            assertEquals(shadow, window.nativeWindow().buttonShadowColor());
            assertEquals(highlight, window.nativeWindow().buttonHighlightColor());
            assertTrue((shadow & 0xFF000000) == 0);
            assertTrue((highlight & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETFOREGROUNDFLASHCOUNT` / `SPI_GETMOUSEVANISH` through generated `SystemParametersInfoW`.
    @Test
    void foregroundFlashAndMouseVanishReadSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "FlashVanish", 32.0, 32.0);
            platform.pump();
            assertTrue(window.foregroundFlashCount() >= 0);
            assertEquals(window.foregroundFlashCount(), window.nativeWindow().foregroundFlashCount());
            assertEquals(window.mouseVanishEnabled(), window.nativeWindow().mouseVanishEnabled());
        } finally {
            platform.close();
        }
    }

    /// Maps the client origin through generated `MapWindowPoints` and matches `ClientToScreen`.
    @Test
    void mapWindowPointsMatchesClientToScreen() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MapWindowPoints", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.ScreenPoint mapped = window.mapClientToScreen(0, 0);
            WindowsNativeWindow.ScreenPoint direct = window.clientToScreen(0, 0);
            assertEquals(direct, mapped);
            assertEquals(mapped, window.nativeWindow().mapClientToScreen(0, 0));
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_3DDKSHADOW` / `COLOR_3DLIGHT` through generated `GetSysColor`.
    @Test
    void threeDEdgeColorsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ThreeDEdgeColors", 32.0, 32.0);
            platform.pump();
            int dark = window.darkShadowColor();
            int light = window.lightEdgeColor();
            assertEquals(dark, window.nativeWindow().darkShadowColor());
            assertEquals(light, window.nativeWindow().lightEdgeColor());
            assertTrue((dark & 0xFF000000) == 0);
            assertTrue((light & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETNONCLIENTMETRICS` through generated `SystemParametersInfoW` into `NONCLIENTMETRICSW`.
    @Test
    void nonClientMetricsReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "NonClientMetrics", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.NonClientMetrics metrics = window.nonClientMetrics();
            assertTrue(metrics.captionHeight() > 0);
            assertTrue(metrics.menuHeight() > 0);
            assertTrue(metrics.scrollWidth() > 0);
            assertTrue(metrics.captionWidth() > 0);
            assertFalse(metrics.captionFontFace().isEmpty());
            assertTrue(metrics.paddedBorderWidth() >= 0);
            assertEquals(metrics, window.nativeWindow().nonClientMetrics());
        } finally {
            platform.close();
        }
    }

    /// Enumerates Z-order siblings through generated `GetTopWindow` / `GetWindow`.
    @Test
    void topWindowAndSiblingsReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "TopWindowSiblings", 32.0, 32.0);
            platform.pump();
            assertEquals(0L, window.topWindow().address());
            assertEquals(0L, window.ownerWindow().address());
            assertEquals(0L, window.childWindow().address());
            MemorySegment top = window.desktopTopWindow();
            assertTrue(window.isWindowHandle(top));
            assertEquals(top.address(), window.nativeWindow().desktopTopWindow().address());
            MemorySegment next = window.nextWindow(top);
            if (next.address() != 0L) {
                assertTrue(window.isWindowHandle(next));
                assertEquals(next.address(), window.nativeWindow().nextWindow(top).address());
                MemorySegment previous = window.previousWindow(next);
                if (previous.address() != 0L) {
                    assertTrue(window.isWindowHandle(previous));
                }
            }
        } finally {
            platform.close();
        }
    }

    /// Reads `GetShellWindow` and `FindWindowW` through generated User32 bindings.
    @Test
    void shellWindowReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ShellWindow", 32.0, 32.0);
            platform.pump();
            MemorySegment shell = window.shellWindow();
            assertTrue(shell.address() != 0L);
            assertEquals(shell.address(), window.nativeWindow().shellWindow().address());
            MemorySegment tray = window.findWindow("Shell_TrayWnd");
            if (tray.address() != 0L) {
                assertEquals(tray.address(), window.nativeWindow().findWindow("Shell_TrayWnd").address());
            }
            WindowsNativeWindow.TopLevelWindows enumerated = window.enumerateTopLevelWindows();
            assertTrue(enumerated.count() >= 1);
            assertTrue(enumerated.containsSelf());
            assertEquals(enumerated, window.nativeWindow().enumerateTopLevelWindows());
            assertEquals(0, window.enumerateChildWindows(window.nativeHandle()));
            assertTrue(window.enumerateChildWindows(window.desktopWindow()) >= 1);
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETMINIMIZEDMETRICS` through generated `SystemParametersInfoW`.
    @Test
    void minimizedMetricsReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "MinimizedMetrics", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.MinimizedMetrics metrics = window.minimizedMetrics();
            assertTrue(metrics.width() > 0);
            assertTrue(metrics.horzGap() >= 0);
            assertTrue(metrics.vertGap() >= 0);
            assertEquals(metrics, window.nativeWindow().minimizedMetrics());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETICONMETRICS` through generated `SystemParametersInfoW` into `ICONMETRICSW`.
    @Test
    void iconMetricsReadsSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "IconMetrics", 32.0, 32.0);
            platform.pump();
            WindowsNativeWindow.IconMetrics metrics = window.iconMetrics();
            assertTrue(metrics.horzSpacing() > 0);
            assertTrue(metrics.vertSpacing() > 0);
            assertFalse(metrics.fontFace().isEmpty());
            assertEquals(metrics, window.nativeWindow().iconMetrics());
        } finally {
            platform.close();
        }
    }

    /// Reads `GetLastActivePopup` / `GetSystemMenu` through generated User32 bindings.
    @Test
    void lastActivePopupAndSystemMenuReadUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "LastPopupMenu", 32.0, 32.0);
            platform.pump();
            assertEquals(window.nativeHandle().address(), window.lastActivePopup().address());
            assertEquals(window.lastActivePopup().address(), window.nativeWindow().lastActivePopup().address());
            assertTrue(window.systemMenu().address() != 0L);
            assertEquals(window.systemMenu().address(), window.nativeWindow().systemMenu().address());
        } finally {
            platform.close();
        }
    }

    /// Reads `SM_CXICONSPACING` / `SM_CYICONSPACING` through generated `GetSystemMetrics`.
    @Test
    void iconSpacingReadsSystemMetrics() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "IconSpacing", 32.0, 32.0);
            platform.pump();
            assertTrue(window.iconSpacingWidth() > 0);
            assertTrue(window.iconSpacingHeight() > 0);
            assertEquals(window.iconSpacingWidth(), window.nativeWindow().iconSpacingWidth());
            assertEquals(window.iconSpacingHeight(), window.nativeWindow().iconSpacingHeight());
        } finally {
            platform.close();
        }
    }

    /// Reads `COLOR_BACKGROUND` through generated `GetSysColor`.
    @Test
    void backgroundColorReadsUser32() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "BackgroundColor", 32.0, 32.0);
            platform.pump();
            int color = window.backgroundColor();
            assertEquals(color, window.nativeWindow().backgroundColor());
            assertTrue((color & 0xFF000000) == 0);
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETICONTITLEWRAP` / `SPI_GETFONTSMOOTHINGTYPE` through generated `SystemParametersInfoW`.
    @Test
    void iconTitleWrapAndFontSmoothingTypeReadSystemParameters() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "IconTitleFont", 32.0, 32.0);
            platform.pump();
            assertEquals(window.iconTitleWrapEnabled(), window.nativeWindow().iconTitleWrapEnabled());
            assertTrue(window.fontSmoothingType() >= 0);
            assertEquals(window.fontSmoothingType(), window.nativeWindow().fontSmoothingType());
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

    /// Rejects NULL COM arguments through generated `DoDragDrop`.
    @Test
    void oleDoDragDropRejectsNullArguments() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "DoDragDrop", 72.0, 72.0);
            platform.pump();
            try (WindowsDropSource source = window.createDropSource()) {
                assertEquals(WindowsDropSource.E_INVALIDARG, source.probeDoDragDrop());
            }
        } finally {
            platform.close();
        }
    }

    /// Cancels through `IDropSource::QueryContinueDrag` on the generated COM vtable.
    @Test
    void oleDropSourceQueryContinueCancelsThroughVtable() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "DropSource", 72.0, 72.0);
            platform.pump();
            try (WindowsDropSource source = window.createDropSource()) {
                assertEquals(WindowsDropSource.DRAGDROP_S_CANCEL, source.invokeQueryContinueDrag());
                assertTrue(source.queryContinueCount() >= 1);
                assertEquals(WindowsDropSource.DRAGDROP_S_USEDEFAULTCURSORS, source.invokeGiveFeedback());
                assertTrue(source.giveFeedbackCount() >= 1);
            }
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETHIGHCONTRAST` through generated `SystemParametersInfoW`.
    @Test
    void systemParametersInfoReadsHighContrast() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "HighContrast", 64.0, 64.0);
            platform.pump();
            window.highContrastOn();
            assertEquals(window.highContrastOn(), window.nativeWindow().highContrastOn());
        } finally {
            platform.close();
        }
    }

    /// Reads `SPI_GETCLIENTAREAANIMATION` through generated `SystemParametersInfoW`.
    @Test
    void systemParametersInfoReadsClientAreaAnimation() throws Exception {
        WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
        try {
            WindowsWindow window = openToplevel(platform, "ClientAnim", 64.0, 64.0);
            platform.pump();
            window.clientAreaAnimationEnabled();
            assertEquals(
                    window.clientAreaAnimationEnabled(),
                    window.nativeWindow().clientAreaAnimationEnabled()
            );
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
            LayoutTree extentTree = new LayoutTree();
            LayoutNode volumeNode = new LayoutFactory(extentTree).leaf(
                    "volume",
                    new Size(160.0f, 24.0f),
                    List.of(),
                    true,
                    SemanticsRole.SLIDER,
                    "Volume",
                    java.util.Set.of(SemanticsAction.INCREMENT, SemanticsAction.DECREMENT),
                    null
            );
            volumeNode.setRangeValue(3.0);
            volumeNode.setRangeExtent(0.0, 10.0);
            extentTree.setRoot(volumeNode);
            extentTree.measure(Constraints.loose(200.0f, 40.0f));
            extentTree.place();
            try (WindowsAutomationProvider extentProvider = window.automationProvider(volumeNode)) {
                assertEquals(0.0, extentProvider.invokePropertyValueDouble(
                        WindowsAutomationProvider.UIA_RANGE_VALUE_MINIMUM_PROPERTY_ID), 0.001);
                assertEquals(10.0, extentProvider.invokePropertyValueDouble(
                        WindowsAutomationProvider.UIA_RANGE_VALUE_MAXIMUM_PROPERTY_ID), 0.001);
                assertEquals(3.0, extentProvider.rangeValue(), 0.001);
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
