package org.glavo.himari.spikes.win32;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.spikes.win32.generated.Win32FfmBindings;
import org.glavo.himari.spikes.win32.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/// Owns a small Unicode top-level window for Windows graphics conformance profiles.
///
/// The borrowed [#handle()] remains valid until this object is closed. All methods must be invoked by the creating
/// thread because the window, callback arena, and message queue are thread-affine.
@NotNullByDefault
public final class Win32SurfaceWindow implements AutoCloseable {
    /// Standard resizable overlapped-window style.
    private static final int WS_OVERLAPPEDWINDOW = 0x00CF0000;

    /// Prevents the conformance window from taking activation.
    private static final int WS_EX_NOACTIVATE = 0x08000000;

    /// Keeps the conformance window out of the taskbar and Alt-Tab list.
    private static final int WS_EX_TOOLWINDOW = 0x00000080;

    /// Shows a window without activating it.
    private static final int SW_SHOWNOACTIVATE = 4;

    /// Hides a window.
    private static final int SW_HIDE = 0;

    /// Removes messages returned by `PeekMessageW`.
    private static final int PM_REMOVE = 0x0001;

    /// Window-close request.
    private static final int WM_CLOSE = 0x0010;

    /// Final window destruction notification.
    private static final int WM_DESTROY = 0x0002;

    /// Thread message-pump termination marker.
    private static final int WM_QUIT = 0x0012;

    /// System-library owner retained for the complete window lifetime.
    private final Win32Libraries libraries;

    /// Generated Win32 bindings retained for callback dispatch and cleanup.
    private final Win32FfmBindings bindings;

    /// Confined arena retaining native strings, records, and the `WndProc` stub.
    private final Arena arena;

    /// Failures contained by the generated callback adapter.
    private final CallbackFailureQueue callbackFailures;

    /// Reusable message-pump record.
    private final MemorySegment messageRecord;

    /// Borrowed current-module handle used to register and unregister the class.
    private final MemorySegment instance;

    /// Retained unique UTF-16 class name.
    private final MemorySegment className;

    /// Whether class registration succeeded and still requires unregistration.
    private boolean classRegistered;

    /// Whether `WM_DESTROY` has run.
    private boolean destroyObserved;

    /// Whether `WM_QUIT` has been removed from the current thread queue.
    private boolean quitObserved;

    /// Native window handle, or `NULL` after destruction.
    private MemorySegment window;

    /// Whether all owned resources have been closed.
    private boolean closed;

    /// Opens native libraries and allocates the retained window state.
    ///
    /// @param libraries the owned native-library lookup
    /// @param arena the owned confined arena
    private Win32SurfaceWindow(Win32Libraries libraries, Arena arena) {
        this.libraries = libraries;
        this.bindings = libraries.bindings();
        this.arena = arena;
        this.callbackFailures = new CallbackFailureQueue();
        this.messageRecord = arena.allocate(Win32Layouts.MSG);
        this.instance = requireModuleHandle(bindings.getModuleHandleW(MemorySegment.NULL));
        this.className = arena.allocateFrom(
                "HimariUID3D12Surface" + Long.toUnsignedString(System.nanoTime()),
                StandardCharsets.UTF_16LE
        );
        this.window = MemorySegment.NULL;
    }

    /// Creates an initially hidden graphics-surface window.
    ///
    /// @param title the non-null window title
    /// @param width the positive initial outer width in physical pixels
    /// @param height the positive initial outer height in physical pixels
    /// @return the initialized window owner
    /// @throws IllegalArgumentException if either dimension is not positive
    /// @throws IllegalStateException if class registration or window creation fails
    public static Win32SurfaceWindow open(String title, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Window dimensions must be positive");
        }
        Win32Libraries libraries = Win32Libraries.open();
        Arena arena = Arena.ofConfined();
        @Nullable Win32SurfaceWindow owner = null;
        try {
            owner = new Win32SurfaceWindow(libraries, arena);
            owner.initialize(title, width, height);
            return owner;
        } catch (RuntimeException | Error failure) {
            if (owner != null) {
                try {
                    owner.close();
                } catch (RuntimeException | Error cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            } else {
                try {
                    arena.close();
                } finally {
                    libraries.close();
                }
            }
            throw failure;
        }
    }

