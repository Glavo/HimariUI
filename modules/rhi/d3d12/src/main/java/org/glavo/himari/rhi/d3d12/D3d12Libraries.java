package org.glavo.himari.rhi.d3d12;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.rhi.d3d12.generated.D3d12FfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.ByteOrder;
import java.util.Locale;

/// Owns the Kernel32, D3D12, and DXGI lookups for one production device session.
@SuppressWarnings("restricted")
@NotNullByDefault
final class D3d12Libraries implements AutoCloseable {
    /// The arena controlling library lookups.
    private final Arena arena;

    /// The generated bindings.
    private final D3d12FfmBindings bindings;

    /// Creates one owner.
    ///
    /// @param arena the lookup arena
    /// @param bindings the bindings
    private D3d12Libraries(Arena arena, D3d12FfmBindings bindings) {
        this.arena = arena;
        this.bindings = bindings;
    }

    /// Opens the required system libraries.
    ///
    /// @return the library owner
    static D3d12Libraries open() {
        requireSupportedHost();
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = open("kernel32.dll", arena)
                    .or(open("d3d12.dll", arena))
                    .or(open("dxgi.dll", arena));
            return new D3d12Libraries(arena, new D3d12FfmBindings(symbols));
        } catch (RuntimeException | Error failure) {
            arena.close();
            throw failure;
        }
    }

    /// Returns the generated bindings.
    ///
    /// @return the bindings
    D3d12FfmBindings bindings() {
        return bindings;
    }

    /// Closes the lookups.
    @Override
    public void close() {
        arena.close();
    }

    /// Opens one system library.
    ///
    /// @param libraryName the DLL name
    /// @param arena the arena
    /// @return the lookup
    private static SymbolLookup open(String libraryName, Arena arena) {
        SymbolLookup lookup = SymbolLookup.libraryLookup(libraryName, arena);
        NativeLibraryLoadAudit.recordSuccessfulLoad(libraryName);
        return lookup;
    }

    /// Verifies the Windows x64 little-endian host.
    private static void requireSupportedHost() {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!operatingSystem.contains("windows")) {
            throw new IllegalStateException("D3D12 backend requires Windows, got " + operatingSystem);
        }
        if (!(architecture.equals("amd64") || architecture.equals("x86_64"))) {
            throw new IllegalStateException("D3D12 backend schema requires x86_64, got " + architecture);
        }
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalStateException("D3D12 backend schema requires little-endian byte order");
        }
    }
}
