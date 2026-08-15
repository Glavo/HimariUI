package org.glavo.himari.platform.wayland;

import org.glavo.himari.platform.wayland.generated.WaylandClientFfmBindings;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.MemorySegment;

/// Owns one `wl_display` connection created through generated FFM bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WaylandDisplay implements AutoCloseable {
    /// Shared libraries.
    private final WaylandLibraries libraries;

    /// Native display pointer.
    private final MemorySegment display;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates one display owner.
    ///
    /// @param libraries the libraries
    /// @param display the native display
    private WaylandDisplay(WaylandLibraries libraries, MemorySegment display) {
        this.libraries = libraries;
        this.display = display;
    }

    /// Connects to the compositor named by `WAYLAND_DISPLAY`, or the default socket.
    ///
    /// @return the display
    public static WaylandDisplay connect() {
        WaylandLibraries libraries = WaylandLibraries.open();
        try {
            MemorySegment connected = libraries.bindings().wlDisplayConnect(MemorySegment.NULL);
            if (connected.address() == 0L) {
                throw new IllegalStateException("wl_display_connect returned NULL");
            }
            return new WaylandDisplay(libraries, connected);
        } catch (RuntimeException | Error failure) {
            libraries.close();
            throw failure;
        }
    }

    /// Returns the native display pointer.
    ///
    /// @return the pointer
    public MemorySegment nativeHandle() {
        requireOpen();
        return display;
    }

    /// Returns the generated client bindings.
    ///
    /// @return the bindings
    WaylandClientFfmBindings bindings() {
        requireOpen();
        return libraries.bindings();
    }

    /// Runs one `wl_display_roundtrip`.
    ///
    /// @return the compositor result
    public int roundtrip() {
        requireOpen();
        return libraries.bindings().wlDisplayRoundtrip(display);
    }

    /// Returns the display file descriptor.
    ///
    /// @return the fd
    public int fileDescriptor() {
        requireOpen();
        return libraries.bindings().wlDisplayGetFd(display);
    }

    /// Disconnects the display and closes the library lookups.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException firstFailure = null;
        try {
            libraries.bindings().wlDisplayDisconnect(display);
        } catch (RuntimeException failure) {
            firstFailure = failure;
        }
        try {
            libraries.close();
        } catch (RuntimeException failure) {
            if (firstFailure == null) {
                firstFailure = failure;
            } else {
                firstFailure.addSuppressed(failure);
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    /// Verifies the display is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Wayland display is closed");
        }
    }
}
