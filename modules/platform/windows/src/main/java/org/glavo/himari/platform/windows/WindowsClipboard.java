package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/// Reads and writes Unicode, ANSI, HTML, RTF, DIB, and `CF_HDROP` clipboard payloads through generated
/// User32 and Kernel32 bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsClipboard {
    /// `CF_TEXT`.
    private static final int CF_TEXT = 1;

    /// `CF_DIB`.
    private static final int CF_DIB = 8;

    /// `CF_UNICODETEXT`.
    private static final int CF_UNICODETEXT = 13;

    /// `CF_HDROP`.
    private static final int CF_HDROP = 15;

    /// `DROPFILES` byte size.
    private static final int DROPFILES_SIZE = 20;

    /// First-stable ANSI clipboard encoding (`windows-1252`).
    private static final Charset ANSI = Charset.forName("windows-1252");

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
        Objects.requireNonNull(text, "text");
        writeBytes(libraries, hwnd, CF_UNICODETEXT, (text + '\0').getBytes(StandardCharsets.UTF_16LE));
    }

    /// Registered `HTML Format` clipboard identifier, or `0` before the first query.
    private static volatile int htmlFormatId;

    /// Registered `Rich Text Format` clipboard identifier, or `0` before the first query.
    private static volatile int rtfFormatId;

    /// Replaces the clipboard ANSI text as `CF_TEXT` encoded in windows-1252.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND, or `NULL` when the caller has no window
    /// @param text the text to publish
    public static void writeAnsi(WindowsLibraries libraries, MemorySegment hwnd, String text) {
        Objects.requireNonNull(text, "text");
        writeBytes(libraries, hwnd, CF_TEXT, (text + '\0').getBytes(ANSI));
    }

    /// Replaces the clipboard `CF_DIB` payload.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND, or `NULL` when the caller has no window
    /// @param dib the BITMAPINFO plus pixel bytes
    public static void writeDib(WindowsLibraries libraries, MemorySegment hwnd, byte[] dib) {
        Objects.requireNonNull(dib, "dib");
        if (dib.length < 40) {
            throw new IllegalArgumentException("CF_DIB payload must include a BITMAPINFOHEADER");
        }
        writeBytes(libraries, hwnd, CF_DIB, dib);
    }

    /// Reads the current `CF_DIB` payload.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND
    /// @return the DIB bytes, or `null` when `CF_DIB` is absent
    public static byte @Nullable [] readDib(WindowsLibraries libraries, MemorySegment hwnd) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Win32FfmBindings bindings = libraries.bindings();
        openClipboard(libraries, hwnd);
        try {
            Win32FfmBindings.GetClipboardDataResult handle = bindings.getClipboardData(CF_DIB);
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
                int length = Math.toIntExact(size.value());
                byte[] dib = new byte[length];
                MemorySegment mapped = locked.value().byteSize() < length
                        ? locked.value().reinterpret(length)
                        : locked.value();
                MemorySegment.copy(mapped, 0L, MemorySegment.ofArray(dib), 0L, length);
                return dib;
            } finally {
                bindings.globalUnlock(handle.value());
            }
        } finally {
            bindings.closeClipboard();
        }
    }

    /// Replaces the clipboard `CF_HDROP` file list.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND, or `NULL` when the caller has no window
    /// @param paths the absolute file paths
    public static void writeDropFiles(WindowsLibraries libraries, MemorySegment hwnd, List<String> paths) {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("CF_HDROP requires at least one path");
        }
        writeBytes(libraries, hwnd, CF_HDROP, encodeDropFiles(paths));
    }

    /// Reads the current `CF_HDROP` file list.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND
    /// @return the paths, or `null` when `CF_HDROP` is absent
    public static @Nullable @Unmodifiable List<String> readDropFiles(
            WindowsLibraries libraries,
            MemorySegment hwnd
    ) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Win32FfmBindings bindings = libraries.bindings();
        openClipboard(libraries, hwnd);
        try {
            Win32FfmBindings.GetClipboardDataResult handle = bindings.getClipboardData(CF_HDROP);
            if (handle.value().address() == 0L) {
                return null;
            }
            Win32FfmBindings.GlobalSizeResult size = bindings.globalSize(handle.value());
            if (size.value() < DROPFILES_SIZE) {
                return null;
            }
            Win32FfmBindings.GlobalLockResult locked = bindings.globalLock(handle.value());
            if (locked.value().address() == 0L) {
                throw new IllegalStateException("GlobalLock failed: " + locked.errorCode());
            }
            try {
                int length = Math.toIntExact(size.value());
                byte[] payload = new byte[length];
                MemorySegment mapped = locked.value().byteSize() < length
                        ? locked.value().reinterpret(length)
                        : locked.value();
                MemorySegment.copy(mapped, 0L, MemorySegment.ofArray(payload), 0L, length);
                return decodeDropFiles(payload);
            } finally {
                bindings.globalUnlock(handle.value());
            }
        } finally {
            bindings.closeClipboard();
        }
    }

    /// Encodes a `DROPFILES` plus double-null-terminated Unicode path list.
    ///
    /// @param paths the absolute file paths
    /// @return the `CF_HDROP` payload
    static byte[] encodeDropFiles(List<String> paths) {
        Objects.requireNonNull(paths, "paths");
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("CF_HDROP requires at least one path");
        }
        int listBytes = 2;
        for (String path : paths) {
            Objects.requireNonNull(path, "path");
            if (path.isEmpty()) {
                throw new IllegalArgumentException("CF_HDROP path must be non-empty");
            }
            listBytes += (path.length() + 1) * 2;
        }
        byte[] payload = new byte[DROPFILES_SIZE + listBytes];
        ByteBuffer header = ByteBuffer.wrap(payload, 0, DROPFILES_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(DROPFILES_SIZE);
        header.putInt(0);
        header.putInt(0);
        header.putInt(0);
        header.putInt(1);
        int offset = DROPFILES_SIZE;
        for (String path : paths) {
            byte[] encoded = (path + '\0').getBytes(StandardCharsets.UTF_16LE);
            System.arraycopy(encoded, 0, payload, offset, encoded.length);
            offset += encoded.length;
        }
        return payload;
    }

    /// Decodes a `DROPFILES` plus double-null-terminated path list.
    ///
    /// @param payload the clipboard bytes
    /// @return the paths, or `null` when the header is truncated
    static @Nullable @Unmodifiable List<String> decodeDropFiles(byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload.length < DROPFILES_SIZE) {
            return null;
        }
        ByteBuffer header = ByteBuffer.wrap(payload, 0, DROPFILES_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        int start = header.getInt();
        header.position(16);
        int wide = header.getInt();
        if (start < DROPFILES_SIZE || start >= payload.length) {
            return null;
        }
        ArrayList<String> paths = new ArrayList<>();
        if (wide != 0) {
            int index = start;
            while (index + 1 < payload.length) {
                int end = index;
                while (end + 1 < payload.length
                        && (payload[end] != 0 || payload[end + 1] != 0)) {
                    end += 2;
                }
                if (end == index) {
                    break;
                }
                paths.add(new String(payload, index, end - index, StandardCharsets.UTF_16LE));
                index = end + 2;
            }
        } else {
            int index = start;
            while (index < payload.length) {
                int end = index;
                while (end < payload.length && payload[end] != 0) {
                    end++;
                }
                if (end == index) {
                    break;
                }
                paths.add(new String(payload, index, end - index, ANSI));
                index = end + 1;
            }
        }
        return paths.isEmpty() ? List.of() : List.copyOf(paths);
    }

    /// Replaces the clipboard `HTML Format` payload.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND, or `NULL` when the caller has no window
    /// @param fragment the HTML fragment
    public static void writeHtml(WindowsLibraries libraries, MemorySegment hwnd, String fragment) {
        Objects.requireNonNull(fragment, "fragment");
        writeBytes(libraries, hwnd, htmlFormat(libraries), htmlDescription(fragment).getBytes(StandardCharsets.UTF_8));
    }

    /// Reads the current `HTML Format` fragment.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND
    /// @return the fragment, or `null` when the format is absent
    public static @Nullable String readHtml(WindowsLibraries libraries, MemorySegment hwnd) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Win32FfmBindings bindings = libraries.bindings();
        openClipboard(libraries, hwnd);
        try {
            Win32FfmBindings.GetClipboardDataResult handle = bindings.getClipboardData(htmlFormat(libraries));
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
                return extractHtmlFragment(readUtf8(locked.value(), size.value()));
            } finally {
                bindings.globalUnlock(handle.value());
            }
        } finally {
            bindings.closeClipboard();
        }
    }

    /// Returns the registered `HTML Format` identifier through generated `RegisterClipboardFormatW`.
    ///
    /// @param libraries the session libraries
    /// @return the positive format id
    public static int htmlFormat(WindowsLibraries libraries) {
        Objects.requireNonNull(libraries, "libraries");
        int cached = htmlFormatId;
        if (cached != 0) {
            return cached;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment name = arena.allocateFrom("HTML Format", StandardCharsets.UTF_16LE);
            Win32FfmBindings.RegisterClipboardFormatWResult registered =
                    libraries.bindings().registerClipboardFormatW(name);
            if (registered.value() == 0) {
                throw new IllegalStateException("RegisterClipboardFormatW failed: " + registered.errorCode());
            }
            htmlFormatId = registered.value();
            return registered.value();
        }
    }

    /// Replaces the clipboard `Rich Text Format` payload.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND, or `NULL` when the caller has no window
    /// @param rtf the RTF document
    public static void writeRtf(WindowsLibraries libraries, MemorySegment hwnd, String rtf) {
        Objects.requireNonNull(rtf, "rtf");
        writeBytes(libraries, hwnd, rtfFormat(libraries), rtf.getBytes(StandardCharsets.UTF_8));
    }

    /// Reads the current `Rich Text Format` document.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND
    /// @return the document, or `null` when the format is absent
    public static @Nullable String readRtf(WindowsLibraries libraries, MemorySegment hwnd) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Win32FfmBindings bindings = libraries.bindings();
        openClipboard(libraries, hwnd);
        try {
            Win32FfmBindings.GetClipboardDataResult handle = bindings.getClipboardData(rtfFormat(libraries));
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
                return readUtf8(locked.value(), size.value());
            } finally {
                bindings.globalUnlock(handle.value());
            }
        } finally {
            bindings.closeClipboard();
        }
    }

    /// Returns the registered `Rich Text Format` identifier through generated `RegisterClipboardFormatW`.
    ///
    /// @param libraries the session libraries
    /// @return the positive format id
    public static int rtfFormat(WindowsLibraries libraries) {
        Objects.requireNonNull(libraries, "libraries");
        int cached = rtfFormatId;
        if (cached != 0) {
            return cached;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment name = arena.allocateFrom("Rich Text Format", StandardCharsets.UTF_16LE);
            Win32FfmBindings.RegisterClipboardFormatWResult registered =
                    libraries.bindings().registerClipboardFormatW(name);
            if (registered.value() == 0) {
                throw new IllegalStateException("RegisterClipboardFormatW failed: " + registered.errorCode());
            }
            rtfFormatId = registered.value();
            return registered.value();
        }
    }

    /// Builds a CF_HTML description for `fragment`.
    static String htmlDescription(String fragment) {
        String startMarker = "<!--StartFragment-->";
        String endMarker = "<!--EndFragment-->";
        String prelude = "Version:0.9\r\nStartHTML:%010d\r\nEndHTML:%010d\r\nStartFragment:%010d\r\nEndFragment:%010d\r\n";
        String html = "<html><body>" + startMarker + fragment + endMarker + "</body></html>";
        int startHtml = String.format(prelude, 0, 0, 0, 0).length();
        int startFragment = startHtml + html.indexOf(startMarker) + startMarker.length();
        int endFragment = startHtml + html.indexOf(endMarker);
        int endHtml = startHtml + html.length();
        return String.format(prelude, startHtml, endHtml, startFragment, endFragment) + html;
    }

    /// Returns the fragment between CF_HTML StartFragment and EndFragment markers.
    static String extractHtmlFragment(String description) {
        int start = description.indexOf("<!--StartFragment-->");
        int end = description.indexOf("<!--EndFragment-->");
        if (start < 0 || end < start) {
            return description;
        }
        return description.substring(start + "<!--StartFragment-->".length(), end);
    }

    /// Empties the clipboard without publishing a replacement format.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND, or `NULL` when the caller has no window
    public static void clear(WindowsLibraries libraries, MemorySegment hwnd) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Win32FfmBindings bindings = libraries.bindings();
        openClipboard(libraries, hwnd);
        try {
            Win32FfmBindings.EmptyClipboardResult emptied = bindings.emptyClipboard();
            if (emptied.value() == 0) {
                throw new IllegalStateException("EmptyClipboard failed: " + emptied.errorCode());
            }
        } finally {
            bindings.closeClipboard();
        }
    }

    /// Writes one clipboard format after emptying the clipboard.
    private static void writeBytes(
            WindowsLibraries libraries,
            MemorySegment hwnd,
            int format,
            byte[] payload
    ) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Objects.requireNonNull(payload, "payload");
        Win32FfmBindings bindings = libraries.bindings();
        openClipboard(libraries, hwnd);
        MemorySegment allocated = MemorySegment.NULL;
        try {
            Win32FfmBindings.EmptyClipboardResult emptied = bindings.emptyClipboard();
            if (emptied.value() == 0) {
                throw new IllegalStateException("EmptyClipboard failed: " + emptied.errorCode());
            }
            Win32FfmBindings.GlobalAllocResult allocation = bindings.globalAlloc(
                    GMEM_MOVEABLE,
                    Integer.toUnsignedLong(payload.length)
            );
            if (allocation.value().address() == 0L) {
                throw new IllegalStateException("GlobalAlloc failed: " + allocation.errorCode());
            }
            allocated = allocation.value();
            Win32FfmBindings.GlobalLockResult locked = bindings.globalLock(allocated);
            if (locked.value().address() == 0L) {
                throw new IllegalStateException("GlobalLock failed: " + locked.errorCode());
            }
            MemorySegment mapped = locked.value().reinterpret(payload.length);
            mapped.copyFrom(MemorySegment.ofArray(payload));
            bindings.globalUnlock(allocated);
            Win32FfmBindings.SetClipboardDataResult published = bindings.setClipboardData(format, allocated);
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

    /// Reads the current ANSI clipboard text.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the owning HWND
    /// @return the text, or `null` when `CF_TEXT` is absent
    public static @Nullable String readAnsi(WindowsLibraries libraries, MemorySegment hwnd) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        Win32FfmBindings bindings = libraries.bindings();
        openClipboard(libraries, hwnd);
        try {
            Win32FfmBindings.GetClipboardDataResult handle = bindings.getClipboardData(CF_TEXT);
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
                return readAnsiBytes(locked.value(), size.value());
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

    /// Decodes a NUL-terminated windows-1252 buffer.
    ///
    /// @param pointer the locked memory
    /// @param byteSize the allocated size
    /// @return the Java string
    private static String readAnsiBytes(MemorySegment pointer, long byteSize) {
        int limit = Math.toIntExact(Math.min(byteSize, Integer.MAX_VALUE));
        MemorySegment mapped = pointer.reinterpret(limit);
        int end = 0;
        while (end < limit && mapped.get(ValueLayout.JAVA_BYTE, end) != 0) {
            end++;
        }
        byte[] bytes = mapped.asSlice(0, end).toArray(ValueLayout.JAVA_BYTE);
        return new String(bytes, ANSI);
    }

    /// Decodes a UTF-8 clipboard payload, stripping a trailing NUL when present.
    private static String readUtf8(MemorySegment pointer, long byteSize) {
        int limit = Math.toIntExact(Math.min(byteSize, Integer.MAX_VALUE));
        MemorySegment mapped = pointer.reinterpret(limit);
        int end = limit;
        if (end > 0 && mapped.get(ValueLayout.JAVA_BYTE, end - 1) == 0) {
            end--;
        }
        byte[] bytes = mapped.asSlice(0, end).toArray(ValueLayout.JAVA_BYTE);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