    /// Returns the borrowed native `HWND`.
    ///
    /// @return the non-null handle, valid until [#close()]
    /// @throws IllegalStateException if this window has been closed
    public MemorySegment handle() {
        requireOpen();
        return window;
    }

    /// Shows the window without activating it and synchronously processes initial painting.
    ///
    /// @throws IllegalStateException if this window has been closed
    public void show() {
        requireOpen();
        bindings.showWindow(window, SW_SHOWNOACTIVATE);
        bindings.updateWindow(window);
        throwContainedCallbackFailure();
    }

    /// Hides the window without destroying its graphics surface.
    ///
    /// @throws IllegalStateException if this window has been closed
    public void hide() {
        requireOpen();
        bindings.showWindow(window, SW_HIDE);
    }

    /// Dispatches every currently available message for the creating thread.
    ///
    /// @return whether no `WM_QUIT` message has been observed
    /// @throws IllegalStateException if this window has been closed or a callback failed
    public boolean pumpMessages() {
        requireOpen();
        while (bindings.peekMessageW(messageRecord, MemorySegment.NULL, 0, 0, PM_REMOVE) != 0) {
            int message = messageRecord.get(ValueLayout.JAVA_INT, Win32Layouts.MSG_MESSAGE_OFFSET);
            if (message == WM_QUIT) {
                quitObserved = true;
                continue;
            }
            bindings.translateMessage(messageRecord);
            bindings.dispatchMessageW(messageRecord);
        }
        throwContainedCallbackFailure();
        return !quitObserved;
    }

    /// Destroys the window, unregisters its class, and invalidates the borrowed handle.
    ///
    /// Repeated calls have no effect. Cleanup attempts every ownership boundary and suppresses later failures onto the
    /// first failure before rethrowing it.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable Throwable firstFailure = null;
        if (window.address() != 0L && !destroyObserved) {
            try {
                Win32FfmBindings.DestroyWindowResult result = bindings.destroyWindow(window);
                if (result.value() == 0) {
                    throw windowsFailure("DestroyWindow", result.errorCode());
                }
            } catch (RuntimeException | Error failure) {
                firstFailure = failure;
            }
        }
        if (destroyObserved) {
            try {
                drainOwnedQuitMessages();
            } catch (RuntimeException | Error failure) {
                firstFailure = mergeFailure(firstFailure, failure);
            }
        }
        window = MemorySegment.NULL;
        if (classRegistered) {
            try {
                Win32FfmBindings.UnregisterClassWResult result = bindings.unregisterClassW(className, instance);
                if (result.value() == 0) {
                    throw windowsFailure("UnregisterClassW", result.errorCode());
                }
                classRegistered = false;
            } catch (RuntimeException | Error failure) {
                firstFailure = mergeFailure(firstFailure, failure);
            }
        }
        try {
            arena.close();
        } catch (RuntimeException | Error failure) {
            firstFailure = mergeFailure(firstFailure, failure);
        }
        try {
            libraries.close();
        } catch (RuntimeException | Error failure) {
            firstFailure = mergeFailure(firstFailure, failure);
        }
        if (firstFailure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        if (firstFailure instanceof Error error) {
            throw error;
        }
    }

    /// Removes thread-level `WM_QUIT` markers posted by this support window's synchronous destruction.
    ///
    /// A surface owner that destroys its own window must not leave its termination marker for a later support window
    /// created on the same thread. Other message kinds remain queued.
    private void drainOwnedQuitMessages() {
        while (bindings.peekMessageW(messageRecord, MemorySegment.NULL, WM_QUIT, WM_QUIT, PM_REMOVE) != 0) {
            quitObserved = true;
        }
    }

