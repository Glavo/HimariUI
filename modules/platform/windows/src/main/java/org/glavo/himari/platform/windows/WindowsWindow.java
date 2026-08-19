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
                || type == org.glavo.himari.layout.input.PointerEventType.NON_CLIENT_UP) {
            int message = switch (type) {
                case ENTER -> 0x0249;
                case LEAVE -> 0x024A;
                case ACTIVATE -> 0x024B;
                case NON_CLIENT_MOVE -> 0x0241;
                case NON_CLIENT_DOWN -> 0x0242;
                case NON_CLIENT_UP -> 0x0243;
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
                    NON_CLIENT_MOVE, NON_CLIENT_DOWN, NON_CLIENT_UP -> 0L;
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
                    NON_CLIENT_MOVE, NON_CLIENT_DOWN, NON_CLIENT_UP -> 0;
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
        String characters = nativeWindow.takeCharacters();
        if (!characters.isEmpty()) {
            ime.updateComposition(characters);
            ime.commit();
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
