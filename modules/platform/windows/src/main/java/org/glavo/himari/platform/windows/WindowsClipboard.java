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

    /// `ERROR_ACCESS_DENIED`.
    public static final int ERROR_ACCESS_DENIED = 5;

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
        openClipboard(libraries, hwnd);
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
        openClipboard(libraries, hwnd);
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
    /// `WS_EX_NOACTIVATE` tool windows cannot own the clipboard on this host and return
    /// `ERROR_ACCESS_DENIED`. Those failures fall back to `OpenClipboard(NULL)` immediately.
    /// Between retries this method drains the thread queue so a clipboard viewer or delayed-render
    /// owner can release the lock.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the preferred owner window
    private static void openClipboard(WindowsLibraries libraries, MemorySegment hwnd) {
        Win32FfmBindings bindings = libraries.bindings();
        int lastError = 0;
        for (int attempt = 0; attempt < 50; attempt++) {
            Win32FfmBindings.OpenClipboardResult owned = bindings.openClipboard(hwnd);
            if (owned.value() != 0) {
                return;
            }
            lastError = owned.errorCode();
            if (hwnd.address() != 0L) {
                Win32FfmBindings.OpenClipboardResult unowned = bindings.openClipboard(MemorySegment.NULL);
                if (unowned.value() != 0) {
                    return;
                }
                lastError = unowned.errorCode();
            }
            libraries.pumpThreadMessages();
            LockSupport.parkNanos(20_000_000L);
        }
        throw new ClipboardUnavailableException(lastError);
    }

    /// Signals that `OpenClipboard` failed after hwnd and `NULL` attempts.
    @NotNullByDefault
    public static final class ClipboardUnavailableException extends IllegalStateException {
        /// Serialization identifier.
        private static final long serialVersionUID = 1L;

        /// Captured `GetLastError` from the last attempt.
        private final int errorCode;

        /// Creates one failure.
        ///
        /// @param errorCode the last `GetLastError` value
        ClipboardUnavailableException(int errorCode) {
            super("OpenClipboard failed: " + errorCode);
            this.errorCode = errorCode;
        }

        /// Returns the last captured error.
        ///
        /// @return the Win32 error
        public int errorCode() {
            return errorCode;
        }

        /// Returns whether the host denied clipboard access.
        ///
        /// @return whether the error is `ERROR_ACCESS_DENIED`
        public boolean accessDenied() {
            return errorCode == ERROR_ACCESS_DENIED;
        }
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