    /// Registers the class and creates the native window.
    ///
    /// @param title the Java window title
    /// @param width the outer width
    /// @param height the outer height
    private void initialize(String title, int width, int height) {
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
            throw windowsFailure("RegisterClassExW", registration.errorCode());
        }
        classRegistered = true;

        MemorySegment nativeTitle = arena.allocateFrom(title, StandardCharsets.UTF_16LE);
        Win32FfmBindings.CreateWindowExWResult creation = bindings.createWindowExW(
                WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW,
                className,
                nativeTitle,
                WS_OVERLAPPEDWINDOW,
                64,
                64,
                width,
                height,
                MemorySegment.NULL,
                MemorySegment.NULL,
                instance,
                MemorySegment.NULL
        );
        if (creation.value().address() == 0L) {
            throw windowsFailure("CreateWindowExW", creation.errorCode());
        }
        window = creation.value();
        throwContainedCallbackFailure();
    }

    /// Dispatches the minimal lifecycle messages owned by this support window.
    ///
    /// @param callbackWindow the borrowed callback `HWND`
    /// @param message the native message identifier
    /// @param wParam the pointer-sized unsigned parameter
    /// @param lParam the pointer-sized signed parameter
    /// @return the native `LRESULT`
    private long windowProcedure(MemorySegment callbackWindow, int message, long wParam, long lParam) {
        return switch (message) {
            case WM_CLOSE -> {
                Win32FfmBindings.DestroyWindowResult result = bindings.destroyWindow(callbackWindow);
                if (result.value() == 0) {
                    throw windowsFailure("DestroyWindow from WM_CLOSE", result.errorCode());
                }
                yield 0L;
            }
            case WM_DESTROY -> {
                destroyObserved = true;
                bindings.postQuitMessage(0);
                yield 0L;
            }
            default -> bindings.defWindowProcW(callbackWindow, message, wParam, lParam);
        };
    }

    /// Throws the earliest failure caught at the `WndProc` boundary.
    private void throwContainedCallbackFailure() {
        @Nullable Throwable failure = callbackFailures.poll();
        if (failure != null) {
            throw new IllegalStateException("Win32 surface callback failed", failure);
        }
    }

    /// Requires this owner and its native window to remain open.
    private void requireOpen() {
        if (closed || window.address() == 0L) {
            throw new IllegalStateException("Win32 surface window is closed");
        }
    }

    /// Extracts a non-null current-module handle.
    ///
    /// @param result the generated Win32 call result
    /// @return the non-null borrowed module handle
    private static MemorySegment requireModuleHandle(Win32FfmBindings.GetModuleHandleWResult result) {
        if (result.value().address() == 0L) {
            throw windowsFailure("GetModuleHandleW", result.errorCode());
        }
        return result.value();
    }

    /// Adds a cleanup failure to an optional earlier failure.
    ///
    /// @param firstFailure the earlier failure, or `null`
    /// @param laterFailure the newly observed failure
    /// @return the first failure with the later failure suppressed, or the later failure when it is first
    private static Throwable mergeFailure(
            @Nullable Throwable firstFailure,
            Throwable laterFailure
    ) {
        if (firstFailure == null) {
            return laterFailure;
        }
        firstFailure.addSuppressed(laterFailure);
        return firstFailure;
    }

    /// Creates a Win32 failure with decimal and hexadecimal error spellings.
    ///
    /// @param operation the failed operation
    /// @param errorCode the captured `GetLastError` value
    /// @return the failure exception
    private static IllegalStateException windowsFailure(String operation, int errorCode) {
        return new IllegalStateException(operation + " failed with Win32 error "
                + Integer.toUnsignedString(errorCode) + " (0x" + Integer.toHexString(errorCode) + ')');
    }
}
