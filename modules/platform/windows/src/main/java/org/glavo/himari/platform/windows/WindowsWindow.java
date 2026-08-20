package org.glavo.himari.platform.windows;

import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.platform.api.PlatformWindow;
import org.glavo.himari.platform.api.SurfaceDescriptor;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowId;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/// Implements [PlatformWindow] for one HWND.
@NotNullByDefault
public final class WindowsWindow implements PlatformWindow {
    /// The owning session.
    private final WindowsPlatform platform;

    /// The native window.
    private final WindowsNativeWindow nativeWindow;

    /// The stable identifier.
    private final WindowId id;

    /// The surface descriptor.
    private final SurfaceDescriptor surface;

    /// Pointer events drained from WndProc and not yet consumed by the application.
    private final ArrayList<PointerEvent> pendingPointers = new ArrayList<>();

    /// Key events drained from WndProc and not yet consumed by the application.
    private final ArrayList<KeyEvent> pendingKeys = new ArrayList<>();

    /// IME session driven by `WM_CHAR` and by explicit test injection.
    private final WindowsImeSession ime = new WindowsImeSession();

    /// The latest snapshot.
    private volatile WindowSnapshot snapshot;

    /// Close completion.
    private final CompletableFuture<Void> closeCompletion = new CompletableFuture<>();

