package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.platform.api.DisplayColorCapabilities;
import org.glavo.himari.platform.api.DisplayColorDescription;
import org.glavo.himari.platform.api.DisplayId;
import org.glavo.himari.platform.api.DisplaySnapshot;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.PhysicalSize;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.util.Locale;

/// Owns the Kernel32, User32, GDI32, DXGI, OLE, and IMM32 lookups for one Windows session.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsLibraries implements AutoCloseable {
    /// Primary display width in pixels.
    private static final int SM_CXSCREEN = 0;

    /// Primary display height in pixels.
    private static final int SM_CYSCREEN = 1;

    /// Peek and remove.
    private static final int PM_REMOVE = 0x0001;

    /// Thread quit.
    private static final int WM_QUIT = 0x0012;

    /// Fallback USER32 DPI when no HWND has reported one.
    private static final int USER_DEFAULT_SCREEN_DPI = 96;

    /// The arena controlling library lookups.
    private final Arena arena;

    /// The generated bindings.
    private final Win32FfmBindings bindings;

    /// Reusable MSG record for the session pump.
    private final MemorySegment messageRecord;

    /// Creates one owner.
    ///
    /// @param arena the lookup arena
    /// @param bindings the bindings
    /// @param messageRecord the reusable MSG
    private WindowsLibraries(Arena arena, Win32FfmBindings bindings, MemorySegment messageRecord) {
        this.arena = arena;
        this.bindings = bindings;
        this.messageRecord = messageRecord;
    }

    /// Opens the required system libraries.
    ///
    /// @return the library owner
    public static WindowsLibraries open() {
        requireSupportedHost();
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = open("kernel32.dll", arena)
                    .or(open("user32.dll", arena))
                    .or(open("gdi32.dll", arena))
                    .or(open("dxgi.dll", arena))
                    .or(open("ole32.dll", arena))
                    .or(open("imm32.dll", arena));
            Win32FfmBindings bindings = new Win32FfmBindings(symbols);
            int ole = bindings.oleInitialize(MemorySegment.NULL);
            if (ole < 0) {
                throw new IllegalStateException("OleInitialize failed with HRESULT " + ole
                        + " (0x" + Integer.toHexString(ole) + ')');
            }
            return new WindowsLibraries(arena, bindings, arena.allocate(Win32Layouts.MSG));
        } catch (RuntimeException | Error failure) {
            arena.close();
            throw failure;
        }
    }

    /// Returns the generated bindings.
    ///
    /// @return the bindings
    Win32FfmBindings bindings() {
        return bindings;
    }

    /// Dispatches every queued thread message through the production WndProc path.
    ///
    /// @return whether `WM_QUIT` was not observed
    public boolean pumpThreadMessages() {
        boolean running = true;
        while (bindings.peekMessageW(messageRecord, MemorySegment.NULL, 0, 0, PM_REMOVE) != 0) {
            int message = messageRecord.get(ValueLayout.JAVA_INT, Win32Layouts.MSG_MESSAGE_OFFSET);
            if (message == WM_QUIT) {
                running = false;
                continue;
            }
            bindings.translateMessage(messageRecord);
            bindings.dispatchMessageW(messageRecord);
        }
        return running;
    }

    /// Reads the primary display from `GetSystemMetrics`.
    ///
    /// @param dpi the current window DPI used to convert physical pixels to logical bounds
    /// @return the display snapshot
    public DisplaySnapshot primaryDisplay(int dpi) {
        int safeDpi = dpi > 0 ? dpi : USER_DEFAULT_SCREEN_DPI;
        int width = bindings.getSystemMetrics(SM_CXSCREEN);
        int height = bindings.getSystemMetrics(SM_CYSCREEN);
        if (width <= 0 || height <= 0) {
            throw new IllegalStateException("GetSystemMetrics returned a non-positive primary display size");
        }
        double scale = safeDpi / (double) USER_DEFAULT_SCREEN_DPI;
        double logicalWidth = width / scale;
        double logicalHeight = height / scale;
        LogicalRect bounds = new LogicalRect(0.0, 0.0, logicalWidth, logicalHeight);
        return new DisplaySnapshot(
                new DisplayId("primary"),
                0,
                bounds,
                bounds,
                new PhysicalSize(width, height),
                scale,
                true,
                0L,
                new DisplayColorCapabilities(DisplayColorDescription.SRGB_SDR, 0L)
        );
    }

    /// Uninitializes OLE and closes the lookups.
    @Override
    public void close() {
        bindings.oleUninitialize();
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
            throw new IllegalStateException("Windows platform requires Windows, got " + operatingSystem);
        }
        if (!(architecture.equals("amd64") || architecture.equals("x86_64"))) {
            throw new IllegalStateException("Windows platform schema requires x86_64, got " + architecture);
        }
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalStateException("Windows platform schema requires little-endian byte order");
        }
    }
}
