package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/// Registers one OLE `IDropTarget` on a production HWND through generated FFM bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsDropTarget implements AutoCloseable {
    /// `IUnknown`.
    private static final UUID IUNKNOWN = UUID.fromString("00000000-0000-0000-c000-000000000046");

    /// `IDropTarget`.
    private static final UUID IDROP_TARGET = UUID.fromString("00000122-0000-0000-c000-000000000046");

    /// `E_NOINTERFACE`.
    private static final int E_NOINTERFACE = 0x8000_4002;

    /// `E_POINTER`.
    private static final int E_POINTER = 0x8000_4003;

    /// `DROPEFFECT_COPY`.
    private static final int DROPEFFECT_COPY = 1;

    /// `S_OK`.
    private static final int S_OK = 0;

    /// Native bindings.
    private final Win32FfmBindings bindings;

    /// Arena owning the COM object and stubs.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue failures = new CallbackFailureQueue();

    /// The HWND that owns the registration.
    private final MemorySegment hwnd;

    /// COM object (`lpVtbl` only).
    private final MemorySegment object;

    /// COM vtable of seven function pointers.
    private final MemorySegment vtable;

    /// Outstanding COM references including this owner's reference.
    private int references = 1;

    /// Number of `IDropTarget::Drop` invocations.
    private int dropCount;

    /// Last drop x in screen pixels.
    private int lastDropX;

    /// Last drop y in screen pixels.
    private int lastDropY;

    /// Unicode text extracted by `IDataObject::GetData`, or `null`.
    private @Nullable String lastDroppedText;

    /// Whether `RegisterDragDrop` succeeded.
    private boolean registered;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates and registers the drop target.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the HWND
    private WindowsDropTarget(WindowsLibraries libraries, MemorySegment hwnd) {
        this.bindings = libraries.bindings();
        this.arena = Arena.ofConfined();
        this.hwnd = hwnd;
        this.vtable = arena.allocate(ValueLayout.ADDRESS, 7);
        this.object = arena.allocate(ValueLayout.ADDRESS);
        object.set(ValueLayout.ADDRESS, 0L, vtable);
        vtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInterface, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIdropTargetDragEnterStub(this::dragEnter, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIdropTargetDragOverStub(this::dragOver, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createIdropTargetDragLeaveStub(this::dragLeave, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 6L, bindings.createIdropTargetDropStub(this::drop, failures, arena));
    }

    /// Registers an `IDropTarget` on `hwnd`.
    ///
    /// @param libraries the session libraries
    /// @param hwnd the native window
    /// @return the registered target
    public static WindowsDropTarget register(WindowsLibraries libraries, MemorySegment hwnd) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(hwnd, "hwnd");
        if (hwnd.address() == 0L) {
            throw new IllegalArgumentException("HWND must not be NULL");
        }
        WindowsDropTarget target = new WindowsDropTarget(libraries, hwnd);
        try {
            int registered = libraries.bindings().registerDragDrop(hwnd, target.object);
            if (registered < 0) {
                throw new IllegalStateException("RegisterDragDrop failed with HRESULT " + registered
                        + " (0x" + Integer.toHexString(registered) + ')');
            }
            target.registered = true;
            return target;
        } catch (RuntimeException | Error failure) {
            target.close();
            throw failure;
        }
    }

    /// Invokes `IDropTarget::Drop` through the generated COM vtable.
    ///
    /// @param x the screen x
    /// @param y the screen y
    public void invokeDrop(int x, int y) {
        invokeDrop(MemorySegment.NULL, x, y);
    }

    /// Invokes `IDropTarget::Drop` with an `IDataObject`.
    ///
    /// @param dataObject the source object, or `NULL`
    /// @param x the screen x
    /// @param y the screen y
    public void invokeDrop(MemorySegment dataObject, int x, int y) {
        requireOpen();
        MemorySegment point = arena.allocate(Win32Layouts.POINTL);
        point.set(ValueLayout.JAVA_INT, Win32Layouts.POINTL_X_OFFSET, x);
        point.set(ValueLayout.JAVA_INT, Win32Layouts.POINTL_Y_OFFSET, y);
        MemorySegment effect = arena.allocate(ValueLayout.JAVA_INT);
        effect.set(ValueLayout.JAVA_INT, 0L, DROPEFFECT_COPY);
        MemorySegment drop = vtable.getAtIndex(ValueLayout.ADDRESS, 6L);
        int result = Win32FfmBindings.invokeIdropTargetDropPointer(
                drop,
                object,
                dataObject,
                0,
                point,
                effect
        );
        if (result < 0) {
            throw new IllegalStateException("IDropTarget::Drop failed with HRESULT " + result
                    + " (0x" + Integer.toHexString(result) + ')');
        }
        throwContained();
    }

    /// Returns the number of native `Drop` invocations.
    ///
    /// @return the count
    public int dropCount() {
        return dropCount;
    }

    /// Returns the last drop x.
    ///
    /// @return the x
    public int lastDropX() {
        return lastDropX;
    }

    /// Returns the last drop y.
    ///
    /// @return the y
    public int lastDropY() {
        return lastDropY;
    }

    /// Returns Unicode text extracted through `IDataObject::GetData`.
    ///
    /// @return the text, or `null` when no Unicode payload was present
    public @Nullable String lastDroppedText() {
        return lastDroppedText;
    }

    /// Revokes the HWND registration and releases this owner's COM reference.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (registered) {
            bindings.revokeDragDrop(hwnd);
            registered = false;
        }
        release(object);
        arena.close();
    }

    /// Implements `IUnknown::QueryInterface`.
    private int queryInterface(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        if (matches(interfaceId, IUNKNOWN) || matches(interfaceId, IDROP_TARGET)) {
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

    /// Implements `IDropTarget::DragEnter`.
    private int dragEnter(
            MemorySegment self,
            MemorySegment dataObject,
            int keyState,
            MemorySegment point,
            MemorySegment effect
    ) {
        writeCopyEffect(effect);
        return S_OK;
    }

    /// Implements `IDropTarget::DragOver`.
    private int dragOver(MemorySegment self, int keyState, MemorySegment point, MemorySegment effect) {
        writeCopyEffect(effect);
        return S_OK;
    }

    /// Implements `IDropTarget::DragLeave`.
    private int dragLeave(MemorySegment self) {
        return S_OK;
    }

    /// Implements `IDropTarget::Drop`.
    private int drop(
            MemorySegment self,
            MemorySegment dataObject,
            int keyState,
            MemorySegment point,
            MemorySegment effect
    ) {
        lastDropX = point.get(ValueLayout.JAVA_INT, Win32Layouts.POINTL_X_OFFSET);
        lastDropY = point.get(ValueLayout.JAVA_INT, Win32Layouts.POINTL_Y_OFFSET);
        lastDroppedText = extractUnicode(dataObject);
        dropCount++;
        writeCopyEffect(effect);
        return S_OK;
    }

    /// Calls `IDataObject::GetData` for `CF_UNICODETEXT`.
    ///
    /// @param dataObject the source, or `NULL`
    /// @return the Unicode payload, or `null`
    private @Nullable String extractUnicode(MemorySegment dataObject) {
        if (dataObject.address() == 0L) {
            return null;
        }
        MemorySegment vtableAddress = dataObject.reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0L);
        if (vtableAddress.address() == 0L) {
            return null;
        }
        MemorySegment getData = vtableAddress.reinterpret(ValueLayout.ADDRESS.byteSize() * 4L)
                .getAtIndex(ValueLayout.ADDRESS, 3L);
        MemorySegment format = arena.allocate(Win32Layouts.FORMATETC);
        format.fill((byte) 0);
        format.set(ValueLayout.JAVA_SHORT, Win32Layouts.FORMATETC_CF_FORMAT_OFFSET,
                (short) WindowsDataObject.CF_UNICODETEXT);
        format.set(ValueLayout.JAVA_INT, Win32Layouts.FORMATETC_DW_ASPECT_OFFSET, 1);
        format.set(ValueLayout.JAVA_INT, Win32Layouts.FORMATETC_LINDEX_OFFSET, -1);
        format.set(ValueLayout.JAVA_INT, Win32Layouts.FORMATETC_TYMED_OFFSET, WindowsDataObject.TYMED_HGLOBAL);
        MemorySegment medium = arena.allocate(Win32Layouts.STGMEDIUM);
        medium.fill((byte) 0);
        int result = Win32FfmBindings.invokeIdataObjectGetDataPointer(getData, dataObject, format, medium);
        if (result < 0) {
            return null;
        }
        try {
            MemorySegment handle = medium.get(ValueLayout.ADDRESS, Win32Layouts.STGMEDIUM_HGLOBAL_OFFSET);
            if (handle.address() == 0L) {
                return null;
            }
            Win32FfmBindings.GlobalLockResult locked = bindings.globalLock(handle);
            if (locked.value().address() == 0L) {
                return null;
            }
            try {
                Win32FfmBindings.GlobalSizeResult size = bindings.globalSize(handle);
                return readUtf16(locked.value(), size.value());
            } finally {
                bindings.globalUnlock(handle);
            }
        } finally {
            bindings.releaseStgMedium(medium);
        }
    }

    /// Decodes a NUL-terminated UTF-16LE buffer.
    private static String readUtf16(MemorySegment pointer, long byteSize) {
        int limit = Math.toIntExact(Math.min(Math.max(byteSize, 0L), Integer.MAX_VALUE));
        MemorySegment mapped = pointer.reinterpret(Math.max(limit, 2));
        int end = 0;
        while (end + 1 < limit) {
            if (mapped.get(ValueLayout.JAVA_SHORT, end) == 0) {
                break;
            }
            end += 2;
        }
        return new String(mapped.asSlice(0, end).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_16LE);
    }

    /// Writes `DROPEFFECT_COPY` when the caller supplied an effect cell.
    private static void writeCopyEffect(MemorySegment effect) {
        if (effect.address() != 0L) {
            effect.set(ValueLayout.JAVA_INT, 0L, DROPEFFECT_COPY);
        }
    }

    /// Compares a native GUID with a Java UUID.
    private static boolean matches(MemorySegment interfaceId, UUID expected) {
        if (interfaceId.address() == 0L) {
            return false;
        }
        MemorySegment guid = interfaceId.reinterpret(Win32Layouts.GUID.byteSize());
        int data1 = guid.get(ValueLayout.JAVA_INT, Win32Layouts.GUID_DATA1_OFFSET);
        short data2 = guid.get(ValueLayout.JAVA_SHORT, Win32Layouts.GUID_DATA2_OFFSET);
        short data3 = guid.get(ValueLayout.JAVA_SHORT, Win32Layouts.GUID_DATA3_OFFSET);
        long most = expected.getMostSignificantBits();
        if (data1 != (int) (most >>> 32) || data2 != (short) (most >>> 16) || data3 != (short) most) {
            return false;
        }
        long least = expected.getLeastSignificantBits();
        for (int index = 0; index < 8; index++) {
            byte actual = guid.get(ValueLayout.JAVA_BYTE, Win32Layouts.GUID_DATA4_0_OFFSET + index);
            byte wanted = (byte) (least >>> (56 - index * 8));
            if (actual != wanted) {
                return false;
            }
        }
        return true;
    }

    /// Throws a contained callback failure.
    private void throwContained() {
        @Nullable Throwable failure = failures.poll();
        if (failure != null) {
            throw new IllegalStateException("IDropTarget callback failed", failure);
        }
    }

    /// Verifies the target is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Windows drop target is closed");
        }
    }
}
