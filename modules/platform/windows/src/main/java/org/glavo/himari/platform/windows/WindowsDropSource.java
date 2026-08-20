package org.glavo.himari.platform.windows;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.UUID;

/// Implements OLE `IDropSource` and probes generated `DoDragDrop`.
///
/// [`#probeDoDragDrop()`] calls `DoDragDrop` with NULL COM arguments so OLE returns
/// `E_INVALIDARG` before a nested drag loop. [`#invokeQueryContinueDrag()`] and
/// [`#invokeGiveFeedback()`] drive the generated COM vtable.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsDropSource implements AutoCloseable {
    /// `IUnknown`.
    private static final UUID IUNKNOWN = UUID.fromString("00000000-0000-0000-c000-000000000046");

    /// `IDropSource`.
    private static final UUID IDROP_SOURCE = UUID.fromString("00000121-0000-0000-c000-000000000046");

    /// `DROPEFFECT_COPY`.
    static final int DROPEFFECT_COPY = 1;

    /// `DRAGDROP_S_CANCEL`.
    public static final int DRAGDROP_S_CANCEL = 0x0004_0101;

    /// `DRAGDROP_S_USEDEFAULTCURSORS`.
    public static final int DRAGDROP_S_USEDEFAULTCURSORS = 0x0004_0102;

    /// `E_INVALIDARG`.
    public static final int E_INVALIDARG = 0x8007_0057;

    /// `S_OK`.
    private static final int S_OK = 0;

    /// `E_NOINTERFACE`.
    private static final int E_NOINTERFACE = 0x8000_4002;

    /// `E_POINTER`.
    private static final int E_POINTER = 0x8000_4003;

    /// Native bindings.
    private final Win32FfmBindings bindings;

    /// Arena owning the COM object.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue failures = new CallbackFailureQueue();

    /// COM object (`lpVtbl` only).
    private final MemorySegment object;

    /// COM vtable of five function pointers.
    private final MemorySegment vtable;

    /// Outstanding COM references including this owner's reference.
    private int references = 1;

    /// Number of `QueryContinueDrag` invocations.
    private int queryContinueCount;

    /// Number of `GiveFeedback` invocations.
    private int giveFeedbackCount;

    /// Last `DoDragDrop` HRESULT.
    private int lastDragResult;

    /// Last `pdwEffect` written by `DoDragDrop`.
    private int lastEffect;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates the drop source.
    ///
    /// @param libraries the session libraries
    private WindowsDropSource(WindowsLibraries libraries) {
        this.bindings = libraries.bindings();
        this.arena = Arena.ofConfined();
        this.vtable = arena.allocate(ValueLayout.ADDRESS, 5);
        this.object = arena.allocate(ValueLayout.ADDRESS);
        object.set(ValueLayout.ADDRESS, 0L, vtable);
        vtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInterface, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createIdropSourceQueryContinueDragStub(this::queryContinueDrag, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createIdropSourceGiveFeedbackStub(this::giveFeedback, failures, arena));
    }

    /// Creates an `IDropSource` for `libraries`.
    ///
    /// @param libraries the session libraries
    /// @return the source
    public static WindowsDropSource create(WindowsLibraries libraries) {
        Objects.requireNonNull(libraries, "libraries");
        return new WindowsDropSource(libraries);
    }

    /// Invokes generated `DoDragDrop` with NULL COM arguments.
    ///
    /// OLE rejects a NULL `IDataObject` or `IDropSource` with `E_INVALIDARG` before
    /// entering the drag loop, so this drives the exported symbol without a nested
    /// message pump.
    ///
    /// @return the `DoDragDrop` HRESULT
    public int probeDoDragDrop() {
        requireOpen();
        MemorySegment effect = arena.allocate(ValueLayout.JAVA_INT);
        effect.set(ValueLayout.JAVA_INT, 0L, 0);
        lastDragResult = bindings.doDragDrop(MemorySegment.NULL, MemorySegment.NULL, DROPEFFECT_COPY, effect);
        lastEffect = effect.get(ValueLayout.JAVA_INT, 0L);
        return lastDragResult;
    }

    /// Invokes `IDropSource::QueryContinueDrag` through the generated COM vtable.
    ///
    /// @return the HRESULT
    public int invokeQueryContinueDrag() {
        requireOpen();
        MemorySegment query = vtable.getAtIndex(ValueLayout.ADDRESS, 3L);
        int result = Win32FfmBindings.invokeIdropSourceQueryContinueDragPointer(query, object, 0, 0);
        throwContained();
        return result;
    }

    /// Invokes `IDropSource::GiveFeedback` through the generated COM vtable.
    ///
    /// @return the HRESULT
    public int invokeGiveFeedback() {
        requireOpen();
        MemorySegment feedback = vtable.getAtIndex(ValueLayout.ADDRESS, 4L);
        int result = Win32FfmBindings.invokeIdropSourceGiveFeedbackPointer(feedback, object, DROPEFFECT_COPY);
        throwContained();
        return result;
    }

    /// Returns how many times `QueryContinueDrag` ran.
    ///
    /// @return the count
    public int queryContinueCount() {
        return queryContinueCount;
    }

    /// Returns how many times `GiveFeedback` ran.
    ///
    /// @return the count
    public int giveFeedbackCount() {
        return giveFeedbackCount;
    }

    /// Returns the last `DoDragDrop` HRESULT.
    ///
    /// @return the HRESULT
    public int lastDragResult() {
        return lastDragResult;
    }

    /// Returns the last `pdwEffect`.
    ///
    /// @return the effect
    public int lastEffect() {
        return lastEffect;
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
        if (matches(interfaceId, IUNKNOWN) || matches(interfaceId, IDROP_SOURCE)) {
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

    /// Implements `IDropSource::QueryContinueDrag` by cancelling immediately.
    private int queryContinueDrag(MemorySegment self, int escapePressed, int keyState) {
        queryContinueCount++;
        return DRAGDROP_S_CANCEL;
    }

    /// Implements `IDropSource::GiveFeedback`.
    private int giveFeedback(MemorySegment self, int effect) {
        giveFeedbackCount++;
        return DRAGDROP_S_USEDEFAULTCURSORS;
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
            throw new IllegalStateException("IDropSource callback failed", failure);
        }
    }

    /// Verifies the source is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Windows drop source is closed");
        }
    }
}
