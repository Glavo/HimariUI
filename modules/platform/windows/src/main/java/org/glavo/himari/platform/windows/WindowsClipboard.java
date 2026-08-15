package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/// Reads and writes Unicode clipboard text through generated User32 and Kernel32 bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsClipboard {
    /// `CF_UNICODETEXT`.
    private static final int CF_UNICODETEXT = 13;

    /// `GMEM_MOVEABLE`.
    private static final int GMEM_MOVEABLE = 0x0002;

    /// Prevents instantiation.
    private WindowsClipboard() {
    }

    /// Replaces the clipboard Unicode text.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND, or `NULL` when the caller has no window
    /// @param text the text to publish
    public static void writeUnicode(WindowsLibraries libraries, MemorySegment hwnd, String text) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Objects.requireNonNull(text, "text");
        Win32FfmBindings bindings = libraries.bindings();
        byte[] utf16 = (text + '\0').getBytes(StandardCharsets.UTF_16LE);
        openClipboard(bindings, hwnd);
        MemorySegment allocated = MemorySegment.NULL;
        try {
            Win32FfmBindings.EmptyClipboardResult emptied = bindings.emptyClipboard();
            if (emptied.value() == 0) {
                throw new IllegalStateException("EmptyClipboard failed: " + emptied.errorCode());
            }
            Win32FfmBindings.GlobalAllocResult allocation = bindings.globalAlloc(
                    GMEM_MOVEABLE,
                    Integer.toUnsignedLong(utf16.length)
            );
            if (allocation.value().address() == 0L) {
                throw new IllegalStateException("GlobalAlloc failed: " + allocation.errorCode());
            }
            allocated = allocation.value();
            Win32FfmBindings.GlobalLockResult locked = bindings.globalLock(allocated);
            if (locked.value().address() == 0L) {
                throw new IllegalStateException("GlobalLock failed: " + locked.errorCode());
            }
            MemorySegment mapped = locked.value().reinterpret(utf16.length);
            mapped.copyFrom(MemorySegment.ofArray(utf16));
            bindings.globalUnlock(allocated);
            Win32FfmBindings.SetClipboardDataResult published = bindings.setClipboardData(CF_UNICODETEXT, allocated);
            if (published.value().address() == 0L) {
                throw new IllegalStateException("SetClipboardData failed: " + published.errorCode());
            }
            allocated = MemorySegment.NULL;
        } finally {
            if (allocated.address() != 0L) {
                bindings.globalFree(allocated);
            }
            bindings.closeClipboard();
        }
    }

    /// Reads the current Unicode clipboard text.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND
    /// @return the text, or `null` when the format is absent
    public static @Nullable String readUnicode(WindowsLibraries libraries, MemorySegment hwnd) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Win32FfmBindings bindings = libraries.bindings();
        openClipboard(bindings, hwnd);
        try {
            Win32FfmBindings.GetClipboardDataResult handle = bindings.getClipboardData(CF_UNICODETEXT);
            if (handle.value().address() == 0L) {
                return null;
            }
            Win32FfmBindings.GlobalSizeResult size = bindings.globalSize(handle.value());
            if (size.value() == 0L) {
                return null;
            }
            Win32FfmBindings.GlobalLockResult locked = bindings.globalLock(handle.value());
            if (locked.value().address() == 0L) {
                throw new IllegalStateException("GlobalLock failed: " + locked.errorCode());
            }
            try {
                return readUtf16(locked.value(), size.value());
            } finally {
                bindings.globalUnlock(handle.value());
            }
        } finally {
            bindings.closeClipboard();
        }
    }

    /// Opens the clipboard, retrying while another owner briefly holds it.
    ///
    /// @param bindings the bindings
    /// @param hwnd the owner window
    private static void openClipboard(Win32FfmBindings bindings, MemorySegment hwnd) {
        int lastError = 0;
        for (int attempt = 0; attempt < 25; attempt++) {
            Win32FfmBindings.OpenClipboardResult opened = bindings.openClipboard(hwnd);
            if (opened.value() != 0) {
                return;
            }
            lastError = opened.errorCode();
            LockSupport.parkNanos(8_000_000L);
        }
        throw new IllegalStateException("OpenClipboard failed: " + lastError);
    }

    /// Decodes a NUL-terminated UTF-16LE buffer.
    ///
    /// @param pointer the locked memory
    /// @param byteSize the allocated size
    /// @return the Java string
    private static String readUtf16(MemorySegment pointer, long byteSize) {
        int limit = Math.toIntExact(Math.min(byteSize, Integer.MAX_VALUE));
        MemorySegment mapped = pointer.reinterpret(limit);
        int end = 0;
        while (end + 1 < limit) {
            short unit = mapped.get(ValueLayout.JAVA_SHORT, end);
            if (unit == 0) {
                break;
            }
            end += 2;
        }
        byte[] bytes = mapped.asSlice(0, end).toArray(ValueLayout.JAVA_BYTE);
        return new String(bytes, StandardCharsets.UTF_16LE);
    }
}
