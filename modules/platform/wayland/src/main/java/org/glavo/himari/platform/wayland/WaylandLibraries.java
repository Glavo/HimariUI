package org.glavo.himari.platform.wayland;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.platform.wayland.generated.WaylandClientFfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.util.Locale;

/// Owns the `libwayland-client` lookup for one Wayland session.
@SuppressWarnings("restricted")
@NotNullByDefault
final class WaylandLibraries implements AutoCloseable {
    /// The arena controlling library lookups.
    private final Arena arena;

    /// The generated bindings.
    private final WaylandClientFfmBindings bindings;

    /// Creates one owner.
    ///
    /// @param arena the lookup arena
    /// @param bindings the bindings
    private WaylandLibraries(Arena arena, WaylandClientFfmBindings bindings) {
        this.arena = arena;
        this.bindings = bindings;
    }

    /// Returns whether this process is a Linux host that may load Wayland.
    ///
    /// @return whether the host is Linux
    static boolean supportedHost() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    /// Opens `libwayland-client`.
    ///
    /// @return the library owner
    static WaylandLibraries open() {
        if (!supportedHost()) {
            throw new IllegalStateException("Wayland requires Linux, got "
                    + System.getProperty("os.name", ""));
        }
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = openLibrary("libwayland-client.so.0", arena);
            return new WaylandLibraries(arena, new WaylandClientFfmBindings(symbols));
        } catch (RuntimeException | Error failure) {
            try {
                SymbolLookup symbols = openLibrary("libwayland-client.so", arena);
                return new WaylandLibraries(arena, new WaylandClientFfmBindings(symbols));
            } catch (RuntimeException | Error retry) {
                arena.close();
                failure.addSuppressed(retry);
                throw failure;
            }
        }
    }

    /// Returns the generated bindings.
    ///
    /// @return the bindings
    WaylandClientFfmBindings bindings() {
        return bindings;
    }

    /// Closes the lookups.
    @Override
    public void close() {
        arena.close();
    }

    /// Opens one system library.
    ///
    /// @param libraryName the soname
    /// @param arena the arena
    /// @return the lookup
    private static SymbolLookup openLibrary(String libraryName, Arena arena) {
        SymbolLookup lookup = SymbolLookup.libraryLookup(libraryName, arena);
        NativeLibraryLoadAudit.recordSuccessfulLoad(libraryName);
        return lookup;
    }
}
