package org.glavo.himari.spikes.win32;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.spikes.win32.generated.Win32FfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.nio.ByteOrder;
import java.util.Locale;

/// Owns the system-library lookups used by one Win32 conformance run.
///
/// Closing this object invalidates the generated bindings' symbol addresses. Callers must close every window,
/// callback arena, and COM interface before closing this object.
@SuppressWarnings("restricted")
@NotNullByDefault
final class Win32Libraries implements AutoCloseable {
    /// The arena controlling the three system-library lookups.
    private final Arena arena;

    /// The generated bindings linked against Kernel32, User32, and DXGI.
    private final Win32FfmBindings bindings;

    /// Creates and links the required system-library lookups.
    ///
    /// @param arena the arena controlling the lookup lifetimes
    /// @param bindings the linked generated bindings
    private Win32Libraries(Arena arena, Win32FfmBindings bindings) {
        this.arena = arena;
        this.bindings = bindings;
    }

    /// Opens the Windows system libraries required by the profile.
    ///
    /// @return a closeable library owner
    /// @throws IllegalStateException if the process is not a little-endian Windows x64 process
    /// @throws java.util.NoSuchElementException if a required symbol is absent
    static Win32Libraries open() {
        requireSupportedHost();
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = open("kernel32.dll", arena)
                    .or(open("user32.dll", arena))
                    .or(open("dxgi.dll", arena));
            return new Win32Libraries(arena, new Win32FfmBindings(symbols));
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
    /// @return the generated bindings, valid until this object is closed
    Win32FfmBindings bindings() {
        return bindings;
    }

    /// Closes all system-library lookups.
    @Override
    public void close() {
        arena.close();
    }

    /// Verifies the target-resolved schema matches the current Java process.
    private static void requireSupportedHost() {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!operatingSystem.contains("windows")) {
            throw new IllegalStateException("SPIKE-WIN-001 requires Windows, got " + operatingSystem);
        }
        if (!(architecture.equals("amd64") || architecture.equals("x86_64"))) {
            throw new IllegalStateException("SPIKE-WIN-001 schema requires x86_64, got " + architecture);
        }
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalStateException("SPIKE-WIN-001 schema requires little-endian byte order");
        }
    }
}
