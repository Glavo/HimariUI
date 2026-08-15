package org.glavo.himari.platform.wayland;

import org.glavo.himari.platform.wayland.generated.WaylandClientFfmBindings;
import org.glavo.himari.platform.wayland.linux.WaylandLinuxHost;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;

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

    /// Returns whether the Linux host package may load Wayland.
    ///
    /// @return whether the host is Linux
    static boolean supportedHost() {
        return WaylandLinuxHost.supported();
    }

    /// Opens `libwayland-client` through the Linux host package.
    ///
    /// @return the library owner
    static WaylandLibraries open() {
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = WaylandLinuxHost.openClient(arena);
            return new WaylandLibraries(arena, new WaylandClientFfmBindings(symbols));
        } catch (RuntimeException | Error failure) {
            arena.close();
            throw failure;
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
}
