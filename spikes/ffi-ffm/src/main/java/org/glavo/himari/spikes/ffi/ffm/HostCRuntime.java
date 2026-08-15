package org.glavo.himari.spikes.ffi.ffm;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Locale;

/// Resolves the fixed C runtime library used by the cross-platform FFM spike.
@NotNullByDefault
final class HostCRuntime {
    /// Prevents instantiation of this utility class.
    private HostCRuntime() {
    }

    /// Opens the host C runtime with a lifetime controlled by `arena`.
    ///
    /// @param arena the arena controlling the library lookup lifetime
    /// @return the fixed system-library symbol lookup
    /// @throws UnsupportedOperationException if the host is outside the required 64-bit little-endian desktop set
    @SuppressWarnings("restricted")
    static SymbolLookup open(Arena arena) {
        requireSupportedTarget();
        String libraryName = libraryName();
        SymbolLookup lookup = SymbolLookup.libraryLookup(libraryName, arena);
        NativeLibraryLoadAudit.recordSuccessfulLoad(libraryName);
        return lookup;
    }

    /// Returns the fixed host C runtime library name.
    ///
    /// @return `ucrtbase.dll`, `libc.so.6`, or `/usr/lib/libSystem.B.dylib`
    /// @throws UnsupportedOperationException if the operating system is unsupported
    static String libraryName() {
        String operatingSystem = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (operatingSystem.contains("windows")) {
            return "ucrtbase.dll";
        }
        if (operatingSystem.contains("linux")) {
            return "libc.so.6";
        }
        if (operatingSystem.contains("mac") || operatingSystem.contains("darwin")) {
            return "/usr/lib/libSystem.B.dylib";
        }
        throw new UnsupportedOperationException("Unsupported desktop operating system: " + operatingSystem);
    }

    /// Verifies the address width, byte order, and architecture required by the canonical fixture.
    ///
    /// @throws UnsupportedOperationException if the current process does not match the fixture target
    private static void requireSupportedTarget() {
        if (ValueLayout.ADDRESS.byteSize() != 8) {
            throw new UnsupportedOperationException(
                    "The FFM fixture requires a 64-bit address layout, got " + ValueLayout.ADDRESS.byteSize()
            );
        }
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
            throw new UnsupportedOperationException("The FFM fixture requires a little-endian target");
        }
        String architecture = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (!architecture.equals("amd64")
                && !architecture.equals("x86_64")
                && !architecture.equals("aarch64")
                && !architecture.equals("arm64")) {
            throw new UnsupportedOperationException("Unsupported desktop architecture: " + architecture);
        }
    }
}
