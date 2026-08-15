package org.glavo.himari.rhi.metal;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.rhi.metal.generated.MetalFfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.util.Locale;

/// Owns the Metal and CoreFoundation lookups for one device session.
@SuppressWarnings("restricted")
@NotNullByDefault
final class MetalLibraries implements AutoCloseable {
    /// The arena controlling library lookups.
    private final Arena arena;

    /// The generated bindings.
    private final MetalFfmBindings bindings;

    /// Creates one owner.
    ///
    /// @param arena the lookup arena
    /// @param bindings the bindings
    private MetalLibraries(Arena arena, MetalFfmBindings bindings) {
        this.arena = arena;
        this.bindings = bindings;
    }

    /// Returns whether this process is a macOS host.
    ///
    /// @return whether the host is macOS
    static boolean supportedHost() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    /// Opens Metal and CoreFoundation.
    ///
    /// @return the library owner
    static MetalLibraries open() {
        if (!supportedHost()) {
            throw new IllegalStateException("Metal requires macOS, got " + System.getProperty("os.name", ""));
        }
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = openLibrary("/System/Library/Frameworks/Metal.framework/Metal", arena)
                    .or(openLibrary("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation", arena));
            return new MetalLibraries(arena, new MetalFfmBindings(symbols));
        } catch (RuntimeException | Error failure) {
            arena.close();
            throw failure;
        }
    }

    /// Returns the generated bindings.
    ///
    /// @return the bindings
    MetalFfmBindings bindings() {
        return bindings;
    }

    /// Closes the lookups.
    @Override
    public void close() {
        arena.close();
    }

    /// Opens one system library.
    ///
    /// @param libraryName the framework path
    /// @param arena the arena
    /// @return the lookup
    private static SymbolLookup openLibrary(String libraryName, Arena arena) {
        SymbolLookup lookup = SymbolLookup.libraryLookup(libraryName, arena);
        NativeLibraryLoadAudit.recordSuccessfulLoad(libraryName);
        return lookup;
    }
}
