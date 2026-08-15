package org.glavo.himari.objc;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.objc.generated.ObjcBlockFfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.util.Locale;

/// Owns the `libSystem` lookup used by `_Block_copy` and `_Block_release`.
@SuppressWarnings("restricted")
@NotNullByDefault
final class ObjcBlockLibraries implements AutoCloseable {
    /// The arena controlling library lookups.
    private final Arena arena;

    /// The generated bindings.
    private final ObjcBlockFfmBindings bindings;

    /// Creates one owner.
    ///
    /// @param arena the lookup arena
    /// @param bindings the bindings
    private ObjcBlockLibraries(Arena arena, ObjcBlockFfmBindings bindings) {
        this.arena = arena;
        this.bindings = bindings;
    }

    /// Returns whether this process is a macOS host.
    ///
    /// @return whether the host is macOS
    static boolean supportedHost() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    /// Opens `libSystem`.
    ///
    /// @return the library owner
    static ObjcBlockLibraries open() {
        if (!supportedHost()) {
            throw new IllegalStateException("Objective-C blocks require macOS, got "
                    + System.getProperty("os.name", ""));
        }
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = SymbolLookup.libraryLookup("libSystem.B.dylib", arena);
            NativeLibraryLoadAudit.recordSuccessfulLoad("libSystem.B.dylib");
            return new ObjcBlockLibraries(arena, new ObjcBlockFfmBindings(symbols));
        } catch (RuntimeException | Error failure) {
            arena.close();
            throw failure;
        }
    }

    /// Returns the generated bindings.
    ///
    /// @return the bindings
    ObjcBlockFfmBindings bindings() {
        return bindings;
    }

    /// Closes the lookups.
    @Override
    public void close() {
        arena.close();
    }
}
