package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.KeyLocation;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerDeviceKind;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Owns one HWND created through generated FFM bindings and normalizes host input.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsNativeWindow implements AutoCloseable {
    /// Standard overlapped style.
    private static final int WS_OVERLAPPEDWINDOW = 0x00CF0000;

    /// Popup style for owner-relative windows.
    private static final int WS_POPUP = 0x80000000;

    /// Tool window style.
    private static final int WS_EX_TOOLWINDOW = 0x00000080;

    /// No-activate style for conformance windows.
    private static final int WS_EX_NOACTIVATE = 0x08000000;

    /// Show without activation.
    private static final int SW_SHOWNOACTIVATE = 4;

    /// Hide.
    private static final int SW_HIDE = 0;

    /// Close request.
    private static final int WM_CLOSE = 0x0010;

    /// Destroyed.
    private static final int WM_DESTROY = 0x0002;

    /// Size change.
    private static final int WM_SIZE = 0x0005;

    /// Per-monitor DPI change.
    private static final int WM_DPICHANGED = 0x02E0;

    /// Pointer move.
    private static final int WM_MOUSEMOVE = 0x0200;

    /// Left button down.
    private static final int WM_LBUTTONDOWN = 0x0201;

    /// Left button up.
    private static final int WM_LBUTTONUP = 0x0202;

    /// Right button down.
    private static final int WM_RBUTTONDOWN = 0x0204;

    /// Right button up.
    private static final int WM_RBUTTONUP = 0x0205;

    /// Middle button down.
    private static final int WM_MBUTTONDOWN = 0x0207;

    /// Middle button up.
    private static final int WM_MBUTTONUP = 0x0208;

    /// Extra button down.
    private static final int WM_XBUTTONDOWN = 0x020B;

    /// Extra button up.
    private static final int WM_XBUTTONUP = 0x020C;

    /// `VK_SHIFT`.
    private static final int VK_SHIFT = 0x10;

    /// `VK_CONTROL`.
    private static final int VK_CONTROL = 0x11;

    /// `VK_MENU` (Alt).
    private static final int VK_MENU = 0x12;

    /// `VK_LWIN`.
    private static final int VK_LWIN = 0x5B;

    /// `VK_RWIN`.
    private static final int VK_RWIN = 0x5C;

    /// Vertical mouse wheel.
    private static final int WM_MOUSEWHEEL = 0x020A;

    /// Horizontal mouse wheel.
    private static final int WM_MOUSEHWHEEL = 0x020E;

    /// Vertical pointer wheel.
    private static final int WM_POINTERWHEEL = 0x0248;

    /// Horizontal pointer wheel.
    private static final int WM_POINTERHWHEEL = 0x0249;

    /// `IDC_ARROW`.
    public static final int IDC_ARROW = 32512;

    /// `WHEEL_DELTA`.
    public static final int WHEEL_DELTA = 120;

    /// `PT_TOUCH`.
    public static final int PT_TOUCH = 2;

    /// `PT_PEN`.
    public static final int PT_PEN = 3;

    /// `PT_MOUSE`.
    public static final int PT_MOUSE = 4;

    /// Pointer update.
    private static final int WM_POINTERUPDATE = 0x0245;

    /// Pointer down.
    private static final int WM_POINTERDOWN = 0x0246;

    /// Pointer up.
    private static final int WM_POINTERUP = 0x0247;

    /// Key down.
    private static final int WM_KEYDOWN = 0x0100;

    /// Key up.
    private static final int WM_KEYUP = 0x0101;

    /// Translated character.
    private static final int WM_CHAR = 0x0102;

    /// Enter move/resize modal loop.
    private static final int WM_ENTERSIZEMOVE = 0x0231;

    /// Exit move/resize modal loop.
    private static final int WM_EXITSIZEMOVE = 0x0232;

    /// Paint request.
    private static final int WM_PAINT = 0x000F;

    /// Background erase request.
    private static final int WM_ERASEBKGND = 0x0014;

    /// Timer.
    private static final int WM_TIMER = 0x0113;

    /// `WM_POWERBROADCAST`.
    static final int WM_POWERBROADCAST = 0x0218;

    /// `PBT_APMSUSPEND`.
    static final int PBT_APMSUSPEND = 0x0004;

    /// `PBT_APMRESUMESUSPEND`.
    static final int PBT_APMRESUMESUSPEND = 0x0007;

    /// Timer id used for modal-loop reentry.
    static final long MODAL_TIMER_ID = 0x484D5249L;

    /// Set-position flags: no z-order or activation.
    private static final int SWP_NOZORDER = 0x0004;

    /// Set-position flags: no activation.
    private static final int SWP_NOACTIVATE = 0x0010;

    /// Default USER32 DPI.
    private static final int USER_DEFAULT_SCREEN_DPI = 96;

    /// Generated bindings.
    private final Win32FfmBindings bindings;

    /// Confined arena for this window.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue callbackFailures;

    /// Module handle.
    private final MemorySegment instance;

    /// Class name.
    private final MemorySegment className;

    /// Reusable RECT for geometry queries.
    private final MemorySegment rectRecord;

    /// Reusable `PAINTSTRUCT` for `WM_PAINT`.
    private final MemorySegment paintRecord;

    /// Close and destroy listener.
    private final Lifecycle lifecycle;

    /// Pointer events received through WndProc since the last drain.
    private final ArrayList<PointerEvent> pointerEvents = new ArrayList<>();

    /// Synthetic pen axes used when `GetPointerPenInfo` has no live contact.
    private @Nullable PenAxes syntheticPenAxes;

    /// Pointer identity that [`#syntheticPenAxes`] applies to.
    private int syntheticPenPointerId = -1;

    /// Synthetic contact ellipse used when `GetPointerTouchInfo` has no live contact.
    private @Nullable ContactArea syntheticContact;

    /// Pointer identity that [`#syntheticContact`] applies to.
    private int syntheticContactPointerId = -1;

    /// Synthetic `POINTER_INFO` flags used when `GetPointerInfo` has no live contact.
    private @Nullable PointerFlags syntheticPointerFlags;

    /// Pointer identity that [`#syntheticPointerFlags`] applies to.
    private int syntheticPointerFlagsId = -1;

    /// Host delivery sequence assigned to the next pointer event.
    private int pointerSequence;

    /// Key events received through WndProc since the last drain.
    private final ArrayList<KeyEvent> keyEvents = new ArrayList<>();

    /// UTF-16 code units received as `WM_CHAR` since the last drain.
    private final StringBuilder characters = new StringBuilder();

    /// Whether `VK_SHIFT` is latched from `WM_KEYDOWN`/`WM_KEYUP`.
    private boolean shiftDown;

    /// Whether `VK_CONTROL` is latched from `WM_KEYDOWN`/`WM_KEYUP`.
    private boolean ctrlDown;

    /// Whether `VK_MENU` is latched from `WM_KEYDOWN`/`WM_KEYUP`.
    private boolean altDown;

    /// Whether `VK_LWIN` or `VK_RWIN` is latched from `WM_KEYDOWN`/`WM_KEYUP`.
    private boolean metaDown;

    /// Whether the class is registered.
    private boolean classRegistered;

    /// Whether WM_DESTROY ran.
    private boolean destroyObserved;

    /// Native handle.
    private MemorySegment window = MemorySegment.NULL;

    /// Latest client width in physical pixels.
    private int clientWidth;

    /// Latest client height in physical pixels.
    private int clientHeight;

    /// Latest window DPI, or `96` when the host reports zero.
    private int dpi = USER_DEFAULT_SCREEN_DPI;

    /// Whether a move/resize modal loop is active.
    private boolean modalLoopActive;

    /// Number of `PBT_APMSUSPEND` deliveries observed through WndProc.
    private int sleepEvents;

    /// Number of resume power broadcasts observed through WndProc.
    private int wakeEvents;

    /// Number of modal-loop timer ticks delivered through WndProc.
    private int modalTimerTicks;

    /// Last presented unassociated 8-bit sRGB RGBA pixels, or `null` before the first present.
    private @Nullable MemorySegment lastRgba;

    /// Width of [#lastRgba].
    private int lastWidth;

    /// Height of [#lastRgba].
    private int lastHeight;

    /// Whether closed.
    private boolean closed;

    /// Receives host close and destroy notifications for one HWND.
    @NotNullByDefault
    interface Lifecycle {
        /// The host asked the application to close the window.
        void closeRequested();

        /// The HWND has been destroyed.
        void destroyed();

        /// A modal-loop timer fired and scheduled UI work may run.
        void modalTick();
    }

    /// Creates the native owner.
    ///
    /// @param libraries the shared libraries
    /// @param arena the window arena
    /// @param lifecycle the close and destroy listener
    private WindowsNativeWindow(WindowsLibraries libraries, Arena arena, Lifecycle lifecycle) {
        this.bindings = libraries.bindings();
        this.arena = arena;
        this.lifecycle = lifecycle;
        this.callbackFailures = new CallbackFailureQueue();
        this.instance = bindings.getModuleHandleW(MemorySegment.NULL).value();
        if (instance.address() == 0L) {
            throw new IllegalStateException("GetModuleHandleW returned NULL");
        }
        this.className = arena.allocateFrom(
                "HimariUIWindow" + Long.toUnsignedString(System.nanoTime()),
                StandardCharsets.UTF_16LE
        );
        this.rectRecord = arena.allocate(Win32Layouts.RECT);
        this.paintRecord = arena.allocate(Win32Layouts.PAINTSTRUCT);
    }

    /// Creates a hidden native window.
    ///
    /// @param libraries the shared libraries
    /// @param title the title
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the width
    /// @param height the height
    /// @param popup whether the window is a popup
    /// @param owner the owner HWND, or NULL
    /// @param lifecycle the close and destroy listener
    /// @return the window
    static WindowsNativeWindow create(
            WindowsLibraries libraries,
            String title,
            int x,
            int y,
            int width,
            int height,
            boolean popup,
            MemorySegment owner,
            Lifecycle lifecycle
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Window dimensions must be positive");
        }
        Arena arena = Arena.ofConfined();
        WindowsNativeWindow nativeWindow = new WindowsNativeWindow(
                libraries,
                arena,
                Objects.requireNonNull(lifecycle, "lifecycle")
        );
        try {
            nativeWindow.initialize(title, x, y, width, height, popup, owner);
            return nativeWindow;
        } catch (RuntimeException | Error failure) {
            nativeWindow.close();
            throw failure;
        }
    }

    /// Returns the HWND.
    ///
    /// @return the handle
    public MemorySegment handle() {
        requireOpen();
        return window;
    }

    /// Returns the module handle used to create the HWND.
    ///
    /// @return the `HINSTANCE`
    public MemorySegment instance() {
        requireOpen();
        return instance;
    }

    /// Returns the latest client width in physical pixels.
    ///
    /// @return the width
    public int clientWidth() {
        return clientWidth;
    }

    /// Returns the latest client height in physical pixels.
    ///
    /// @return the height
    public int clientHeight() {
        return clientHeight;
    }

    /// Returns the latest window DPI.
    ///
    /// @return the DPI, at least `96` when the host reports zero
    public int dpi() {
        return dpi;
    }

    /// Returns the physical-pixels-per-logical-pixel scale implied by [#dpi()].
    ///
    /// @return the positive scale
    public double scaleFactor() {
        return dpi / (double) USER_DEFAULT_SCREEN_DPI;
    }

    /// Shows the window without activation.
    public void show() {
        requireOpen();
        bindings.showWindow(window, SW_SHOWNOACTIVATE);
        bindings.updateWindow(window);
        refreshGeometry();
        throwContained();
    }

    /// Hides the window.
    public void hide() {
        requireOpen();
        bindings.showWindow(window, SW_HIDE);
    }

    /// Moves and resizes the window.
    ///
    /// @param x the origin x
    /// @param y the origin y
    /// @param width the width
    /// @param height the height
    public void setBounds(int x, int y, int width, int height) {
        requireOpen();
        bindings.setWindowPos(window, MemorySegment.NULL, x, y, width, height, SWP_NOZORDER | SWP_NOACTIVATE);
        refreshGeometry();
    }

    /// Returns whether a move/resize modal loop is active.
    ///
    /// @return whether the loop is active
    public boolean modalLoopActive() {
        return modalLoopActive;
    }

    /// Returns the number of modal-loop timer ticks observed.
    ///
    /// @return the tick count
    public int modalTimerTicks() {
        return modalTimerTicks;
    }

    /// Returns observed `PBT_APMSUSPEND` deliveries.
    ///
    /// @return the sleep count
    public int sleepEvents() {
        return sleepEvents;
    }

    /// Returns observed `PBT_APMRESUMESUSPEND` deliveries.
    ///
    /// @return the wake count
    public int wakeEvents() {
        return wakeEvents;
    }

    /// Delivers `WM_DPICHANGED` through the production WndProc with a suggested `RECT`.
    ///
    /// @param nextDpi the new DPI for both axes
    /// @param left the suggested left
    /// @param top the suggested top
    /// @param right the suggested right
    /// @param bottom the suggested bottom
    /// @return the `WndProc` result
    public long applyDpiChange(int nextDpi, int left, int top, int right, int bottom) {
        if (nextDpi <= 0) {
            throw new IllegalArgumentException("DPI must be positive");
        }
        MemorySegment rect = arena.allocate(Win32Layouts.RECT);
        rect.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET, left);
        rect.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET, top);
        rect.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET, right);
        rect.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET, bottom);
        long packed = (nextDpi & 0xFFFFL) | ((long) nextDpi << 16);
        return sendMessage(WM_DPICHANGED, packed, rect.address());
    }

    /// Sends one message synchronously through the production WndProc.
    ///
    /// @param message the Win32 message identifier
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the `WndProc` result
    public long sendMessage(int message, long wParam, long lParam) {
        requireOpen();
        return bindings.sendMessageW(window, message, wParam, lParam);
    }

    /// Blits unassociated 8-bit sRGB RGBA pixels into this HWND and retains them for `WM_PAINT`.
    ///
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the scanline count reported by `SetDIBitsToDevice`
    public int presentSdrRgba(MemorySegment rgba, int width, int height) {
        requireOpen();
        Objects.requireNonNull(rgba, "rgba");
        MemorySegment deviceContext = bindings.getDc(window);
        if (deviceContext.address() == 0L) {
            throw new IllegalStateException("GetDC returned NULL");
        }
        int scanlines;
        try {
            scanlines = WindowsSoftwarePresent.presentSdrRgba(bindings, deviceContext, rgba, width, height);
        } finally {
            if (bindings.releaseDc(window, deviceContext) == 0) {
                throw new IllegalStateException("ReleaseDC failed");
            }
        }
        lastRgba = MemorySegment.ofArray(rgba.toArray(ValueLayout.JAVA_BYTE));
        lastWidth = width;
        lastHeight = height;
        Win32FfmBindings.InvalidateRectResult invalidated = bindings.invalidateRect(window, MemorySegment.NULL, 0);
        if (invalidated.value() == 0) {
            throw new IllegalStateException("InvalidateRect failed: " + invalidated.errorCode());
        }
        return scanlines;
    }

    /// Maps a `POINTER_INPUT_TYPE` onto a device kind.
    ///
    /// @param pointerType the `GetPointerType` result
    /// @return the device kind
    public static PointerDeviceKind deviceKindFromPointerType(int pointerType) {
        return switch (pointerType) {
            case PT_PEN -> PointerDeviceKind.PEN;
            case PT_MOUSE -> PointerDeviceKind.MOUSE;
            default -> PointerDeviceKind.TOUCH;
        };
    }

    /// Loads a system cursor and installs it with generated `LoadCursorW` / `SetCursor`.
    ///
    /// @param cursorId a `MAKEINTRESOURCE` identifier such as [`#IDC_ARROW`]
    /// @return whether both calls returned a non-null handle
    public boolean setSystemCursor(int cursorId) {
        requireOpen();
        if (cursorId <= 0 || cursorId > 0xFFFF) {
            throw new IllegalArgumentException("cursorId must be a 16-bit resource identifier");
        }
        Win32FfmBindings.LoadCursorWResult loaded = bindings.loadCursorW(
                MemorySegment.NULL,
                MemorySegment.ofAddress(cursorId)
        );
        if (loaded.value().address() == 0L) {
            return false;
        }
        MemorySegment previous = bindings.setCursor(loaded.value());
        return previous.address() != 0L || loaded.value().address() != 0L;
    }

    /// Returns whether generated `GetCapture` reports this HWND.
    ///
    /// @return whether this window owns mouse capture
    public boolean captured() {
        requireOpen();
        return bindings.getCapture().address() == window.address();
    }

    /// `MK_LBUTTON`.
    private static final int MK_LBUTTON = 0x0001;

    /// `MK_RBUTTON`.
    private static final int MK_RBUTTON = 0x0002;

    /// `MK_MBUTTON`.
    private static final int MK_MBUTTON = 0x0010;

    /// `POINTER_MESSAGE_FLAG_FIRSTBUTTON`.
    private static final int POINTER_MESSAGE_FLAG_FIRSTBUTTON = 0x0010;

    /// `POINTER_MESSAGE_FLAG_SECONDBUTTON`.
    private static final int POINTER_MESSAGE_FLAG_SECONDBUTTON = 0x0020;

    /// `POINTER_MESSAGE_FLAG_THIRDBUTTON`.
    private static final int POINTER_MESSAGE_FLAG_THIRDBUTTON = 0x0040;

    /// Builds one wheel event from `wParam`/`lParam`.
    ///
    /// @param type `WHEEL` or `WHEEL_HORIZONTAL`
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @param device the physical pointer
    /// @return the event
    private PointerEvent wheelEvent(
            PointerEventType type,
            long wParam,
            long lParam,
            PointerDeviceKind device
    ) {
        short delta = (short) ((wParam >>> 16) & 0xFFFFL);
        return new PointerEvent(
                type,
                lowWord(lParam),
                highWord(lParam),
                device,
                delta / (float) WHEEL_DELTA,
                0,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                messageTime(),
                mouseButtons(PointerEventType.WHEEL, wParam),
                nextPointerSequence(),
                false
        );
    }

    /// Returns `GetMessageTime` for the message currently dispatched to WndProc.
    ///
    /// @return milliseconds since boot, or `0` when the host reports none
    private long messageTime() {
        int time = bindings.getMessageTime();
        if (time == 0) {
            time = bindings.getTickCount();
        }
        return Integer.toUnsignedLong(time);
    }

    /// Assigns the next per-window pointer sequence identifier.
    ///
    /// @return the positive sequence
    private int nextPointerSequence() {
        pointerSequence++;
        return pointerSequence;
    }

    /// Decodes `MK_*` mouse buttons plus the message kind.
    ///
    /// @param type the normalized type
    /// @param wParam the message `wParam`
    /// @return the button mask
    static int mouseButtons(PointerEventType type, long wParam) {
        int mk = (int) (wParam & 0xFFFFL);
        int buttons = 0;
        if ((mk & MK_LBUTTON) != 0 || type == PointerEventType.DOWN) {
            buttons |= PointerEvent.BUTTON_PRIMARY;
        }
        if ((mk & MK_RBUTTON) != 0 || type == PointerEventType.SECONDARY_DOWN) {
            buttons |= PointerEvent.BUTTON_SECONDARY;
        }
        if ((mk & MK_MBUTTON) != 0 || type == PointerEventType.MIDDLE_DOWN) {
            buttons |= PointerEvent.BUTTON_MIDDLE;
        }
        return buttons;
    }

    /// Decodes `POINTER_MESSAGE_FLAG_*` from the high word of a `WM_POINTER*` `wParam`.
    ///
    /// @param type the normalized type
    /// @param wParam the message `wParam`
    /// @return the button mask
    static int pointerButtons(PointerEventType type, long wParam) {
        int flags = highWord(wParam);
        int buttons = 0;
        if ((flags & POINTER_MESSAGE_FLAG_FIRSTBUTTON) != 0 || type == PointerEventType.DOWN) {
            buttons |= PointerEvent.BUTTON_PRIMARY;
        }
        if ((flags & POINTER_MESSAGE_FLAG_SECONDBUTTON) != 0 || type == PointerEventType.SECONDARY_DOWN) {
            buttons |= PointerEvent.BUTTON_SECONDARY;
        }
        if ((flags & POINTER_MESSAGE_FLAG_THIRDBUTTON) != 0 || type == PointerEventType.MIDDLE_DOWN) {
            buttons |= PointerEvent.BUTTON_MIDDLE;
        }
        return buttons;
    }

    /// Queries `GetPointerType` for `pointerId`.
    ///
    /// @param pointerId the pointer identity from `wParam`
    /// @return the type, or `0` when the query fails
    public int queryPointerType(int pointerId) {
        requireOpen();
        MemorySegment typeOut = arena.allocate(ValueLayout.JAVA_INT);
        Win32FfmBindings.GetPointerTypeResult result = bindings.getPointerType(pointerId, typeOut);
        if (result.value() == 0) {
            return 0;
        }
        return typeOut.get(ValueLayout.JAVA_INT, 0);
    }

    /// `PEN_MASK_PRESSURE`.
    public static final int PEN_MASK_PRESSURE = 0x00000001;

    /// `PEN_FLAG_INVERTED`.
    public static final int PEN_FLAG_INVERTED = 0x00000002;

    /// `PEN_FLAG_ERASER`.
    public static final int PEN_FLAG_ERASER = 0x00000004;

    /// `PEN_MASK_ROTATION`.
    public static final int PEN_MASK_ROTATION = 0x00000002;

    /// `PEN_MASK_TILT_X`.
    public static final int PEN_MASK_TILT_X = 0x00000004;

    /// `PEN_MASK_TILT_Y`.
    public static final int PEN_MASK_TILT_Y = 0x00000008;

    /// Maximum Win32 pen pressure.
    public static final int PEN_MAX_PRESSURE = 1024;

    /// Stores decoded pen axes from [`#queryPenInfo(int)`].
    ///
    /// @param pressure normalized pressure in `[0, 1]`
    /// @param tiltX tilt from the YZ plane in degrees
    /// @param tiltY tilt from the XZ plane in degrees
    /// @param rotation clockwise barrel rotation in degrees in `[0, 359]`
    /// @param inverted whether `PEN_FLAG_INVERTED` is set
    /// @param eraser whether `PEN_FLAG_ERASER` is set
    public record PenAxes(
            float pressure,
            float tiltX,
            float tiltY,
            float rotation,
            boolean inverted,
            boolean eraser
    ) {
        /// Creates axes with no invert or eraser bit.
        ///
        /// @param pressure normalized pressure
        /// @param tiltX tilt from the YZ plane
        /// @param tiltY tilt from the XZ plane
        /// @param rotation clockwise barrel rotation
        public PenAxes(float pressure, float tiltX, float tiltY, float rotation) {
            this(pressure, tiltX, tiltY, rotation, false, false);
        }

        /// Creates axes with invert and no eraser bit.
        ///
        /// @param pressure normalized pressure
        /// @param tiltX tilt from the YZ plane
        /// @param tiltY tilt from the XZ plane
        /// @param rotation clockwise barrel rotation
        /// @param inverted whether the stylus is inverted
        public PenAxes(float pressure, float tiltX, float tiltY, float rotation, boolean inverted) {
            this(pressure, tiltX, tiltY, rotation, inverted, false);
        }
    }

    /// `TOUCH_MASK_CONTACTAREA`.
    public static final int TOUCH_MASK_CONTACTAREA = 0x00000001;

    /// `TOUCH_MASK_ORIENTATION`.
    public static final int TOUCH_MASK_ORIENTATION = 0x00000002;

    /// Stores a decoded `POINTER_TOUCH_INFO` contact ellipse and orientation.
    ///
    /// @param width contact width in pixels
    /// @param height contact height in pixels
    /// @param orientation clockwise contact angle in degrees in `[0, 359]`; `0` when unreported
    public record ContactArea(float width, float height, float orientation) {
        /// Validates the ellipse.
        public ContactArea {
            if (!Float.isFinite(width) || !Float.isFinite(height) || !Float.isFinite(orientation)
                    || width < 0.0f || height < 0.0f) {
                throw new IllegalArgumentException("contact size must be finite and nonnegative");
            }
            if (orientation < 0.0f || orientation > 359.0f) {
                throw new IllegalArgumentException("orientation must be in [0, 359] degrees");
            }
        }

        /// Creates an ellipse with no reported orientation.
        ///
        /// @param width contact width
        /// @param height contact height
        public ContactArea(float width, float height) {
            this(width, height, 0.0f);
        }
    }

    /// `POINTER_FLAG_INRANGE`.
    public static final int POINTER_FLAG_INRANGE = 0x00000002;

    /// `POINTER_FLAG_INCONTACT`.
    public static final int POINTER_FLAG_INCONTACT = 0x00000004;

    /// `POINTER_FLAG_CANCELED`.
    public static final int POINTER_FLAG_CANCELED = 0x00008000;

    /// `POINTER_FLAG_PRIMARY`.
    public static final int POINTER_FLAG_PRIMARY = 0x00002000;

    /// `POINTER_FLAG_FIRSTBUTTON`.
    public static final int POINTER_FLAG_FIRSTBUTTON = 0x00000010;

    /// `POINTER_FLAG_SECONDBUTTON`.
    public static final int POINTER_FLAG_SECONDBUTTON = 0x00000020;

    /// `POINTER_FLAG_THIRDBUTTON`.
    public static final int POINTER_FLAG_THIRDBUTTON = 0x00000040;

    /// `POINTER_FLAG_FOURTHBUTTON`.
    public static final int POINTER_FLAG_FOURTHBUTTON = 0x00000080;

    /// `POINTER_FLAG_FIFTHBUTTON`.
    public static final int POINTER_FLAG_FIFTHBUTTON = 0x00000100;

    /// `POINTER_FLAG_NEW`.
    public static final int POINTER_FLAG_NEW = 0x00000001;

    /// `POINTER_FLAG_CONFIDENCE`.
    public static final int POINTER_FLAG_CONFIDENCE = 0x00004000;

    /// `POINTER_FLAG_DOWN`.
    public static final int POINTER_FLAG_DOWN = 0x00010000;

    /// `POINTER_FLAG_UPDATE`.
    public static final int POINTER_FLAG_UPDATE = 0x00020000;

    /// `POINTER_FLAG_WHEEL`.
    public static final int POINTER_FLAG_WHEEL = 0x00080000;

    /// `POINTER_FLAG_HWHEEL`.
    public static final int POINTER_FLAG_HWHEEL = 0x00100000;

    /// `POINTER_FLAG_CAPTURECHANGED`.
    public static final int POINTER_FLAG_CAPTURECHANGED = 0x00200000;

    /// `POINTER_FLAG_HASTRANSFORM`.
    public static final int POINTER_FLAG_HASTRANSFORM = 0x00400000;

    /// `POINTER_FLAG_UP`.
    public static final int POINTER_FLAG_UP = 0x00040000;

    /// `POINTER_MOD_SHIFT` in `POINTER_INFO.dwKeyStates`.
    public static final int POINTER_MOD_SHIFT = 0x00000004;

    /// `POINTER_MOD_CTRL` in `POINTER_INFO.dwKeyStates`.
    public static final int POINTER_MOD_CTRL = 0x00000008;

    /// `POINTER_CHANGE_FIRSTBUTTON_DOWN`.
    public static final int POINTER_CHANGE_FIRSTBUTTON_DOWN = 1;

    /// Stores decoded `POINTER_INFO` frame id plus hover, contact, canceled, primary, and button bits.
    ///
    /// @param frameId the host frame identity; `0` when unreported
    /// @param inRange whether `POINTER_FLAG_INRANGE` is set
    /// @param inContact whether `POINTER_FLAG_INCONTACT` is set
    /// @param canceled whether `POINTER_FLAG_CANCELED` is set
    /// @param primary whether `POINTER_FLAG_PRIMARY` is set
    /// @param firstButton whether `POINTER_FLAG_FIRSTBUTTON` is set
    /// @param secondButton whether `POINTER_FLAG_SECONDBUTTON` is set
    /// @param thirdButton whether `POINTER_FLAG_THIRDBUTTON` is set
    /// @param fourthButton whether `POINTER_FLAG_FOURTHBUTTON` is set
    /// @param fifthButton whether `POINTER_FLAG_FIFTHBUTTON` is set
    /// @param newPointer whether `POINTER_FLAG_NEW` is set
    /// @param confidence whether `POINTER_FLAG_CONFIDENCE` is set
    /// @param down whether `POINTER_FLAG_DOWN` is set
    /// @param update whether `POINTER_FLAG_UPDATE` is set
    /// @param wheel whether `POINTER_FLAG_WHEEL` is set
    /// @param horizontalWheel whether `POINTER_FLAG_HWHEEL` is set
    /// @param captureChanged whether `POINTER_FLAG_CAPTURECHANGED` is set
    /// @param hasTransform whether `POINTER_FLAG_HASTRANSFORM` is set
    /// @param up whether `POINTER_FLAG_UP` is set
    /// @param historyCount host `POINTER_INFO.historyCount`; `0` when unreported
    /// @param keyStates host `POINTER_INFO.dwKeyStates`; `0` when unreported
    /// @param buttonChangeType host `POINTER_INFO.ButtonChangeType`; `0` when unreported
    /// @param inputData host `POINTER_INFO.InputData`; `0` when unreported
    /// @param performanceCount host `POINTER_INFO.PerformanceCount`; `0` when unreported
    /// @param rawX host `POINTER_INFO.ptPixelLocationRaw.x`; `0` when unreported
    /// @param rawY host `POINTER_INFO.ptPixelLocationRaw.y`; `0` when unreported
    /// @param himetricX host `POINTER_INFO.ptHimetricLocation.x`; `0` when unreported
    /// @param himetricY host `POINTER_INFO.ptHimetricLocation.y`; `0` when unreported
    /// @param himetricRawX host `POINTER_INFO.ptHimetricLocationRaw.x`; `0` when unreported
    /// @param himetricRawY host `POINTER_INFO.ptHimetricLocationRaw.y`; `0` when unreported
    /// @param pointerTime host `POINTER_INFO.dwTime`; `0` when unreported
    public record PointerFlags(
            int frameId,
            boolean inRange,
            boolean inContact,
            boolean canceled,
            boolean primary,
            boolean firstButton,
            boolean secondButton,
            boolean thirdButton,
            boolean fourthButton,
            boolean fifthButton,
            boolean newPointer,
            boolean confidence,
            boolean down,
            boolean update,
            boolean wheel,
            boolean horizontalWheel,
            boolean captureChanged,
            boolean hasTransform,
            boolean up,
            int historyCount,
            int keyStates,
            int buttonChangeType,
            int inputData,
            long performanceCount,
            int rawX,
            int rawY,
            int himetricX,
            int himetricY,
            int himetricRawX,
            int himetricRawY,
            int pointerTime
    ) {
        /// Validates the frame identity, history count, and remaining `POINTER_INFO` integers.
        public PointerFlags {
            if (frameId < 0) {
                throw new IllegalArgumentException("frameId must be non-negative");
            }
            if (historyCount < 0) {
                throw new IllegalArgumentException("historyCount must be non-negative");
            }
            if (keyStates < 0) {
                throw new IllegalArgumentException("keyStates must be non-negative");
            }
            if (buttonChangeType < 0) {
                throw new IllegalArgumentException("buttonChangeType must be non-negative");
            }
            if (performanceCount < 0L) {
                throw new IllegalArgumentException("performanceCount must be non-negative");
            }
            if (pointerTime < 0) {
                throw new IllegalArgumentException("pointerTime must be non-negative");
            }
        }

        /// Creates flags with a first-button bit and no second-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, false, false);
        }

        /// Creates flags with a second-button bit and no third-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, false, false, false);
        }

        /// Creates flags with a third-button bit and no fourth-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, thirdButton, false, false, false, false);
        }

        /// Creates flags with a fourth-button bit and no fifth-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, thirdButton, fourthButton, false, false, false);
        }

        /// Creates flags with a fifth-button bit and no new-pointer bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, thirdButton, fourthButton, fifthButton, false, false);
        }

        /// Creates flags with a new-pointer bit and no confidence bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer
        ) {
            this(frameId, inRange, inContact, canceled, primary, firstButton, secondButton, thirdButton, fourthButton, fifthButton, newPointer, false);
        }

        /// Creates flags with a confidence bit and no remaining `POINTER_INFO` flags.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false
            );
        }

        /// Creates flags with remaining `POINTER_INFO` bits except `POINTER_FLAG_UP`.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    false
            );
        }

        /// Creates flags with `POINTER_FLAG_UP` and no reported history count.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    0
            );
        }

        /// Creates flags with a reported history count and no key-state or button-change fields.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    0,
                    0
            );
        }

        /// Creates flags with key-state and button-change fields, and no remaining `POINTER_INFO` integers.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    0,
                    0L
            );
        }

        /// Creates flags with input-data and performance-count, and no raw or himetric locations.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        /// @param inputData host extra input data
        /// @param performanceCount host performance counter
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType,
                int inputData,
                long performanceCount
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    inputData,
                    performanceCount,
                    0,
                    0,
                    0,
                    0
            );
        }

        /// Creates flags with raw and himetric locations, and no raw himetric locations.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        /// @param inputData host extra input data
        /// @param performanceCount host performance counter
        /// @param rawX raw pixel X
        /// @param rawY raw pixel Y
        /// @param himetricX himetric X
        /// @param himetricY himetric Y
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType,
                int inputData,
                long performanceCount,
                int rawX,
                int rawY,
                int himetricX,
                int himetricY
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    inputData,
                    performanceCount,
                    rawX,
                    rawY,
                    himetricX,
                    himetricY,
                    0,
                    0
            );
        }

        /// Creates flags with raw himetric locations and no `POINTER_INFO.dwTime`.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        /// @param firstButton whether the first button is down
        /// @param secondButton whether the second button is down
        /// @param thirdButton whether the third button is down
        /// @param fourthButton whether the fourth button is down
        /// @param fifthButton whether the fifth button is down
        /// @param newPointer whether this is a newly sighted pointer
        /// @param confidence whether the host reports a confident contact
        /// @param down whether the contact is beginning
        /// @param update whether this is an update
        /// @param wheel whether a vertical wheel tick is present
        /// @param horizontalWheel whether a horizontal wheel tick is present
        /// @param captureChanged whether capture changed
        /// @param hasTransform whether a pointer transform is present
        /// @param up whether the contact is ending
        /// @param historyCount host history count
        /// @param keyStates host modifier bits
        /// @param buttonChangeType host button-change kind
        /// @param inputData host extra input data
        /// @param performanceCount host performance counter
        /// @param rawX raw pixel X
        /// @param rawY raw pixel Y
        /// @param himetricX himetric X
        /// @param himetricY himetric Y
        /// @param himetricRawX raw himetric X
        /// @param himetricRawY raw himetric Y
        public PointerFlags(
                int frameId,
                boolean inRange,
                boolean inContact,
                boolean canceled,
                boolean primary,
                boolean firstButton,
                boolean secondButton,
                boolean thirdButton,
                boolean fourthButton,
                boolean fifthButton,
                boolean newPointer,
                boolean confidence,
                boolean down,
                boolean update,
                boolean wheel,
                boolean horizontalWheel,
                boolean captureChanged,
                boolean hasTransform,
                boolean up,
                int historyCount,
                int keyStates,
                int buttonChangeType,
                int inputData,
                long performanceCount,
                int rawX,
                int rawY,
                int himetricX,
                int himetricY,
                int himetricRawX,
                int himetricRawY
        ) {
            this(
                    frameId,
                    inRange,
                    inContact,
                    canceled,
                    primary,
                    firstButton,
                    secondButton,
                    thirdButton,
                    fourthButton,
                    fifthButton,
                    newPointer,
                    confidence,
                    down,
                    update,
                    wheel,
                    horizontalWheel,
                    captureChanged,
                    hasTransform,
                    up,
                    historyCount,
                    keyStates,
                    buttonChangeType,
                    inputData,
                    performanceCount,
                    rawX,
                    rawY,
                    himetricX,
                    himetricY,
                    himetricRawX,
                    himetricRawY,
                    0
            );
        }

        /// Creates flags with a primary-contact bit and no first-button bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        /// @param primary whether this is the primary contact
        public PointerFlags(int frameId, boolean inRange, boolean inContact, boolean canceled, boolean primary) {
            this(frameId, inRange, inContact, canceled, primary, false, false, false);
        }

        /// Creates flags with a canceled bit and no primary-contact bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        /// @param canceled whether the contact was canceled
        public PointerFlags(int frameId, boolean inRange, boolean inContact, boolean canceled) {
            this(frameId, inRange, inContact, canceled, false, false, false, false);
        }

        /// Creates flags with a frame identity and no canceled bit.
        ///
        /// @param frameId the host frame identity
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        public PointerFlags(int frameId, boolean inRange, boolean inContact) {
            this(frameId, inRange, inContact, false, false, false, false, false);
        }

        /// Creates flags with no reported frame identity.
        ///
        /// @param inRange whether the pointer is in range
        /// @param inContact whether the pointer is in contact
        public PointerFlags(boolean inRange, boolean inContact) {
            this(0, inRange, inContact, false, false, false, false, false);
        }
    }

    /// Installs pen axes used by the next [`#queryPenInfo(int)`] when the host has no contact.
    ///
    /// @param pointerId the pointer identity
    /// @param axes the axes
    public void installPenAxes(int pointerId, PenAxes axes) {
        Objects.requireNonNull(axes, "axes");
        this.syntheticPenPointerId = pointerId;
        this.syntheticPenAxes = axes;
    }

    /// Installs a contact ellipse used by the next [`#queryTouchInfo(int)`] when the host has no contact.
    ///
    /// @param pointerId the pointer identity
    /// @param contact the ellipse
    public void installTouchContact(int pointerId, ContactArea contact) {
        Objects.requireNonNull(contact, "contact");
        this.syntheticContactPointerId = pointerId;
        this.syntheticContact = contact;
    }

    /// Queries generated `GetPointerTouchInfo` for `pointerId`.
    ///
    /// A failed query returns [#installTouchContact] for `pointerId`, or zeros when none exist.
    ///
    /// @param pointerId the pointer identity
    /// @return the contact ellipse
    public ContactArea queryTouchInfo(int pointerId) {
        requireOpen();
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_TOUCH_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerTouchInfoResult result = bindings.getPointerTouchInfo(pointerId, info);
        if (result.value() == 0) {
            if (syntheticContact != null && pointerId == syntheticContactPointerId) {
                return syntheticContact;
            }
            return new ContactArea(0.0f, 0.0f);
        }
        return decodeTouchInfo(info);
    }

    /// Reads `rcContact` and `orientation` from a packed `POINTER_TOUCH_INFO`.
    ///
    /// @param info the packed structure
    /// @return the ellipse and orientation; unset mask bits report `0`
    public static ContactArea decodeTouchInfo(MemorySegment info) {
        Objects.requireNonNull(info, "info");
        int mask = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_TOUCHMASK_OFFSET);
        float width = 0.0f;
        float height = 0.0f;
        if ((mask & TOUCH_MASK_CONTACTAREA) != 0) {
            int left = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTLEFT_OFFSET);
            int top = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTTOP_OFFSET);
            int right = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTRIGHT_OFFSET);
            int bottom = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_RCCONTACTBOTTOM_OFFSET);
            width = Math.max(0, right - left);
            height = Math.max(0, bottom - top);
        }
        float orientation = 0.0f;
        if ((mask & TOUCH_MASK_ORIENTATION) != 0) {
            int raw = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_TOUCH_INFO_ORIENTATION_OFFSET);
            orientation = Math.clamp(raw, 0.0f, 359.0f);
        }
        return new ContactArea(width, height, orientation);
    }

    /// Installs pointer-info flags used by the next [`#queryPointerInfo(int)`] when the host has no contact.
    ///
    /// @param pointerId the pointer identity
    /// @param flags the hover and contact bits
    public void installPointerFlags(int pointerId, PointerFlags flags) {
        Objects.requireNonNull(flags, "flags");
        this.syntheticPointerFlagsId = pointerId;
        this.syntheticPointerFlags = flags;
    }

    /// Queries generated `GetPointerInfo` for `pointerId`.
    ///
    /// A failed query returns [#installPointerFlags] for `pointerId`, or both bits unset when none exist.
    ///
    /// @param pointerId the pointer identity
    /// @return the hover and contact bits
    public PointerFlags queryPointerInfo(int pointerId) {
        requireOpen();
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerInfoResult result = bindings.getPointerInfo(pointerId, info);
        if (result.value() == 0) {
            if (syntheticPointerFlags != null && pointerId == syntheticPointerFlagsId) {
                return syntheticPointerFlags;
            }
            return new PointerFlags(false, false);
        }
        return decodePointerInfo(info);
    }

    /// Reads `frameId`, `historyCount`, and `POINTER_FLAG_*` bits from a packed `POINTER_INFO`.
    ///
    /// @param info the packed structure
    /// @return the frame identity plus hover and contact bits
    public static PointerFlags decodePointerInfo(MemorySegment info) {
        Objects.requireNonNull(info, "info");
        int frameId = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_FRAMEID_OFFSET));
        int flags = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_POINTERFLAGS_OFFSET);
        int historyCount = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HISTORYCOUNT_OFFSET));
        int inputData = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_INPUTDATA_OFFSET);
        int keyStates = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_KEYSTATES_OFFSET));
        long performanceCount = Math.max(0L, info.get(ValueLayout.JAVA_LONG, Win32Layouts.POINTER_INFO_PERFORMANCECOUNT_OFFSET));
        int buttonChangeType = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_BUTTONCHANGETYPE_OFFSET));
        int rawX = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_PIXELLOCATIONRAWX_OFFSET);
        int rawY = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_PIXELLOCATIONRAWY_OFFSET);
        int himetricX = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HIMETRICLOCATIONX_OFFSET);
        int himetricY = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HIMETRICLOCATIONY_OFFSET);
        int himetricRawX = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HIMETRICLOCATIONRAWX_OFFSET);
        int himetricRawY = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_HIMETRICLOCATIONRAWY_OFFSET);
        int pointerTime = Math.max(0, info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_INFO_DWTIME_OFFSET));
        return new PointerFlags(
                frameId,
                (flags & POINTER_FLAG_INRANGE) != 0,
                (flags & POINTER_FLAG_INCONTACT) != 0,
                (flags & POINTER_FLAG_CANCELED) != 0,
                (flags & POINTER_FLAG_PRIMARY) != 0,
                (flags & POINTER_FLAG_FIRSTBUTTON) != 0,
                (flags & POINTER_FLAG_SECONDBUTTON) != 0,
                (flags & POINTER_FLAG_THIRDBUTTON) != 0,
                (flags & POINTER_FLAG_FOURTHBUTTON) != 0,
                (flags & POINTER_FLAG_FIFTHBUTTON) != 0,
                (flags & POINTER_FLAG_NEW) != 0,
                (flags & POINTER_FLAG_CONFIDENCE) != 0,
                (flags & POINTER_FLAG_DOWN) != 0,
                (flags & POINTER_FLAG_UPDATE) != 0,
                (flags & POINTER_FLAG_WHEEL) != 0,
                (flags & POINTER_FLAG_HWHEEL) != 0,
                (flags & POINTER_FLAG_CAPTURECHANGED) != 0,
                (flags & POINTER_FLAG_HASTRANSFORM) != 0,
                (flags & POINTER_FLAG_UP) != 0,
                historyCount,
                keyStates,
                buttonChangeType,
                inputData,
                performanceCount,
                rawX,
                rawY,
                himetricX,
                himetricY,
                himetricRawX,
                himetricRawY,
                pointerTime
        );
    }

    /// Queries generated `GetPointerPenInfo` for `pointerId`.
    ///
    /// A failed query returns [#installPenAxes] axes for `pointerId`, or zeros when none exist.
    ///
    /// @param pointerId the pointer identity
    /// @return the axes
    public PenAxes queryPenInfo(int pointerId) {
        requireOpen();
        MemorySegment info = arena.allocate(Win32Layouts.POINTER_PEN_INFO);
        info.fill((byte) 0);
        Win32FfmBindings.GetPointerPenInfoResult result = bindings.getPointerPenInfo(pointerId, info);
        if (result.value() == 0) {
            if (syntheticPenAxes != null && pointerId == syntheticPenPointerId) {
                return syntheticPenAxes;
            }
            return new PenAxes(0.0f, 0.0f, 0.0f, 0.0f);
        }
        return decodePenInfo(info);
    }

    /// Reads pressure and tilt from a `POINTER_PEN_INFO` record.
    ///
    /// @param info the packed structure
    /// @return the axes
    public static PenAxes decodePenInfo(MemorySegment info) {
        Objects.requireNonNull(info, "info");
        int mask = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_PENMASK_OFFSET);
        float pressure = 0.0f;
        if ((mask & PEN_MASK_PRESSURE) != 0) {
            int raw = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_PRESSURE_OFFSET);
            pressure = Math.clamp(raw / (float) PEN_MAX_PRESSURE, 0.0f, 1.0f);
        }
        float tiltX = 0.0f;
        if ((mask & PEN_MASK_TILT_X) != 0) {
            tiltX = Math.clamp(
                    info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_TILTX_OFFSET),
                    -90.0f,
                    90.0f
            );
        }
        float tiltY = 0.0f;
        if ((mask & PEN_MASK_TILT_Y) != 0) {
            tiltY = Math.clamp(
                    info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_TILTY_OFFSET),
                    -90.0f,
                    90.0f
            );
        }
        float rotation = 0.0f;
        if ((mask & PEN_MASK_ROTATION) != 0) {
            int raw = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_ROTATION_OFFSET);
            rotation = Math.clamp(raw, 0.0f, 359.0f);
        }
        int flags = info.get(ValueLayout.JAVA_INT, Win32Layouts.POINTER_PEN_INFO_PENFLAGS_OFFSET);
        boolean inverted = (flags & PEN_FLAG_INVERTED) != 0;
        boolean eraser = (flags & PEN_FLAG_ERASER) != 0;
        return new PenAxes(pressure, tiltX, tiltY, rotation, inverted, eraser);
    }

    /// Resolves the device for one `WM_POINTER*` `wParam`.
    private PointerDeviceKind pointerDevice(long wParam) {
        int pointerId = lowWord(wParam);
        if (syntheticPenAxes != null && pointerId == syntheticPenPointerId) {
            return PointerDeviceKind.PEN;
        }
        if (syntheticContact != null && pointerId == syntheticContactPointerId) {
            return PointerDeviceKind.TOUCH;
        }
        int type = queryPointerType(pointerId);
        if (type == 0) {
            return PointerDeviceKind.TOUCH;
        }
        return deviceKindFromPointerType(type);
    }

    /// Builds one `WM_POINTER*` event, including pen axes when the contact is a stylus.
    ///
    /// @param type the normalized type
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the event
    private PointerEvent pointerMessage(PointerEventType type, long wParam, long lParam) {
        PointerDeviceKind device = pointerDevice(wParam);
        int pointerId = lowWord(wParam);
        float pressure = 0.0f;
        float tiltX = 0.0f;
        float tiltY = 0.0f;
        float rotation = 0.0f;
        boolean inverted = false;
        boolean eraser = false;
        if (device == PointerDeviceKind.PEN) {
            PenAxes axes = queryPenInfo(pointerId);
            pressure = axes.pressure();
            tiltX = axes.tiltX();
            tiltY = axes.tiltY();
            rotation = axes.rotation();
            inverted = axes.inverted();
            eraser = axes.eraser();
        }
        ContactArea contact = queryTouchInfo(pointerId);
        PointerFlags flags = queryPointerInfo(pointerId);
        return new PointerEvent(
                type,
                lowWord(lParam),
                highWord(lParam),
                device,
                0.0f,
                pointerId,
                pressure,
                tiltX,
                tiltY,
                rotation,
                messageTime(),
                pointerButtons(type, wParam),
                nextPointerSequence(),
                false,
                inverted,
                eraser,
                contact.width(),
                contact.height(),
                contact.orientation(),
                flags.inRange(),
                flags.inContact(),
                flags.frameId(),
                flags.canceled(),
                flags.primary(),
                flags.firstButton(),
                flags.secondButton(),
                flags.thirdButton(),
                flags.fourthButton(),
                flags.fifthButton(),
                flags.newPointer(),
                flags.confidence(),
                flags.down(),
                flags.update(),
                flags.wheel(),
                flags.horizontalWheel(),
                flags.captureChanged(),
                flags.hasTransform(),
                flags.up(),
                flags.historyCount(),
                flags.keyStates(),
                flags.buttonChangeType(),
                flags.inputData(),
                flags.performanceCount(),
                flags.rawX(),
                flags.rawY(),
                flags.himetricX(),
                flags.himetricY(),
                flags.himetricRawX(),
                flags.himetricRawY(),
                flags.pointerTime()
        );
    }

    /// Builds one `WM_XBUTTON*` event with `BUTTON_X1` or `BUTTON_X2`.
    ///
    /// @param down whether the extra button pressed
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the event
    private PointerEvent xButtonEvent(boolean down, long wParam, long lParam) {
        int which = highWord(wParam);
        int buttons = 0;
        if (down && which == 1) {
            buttons = PointerEvent.BUTTON_X1;
        } else if (down && which == 2) {
            buttons = PointerEvent.BUTTON_X2;
        }
        return new PointerEvent(
                down ? PointerEventType.DOWN : PointerEventType.UP,
                lowWord(lParam),
                highWord(lParam),
                PointerDeviceKind.MOUSE,
                0.0f,
                0,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                messageTime(),
                buttons,
                nextPointerSequence(),
                false
        );
    }

    /// Builds one `WM_MOUSE*` event with host timestamp, buttons, and sequence.
    ///
    /// @param type the normalized type
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    /// @return the event
    private PointerEvent mouseEvent(PointerEventType type, long wParam, long lParam) {
        return new PointerEvent(
                type,
                lowWord(lParam),
                highWord(lParam),
                PointerDeviceKind.MOUSE,
                0.0f,
                0,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                messageTime(),
                mouseButtons(type, wParam),
                nextPointerSequence(),
                false
        );
    }

    /// Builds one `WM_KEY*` event from `lParam` scan-code and previous-state bits.
    ///
    /// @param type the normalized type
    /// @param key the logical key
    /// @param lParam the message `lParam`
    /// @return the event
    private KeyEvent keyEvent(KeyEventType type, LogicalKey key, int virtualKey, long lParam) {
        int scanCode = (int) ((lParam >>> 16) & 0xFFL);
        boolean repeat = type == KeyEventType.DOWN && (lParam & (1L << 30)) != 0L;
        boolean extended = (lParam & (1L << 24)) != 0L;
        return new KeyEvent(
                type,
                key,
                shiftDown,
                ctrlDown,
                altDown,
                scanCode,
                repeat,
                extended,
                metaDown,
                keyLocation(virtualKey, extended),
                messageTime()
        );
    }

    /// Maps a virtual-key and `KF_EXTENDED` bit onto a physical location.
    ///
    /// @param virtualKey the `wParam` virtual-key code
    /// @param extended whether `KF_EXTENDED` was set
    /// @return the location
    private static KeyLocation keyLocation(int virtualKey, boolean extended) {
        if (virtualKey == VK_LWIN) {
            return KeyLocation.LEFT;
        }
        if (virtualKey == VK_RWIN) {
            return KeyLocation.RIGHT;
        }
        if (!extended && ((virtualKey >= 0x21 && virtualKey <= 0x28) || virtualKey == 0x2E)) {
            return KeyLocation.NUMPAD;
        }
        return KeyLocation.STANDARD;
    }

    /// Posts one message to this HWND so the production WndProc delivers it.
    ///
    /// @param message the Win32 message identifier
    /// @param wParam the message `wParam`
    /// @param lParam the message `lParam`
    public void postMessage(int message, long wParam, long lParam) {
        requireOpen();
        Win32FfmBindings.PostMessageWResult result = bindings.postMessageW(window, message, wParam, lParam);
        if (result.value() == 0) {
            throw new IllegalStateException("PostMessageW failed: " + result.errorCode());
        }
    }

    /// Removes and returns pointer events delivered through WndProc since the last drain.
    ///
    /// @return the events in delivery order
    public @Unmodifiable List<PointerEvent> takePointerEvents() {
        List<PointerEvent> copy = List.copyOf(pointerEvents);
        pointerEvents.clear();
        return copy;
    }

    /// Removes and returns key events delivered through WndProc since the last drain.
    ///
    /// @return the events in delivery order
    public @Unmodifiable List<KeyEvent> takeKeyEvents() {
        List<KeyEvent> copy = List.copyOf(keyEvents);
        keyEvents.clear();
        return copy;
    }

    /// Removes and returns `WM_CHAR` text delivered through WndProc since the last drain.
    ///
    /// @return the committed characters, possibly empty
    public String takeCharacters() {
        String text = characters.toString();
        characters.setLength(0);
        return text;
    }

    /// Destroys the HWND.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        endModalLoop();
        if (window.address() != 0L && !destroyObserved) {
            bindings.destroyWindow(window);
        }
        window = MemorySegment.NULL;
        if (classRegistered) {
            bindings.unregisterClassW(className, instance);
            classRegistered = false;
        }
        arena.close();
    }

    /// Registers the class and creates the HWND.
    private void initialize(
            String title,
            int x,
            int y,
            int width,
            int height,
            boolean popup,
            MemorySegment owner
    ) {
        MemorySegment callback = bindings.createWndProcStub(this::windowProcedure, callbackFailures, arena);
        MemorySegment windowClass = arena.allocate(Win32Layouts.WNDCLASSEXW);
        windowClass.fill((byte) 0);
        windowClass.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.WNDCLASSEXW_CB_SIZE_OFFSET,
                Math.toIntExact(Win32Layouts.WNDCLASSEXW.byteSize())
        );
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_WND_PROC_OFFSET, callback);
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_INSTANCE_OFFSET, instance);
        windowClass.set(ValueLayout.ADDRESS, Win32Layouts.WNDCLASSEXW_CLASS_NAME_OFFSET, className);
        Win32FfmBindings.RegisterClassExWResult registration = bindings.registerClassExW(windowClass);
        if (registration.value() == 0) {
            throw new IllegalStateException("RegisterClassExW failed: " + registration.errorCode());
        }
        classRegistered = true;
        MemorySegment nativeTitle = arena.allocateFrom(title, StandardCharsets.UTF_16LE);
        int style = popup ? WS_POPUP : WS_OVERLAPPEDWINDOW;
        Win32FfmBindings.CreateWindowExWResult creation = bindings.createWindowExW(
                WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW,
                className,
                nativeTitle,
                style,
                x,
                y,
                width,
                height,
                owner,
                MemorySegment.NULL,
                instance,
                MemorySegment.NULL
        );
        if (creation.value().address() == 0L) {
            throw new IllegalStateException("CreateWindowExW failed: " + creation.errorCode());
        }
        window = creation.value();
        refreshGeometry();
        throwContained();
    }

    /// Dispatches lifecycle, DPI, and normalized input messages.
    ///
    /// Destroying one window does not post `WM_QUIT`; the session stays alive for remaining HWNDs.
    private long windowProcedure(MemorySegment callbackWindow, int message, long wParam, long lParam) {
        return switch (message) {
            case WM_CLOSE -> {
                lifecycle.closeRequested();
                yield 0L;
            }
            case WM_DESTROY -> {
                destroyObserved = true;
                lifecycle.destroyed();
                yield 0L;
            }
            case WM_PAINT -> paint(callbackWindow);
            case WM_ERASEBKGND -> lastRgba == null
                    ? bindings.defWindowProcW(callbackWindow, message, wParam, lParam)
                    : 1L;
            case WM_SIZE -> {
                clientWidth = lowWord(lParam);
                clientHeight = highWord(lParam);
                yield 0L;
            }
            case WM_DPICHANGED -> {
                int nextDpi = lowWord(wParam);
                if (nextDpi > 0) {
                    dpi = nextDpi;
                }
                applySuggestedDpiRect(lParam);
                refreshClientRect();
                yield 0L;
            }
            case WM_MOUSEMOVE -> {
                pointerEvents.add(mouseEvent(PointerEventType.MOVE, wParam, lParam));
                yield 0L;
            }
            case WM_LBUTTONDOWN -> {
                bindings.setCapture(window);
                pointerEvents.add(mouseEvent(PointerEventType.DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_LBUTTONUP -> {
                bindings.releaseCapture();
                pointerEvents.add(mouseEvent(PointerEventType.UP, wParam, lParam));
                yield 0L;
            }
            case WM_RBUTTONDOWN -> {
                pointerEvents.add(mouseEvent(PointerEventType.SECONDARY_DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_RBUTTONUP -> {
                pointerEvents.add(mouseEvent(PointerEventType.SECONDARY_UP, wParam, lParam));
                yield 0L;
            }
            case WM_MBUTTONDOWN -> {
                pointerEvents.add(mouseEvent(PointerEventType.MIDDLE_DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_MBUTTONUP -> {
                pointerEvents.add(mouseEvent(PointerEventType.MIDDLE_UP, wParam, lParam));
                yield 0L;
            }
            case WM_XBUTTONDOWN -> {
                pointerEvents.add(xButtonEvent(true, wParam, lParam));
                yield 0L;
            }
            case WM_XBUTTONUP -> {
                pointerEvents.add(xButtonEvent(false, wParam, lParam));
                yield 0L;
            }
            case WM_MOUSEWHEEL -> {
                pointerEvents.add(wheelEvent(PointerEventType.WHEEL, wParam, lParam, PointerDeviceKind.MOUSE));
                yield 0L;
            }
            case WM_MOUSEHWHEEL -> {
                pointerEvents.add(wheelEvent(PointerEventType.WHEEL_HORIZONTAL, wParam, lParam, PointerDeviceKind.MOUSE));
                yield 0L;
            }
            case WM_POINTERWHEEL -> {
                pointerEvents.add(wheelEvent(PointerEventType.WHEEL, wParam, lParam, pointerDevice(wParam)));
                yield 0L;
            }
            case WM_POINTERHWHEEL -> {
                pointerEvents.add(wheelEvent(PointerEventType.WHEEL_HORIZONTAL, wParam, lParam, pointerDevice(wParam)));
                yield 0L;
            }
            case WM_POINTERUPDATE -> {
                pointerEvents.add(pointerMessage(PointerEventType.MOVE, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERDOWN -> {
                pointerEvents.add(pointerMessage(PointerEventType.DOWN, wParam, lParam));
                yield 0L;
            }
            case WM_POINTERUP -> {
                pointerEvents.add(pointerMessage(PointerEventType.UP, wParam, lParam));
                yield 0L;
            }
            case WM_KEYDOWN -> {
                int virtualKey = (int) wParam;
                latchModifier(virtualKey, true);
                @Nullable LogicalKey key = logicalKey(virtualKey);
                if (key != null) {
                    keyEvents.add(keyEvent(KeyEventType.DOWN, key, virtualKey, lParam));
                }
                yield 0L;
            }
            case WM_KEYUP -> {
                int virtualKey = (int) wParam;
                latchModifier(virtualKey, false);
                @Nullable LogicalKey key = logicalKey(virtualKey);
                if (key != null) {
                    keyEvents.add(keyEvent(KeyEventType.UP, key, virtualKey, lParam));
                }
                yield 0L;
            }
            case WM_CHAR -> {
                int codeUnit = (int) wParam;
                if (codeUnit >= 0x20 && codeUnit != 0x7F && codeUnit <= 0xFFFF) {
                    characters.append((char) codeUnit);
                }
                yield 0L;
            }
            case WM_ENTERSIZEMOVE -> {
                beginModalLoop();
                yield 0L;
            }
            case WM_EXITSIZEMOVE -> {
                endModalLoop();
                yield 0L;
            }
            case WM_TIMER -> {
                if (wParam == MODAL_TIMER_ID && modalLoopActive) {
                    modalTimerTicks++;
                    lifecycle.modalTick();
                }
                yield 0L;
            }
            case WM_POWERBROADCAST -> {
                int event = (int) wParam;
                if (event == PBT_APMSUSPEND) {
                    sleepEvents++;
                } else if (event == PBT_APMRESUMESUSPEND) {
                    wakeEvents++;
                }
                yield 1L;
            }
            default -> bindings.defWindowProcW(callbackWindow, message, wParam, lParam);
        };
    }

    /// Replays the last presented RGBA frame through `BeginPaint` and `SetDIBitsToDevice`.
    ///
    /// @param callbackWindow the HWND receiving `WM_PAINT`
    /// @return zero after handling
    private long paint(MemorySegment callbackWindow) {
        if (lastRgba == null) {
            return bindings.defWindowProcW(callbackWindow, WM_PAINT, 0L, 0L);
        }
        paintRecord.fill((byte) 0);
        MemorySegment deviceContext = bindings.beginPaint(callbackWindow, paintRecord);
        if (deviceContext.address() == 0L) {
            throw new IllegalStateException("BeginPaint returned NULL");
        }
        try {
            WindowsSoftwarePresent.presentSdrRgba(bindings, deviceContext, lastRgba, lastWidth, lastHeight);
        } finally {
            if (bindings.endPaint(callbackWindow, paintRecord) == 0) {
                throw new IllegalStateException("EndPaint failed");
            }
        }
        return 0L;
    }

    /// Starts the move/resize modal timer.
    private void beginModalLoop() {
        modalLoopActive = true;
        Win32FfmBindings.SetTimerResult timer = bindings.setTimer(window, MODAL_TIMER_ID, 16, MemorySegment.NULL);
        if (timer.value() == 0L) {
            throw new IllegalStateException("SetTimer failed: " + timer.errorCode());
        }
    }

    /// Stops the move/resize modal timer.
    private void endModalLoop() {
        if (modalLoopActive) {
            bindings.killTimer(window, MODAL_TIMER_ID);
        }
        modalLoopActive = false;
    }

    /// Reads DPI and client size from the live HWND.
    private void refreshGeometry() {
        int queried = bindings.getDpiForWindow(window);
        if (queried > 0) {
            dpi = queried;
        }
        refreshClientRect();
    }

    /// Reads the client rectangle.
    private void refreshClientRect() {
        rectRecord.fill((byte) 0);
        Win32FfmBindings.GetClientRectResult result = bindings.getClientRect(window, rectRecord);
        if (result.value() == 0) {
            throw new IllegalStateException("GetClientRect failed: " + result.errorCode());
        }
        int right = rectRecord.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET);
        int bottom = rectRecord.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET);
        clientWidth = Math.max(0, right);
        clientHeight = Math.max(0, bottom);
    }

    /// Applies the suggested window rectangle supplied with `WM_DPICHANGED`.
    ///
    /// @param lParam the pointer to the suggested `RECT`
    private void applySuggestedDpiRect(long lParam) {
        if (lParam == 0L) {
            return;
        }
        MemorySegment suggested = MemorySegment.ofAddress(lParam).reinterpret(Win32Layouts.RECT.byteSize());
        int left = suggested.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET);
        int top = suggested.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET);
        int right = suggested.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET);
        int bottom = suggested.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET);
        bindings.setWindowPos(
                window,
                MemorySegment.NULL,
                left,
                top,
                Math.max(1, right - left),
                Math.max(1, bottom - top),
                SWP_NOZORDER | SWP_NOACTIVATE
        );
    }

    /// Latches Shift, Control, or Alt from a virtual-key code.
    ///
    /// @param virtualKey the `wParam` virtual-key code
    /// @param down whether the key is pressed
    private void latchModifier(int virtualKey, boolean down) {
        if (virtualKey == VK_SHIFT) {
            shiftDown = down;
        } else if (virtualKey == VK_CONTROL) {
            ctrlDown = down;
        } else if (virtualKey == VK_MENU) {
            altDown = down;
        } else if (virtualKey == VK_LWIN || virtualKey == VK_RWIN) {
            metaDown = down;
        }
    }

    /// Maps a virtual-key code onto the layout logical-key set.
    ///
    /// @param virtualKey the `wParam` virtual-key code
    /// @return the logical key, or `null` when the key is outside the first-stable set
    private static @Nullable LogicalKey logicalKey(int virtualKey) {
        return switch (virtualKey) {
            case 0x08 -> LogicalKey.BACKSPACE;
            case 0x09 -> LogicalKey.TAB;
            case 0x1B -> LogicalKey.ESCAPE;
            case 0x0D -> LogicalKey.ENTER;
            case 0x20 -> LogicalKey.SPACE;
            case 0x21 -> LogicalKey.PAGE_UP;
            case 0x22 -> LogicalKey.PAGE_DOWN;
            case 0x23 -> LogicalKey.END;
            case 0x24 -> LogicalKey.HOME;
            case 0x25 -> LogicalKey.ARROW_LEFT;
            case 0x26 -> LogicalKey.ARROW_UP;
            case 0x27 -> LogicalKey.ARROW_RIGHT;
            case 0x28 -> LogicalKey.ARROW_DOWN;
            case 0x2E -> LogicalKey.DELETE;
            case 0x5B, 0x5C -> LogicalKey.META;
            default -> null;
        };
    }

    /// Returns the signed low word of a packed `LPARAM`.
    ///
    /// @param value the packed value
    /// @return the low 16 bits as a signed coordinate
    static int lowWord(long value) {
        return (short) value;
    }

    /// Returns the signed high word of a packed `LPARAM`.
    ///
    /// @param value the packed value
    /// @return the high 16 bits as a signed coordinate
    static int highWord(long value) {
        return (short) (value >>> 16);
    }

    /// Packs client coordinates into a mouse `LPARAM`.
    ///
    /// @param x the client x
    /// @param y the client y
    /// @return the packed parameter
    static long packPointer(int x, int y) {
        return (Integer.toUnsignedLong(y & 0xFFFF) << 16) | Integer.toUnsignedLong(x & 0xFFFF);
    }

    /// Throws a contained callback failure.
    private void throwContained() {
        @Nullable Throwable failure = callbackFailures.poll();
        if (failure != null) {
            throw new IllegalStateException("Windows window callback failed", failure);
        }
    }

    /// Verifies the window is open.
    private void requireOpen() {
        if (closed || window.address() == 0L) {
            throw new IllegalStateException("Windows native window is closed");
        }
    }
}
