package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
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

    /// Key events received through WndProc since the last drain.
    private final ArrayList<KeyEvent> keyEvents = new ArrayList<>();

    /// UTF-16 code units received as `WM_CHAR` since the last drain.
    private final StringBuilder characters = new StringBuilder();

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

    /// Number of modal-loop timer ticks delivered through WndProc.
    private int modalTimerTicks;

    /// Last presented unassociated 8-bit sRGB RGBA pixels, or `null` before the first present.
    private byte @Nullable [] lastRgba;

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
    public int presentSdrRgba(byte[] rgba, int width, int height) {
        requireOpen();
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
        lastRgba = rgba.clone();
        lastWidth = width;
        lastHeight = height;
        Win32FfmBindings.InvalidateRectResult invalidated = bindings.invalidateRect(window, MemorySegment.NULL, 0);
        if (invalidated.value() == 0) {
            throw new IllegalStateException("InvalidateRect failed: " + invalidated.errorCode());
        }
        return scanlines;
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
                pointerEvents.add(new PointerEvent(PointerEventType.MOVE, lowWord(lParam), highWord(lParam)));
                yield 0L;
            }
            case WM_LBUTTONDOWN -> {
                pointerEvents.add(new PointerEvent(PointerEventType.DOWN, lowWord(lParam), highWord(lParam)));
                yield 0L;
            }
            case WM_LBUTTONUP -> {
                pointerEvents.add(new PointerEvent(PointerEventType.UP, lowWord(lParam), highWord(lParam)));
                yield 0L;
            }
            case WM_KEYDOWN -> {
                @Nullable LogicalKey key = logicalKey((int) wParam);
                if (key != null) {
                    keyEvents.add(new KeyEvent(KeyEventType.DOWN, key));
                }
                yield 0L;
            }
            case WM_KEYUP -> {
                @Nullable LogicalKey key = logicalKey((int) wParam);
                if (key != null) {
                    keyEvents.add(new KeyEvent(KeyEventType.UP, key));
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

    /// Maps a virtual-key code onto the layout logical-key set.
    ///
    /// @param virtualKey the `wParam` virtual-key code
    /// @return the logical key, or `null` when the key is outside the first-stable set
    private static @Nullable LogicalKey logicalKey(int virtualKey) {
        return switch (virtualKey) {
            case 0x09 -> LogicalKey.TAB;
            case 0x1B -> LogicalKey.ESCAPE;
            case 0x0D -> LogicalKey.ENTER;
            case 0x20 -> LogicalKey.SPACE;
            case 0x25 -> LogicalKey.ARROW_LEFT;
            case 0x26 -> LogicalKey.ARROW_UP;
            case 0x27 -> LogicalKey.ARROW_RIGHT;
            case 0x28 -> LogicalKey.ARROW_DOWN;
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
