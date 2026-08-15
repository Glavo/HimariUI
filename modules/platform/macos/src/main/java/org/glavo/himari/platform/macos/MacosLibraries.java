package org.glavo.himari.platform.macos;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.platform.macos.generated.ObjcFfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.util.Locale;

/// Owns the `libobjc` lookup for one macOS session.
@SuppressWarnings("restricted")
@NotNullByDefault
final class MacosLibraries implements AutoCloseable {
    /// The arena controlling library lookups.
    private final Arena arena;

    /// The generated bindings.
    private final ObjcFfmBindings bindings;

    /// Creates one owner.
    ///
    /// @param arena the lookup arena
    /// @param bindings the bindings
    private MacosLibraries(Arena arena, ObjcFfmBindings bindings) {
        this.arena = arena;
        this.bindings = bindings;
    }

    /// Returns whether this process is a macOS host.
    ///
    /// @return whether the host is macOS
    static boolean supportedHost() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    /// Opens `libobjc`.
    ///
    /// @return the library owner
    static MacosLibraries open() {
        if (!supportedHost()) {
            throw new IllegalStateException("macOS platform requires macOS, got "
                    + System.getProperty("os.name", ""));
        }
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = openLibrary("libobjc.A.dylib", arena);
            return new MacosLibraries(arena, new ObjcFfmBindings(symbols));
        } catch (RuntimeException | Error failure) {
            try {
                SymbolLookup symbols = openLibrary("/usr/lib/libobjc.A.dylib", arena);
                return new MacosLibraries(arena, new ObjcFfmBindings(symbols));
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
    ObjcFfmBindings bindings() {
        return bindings;
    }

    /// Closes the lookups.
    @Override
    public void close() {
        arena.close();
    }

    /// Opens one system library.
    ///
    /// @param libraryName the dylib name or path
    /// @param arena the arena
    /// @return the lookup
    private static SymbolLookup openLibrary(String libraryName, Arena arena) {
        SymbolLookup lookup = SymbolLookup.libraryLookup(libraryName, arena);
        NativeLibraryLoadAudit.recordSuccessfulLoad(libraryName);
        return lookup;
    }
}