    /// Creates one window.
    ///
    /// @param platform the session
    /// @param nativeWindow the HWND owner
    /// @param snapshot the initial snapshot
    WindowsWindow(WindowsPlatform platform, WindowsNativeWindow nativeWindow, WindowSnapshot snapshot) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.nativeWindow = Objects.requireNonNull(nativeWindow, "nativeWindow");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.id = snapshot.id();
        this.surface = snapshot.surface();
    }

    /// {@inheritDoc}
    @Override
    public WindowId id() {
        return id;
    }

    /// {@inheritDoc}
    @Override
    public SurfaceDescriptor surface() {
        return surface;
    }

    /// {@inheritDoc}
    @Override
    public WindowSnapshot snapshot() {
        return snapshot;
    }

    /// Returns the native HWND for RHI attachment.
    ///
    /// @return the handle
    public MemorySegment nativeHandle() {
        return nativeWindow.handle();
    }

    /// Returns the window DPI last reported by `GetDpiForWindow`.
    ///
    /// @return the DPI
    public int dpi() {
        return nativeWindow.dpi();
    }

    /// Reads effective monitor DPI through `MonitorFromWindow` and `GetDpiForMonitor`.
    ///
    /// @return the positive X/Y DPI pair
    public WindowsNativeWindow.MonitorDpi monitorDpi() {
        return nativeWindow.monitorDpi();
    }

    /// Reads monitor bounds through `MonitorFromWindow` and `GetMonitorInfoW`.
    ///
    /// @return the monitor and work rectangles
    public WindowsNativeWindow.MonitorInfo monitorInfo() {
        return nativeWindow.monitorInfo();
    }

    /// Reads `SM_SWAPBUTTON`.
    ///
    /// @return whether the primary and secondary mouse buttons are swapped
    public boolean swapButtons() {
        return nativeWindow.swapButtons();
    }

    /// Reads `SM_CXDRAG`.
    ///
    /// @return the horizontal drag threshold in pixels
    public int dragThresholdX() {
        return nativeWindow.dragThresholdX();
    }

    /// Reads `SM_CYDRAG`.
    ///
    /// @return the vertical drag threshold in pixels
    public int dragThresholdY() {
        return nativeWindow.dragThresholdY();
    }

    /// Reads `SPI_GETMOUSESPEED`.
    ///
    /// @return the mouse speed in `[1, 20]`
    public int mouseSpeed() {
        return nativeWindow.mouseSpeed();
    }

    /// Reads `GetDoubleClickTime`.
    ///
    /// @return the double-click interval in milliseconds
    public int doubleClickTime() {
        return nativeWindow.doubleClickTime();
    }

    /// Reads `SM_CXDOUBLECLK`.
    ///
    /// @return the horizontal double-click rectangle width in pixels
    public int doubleClickThresholdX() {
        return nativeWindow.doubleClickThresholdX();
    }

    /// Reads `SM_CYDOUBLECLK`.
    ///
    /// @return the vertical double-click rectangle height in pixels
    public int doubleClickThresholdY() {
        return nativeWindow.doubleClickThresholdY();
    }

    /// Reads `GetCaretBlinkTime`.
    ///
    /// @return the caret blink interval in milliseconds, or `-1` when blink is disabled
    public int caretBlinkTime() {
        return nativeWindow.caretBlinkTime();
    }

    /// Reads `SPI_GETKEYBOARDDELAY`.
    ///
    /// @return the keyboard repeat delay in `[0, 3]`
    public int keyboardDelay() {
        return nativeWindow.keyboardDelay();
    }

    /// Reads `SPI_GETKEYBOARDSPEED`.
    ///
    /// @return the keyboard repeat speed in `[0, 31]`
    public int keyboardSpeed() {
        return nativeWindow.keyboardSpeed();
    }

    /// Reads `SPI_GETCARETWIDTH`.
    ///
    /// @return the caret width in pixels
    public int caretWidth() {
        return nativeWindow.caretWidth();
    }

    /// Reads `SPI_GETMOUSEHOVERTIME`.
    ///
    /// @return the hover time in milliseconds
    public int mouseHoverTime() {
        return nativeWindow.mouseHoverTime();
    }

    /// Reads `SPI_GETMOUSEHOVERWIDTH`.
    ///
    /// @return the hover rectangle width in pixels
    public int mouseHoverWidth() {
        return nativeWindow.mouseHoverWidth();
    }

    /// Reads `SPI_GETMOUSEHOVERHEIGHT`.
    ///
    /// @return the hover rectangle height in pixels
    public int mouseHoverHeight() {
        return nativeWindow.mouseHoverHeight();
    }

    /// Reads `SM_CXICON`.
    ///
    /// @return the default icon width in pixels
    public int iconWidth() {
        return nativeWindow.iconWidth();
    }

    /// Reads `SM_CYICON`.
    ///
    /// @return the default icon height in pixels
    public int iconHeight() {
        return nativeWindow.iconHeight();
    }

    /// Reads loaded keyboard layouts through `GetKeyboardLayoutList`.
    ///
    /// @return the layout handle addresses
    public long @Unmodifiable [] keyboardLayouts() {
        return nativeWindow.keyboardLayouts();
    }

    /// Reads `GetKeyboardLayoutNameW`.
    ///
    /// @return the KLID string
    public String keyboardLayoutName() {
        return nativeWindow.keyboardLayoutName();
    }

    /// Reads `SPI_GETFONTSMOOTHING`.
    ///
    /// @return whether font smoothing is enabled
    public boolean fontSmoothingEnabled() {
        return nativeWindow.fontSmoothingEnabled();
    }

    /// Reads `SPI_GETKEYBOARDPREF`.
    ///
    /// @return whether the user prefers the keyboard over the mouse
    public boolean keyboardPreferred() {
        return nativeWindow.keyboardPreferred();
    }

    /// Reads `SM_CXSMICON`.
    ///
    /// @return the small icon width in pixels
    public int smallIconWidth() {
        return nativeWindow.smallIconWidth();
    }

    /// Reads `SM_CYSMICON`.
    ///
    /// @return the small icon height in pixels
    public int smallIconHeight() {
        return nativeWindow.smallIconHeight();
    }

    /// Reads `SM_CXCURSOR`.
    ///
    /// @return the cursor width in pixels
    public int cursorWidth() {
        return nativeWindow.cursorWidth();
    }

    /// Reads `SM_CYCURSOR`.
    ///
    /// @return the cursor height in pixels
    public int cursorHeight() {
        return nativeWindow.cursorHeight();
    }

    /// Reads `COLOR_WINDOW`.
    ///
    /// @return the window background `COLORREF`
    public int windowColor() {
        return nativeWindow.windowColor();
    }

    /// Reads `COLOR_WINDOWTEXT`.
    ///
    /// @return the window text `COLORREF`
    public int windowTextColor() {
        return nativeWindow.windowTextColor();
    }

    /// Reads `SPI_GETDROPSHADOW`.
    ///
    /// @return whether drop shadows are enabled
    public boolean dropShadowEnabled() {
        return nativeWindow.dropShadowEnabled();
    }

    /// Reads `SM_CYCAPTION`.
    ///
    /// @return the caption height in pixels
    public int captionHeight() {
        return nativeWindow.captionHeight();
    }

    /// Reads `SPI_GETMENUANIMATION`.
    ///
    /// @return whether menu animation is enabled
    public boolean menuAnimationEnabled() {
        return nativeWindow.menuAnimationEnabled();
    }

    /// Reads `SPI_GETFLATMENU`.
    ///
    /// @return whether menus use a flat appearance
    public boolean flatMenuEnabled() {
        return nativeWindow.flatMenuEnabled();
    }

    /// Reads `SM_CYMENU`.
    ///
    /// @return the single-line menu bar height in pixels
    public int menuHeight() {
        return nativeWindow.menuHeight();
    }

    /// Reads `SM_CXBORDER`.
    ///
    /// @return the window border width in pixels
    public int borderWidth() {
        return nativeWindow.borderWidth();
    }

    /// Reads `SM_CYBORDER`.
    ///
    /// @return the window border height in pixels
    public int borderHeight() {
        return nativeWindow.borderHeight();
    }

    /// Reads `SPI_GETMENUDROPALIGNMENT`.
    ///
    /// @return whether popup menus drop to the left of the menu item
    public boolean menuDropAlignsLeft() {
        return nativeWindow.menuDropAlignsLeft();
    }

    /// Reads `SPI_GETMENUFADE`.
    ///
    /// @return whether menu fade animation is enabled
    public boolean menuFadeEnabled() {
        return nativeWindow.menuFadeEnabled();
    }

    /// Reads `SPI_GETCOMBOBOXANIMATION`.
    ///
    /// @return whether combo-box slide animation is enabled
    public boolean comboBoxAnimationEnabled() {
        return nativeWindow.comboBoxAnimationEnabled();
    }

    /// Reads `COLOR_HIGHLIGHT`.
    ///
    /// @return the selection background `COLORREF`
    public int highlightColor() {
        return nativeWindow.highlightColor();
    }

    /// Reads `COLOR_HIGHLIGHTTEXT`.
    ///
    /// @return the selection text `COLORREF`
    public int highlightTextColor() {
        return nativeWindow.highlightTextColor();
    }

    /// Reads `SM_CXFRAME`.
    ///
    /// @return the sizing-border width in pixels
    public int frameWidth() {
        return nativeWindow.frameWidth();
    }

    /// Reads `SM_CYFRAME`.
    ///
    /// @return the sizing-border height in pixels
    public int frameHeight() {
        return nativeWindow.frameHeight();
    }

    /// Reads `SPI_GETTOOLTIPANIMATION`.
    ///
    /// @return whether tooltip animation is enabled
    public boolean tooltipAnimationEnabled() {
        return nativeWindow.tooltipAnimationEnabled();
    }

    /// Reads `COLOR_GRAYTEXT`.
    ///
    /// @return the disabled-text `COLORREF`
    public int grayTextColor() {
        return nativeWindow.grayTextColor();
    }

    /// Reads `SM_CXFULLSCREEN`.
    ///
    /// @return the maximized-window client width in pixels
    public int fullscreenWidth() {
        return nativeWindow.fullscreenWidth();
    }

    /// Reads `SM_CYFULLSCREEN`.
    ///
    /// @return the maximized-window client height in pixels
    public int fullscreenHeight() {
        return nativeWindow.fullscreenHeight();
    }

    /// Reads `SPI_GETSELECTIONFADE`.
    ///
    /// @return whether menu-selection fade is enabled
    public boolean selectionFadeEnabled() {
        return nativeWindow.selectionFadeEnabled();
    }

    /// Reads `SPI_GETLISTBOXSMOOTHSCROLLING`.
    ///
    /// @return whether list-box smooth scrolling is enabled
    public boolean listBoxSmoothScrollingEnabled() {
        return nativeWindow.listBoxSmoothScrollingEnabled();
    }

    /// Reads `SPI_GETSNAPTODEFBUTTON`.
    ///
    /// @return whether the mouse snaps to the default button
    public boolean snapToDefaultButtonEnabled() {
        return nativeWindow.snapToDefaultButtonEnabled();
    }

    /// Reads `COLOR_BTNFACE`.
    ///
    /// @return the 3-D face `COLORREF`
    public int buttonFaceColor() {
        return nativeWindow.buttonFaceColor();
    }

    /// Reads `SM_CXHSCROLL`.
    ///
    /// @return the horizontal-scrollbar arrow width in pixels
    public int horizontalScrollArrowWidth() {
        return nativeWindow.horizontalScrollArrowWidth();
    }

    /// Reads `SM_CYHSCROLL`.
    ///
    /// @return the horizontal-scrollbar height in pixels
    public int horizontalScrollBarHeight() {
        return nativeWindow.horizontalScrollBarHeight();
    }

    /// Reads `SM_CXVSCROLL`.
    ///
    /// @return the vertical-scrollbar width in pixels
    public int verticalScrollBarWidth() {
        return nativeWindow.verticalScrollBarWidth();
    }

    /// Reads `SM_CYVSCROLL`.
    ///
    /// @return the vertical-scrollbar arrow height in pixels
    public int verticalScrollArrowHeight() {
        return nativeWindow.verticalScrollArrowHeight();
    }

    /// Reads `SPI_GETMENUUNDERLINES`.
    ///
    /// @return whether menu access-key underlines are always shown
    public boolean menuUnderlinesEnabled() {
        return nativeWindow.menuUnderlinesEnabled();
    }

    /// Reads `SPI_GETHOTTRACKING`.
    ///
    /// @return whether hot-tracking of user-interface elements is enabled
    public boolean hotTrackingEnabled() {
        return nativeWindow.hotTrackingEnabled();
    }

    /// Reads `COLOR_BTNTEXT`.
    ///
    /// @return the button-text `COLORREF`
    public int buttonTextColor() {
        return nativeWindow.buttonTextColor();
    }

    /// Reads `COLOR_INACTIVEBORDER`.
    ///
    /// @return the inactive-window border `COLORREF`
    public int inactiveBorderColor() {
        return nativeWindow.inactiveBorderColor();
    }

    /// Delivers `WM_DPICHANGED` through the production WndProc.
    ///
    /// @param nextDpi the new DPI for both axes
    /// @param left the suggested left
    /// @param top the suggested top
    /// @param right the suggested right
    /// @param bottom the suggested bottom
    /// @return the `WndProc` result
    public long applyDpiChange(int nextDpi, int left, int top, int right, int bottom) {
        return nativeWindow.applyDpiChange(nextDpi, left, top, right, bottom);
    }

    /// Returns the IME session attached to this window.
    ///
    /// @return the session
    public WindowsImeSession ime() {
        return ime;
    }

    /// Returns pointer events delivered through WndProc since the last drain.
    ///
    /// @return the events
    public @Unmodifiable List<PointerEvent> takePointerEvents() {
        List<PointerEvent> copy = List.copyOf(pendingPointers);
        pendingPointers.clear();
        return copy;
    }

    /// Queries generated `GetPointerType` for `pointerId`.
    ///
    /// @param pointerId the pointer identity
    /// @return the type, or `0` when the query fails
    public int queryPointerType(int pointerId) {
        return nativeWindow.queryPointerType(pointerId);
    }

    /// Queries generated `GetPointerPenInfo` for `pointerId`.
    ///
    /// @param pointerId the pointer identity
    /// @return the axes
    public WindowsNativeWindow.PenAxes queryPenInfo(int pointerId) {
        return nativeWindow.queryPenInfo(pointerId);
    }

    /// Queries generated `GetPointerTouchInfo` for `pointerId`.
    ///
    /// @param pointerId the pointer identity
    /// @return the contact ellipse
    public WindowsNativeWindow.ContactArea queryTouchInfo(int pointerId) {
        return nativeWindow.queryTouchInfo(pointerId);
    }

    /// Queries generated `GetPointerInfo` for `pointerId`.
    ///
    /// @param pointerId the pointer identity
    /// @return the hover and contact bits
    public WindowsNativeWindow.PointerFlags queryPointerInfo(int pointerId) {
        return nativeWindow.queryPointerInfo(pointerId);
    }

    /// Returns the last `GetPointerFrameInfo` pointer count from the production pointer path.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameCount() {
        return nativeWindow.lastPointerFrameCount();
    }

    /// Returns the last `GetPointerFrameInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameHistoryEntries() {
        return nativeWindow.lastPointerFrameHistoryEntries();
    }

    /// Returns the last `POINTER_INFO.sourceDevice` handle address.
    ///
    /// @return the handle, or `-1` before a query
    public long lastPointerSourceDevice() {
        return nativeWindow.lastPointerSourceDevice();
    }

    /// Returns the last `POINTER_INFO.hwndTarget` handle address.
    ///
    /// @return the handle, or `-1` before a query
    public long lastPointerHwndTarget() {
        return nativeWindow.lastPointerHwndTarget();
    }

    /// Returns the last `GetPointerFramePenInfo` pointer count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFramePenCount() {
        return nativeWindow.lastPointerFramePenCount();
    }

    /// Returns the last `GetPointerFrameTouchInfo` pointer count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameTouchCount() {
        return nativeWindow.lastPointerFrameTouchCount();
    }

    /// Returns the last `ImmGetCandidateListCountW` list count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastCandidateListCount() {
        return nativeWindow.lastCandidateListCount();
    }

    /// Returns whether generated `RegisterRawInputDevices` succeeded for this HWND.
    ///
    /// @return whether keyboard and mouse raw input is registered
    public boolean rawInputRegistered() {
        return nativeWindow.rawInputRegistered();
    }

    /// Returns the last `GetRawInputData` header size from `WM_INPUT`.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputBytes() {
        return nativeWindow.lastRawInputBytes();
    }

    /// Returns the last `GetPointerInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerHistoryCount() {
        return nativeWindow.lastPointerHistoryCount();
    }

    /// Returns the last `GetPointerPenInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerPenHistoryCount() {
        return nativeWindow.lastPointerPenHistoryCount();
    }

    /// Returns the last `GetPointerTouchInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerTouchHistoryCount() {
        return nativeWindow.lastPointerTouchHistoryCount();
    }

    /// Returns the last `GetRawInputBuffer` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputBufferBytes() {
        return nativeWindow.lastRawInputBufferBytes();
    }

    /// Returns the last `GetRawInputBuffer` packet count.
    ///
    /// @return the packet count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputBufferPackets() {
        return nativeWindow.lastRawInputBufferPackets();
    }

    /// Returns the last `GetRegisteredRawInputDevices` device count.
    ///
    /// @return the device count, or `Integer.MIN_VALUE` before a query
    public int lastRegisteredRawInputDevices() {
        return nativeWindow.lastRegisteredRawInputDevices();
    }

    /// Returns the last `ImmGetGuideLineW` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastGuideLineBytes() {
        return nativeWindow.lastGuideLineBytes();
    }

    /// Returns the last `ImmGetCompositionFontW` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastCompositionFontResult() {
        return nativeWindow.lastCompositionFontResult();
    }

    /// Returns the last `ImmGetCompositionFontW` face name.
    ///
    /// @return the face, possibly empty
    public String lastCompositionFontFace() {
        return nativeWindow.lastCompositionFontFace();
    }

    /// Returns the last `GetPointerCursorId` value.
    ///
    /// @return the cursor id, or `Integer.MIN_VALUE` before a query
    public int lastPointerCursorId() {
        return nativeWindow.lastPointerCursorId();
    }

    /// Returns the last `GetPointerDevice` handle address.
    ///
    /// @return the handle, or `-1` before a query
    public long lastPointerDevice() {
        return nativeWindow.lastPointerDevice();
    }

    /// Returns the last `GetRawInputDeviceInfoW` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputDeviceInfoBytes() {
        return nativeWindow.lastRawInputDeviceInfoBytes();
    }

    /// Returns the last `GetRawInputDeviceList` device count.
    ///
    /// @return the device count, or `Integer.MIN_VALUE` before a query
    public int lastRawInputDeviceListCount() {
        return nativeWindow.lastRawInputDeviceListCount();
    }

    /// Returns the last `WM_INPUT_DEVICE_CHANGE` `wParam`.
    ///
    /// @return the change kind, or `Integer.MIN_VALUE` before a delivery
    public int lastInputDeviceChange() {
        return nativeWindow.lastInputDeviceChange();
    }

    /// Posts `WM_INPUT_DEVICE_CHANGE` through the production WndProc.
    ///
    /// @param change `GIDC_ARRIVAL` (`1`) or `GIDC_REMOVAL` (`2`)
    public void postInputDeviceChange(int change) {
        nativeWindow.postMessage(0x00FE, Integer.toUnsignedLong(change), 0L);
    }

    /// Returns the last `ImmGetConversionStatus` conversion bits.
    ///
    /// @return the bits, or `Integer.MIN_VALUE` before a query
    public int lastConversionStatus() {
        return nativeWindow.lastConversionStatus();
    }

    /// Returns the last `ImmGetOpenStatus` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastImeOpenStatus() {
        return nativeWindow.lastImeOpenStatus();
    }

    /// Returns the last `ImmGetIMEFileNameW` path.
    ///
    /// @return the path, possibly empty
    public String lastImeFileName() {
        return nativeWindow.lastImeFileName();
    }

    /// Returns the last `GetPointerDeviceRects` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastPointerDeviceRectsResult() {
        return nativeWindow.lastPointerDeviceRectsResult();
    }

    /// Returns the last `GetPointerDeviceProperties` property count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerDevicePropertyCount() {
        return nativeWindow.lastPointerDevicePropertyCount();
    }

    /// Returns the last `GetPointerDevices` device count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerDeviceCount() {
        return nativeWindow.lastPointerDeviceCount();
    }

    /// Returns the last `GetPointerDeviceCursors` cursor count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerDeviceCursorCount() {
        return nativeWindow.lastPointerDeviceCursorCount();
    }

    /// Returns the last `ImmSetCompositionFontW` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetCompositionFontResult() {
        return nativeWindow.lastSetCompositionFontResult();
    }

    /// Returns the last `IsMouseInPointerEnabled` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastMouseInPointerEnabled() {
        return nativeWindow.lastMouseInPointerEnabled();
    }

    /// Returns the last `SkipPointerFrameMessages` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastSkipPointerFrameResult() {
        return nativeWindow.lastSkipPointerFrameResult();
    }

    /// Returns the last `ImmSetConversionStatus` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetConversionStatusResult() {
        return nativeWindow.lastSetConversionStatusResult();
    }

    /// Returns the last `ImmSetOpenStatus` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetOpenStatusResult() {
        return nativeWindow.lastSetOpenStatusResult();
    }

    /// Installs pointer-info flags used when `GetPointerInfo` has no live contact.
    ///
    /// @param pointerId the pointer identity
    /// @param flags the hover and contact bits
    public void installPointerFlags(int pointerId, WindowsNativeWindow.PointerFlags flags) {
        nativeWindow.installPointerFlags(pointerId, flags);
    }

    /// Returns the physical-pixels-per-logical-pixel scale implied by [#dpi()].
    ///
    /// @return the positive scale
    public double scaleFactor() {
        return nativeWindow.scaleFactor();
    }

    /// Posts a pen `WM_POINTER*` through the production WndProc with the supplied axes.
    ///
    /// When the host has no live stylus contact, [`WindowsNativeWindow#queryPenInfo(int)`] uses
    /// `axes` so the delivered [`PointerEvent`] carries pressure, tilt, and rotation.
    ///
    /// @param type the pointer kind
    /// @param x the client x
    /// @param y the client y
    /// @param pointerId the host pointer identity
    /// @param axes the pen axes
    public void postPen(
            org.glavo.himari.layout.input.PointerEventType type,
            int x,
            int y,
            int pointerId,
            WindowsNativeWindow.PenAxes axes
    ) {
        nativeWindow.installPenAxes(pointerId, axes);
        postPointer(type, x, y, org.glavo.himari.layout.input.PointerDeviceKind.PEN, pointerId);
    }

    /// Posts a touch `WM_POINTER*` through the production WndProc with `rcContact`.
    ///
    /// When the host has no live touch contact, [`WindowsNativeWindow#queryTouchInfo(int)`]
    /// uses `contact` so the delivered [`org.glavo.himari.layout.input.PointerEvent`] carries
    /// width and height.
    ///
    /// @param type the pointer kind
    /// @param x the client x
    /// @param y the client y
    /// @param pointerId the host pointer identity
    /// @param contact the contact ellipse
    public void postTouch(
            org.glavo.himari.layout.input.PointerEventType type,
            int x,
            int y,
            int pointerId,
            WindowsNativeWindow.ContactArea contact
    ) {
        nativeWindow.installTouchContact(pointerId, contact);
        postPointer(type, x, y, org.glavo.himari.layout.input.PointerDeviceKind.TOUCH, pointerId);
    }

    /// Posts one wheel notch through the production WndProc.
    ///
    /// @param x the client x
    /// @param y the client y
    /// @param notches signed `WHEEL_DELTA` multiples
    public void postWheel(int x, int y, int notches) {
        long packed = WindowsNativeWindow.packPointer(x, y);
        long wParam = (Short.toUnsignedLong((short) (notches * WindowsNativeWindow.WHEEL_DELTA))) << 16;
        nativeWindow.postMessage(0x020A, wParam, packed);
    }

    /// Loads a system cursor and installs it through generated User32 bindings.
    ///
    /// @param cursorId a `MAKEINTRESOURCE` identifier such as [`WindowsNativeWindow#IDC_ARROW`]
    /// @return whether the cursor handle was non-null
    public boolean setSystemCursor(int cursorId) {
        return nativeWindow.setSystemCursor(cursorId);
    }

    /// Returns whether this HWND currently owns mouse capture.
    ///
    /// @return whether `GetCapture` reports this window
    public boolean captured() {
        return nativeWindow.captured();
    }

    /// Returns the cursor currently installed by `GetCursor`.
    ///
    /// @return the cursor handle, or a null segment when none is installed
    public MemorySegment currentCursor() {
        return nativeWindow.currentCursor();
    }

    /// Reads the screen cursor location through `GetCursorPos`.
    ///
    /// @return the screen coordinates
    public WindowsNativeWindow.ScreenPoint cursorPosition() {
        return nativeWindow.cursorPosition();
    }

    /// Adjusts the thread cursor display count through `ShowCursor`.
    ///
    /// @param show `true` increments the display count; `false` decrements it
    /// @return the display count after the adjustment
    public int showCursor(boolean show) {
        return nativeWindow.showCursor(show);
    }

    /// Reads `SPI_GETWHEELSCROLLLINES`.
    ///
    /// @return the unsigned line count, or `0xFFFFFFFF` when one page is configured
    public int wheelScrollLines() {
        return nativeWindow.wheelScrollLines();
    }

    /// Reads `SPI_GETWHEELSCROLLCHARS`.
    ///
    /// @return the unsigned character count, or `0xFFFFFFFF` when one page is configured
    public int wheelScrollChars() {
        return nativeWindow.wheelScrollChars();
    }

    /// Reads `SPI_GETHIGHCONTRAST`.
    ///
    /// @return whether high contrast is on
    public boolean highContrastOn() {
        return nativeWindow.highContrastOn();
    }

    /// Reads `SPI_GETCLIENTAREAANIMATION`.
    ///
    /// @return whether client-area animations are enabled
    public boolean clientAreaAnimationEnabled() {
        return nativeWindow.clientAreaAnimationEnabled();
    }

    /// Creates an OLE `IDropSource` owned by this session.
    ///
    /// @return the source
    public WindowsDropSource createDropSource() {
        return WindowsDropSource.create(platform.libraries());
    }

    /// Writes the screen cursor location through `SetCursorPos`.
    ///
    /// @param x the screen x in pixels
    /// @param y the screen y in pixels
    /// @return whether the host accepted the coordinates
    public boolean setCursorPosition(int x, int y) {
        return nativeWindow.setCursorPosition(x, y);
    }

    /// Reads cursor visibility and screen position through `GetCursorInfo`.
    ///
    /// @return the host cursor snapshot
    public WindowsNativeWindow.CursorInfo cursorInfo() {
        return nativeWindow.cursorInfo();
    }

    /// Reads the current cursor clip rectangle through `GetClipCursor`.
    ///
    /// @return the clip rectangle in screen pixels
    public WindowsNativeWindow.ClipRect clipCursorRect() {
        return nativeWindow.clipCursorRect();
    }

    /// Releases the thread cursor clip through `ClipCursor(NULL)`.
    ///
    /// @return whether the host accepted the release
    public boolean releaseCursorClip() {
        return nativeWindow.releaseCursorClip();
    }

    /// Returns key events delivered through WndProc since the last drain.
    ///
    /// @return the events
    public @Unmodifiable List<KeyEvent> takeKeyEvents() {
        List<KeyEvent> copy = List.copyOf(pendingKeys);
        pendingKeys.clear();
        return copy;
    }

    /// Writes Unicode text to the system clipboard through generated FFM bindings.
    ///
    /// @param text the text
    public void writeClipboard(String text) {
        WindowsClipboard.writeUnicode(platform.libraries(), nativeHandle(), text);
    }

    /// Reads Unicode text from the system clipboard.
    ///
    /// @return the text, or `null` when the format is absent
    public @Nullable String readClipboard() {
        return WindowsClipboard.readUnicode(platform.libraries(), nativeHandle());
    }

    /// Writes ANSI `CF_TEXT` to the system clipboard.
    ///
    /// @param text the text
    public void writeAnsiClipboard(String text) {
        WindowsClipboard.writeAnsi(platform.libraries(), nativeHandle(), text);
    }

    /// Reads ANSI `CF_TEXT` from the system clipboard.
    ///
    /// @return the text, or `null` when the format is absent
    public @Nullable String readAnsiClipboard() {
        return WindowsClipboard.readAnsi(platform.libraries(), nativeHandle());
    }

    /// Writes `HTML Format` to the system clipboard.
    ///
    /// @param fragment the HTML fragment
    public void writeHtmlClipboard(String fragment) {
        WindowsClipboard.writeHtml(platform.libraries(), nativeHandle(), fragment);
    }

    /// Writes `CF_DIB` to the system clipboard.
    ///
    /// @param dib the BITMAPINFO plus pixel bytes
    public void writeDibClipboard(byte[] dib) {
        WindowsClipboard.writeDib(platform.libraries(), nativeHandle(), dib);
    }

    /// Reads `CF_DIB` from the system clipboard.
    ///
    /// @return the DIB bytes, or `null` when the format is absent
    public byte @Nullable [] readDibClipboard() {
        return WindowsClipboard.readDib(platform.libraries(), nativeHandle());
    }

    /// Writes `CF_HDROP` to the system clipboard.
    ///
    /// @param paths the absolute file paths
    public void writeDropFilesClipboard(List<String> paths) {
        WindowsClipboard.writeDropFiles(platform.libraries(), nativeHandle(), paths);
    }

    /// Reads `CF_HDROP` from the system clipboard.
    ///
    /// @return the paths, or `null` when the format is absent
    public @Nullable @Unmodifiable List<String> readDropFilesClipboard() {
        return WindowsClipboard.readDropFiles(platform.libraries(), nativeHandle());
    }

    /// Writes `Rich Text Format` to the system clipboard.
    ///
    /// @param rtf the RTF document
    public void writeRtfClipboard(String rtf) {
        WindowsClipboard.writeRtf(platform.libraries(), nativeHandle(), rtf);
    }

    /// Reads the `Rich Text Format` document from the system clipboard.
    ///
    /// @return the document, or `null` when the format is absent
    public @Nullable String readRtfClipboard() {
        return WindowsClipboard.readRtf(platform.libraries(), nativeHandle());
    }

    /// Returns the registered `Rich Text Format` clipboard identifier.
    ///
    /// @return the positive format id
    public int rtfClipboardFormat() {
        return WindowsClipboard.rtfFormat(platform.libraries());
    }

    /// Reads the `HTML Format` fragment from the system clipboard.
    ///
    /// @return the fragment, or `null` when the format is absent
    public @Nullable String readHtmlClipboard() {
        return WindowsClipboard.readHtml(platform.libraries(), nativeHandle());
    }

    /// Returns the registered `HTML Format` clipboard identifier.
    ///
    /// @return the positive format id
    public int htmlClipboardFormat() {
        return WindowsClipboard.htmlFormat(platform.libraries());
    }

    /// Empties the system clipboard.
    public void clearClipboard() {
        WindowsClipboard.clear(platform.libraries(), nativeHandle());
    }

    /// Registers an OLE drop target on this HWND.
    ///
    /// @return the registered target
    public WindowsDropTarget registerDropTarget() {
        return WindowsDropTarget.register(platform.libraries(), nativeHandle());
    }

    /// Creates a Unicode `IDataObject` owned by this session.
    ///
    /// @param text the payload
    /// @return the data object
    public WindowsDataObject createUnicodeDataObject(String text) {
        return WindowsDataObject.unicode(platform.libraries(), text);
    }

    /// Creates a `CF_HDROP` `IDataObject` owned by this session.
    ///
    /// @param paths the absolute file paths
    /// @return the data object
    public WindowsDataObject createDropFilesDataObject(List<String> paths) {
        return WindowsDataObject.files(platform.libraries(), paths);
    }

    /// Pushes the IME candidate rectangle through IMM32.
    ///
    /// @return whether `ImmSetCompositionWindow` and `ImmSetCandidateWindow` both succeeded
    public boolean applyImeCandidate() {
        return WindowsImmSession.applyCandidateRectangle(platform.libraries(), nativeHandle(), ime);
    }

    /// Creates the in-process TSF thread manager.
    ///
    /// @return the session
    public WindowsTsfSession openTsf() {
        return WindowsTsfSession.open(platform.libraries());
    }

    /// Creates an `ITextStoreACP` bound to this window's IME session.
    ///
    /// @return the store
    public WindowsTextStore createTextStore() {
        return WindowsTextStore.of(platform.libraries(), ime, nativeHandle());
    }

    /// Creates an `IRawElementProviderSimple` for one semantics node.
    ///
    /// @param node the projected node
    /// @return the provider
    public WindowsAutomationProvider automationProvider(SemanticsNode node) {
        return WindowsAutomationProvider.of(platform.libraries(), node, nativeHandle());
    }

    /// Creates an `IRawElementProviderSimple` whose text ranges follow `live`.
    ///
    /// @param live the live layout node
    /// @return the provider
    public WindowsAutomationProvider automationProvider(LayoutNode live) {
        return WindowsAutomationProvider.of(platform.libraries(), live, nativeHandle());
    }

    /// Returns the module handle used to create this HWND.
    ///
    /// @return the `HINSTANCE`
    public MemorySegment moduleHandle() {
        return nativeWindow.instance();
    }

    /// Returns whether a move/resize modal loop is active.
    ///
    /// @return whether the loop is active
    public boolean modalLoopActive() {
        return nativeWindow.modalLoopActive();
    }

    /// Returns modal-loop timer ticks delivered through WndProc.
    ///
    /// @return the tick count
    public int modalTimerTicks() {
        return nativeWindow.modalTimerTicks();
    }

    /// `WM_POWERBROADCAST`.
    public static final int WM_POWERBROADCAST = WindowsNativeWindow.WM_POWERBROADCAST;

    /// `PBT_APMSUSPEND`.
    public static final int PBT_APMSUSPEND = WindowsNativeWindow.PBT_APMSUSPEND;

    /// `PBT_APMRESUMESUSPEND`.
    public static final int PBT_APMRESUMESUSPEND = WindowsNativeWindow.PBT_APMRESUMESUSPEND;

    /// Sends one message synchronously through the production WndProc.
    ///
    /// @param message the Win32 message identifier
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the `WndProc` result
    public long sendMessage(int message, long wParam, long lParam) {
        return nativeWindow.sendMessage(message, wParam, lParam);
    }

    /// Returns observed `PBT_APMSUSPEND` deliveries through WndProc.
    ///
    /// @return the sleep count
    public int sleepEvents() {
        return nativeWindow.sleepEvents();
    }

    /// Returns observed `PBT_APMRESUMESUSPEND` deliveries through WndProc.
    ///
    /// @return the wake count
    public int wakeEvents() {
        return nativeWindow.wakeEvents();
    }

    /// Blits unassociated 8-bit sRGB RGBA pixels into this HWND and retains them for `WM_PAINT`.
    ///
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the scanline count reported by `SetDIBitsToDevice`
    public int presentSdrRgba(MemorySegment rgba, int width, int height) {
        return nativeWindow.presentSdrRgba(rgba, width, height);
    }

    /// Posts a left-button pointer sequence through the production WndProc.
    ///
    /// @param type the pointer kind
    /// @param x the client x
    /// @param y the client y
    public void postPointer(org.glavo.himari.layout.input.PointerEventType type, int x, int y) {
        postPointer(type, x, y, org.glavo.himari.layout.input.PointerDeviceKind.MOUSE);
    }

    /// Posts a pointer sequence through the production WndProc.
    ///
    /// Mouse devices use posted `WM_MOUSE*`. Touch and pen devices send `WM_POINTER*`
    /// synchronously because `PostMessageW` rejects those identifiers. `WM_POINTER*` is classified
    /// with generated `GetPointerType`; a failed query stays
    /// [`org.glavo.himari.layout.input.PointerDeviceKind#TOUCH`].
    ///
    /// @param type the pointer kind
    /// @param x the client x
    /// @param y the client y
    /// @param device the physical pointer
    public void postPointer(
            org.glavo.himari.layout.input.PointerEventType type,
            int x,
            int y,
            org.glavo.himari.layout.input.PointerDeviceKind device
    ) {
        postPointer(type, x, y, device, 0);
    }

    /// Posts a pointer sequence with an explicit host pointer identity.
    ///
    /// Touch and pen `WM_POINTER*` place `pointerId` in the low word of `wParam`.
    ///
    /// @param type the pointer kind
    /// @param x the client x
    /// @param y the client y
    /// @param device the physical pointer
    /// @param pointerId the host pointer identity
    public void postPointer(
            org.glavo.himari.layout.input.PointerEventType type,
            int x,
            int y,
            org.glavo.himari.layout.input.PointerDeviceKind device,
            int pointerId
    ) {
        Objects.requireNonNull(device, "device");
        long packed = WindowsNativeWindow.packPointer(x, y);
        int identity = Math.max(0, pointerId);
        if (type == org.glavo.himari.layout.input.PointerEventType.ENTER
                || type == org.glavo.himari.layout.input.PointerEventType.LEAVE
                || type == org.glavo.himari.layout.input.PointerEventType.CAPTURE_CHANGED
                || type == org.glavo.himari.layout.input.PointerEventType.ACTIVATE
                || type == org.glavo.himari.layout.input.PointerEventType.NON_CLIENT_MOVE
                || type == org.glavo.himari.layout.input.PointerEventType.NON_CLIENT_DOWN
                || type == org.glavo.himari.layout.input.PointerEventType.NON_CLIENT_UP
                || type == org.glavo.himari.layout.input.PointerEventType.ROUTED_TO
                || type == org.glavo.himari.layout.input.PointerEventType.ROUTED_AWAY
                || type == org.glavo.himari.layout.input.PointerEventType.ROUTED_RELEASED) {
            int message = switch (type) {
                case ENTER -> 0x0249;
                case LEAVE -> 0x024A;
                case ACTIVATE -> 0x024B;
                case NON_CLIENT_MOVE -> 0x0241;
                case NON_CLIENT_DOWN -> 0x0242;
                case NON_CLIENT_UP -> 0x0243;
                case ROUTED_TO -> 0x0251;
                case ROUTED_AWAY -> 0x0252;
                case ROUTED_RELEASED -> 0x0253;
                default -> 0x024C;
            };
            nativeWindow.sendMessage(message, pointerWParam(identity, type), packed);
            return;
        }
        switch (device) {
            case MOUSE -> {
                int message = switch (type) {
                    case MOVE -> 0x0200;
                    case DOWN -> 0x0201;
                    case UP -> 0x0202;
                    case WHEEL -> 0x020A;
                    case WHEEL_HORIZONTAL -> 0x020E;
                    case SECONDARY_DOWN -> 0x0204;
                    case SECONDARY_UP -> 0x0205;
                    case MIDDLE_DOWN -> 0x0207;
                    case MIDDLE_UP -> 0x0208;
                    case ENTER -> 0x0249;
                    case LEAVE -> 0x024A;
                    case CAPTURE_CHANGED -> 0x024C;
                    case ACTIVATE -> 0x024B;
                    case NON_CLIENT_MOVE -> 0x0241;
                    case NON_CLIENT_DOWN -> 0x0242;
                    case NON_CLIENT_UP -> 0x0243;
                    case ROUTED_TO -> 0x0251;
                    case ROUTED_AWAY -> 0x0252;
                    case ROUTED_RELEASED -> 0x0253;
                };
                long wParam = type == org.glavo.himari.layout.input.PointerEventType.WHEEL
                        || type == org.glavo.himari.layout.input.PointerEventType.WHEEL_HORIZONTAL
                        ? ((long) WindowsNativeWindow.WHEEL_DELTA) << 16
                        : mouseWParam(type);
                nativeWindow.postMessage(message, wParam, packed);
            }
            case TOUCH, PEN -> {
                int message = switch (type) {
                    case MOVE -> 0x0245;
                    case DOWN -> 0x0246;
                    case UP -> 0x0247;
                    case WHEEL -> 0x024E;
                    case WHEEL_HORIZONTAL -> 0x024F;
                    case SECONDARY_DOWN, SECONDARY_UP, MIDDLE_DOWN, MIDDLE_UP -> 0x0246;
                    case ENTER -> 0x0249;
                    case LEAVE -> 0x024A;
                    case CAPTURE_CHANGED -> 0x024C;
                    case ACTIVATE -> 0x024B;
                    case NON_CLIENT_MOVE -> 0x0241;
                    case NON_CLIENT_DOWN -> 0x0242;
                    case NON_CLIENT_UP -> 0x0243;
                    case ROUTED_TO -> 0x0251;
                    case ROUTED_AWAY -> 0x0252;
                    case ROUTED_RELEASED -> 0x0253;
                };
                long wParam = type == org.glavo.himari.layout.input.PointerEventType.WHEEL
                        || type == org.glavo.himari.layout.input.PointerEventType.WHEEL_HORIZONTAL
                        ? ((long) WindowsNativeWindow.WHEEL_DELTA) << 16
                        : pointerWParam(identity, type);
                nativeWindow.sendMessage(message, wParam, packed);
            }
        }
    }

    /// Packs `MK_*` bits for a posted `WM_MOUSE*` message.
    ///
    /// @param type the pointer kind
    /// @return the `wParam`
    private static long mouseWParam(org.glavo.himari.layout.input.PointerEventType type) {
        return switch (type) {
            case DOWN, MOVE -> 0x0001L;
            case SECONDARY_DOWN -> 0x0002L;
            case MIDDLE_DOWN -> 0x0010L;
            case UP, SECONDARY_UP, MIDDLE_UP, WHEEL, WHEEL_HORIZONTAL, ENTER, LEAVE, CAPTURE_CHANGED, ACTIVATE,
                    NON_CLIENT_MOVE, NON_CLIENT_DOWN, NON_CLIENT_UP, ROUTED_TO, ROUTED_AWAY, ROUTED_RELEASED -> 0L;
        };
    }

    /// Packs pointer identity and `POINTER_MESSAGE_FLAG_*` for a posted `WM_POINTER*`.
    ///
    /// @param pointerId the host pointer identity
    /// @param type the pointer kind
    /// @return the `wParam`
    private static long pointerWParam(int pointerId, org.glavo.himari.layout.input.PointerEventType type) {
        long wParam = Integer.toUnsignedLong(pointerId);
        int flags = switch (type) {
            case DOWN -> 0x0010;
            case SECONDARY_DOWN -> 0x0020;
            case MIDDLE_DOWN -> 0x0040;
            case MOVE, UP, WHEEL, WHEEL_HORIZONTAL, SECONDARY_UP, MIDDLE_UP, ENTER, LEAVE, CAPTURE_CHANGED, ACTIVATE,
                    NON_CLIENT_MOVE, NON_CLIENT_DOWN, NON_CLIENT_UP, ROUTED_TO, ROUTED_AWAY, ROUTED_RELEASED -> 0;
        };
        return wParam | ((long) flags << 16);
    }

    /// Posts a virtual-key event through the production WndProc.
    ///
    /// @param down whether this is `WM_KEYDOWN`
    /// @param virtualKey the virtual-key code
    public void postVirtualKey(boolean down, int virtualKey) {
        postVirtualKey(down, virtualKey, 0, false);
    }

    /// Posts a virtual-key event with a physical scan code and auto-repeat bit.
    ///
    /// @param down whether this is `WM_KEYDOWN`
    /// @param virtualKey the virtual-key code
    /// @param scanCode the OEM scan code placed in bits 16–23 of `lParam`
    /// @param repeat whether bit 30 (`KF_REPEAT` previous-state) is set
    public void postVirtualKey(boolean down, int virtualKey, int scanCode, boolean repeat) {
        long lParam = 1L;
        lParam |= Integer.toUnsignedLong(scanCode & 0xFF) << 16;
        if (repeat) {
            lParam |= 1L << 30;
        }
        nativeWindow.postMessage(down ? 0x0100 : 0x0101, Integer.toUnsignedLong(virtualKey), lParam);
    }

    /// Posts a virtual-key event with scan code, auto-repeat, and `KF_EXTENDED`.
    ///
    /// @param down whether this is `WM_KEYDOWN`
    /// @param virtualKey the virtual-key code
    /// @param scanCode the OEM scan code placed in bits 16–23 of `lParam`
    /// @param repeat whether bit 30 is set
    /// @param extended whether bit 24 (`KF_EXTENDED`) is set
    public void postVirtualKey(boolean down, int virtualKey, int scanCode, boolean repeat, boolean extended) {
        long lParam = 1L;
        lParam |= Integer.toUnsignedLong(scanCode & 0xFF) << 16;
        if (extended) {
            lParam |= 1L << 24;
        }
        if (repeat) {
            lParam |= 1L << 30;
        }
        nativeWindow.postMessage(down ? 0x0100 : 0x0101, Integer.toUnsignedLong(virtualKey), lParam);
    }

    /// Posts a `WM_SYSKEYDOWN` or `WM_SYSKEYUP` through the production WndProc.
    ///
    /// @param down whether this is `WM_SYSKEYDOWN`
    /// @param virtualKey the virtual-key code
    public void postSysVirtualKey(boolean down, int virtualKey) {
        nativeWindow.postMessage(down ? 0x0104 : 0x0105, Integer.toUnsignedLong(virtualKey), 1L | (1L << 29));
    }

    /// Posts a `WM_XBUTTON*` through the production WndProc.
    ///
    /// @param down whether this is `WM_XBUTTONDOWN`
    /// @param x the client x
    /// @param y the client y
    /// @param button `1` for `XBUTTON1`, `2` for `XBUTTON2`
    public void postXButton(boolean down, int x, int y, int button) {
        if (button != 1 && button != 2) {
            throw new IllegalArgumentException("X button must be 1 or 2");
        }
        long packed = WindowsNativeWindow.packPointer(x, y);
        long wParam = Integer.toUnsignedLong(button) << 16;
        nativeWindow.postMessage(down ? 0x020B : 0x020C, wParam, packed);
    }

    /// Posts a `WM_CHAR` through the production WndProc.
    ///
    /// @param codeUnit the UTF-16 code unit
    public void postChar(char codeUnit) {
        nativeWindow.postMessage(0x0102, codeUnit, 0L);
    }

    /// Posts a `WM_SYSCHAR` through the production WndProc.
    ///
    /// @param codeUnit the UTF-16 code unit
    public void postSysChar(char codeUnit) {
        nativeWindow.postMessage(0x0106, codeUnit, 0L);
    }

    /// Posts a `WM_SYSDEADCHAR` through the production WndProc.
    ///
    /// @param codeUnit the dead-key UTF-16 code unit
    public void postSysDeadChar(char codeUnit) {
        nativeWindow.postMessage(0x0107, codeUnit, 0L);
    }

    /// Posts a `WM_DEADCHAR` through the production WndProc.
    ///
    /// @param codeUnit the dead-key UTF-16 code unit
    public void postDeadChar(char codeUnit) {
        nativeWindow.postMessage(0x0103, codeUnit, 0L);
    }

    /// Posts a `WM_UNICHAR` through the production WndProc.
    ///
    /// @param codePoint the Unicode scalar value
    public void postUnichar(int codePoint) {
        nativeWindow.postMessage(0x0109, Integer.toUnsignedLong(codePoint), 0L);
    }

    /// Sends the `UNICODE_NOCHAR` probe and returns whether WndProc advertised `WM_UNICHAR` support.
    ///
    /// @return whether the production WndProc returned `TRUE`
    public boolean supportsUnichar() {
        return nativeWindow.sendMessage(0x0109, 0xFFFFL, 0L) != 0L;
    }

    /// Returns generated `GetKeyState` for `virtualKey`.
    ///
    /// @param virtualKey a Win32 virtual-key code
    /// @return the raw `SHORT` result
    public short keyState(int virtualKey) {
        return nativeWindow.keyState(virtualKey);
    }

    /// Copies generated `GetKeyboardState` into `state`.
    ///
    /// @param state a 256-byte destination
    /// @return whether `GetKeyboardState` succeeded
    public boolean copyKeyboardState(byte[] state) {
        return nativeWindow.copyKeyboardState(state);
    }

    /// Returns generated `GetAsyncKeyState` for `virtualKey`.
    ///
    /// @param virtualKey a Win32 virtual-key code
    /// @return the raw `SHORT` result
    public short asyncKeyState(int virtualKey) {
        return nativeWindow.asyncKeyState(virtualKey);
    }

    /// Translates `virtualKey` through generated `ToUnicodeW` without mutating dead-key state.
    ///
    /// @param virtualKey the virtual-key code
    /// @param scanCode the OEM scan code
    /// @return the translated string, or `null` when `ToUnicodeW` wrote no characters
    public @Nullable String translateVirtualKey(int virtualKey, int scanCode) {
        return nativeWindow.translateVirtualKey(virtualKey, scanCode);
    }

    /// Removes printable `ToUnicodeW` text delivered on key-down since the last drain.
    ///
    /// @return the translated characters, possibly empty
    public String takeTranslatedCharacters() {
        return nativeWindow.takeTranslatedCharacters();
    }

    /// Maps `character` onto a virtual-key plus shift state through generated `VkKeyScanW`.
    ///
    /// @param character the UTF-16 code unit
    /// @return the raw `SHORT` result
    public short scanVirtualKey(char character) {
        return nativeWindow.scanVirtualKey(character);
    }

    /// Returns the last `VkKeyScanW` result observed on `WM_CHAR`.
    ///
    /// @return the raw `SHORT` result
    public short lastCharVirtualKeyScan() {
        return nativeWindow.lastCharVirtualKeyScan();
    }

    /// Returns the last `GetKeyNameTextW` string observed on key-down.
    ///
    /// @return the name, possibly empty
    public String lastKeyName() {
        return nativeWindow.lastKeyName();
    }

    /// Returns the last `GetKeyboardLayout` handle address observed on key-down.
    ///
    /// @return the layout handle, or `0` before the first key-down
    public long lastKeyboardLayout() {
        return nativeWindow.lastKeyboardLayout();
    }

    /// Returns the last `ImmGetCompositionStringW` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionStringBytes() {
        return nativeWindow.lastCompositionStringBytes();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_CURSORPOS` result.
    ///
    /// @return the cursor, or `Integer.MIN_VALUE` before a query
    public int lastCompositionCursor() {
        return nativeWindow.lastCompositionCursor();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPATTR` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionAttrBytes() {
        return nativeWindow.lastCompositionAttrBytes();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPREADSTR` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionReadingBytes() {
        return nativeWindow.lastCompositionReadingBytes();
    }

    /// Returns the last `ImmGetCompositionWindow` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastCompositionWindowResult() {
        return nativeWindow.lastCompositionWindowResult();
    }

    /// Returns the last `ImmGetCandidateWindow` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastCandidateWindowResult() {
        return nativeWindow.lastCandidateWindowResult();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPCLAUSE` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionClauseBytes() {
        return nativeWindow.lastCompositionClauseBytes();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_RESULTREADSTR` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastResultReadingBytes() {
        return nativeWindow.lastResultReadingBytes();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_RESULTCLAUSE` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastResultClauseBytes() {
        return nativeWindow.lastResultClauseBytes();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_DELTASTART` result.
    ///
    /// @return the offset, or `Integer.MIN_VALUE` before a query
    public int lastCompositionDeltaStart() {
        return nativeWindow.lastCompositionDeltaStart();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPREADATTR` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionReadingAttrBytes() {
        return nativeWindow.lastCompositionReadingAttrBytes();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_COMPREADCLAUSE` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastCompositionReadingClauseBytes() {
        return nativeWindow.lastCompositionReadingClauseBytes();
    }

    /// Returns the last `ImmGetCompositionStringW` `GCS_RESULTREADCLAUSE` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastResultReadingClauseBytes() {
        return nativeWindow.lastResultReadingClauseBytes();
    }

    /// Returns the last `GetPointerFramePenInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFramePenHistoryEntries() {
        return nativeWindow.lastPointerFramePenHistoryEntries();
    }

    /// Returns the last `GetPointerFrameTouchInfoHistory` entry count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastPointerFrameTouchHistoryEntries() {
        return nativeWindow.lastPointerFrameTouchHistoryEntries();
    }

    /// Returns the last `ImmGetVirtualKey` result.
    ///
    /// @return the virtual key, or `Integer.MIN_VALUE` before a query
    public int lastImeVirtualKey() {
        return nativeWindow.lastImeVirtualKey();
    }

    /// Returns the last `WM_IME_REQUEST` command.
    ///
    /// @return the `IMR_*` command, or `Integer.MIN_VALUE` before a request
    public int lastImeRequest() {
        return nativeWindow.lastImeRequest();
    }

    /// Returns the last `WM_IME_REQUEST` byte count returned to the host.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a request
    public int lastImeRequestBytes() {
        return nativeWindow.lastImeRequestBytes();
    }

    /// Returns the last `IMECHARPOSITION.dwCharPos`.
    ///
    /// @return the character offset, or `Integer.MIN_VALUE` before a query
    public int lastImeCharPos() {
        return nativeWindow.lastImeCharPos();
    }

    /// Returns the previous `HIMC` from `ImmAssociateContext`.
    ///
    /// @return the handle address, or `-1` before a call
    public long lastAssociateContext() {
        return nativeWindow.lastAssociateContext();
    }

    /// Returns the last `ImmAssociateContextEx` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a call
    public int lastAssociateContextExResult() {
        return nativeWindow.lastAssociateContextExResult();
    }

    /// Returns the last `ImmIsIME` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastImeIsIme() {
        return nativeWindow.lastImeIsIme();
    }

    /// Returns the last `ImmGetImeMenuItemsW` item count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastImeMenuItemCount() {
        return nativeWindow.lastImeMenuItemCount();
    }

    /// Returns the last `ImmEscapeW` result.
    ///
    /// @return the `LRESULT`, or `Long.MIN_VALUE` before a query
    public long lastImeEscapeResult() {
        return nativeWindow.lastImeEscapeResult();
    }

    /// Returns the last `ImmGetDescriptionW` character count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastImeDescriptionChars() {
        return nativeWindow.lastImeDescriptionChars();
    }

    /// Returns the last `ImmGetDescriptionW` string.
    ///
    /// @return the description, possibly empty
    public String lastImeDescription() {
        return nativeWindow.lastImeDescription();
    }

    /// Returns the last `ImmGetProperty` bits.
    ///
    /// @return the bits, or `Integer.MIN_VALUE` before a query
    public int lastImeProperty() {
        return nativeWindow.lastImeProperty();
    }

    /// Returns the last `ImmGetDefaultIMEWnd` handle address.
    ///
    /// @return the handle, or `-1` before a query
    public long lastDefaultImeWnd() {
        return nativeWindow.lastDefaultImeWnd();
    }

    /// Returns the last `ImmGetStatusWindowPos` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastStatusWindowPosResult() {
        return nativeWindow.lastStatusWindowPosResult();
    }

    /// Returns the last `ImmSetStatusWindowPos` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetStatusWindowPosResult() {
        return nativeWindow.lastSetStatusWindowPosResult();
    }

    /// Returns the last `ImmGetHotKey` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastImeHotKeyResult() {
        return nativeWindow.lastImeHotKeyResult();
    }

    /// Returns the last `ImmSetHotKey` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a write
    public int lastSetHotKeyResult() {
        return nativeWindow.lastSetHotKeyResult();
    }

    /// Returns the last `ImmGetConversionListW` `GCL_REVERSECONVERSION` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastConversionReverseBytes() {
        return nativeWindow.lastConversionReverseBytes();
    }

    /// Returns the last `ImmIsUIMessageW` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastImeIsUiMessage() {
        return nativeWindow.lastImeIsUiMessage();
    }

    /// Returns the last `ImmGetRegisterWordStyleW` style count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastRegisterWordStyleCount() {
        return nativeWindow.lastRegisterWordStyleCount();
    }

    /// Returns the last `ImmEnumInputContext` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastEnumInputContextResult() {
        return nativeWindow.lastEnumInputContextResult();
    }

    /// Returns the number of contexts delivered to the last `IMCENUMPROC`.
    ///
    /// @return the count
    public int lastEnumInputContextCount() {
        return nativeWindow.lastEnumInputContextCount();
    }

    /// Returns the last `ImmEnumRegisterWordW` count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastEnumRegisterWordCount() {
        return nativeWindow.lastEnumRegisterWordCount();
    }

    /// Returns the number of words delivered to the last `REGISTERWORDENUMPROCW`.
    ///
    /// @return the count
    public int lastEnumRegisterWordHits() {
        return nativeWindow.lastEnumRegisterWordHits();
    }

    /// Returns the last `ImmRequestMessageW` result.
    ///
    /// @return the `LRESULT`, or `Long.MIN_VALUE` before a query
    public long lastImmRequestMessageResult() {
        return nativeWindow.lastImmRequestMessageResult();
    }

    /// Returns the last `ImmRegisterWordW` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastRegisterWordResult() {
        return nativeWindow.lastRegisterWordResult();
    }

    /// Returns the last `ImmUnregisterWordW` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastUnregisterWordResult() {
        return nativeWindow.lastUnregisterWordResult();
    }

    /// Returns the last `CountClipboardFormats` result.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastClipboardFormatCount() {
        return nativeWindow.lastClipboardFormatCount();
    }

    /// Returns the last `GetClipboardSequenceNumber` result.
    ///
    /// @return the sequence, or `Integer.MIN_VALUE` before a query
    public int lastClipboardSequence() {
        return nativeWindow.lastClipboardSequence();
    }

    /// Returns the last `GetClipboardOwner` handle address.
    ///
    /// @return the handle address, or `-1` before a query
    public long lastClipboardOwner() {
        return nativeWindow.lastClipboardOwner();
    }

    /// Returns the last `GetOpenClipboardWindow` handle address.
    ///
    /// @return the handle address, or `-1` before a query
    public long lastOpenClipboardWindow() {
        return nativeWindow.lastOpenClipboardWindow();
    }

    /// Returns the last `IsClipboardFormatAvailable(CF_UNICODETEXT)` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastClipboardUnicodeAvailable() {
        return nativeWindow.lastClipboardUnicodeAvailable();
    }

    /// Returns the last `GetPriorityClipboardFormat` result.
    ///
    /// @return the format, `0`, or `-1`, or `Integer.MIN_VALUE` before a query
    public int lastPriorityClipboardFormat() {
        return nativeWindow.lastPriorityClipboardFormat();
    }

    /// Returns the first format from `EnumClipboardFormats(0)`.
    ///
    /// @return the format, `0` when the clipboard is empty, or `Integer.MIN_VALUE` before a query
    public int lastEnumClipboardFormat() {
        return nativeWindow.lastEnumClipboardFormat();
    }

    /// Returns the number of formats walked by `EnumClipboardFormats`.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastEnumClipboardFormatCount() {
        return nativeWindow.lastEnumClipboardFormatCount();
    }

    /// Returns the last `GetClipboardFormatNameW` character count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastClipboardFormatNameChars() {
        return nativeWindow.lastClipboardFormatNameChars();
    }

    /// Returns the last `GetUpdatedClipboardFormats` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastUpdatedClipboardFormatsResult() {
        return nativeWindow.lastUpdatedClipboardFormatsResult();
    }

    /// Returns the last `GetUpdatedClipboardFormats` reported format count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastUpdatedClipboardFormatCount() {
        return nativeWindow.lastUpdatedClipboardFormatCount();
    }

    /// Returns the last `AddClipboardFormatListener` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastAddClipboardFormatListenerResult() {
        return nativeWindow.lastAddClipboardFormatListenerResult();
    }

    /// Returns the last `RemoveClipboardFormatListener` BOOL result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a query
    public int lastRemoveClipboardFormatListenerResult() {
        return nativeWindow.lastRemoveClipboardFormatListenerResult();
    }

    /// Returns the number of `WM_CLIPBOARDUPDATE` deliveries.
    ///
    /// @return the count
    public int lastClipboardUpdateCount() {
        return nativeWindow.lastClipboardUpdateCount();
    }

    /// Returns the last `ImmGetConversionListW` byte count.
    ///
    /// @return the byte count, or `Integer.MIN_VALUE` before a query
    public int lastConversionListBytes() {
        return nativeWindow.lastConversionListBytes();
    }

    /// Returns the last `ImmGetConversionListW` `GCL_REVERSE_LENGTH` result.
    ///
    /// @return the length, or `Integer.MIN_VALUE` before a query
    public int lastConversionReverseLength() {
        return nativeWindow.lastConversionReverseLength();
    }

    /// Returns the last `ImmSimulateHotKey` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a call
    public int lastSimulateHotKeyResult() {
        return nativeWindow.lastSimulateHotKeyResult();
    }

    /// Returns the last `WM_IME_CONTROL` command.
    ///
    /// @return the `IMC_*` command, or `Integer.MIN_VALUE` before a delivery
    public int lastImeControl() {
        return nativeWindow.lastImeControl();
    }

    /// Returns the `HIMC` created by `ImmCreateContext`.
    ///
    /// @return the handle address, or `-1` before a call
    public long lastCreateContext() {
        return nativeWindow.lastCreateContext();
    }

    /// Returns the last `WM_IME_SETCONTEXT` `wParam`.
    ///
    /// @return the activation flag, or `Integer.MIN_VALUE` before a delivery
    public int lastImeSetContext() {
        return nativeWindow.lastImeSetContext();
    }

    /// Returns the last `WM_IME_SELECT` `wParam`.
    ///
    /// @return the selection flag, or `Integer.MIN_VALUE` before a delivery
    public int lastImeSelect() {
        return nativeWindow.lastImeSelect();
    }

    /// Sends `WM_IME_REQUEST` through the production WndProc.
    ///
    /// The host delivers this message synchronously; `PostMessageW` rejects it with
    /// `ERROR_MESSAGE_SYNC_ONLY`.
    ///
    /// @param command an `IMR_*` command
    /// @return the byte count returned by the production handler
    public long sendImeRequest(int command) {
        return nativeWindow.sendMessage(0x0288, Integer.toUnsignedLong(command), 0L);
    }

    /// Returns the last `ImmNotifyIME` result.
    ///
    /// @return the BOOL result, or `Integer.MIN_VALUE` before a notify
    public int lastImmNotifyResult() {
        return nativeWindow.lastImmNotifyResult();
    }

    /// Returns whether `WM_IME_STARTCOMPOSITION` is unmatched.
    ///
    /// @return whether an IMM32 composition is active
    public boolean imeActive() {
        return nativeWindow.imeActive();
    }

    /// Returns whether `TrackMouseEvent(TME_LEAVE)` is outstanding.
    ///
    /// @return whether leave tracking is armed
    public boolean mouseLeaveTracked() {
        return nativeWindow.mouseLeaveTracked();
    }

    /// Returns whether the last generated `TrackMouseEvent` call succeeded.
    ///
    /// @return whether the host accepted leave tracking
    public boolean lastTrackMouseEventSucceeded() {
        return nativeWindow.lastTrackMouseEventSucceeded();
    }

    /// Reads one IMM32 composition index through generated `ImmGetCompositionStringW`.
    ///
    /// @param index `GCS_COMPSTR` or `GCS_RESULTSTR`
    /// @return the string, or empty when the host has no composition
    public String compositionString(int index) {
        return nativeWindow.compositionString(index);
    }

    /// Writes one IMM32 composition string through generated `ImmSetCompositionStringW`.
    ///
    /// @param text the composition string
    /// @return whether the host accepted the string
    public boolean setCompositionString(String text) {
        return nativeWindow.setCompositionString(text);
    }

    /// Posts `WM_IME_STARTCOMPOSITION` through the production WndProc.
    public void postImeStart() {
        nativeWindow.postMessage(0x010D, 0L, 0L);
    }

    /// Posts `WM_IME_COMPOSITION` through the production WndProc.
    ///
    /// @param gcs the `GCS_*` bits placed in `lParam`
    public void postImeComposition(int gcs) {
        nativeWindow.postMessage(0x010F, 0L, Integer.toUnsignedLong(gcs));
    }

    /// Posts `WM_IME_ENDCOMPOSITION` through the production WndProc.
    public void postImeEnd() {
        nativeWindow.postMessage(0x010E, 0L, 0L);
    }

    /// Posts `WM_IME_NOTIFY` through the production WndProc.
    ///
    /// @param command an `IMN_*` command
    /// @param listIndex the candidate-list index
    public void postImeNotify(int command, int listIndex) {
        nativeWindow.postMessage(0x0282, Integer.toUnsignedLong(command), Integer.toUnsignedLong(listIndex));
    }

    /// Returns the last `ImmGetCandidateListW` candidate count.
    ///
    /// @return the count, or `Integer.MIN_VALUE` before a query
    public int lastCandidateCount() {
        return nativeWindow.lastCandidateCount();
    }

    /// Returns the last `ImmGetCandidateListW` selection index.
    ///
    /// @return the selection
    public int lastCandidateSelection() {
        return nativeWindow.lastCandidateSelection();
    }

    /// Returns the last `ImmGetCandidateListW` page strings.
    ///
    /// @return the page, possibly empty
    public @Unmodifiable List<String> lastCandidatePage() {
        return nativeWindow.lastCandidatePage();
    }

    /// Posts `WM_IME_CHAR` through the production WndProc.
    ///
    /// @param codeUnit the UTF-16 code unit
    public void postImeChar(char codeUnit) {
        nativeWindow.postMessage(0x0286, codeUnit, 0L);
    }

    /// Posts `WM_MOUSELEAVE` through the production WndProc.
    public void postMouseLeave() {
        nativeWindow.postMessage(0x02A3, 0L, 0L);
    }

    /// {@inheritDoc}
    @Override
    public CompletionStage<WindowSnapshot> configure(WindowConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        if (platform.eventLoop().isOwnerThread()) {
            return CompletableFuture.completedFuture(platform.configureWindow(this, configuration));
        }
        CompletableFuture<WindowSnapshot> future = new CompletableFuture<>();
        platform.eventLoop().post(() -> {
            try {
                future.complete(platform.configureWindow(this, configuration));
            } catch (RuntimeException | Error failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    /// {@inheritDoc}
    @Override
    public void requestRedraw() {
        platform.eventLoop().post(() -> platform.requestRedraw(this));
    }

    /// {@inheritDoc}
    @Override
    public CompletionStage<Void> closeAsync() {
        if (platform.eventLoop().isOwnerThread()) {
            platform.closeWindow(this);
            return closeCompletion;
        }
        platform.eventLoop().post(() -> platform.closeWindow(this));
        return closeCompletion;
    }

    /// {@inheritDoc}
    @Override
    public boolean isClosed() {
        return snapshot.lifecycle() == org.glavo.himari.platform.api.WindowLifecycle.CLOSED;
    }

    /// Returns the native owner.
    ///
    /// @return the native window
    WindowsNativeWindow nativeWindow() {
        return nativeWindow;
    }

    /// Drains WndProc-normalized input into the Java queues and IME session.
    void consumeNativeInput() {
        pendingPointers.addAll(nativeWindow.takePointerEvents());
        pendingKeys.addAll(nativeWindow.takeKeyEvents());
        String dead = nativeWindow.takeDeadCharacters();
        if (!dead.isEmpty()) {
            ime.updateComposition(dead);
        }
        String composition = nativeWindow.takeImeComposition();
        if (!composition.isEmpty()) {
            ime.updateComposition(composition);
        }
        if (nativeWindow.lastCompositionCursor() != Integer.MIN_VALUE) {
            ime.setCompositionCursor(nativeWindow.lastCompositionCursor());
        }
        if (nativeWindow.lastCompositionAttrBytes() != Integer.MIN_VALUE) {
            ime.setCompositionAttributes(nativeWindow.lastCompositionAttributes());
        }
        String reading = nativeWindow.takeImeReading();
        if (!reading.isEmpty() || nativeWindow.lastCompositionReadingBytes() != Integer.MIN_VALUE) {
            ime.setCompositionReading(reading);
        }
        if (nativeWindow.lastCompositionClauseBytes() != Integer.MIN_VALUE) {
            ime.setCompositionClause(nativeWindow.lastCompositionClause());
        }
        String resultReading = nativeWindow.takeImeResultReading();
        if (!resultReading.isEmpty() || nativeWindow.lastResultReadingBytes() != Integer.MIN_VALUE) {
            ime.setResultReading(resultReading);
        }
        if (nativeWindow.lastResultClauseBytes() != Integer.MIN_VALUE) {
            ime.setResultClause(nativeWindow.lastResultClause());
        }
        if (nativeWindow.lastCompositionDeltaStart() != Integer.MIN_VALUE) {
            ime.setCompositionDeltaStart(nativeWindow.lastCompositionDeltaStart());
        }
        if (nativeWindow.lastCompositionReadingAttrBytes() != Integer.MIN_VALUE) {
            ime.setCompositionReadingAttributes(nativeWindow.lastCompositionReadingAttributes());
        }
        if (nativeWindow.lastCompositionReadingClauseBytes() != Integer.MIN_VALUE) {
            ime.setCompositionReadingClause(nativeWindow.lastCompositionReadingClause());
        }
        if (nativeWindow.lastResultReadingClauseBytes() != Integer.MIN_VALUE) {
            ime.setResultReadingClause(nativeWindow.lastResultReadingClause());
        }
        String imeResult = nativeWindow.takeImeResult();
        if (!imeResult.isEmpty()) {
            ime.updateComposition(imeResult);
            ime.commit();
        }
        String characters = nativeWindow.takeCharacters();
        if (!characters.isEmpty()) {
            ime.updateComposition(characters);
            ime.commit();
        }
        if (nativeWindow.takeImeEnded() && ime.composition() != null) {
            ime.cancel();
        }
        ime.setGuideline(nativeWindow.takeGuideline());
        if (ime.pendingConversion() >= 0) {
            nativeWindow.setConversionStatus(ime.pendingConversion(), ime.pendingSentence());
            ime.clearPendingConversion();
        }
        if (ime.pendingOpen() != null) {
            nativeWindow.setOpenStatus(ime.pendingOpen());
            ime.clearPendingOpen();
        }
        if (ime.pendingFontFace() != null) {
            nativeWindow.setCompositionFontFace(ime.pendingFontFace());
            ime.clearPendingFontFace();
        }
        nativeWindow.publishImeDocument(
                ime.surroundingText(),
                ime.lastCommitted() == null ? "" : ime.lastCommitted(),
                ime.candidateX(),
                ime.candidateY(),
                ime.candidateWidth(),
                ime.candidateHeight()
        );
        if (ime.candidateWidth() > 0.0f || ime.candidateHeight() > 0.0f) {
            nativeWindow.setStatusWindowPos(Math.round(ime.candidateX()), Math.round(ime.candidateY()));
        }
    }

    /// Publishes a replacement snapshot.
    ///
    /// @param next the next snapshot
    void publish(WindowSnapshot next) {
        snapshot = next;
    }

    /// Completes close.
    void completeClose() {
        closeCompletion.complete(null);
    }

    /// Returns the close future.
    ///
    /// @return the future
    CompletableFuture<Void> closeFuture() {
        return closeCompletion;
    }
}
