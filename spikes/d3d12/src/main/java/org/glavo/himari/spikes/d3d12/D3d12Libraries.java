package org.glavo.himari.spikes.d3d12;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.spikes.d3d12.generated.D3d12FfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.ByteOrder;
import java.util.Locale;

/// Owns the system-library lookups used by one D3D12 conformance run.
///
/// Closing this object invalidates the generated bindings' symbol addresses. Callers must release every COM
/// interface created through the bindings before closing this owner.
@SuppressWarnings("restricted")
@NotNullByDefault
final class D3d12Libraries implements AutoCloseable {
    /// Arena controlling the three system-library lookups.
    private final Arena arena;

    /// Generated bindings linked against Kernel32, D3D12, and DXGI.
    private final D3d12FfmBindings bindings;

    /// Creates one linked library owner.
    ///
    /// @param arena the arena controlling the lookup lifetimes
    /// @param bindings the generated bindings
    private D3d12Libraries(Arena arena, D3d12FfmBindings bindings) {
        this.arena = arena;
        this.bindings = bindings;
    }

    /// Opens the Windows system libraries required by the profile.
    ///
    /// @return a closeable library owner
    /// @throws IllegalStateException if the process does not match the Windows x64 ABI fixture
    /// @throws java.util.NoSuchElementException if a required symbol is absent
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

    /// Opens and records one successful direct system-library lookup.
    ///
    /// @param libraryName the fixed Windows system-library name
    /// @param arena the arena controlling lookup lifetime
    /// @return the successful symbol lookup
    private static SymbolLookup open(String libraryName, Arena arena) {
        SymbolLookup lookup = SymbolLookup.libraryLookup(libraryName, arena);
        NativeLibraryLoadAudit.recordSuccessfulLoad(libraryName);
        return lookup;
    }

    /// Returns the linked generated bindings.
    ///
    /// @return the bindings, valid until this owner is closed
    D3d12FfmBindings bindings() {
        return bindings;
    }

    /// Closes every native-library lookup.
    @Override
    public void close() {
        arena.close();
    }

    /// Verifies that the target-resolved schema matches the current process.
    private static void requireSupportedHost() {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!operatingSystem.contains("windows")) {
            throw new IllegalStateException("SPIKE-D3D12-001 requires Windows, got " + operatingSystem);
        }
        if (!(architecture.equals("amd64") || architecture.equals("x86_64"))) {
            throw new IllegalStateException("SPIKE-D3D12-001 schema requires x86_64, got " + architecture);
        }
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalStateException("SPIKE-D3D12-001 schema requires little-endian byte order");
        }
    }
}
