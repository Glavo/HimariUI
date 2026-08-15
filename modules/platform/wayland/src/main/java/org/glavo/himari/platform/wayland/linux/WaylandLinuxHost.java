package org.glavo.himari.platform.wayland.linux;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.util.Locale;

/// Linux host connect and load path for the Wayland protocol module.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WaylandLinuxHost {
    /// Prevents instantiation.
    private WaylandLinuxHost() {
    }

    /// Returns whether this process is a Linux client that may load `libwayland-client`.
    ///
    /// @return whether the host is Linux
    public static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }

    /// Opens `libwayland-client` on a Linux host.
    ///
    /// @param arena the lookup arena
    /// @return the symbol lookup
    public static SymbolLookup openClient(Arena arena) {
        if (!supported()) {
            throw new IllegalStateException("Wayland Linux host requires Linux, got "
                    + System.getProperty("os.name", ""));
        }
        try {
            return openLibrary("libwayland-client.so.0", arena);
        } catch (RuntimeException | Error failure) {
            try {
                return openLibrary("libwayland-client.so", arena);
            } catch (RuntimeException | Error retry) {
                failure.addSuppressed(retry);
                throw failure;
            }
        }
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
