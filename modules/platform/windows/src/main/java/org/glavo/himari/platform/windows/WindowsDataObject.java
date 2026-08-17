package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/// Implements OLE `IDataObject` for Unicode text and `CF_HDROP` through generated FFM bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsDataObject implements AutoCloseable {
    /// `IUnknown`.
    private static final UUID IUNKNOWN = UUID.fromString("00000000-0000-0000-c000-000000000046");

    /// `IDataObject`.
    private static final UUID IDATA_OBJECT = UUID.fromString("0000010e-0000-0000-c000-000000000046");

    /// `CF_UNICODETEXT`.
    static final int CF_UNICODETEXT = 13;

    /// `CF_HDROP`.
    static final int CF_HDROP = 15;

    /// `TYMED_HGLOBAL`.
    static final int TYMED_HGLOBAL = 1;

    /// `DVASPECT_CONTENT`.
    private static final int DVASPECT_CONTENT = 1;

    /// `GMEM_MOVEABLE`.
    private static final int GMEM_MOVEABLE = 0x0002;

    /// `S_OK`.
    private static final int S_OK = 0;

    /// `E_NOINTERFACE`.
    private static final int E_NOINTERFACE = 0x8000_4002;

    /// `E_POINTER`.
    private static final int E_POINTER = 0x8000_4003;

    /// `DATA_E_FORMATETC`.
    private static final int DATA_E_FORMATETC = 0x8004_0064;

    /// Native bindings.
    private final Win32FfmBindings bindings;

    /// Arena owning the COM object.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue failures = new CallbackFailureQueue();

    /// Published Unicode payload, or empty when this object publishes only files.
    private final String text;

    /// Published `CF_HDROP` paths, or `null` when this object publishes only text.
    private final @Nullable @Unmodifiable List<String> paths;

    /// COM object.
    private final MemorySegment object;

    /// COM vtable (IUnknown + GetData + QueryGetData).
    private final MemorySegment vtable;

    /// Outstanding references.
    private int references = 1;

    /// Whether closed.
    private boolean closed;

    /// Creates one data object.
    ///
    /// @param bindings the bindings
    /// @param text the Unicode payload, empty when only files are published
    /// @param paths the file paths, or `null`
    private WindowsDataObject(Win32FfmBindings bindings, String text, @Nullable List<String> paths) {
        this.bindings = bindings;
        this.text = text;
        this.paths = paths == null ? null : List.copyOf(paths);
        this.arena = Arena.ofConfined();
        this.vtable = arena.allocate(ValueLayout.ADDRESS, 12);
        this.object = arena.allocate(ValueLayout.ADDRESS);
        object.set(ValueLayout.ADDRESS, 0L, vtable);
        vtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInterface, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIdataObjectGetDataStub(this::getData, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIdataObjectQueryGetDataStub(this::queryGetData, failures, arena));
    }

    /// Creates a Unicode `IDataObject`.
    ///
    /// @param libraries the session libraries
    /// @param text the payload
    /// @return the data object
    public static WindowsDataObject unicode(WindowsLibraries libraries, String text) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(text, "text");
        return new WindowsDataObject(libraries.bindings(), text, null);
    }

    /// Creates a `CF_HDROP` `IDataObject`.
    ///
    /// @param libraries the session libraries
    /// @param paths the absolute file paths
    /// @return the data object
    public static WindowsDataObject files(WindowsLibraries libraries, List<String> paths) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(paths, "paths");
        return new WindowsDataObject(libraries.bindings(), "", paths);
    }

    /// Returns the native `IDataObject` pointer.
    ///
    /// @return the COM object
    MemorySegment nativeObject() {
        requireOpen();
        return object;
    }

    /// Returns the published text.
    ///
    /// @return the text
    public String text() {
        return text;
    }

    /// Releases this owner's COM reference.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        release(object);
        arena.close();
    }

    /// Implements `IUnknown::QueryInterface`.
    private int queryInterface(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        if (WindowsCom.matches(interfaceId, IUNKNOWN) || WindowsCom.matches(interfaceId, IDATA_OBJECT)) {
            result.set(ValueLayout.ADDRESS, 0L, object);
            addRef(self);
            return S_OK;
        }
        result.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return E_NOINTERFACE;
    }

    /// Implements `IUnknown::AddRef`.
    private int addRef(MemorySegment self) {
        references = Math.incrementExact(references);
        return references;
    }

    /// Implements `IUnknown::Release`.
    private int release(MemorySegment self) {
        references = Math.max(0, references - 1);
        return references;
    }

    /// Implements `IDataObject::GetData` for `CF_UNICODETEXT` or `CF_HDROP`.
    private int getData(MemorySegment self, MemorySegment format, MemorySegment medium) {
        if (format.address() == 0L || medium.address() == 0L) {
            return E_POINTER;
        }
        if (!supports(format)) {
            return DATA_E_FORMATETC;
        }
        byte[] utf16 = clipFormat(format) == CF_HDROP && paths != null
                ? WindowsClipboard.encodeDropFiles(paths)
                : (text + '\0').getBytes(StandardCharsets.UTF_16LE);
        Win32FfmBindings.GlobalAllocResult allocation = bindings.globalAlloc(
                GMEM_MOVEABLE,
                Integer.toUnsignedLong(utf16.length)
        );
        if (allocation.value().address() == 0L) {
            return 0x8007_000E;
        }
        Win32FfmBindings.GlobalLockResult locked = bindings.globalLock(allocation.value());
        if (locked.value().address() == 0L) {
            bindings.globalFree(allocation.value());
            return 0x8007_000E;
        }
        locked.value().reinterpret(utf16.length).copyFrom(MemorySegment.ofArray(utf16));
        bindings.globalUnlock(allocation.value());
        MemorySegment out = medium.reinterpret(Win32Layouts.STGMEDIUM.byteSize());
        out.fill((byte) 0);
        out.set(ValueLayout.JAVA_INT, Win32Layouts.STGMEDIUM_TYMED_OFFSET, TYMED_HGLOBAL);
        out.set(ValueLayout.ADDRESS, Win32Layouts.STGMEDIUM_HGLOBAL_OFFSET, allocation.value());
        return S_OK;
    }

    /// Implements `IDataObject::QueryGetData`.
    private int queryGetData(MemorySegment self, MemorySegment format) {
        if (format.address() == 0L) {
            return E_POINTER;
        }
        return supports(format) ? S_OK : DATA_E_FORMATETC;
    }

    /// Returns whether `format` requests Unicode HGLOBAL text or `CF_HDROP`.
    private boolean supports(MemorySegment format) {
        MemorySegment record = format.reinterpret(Win32Layouts.FORMATETC.byteSize());
        int clip = Short.toUnsignedInt(record.get(ValueLayout.JAVA_SHORT, Win32Layouts.FORMATETC_CF_FORMAT_OFFSET));
        int tymed = record.get(ValueLayout.JAVA_INT, Win32Layouts.FORMATETC_TYMED_OFFSET);
        int aspect = record.get(ValueLayout.JAVA_INT, Win32Layouts.FORMATETC_DW_ASPECT_OFFSET);
        boolean hglobal = (tymed & TYMED_HGLOBAL) != 0 && (aspect == 0 || aspect == DVASPECT_CONTENT);
        if (!hglobal) {
            return false;
        }
        if (clip == CF_HDROP) {
            return paths != null;
        }
        return clip == CF_UNICODETEXT && paths == null;
    }

    /// Reads `cfFormat` from a `FORMATETC`.
    private static int clipFormat(MemorySegment format) {
        MemorySegment record = format.reinterpret(Win32Layouts.FORMATETC.byteSize());
        return Short.toUnsignedInt(record.get(ValueLayout.JAVA_SHORT, Win32Layouts.FORMATETC_CF_FORMAT_OFFSET));
    }

    /// Verifies the object is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Windows data object is closed");
        }
    }
}
