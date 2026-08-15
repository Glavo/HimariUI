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

/// Implements `ITextStoreACP` against a window-attached [WindowsImeSession], including geometry
/// and the remaining embedded/attribute slots.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsTextStore implements AutoCloseable {
    /// `IUnknown`.
    private static final UUID IUNKNOWN = UUID.fromString("00000000-0000-0000-c000-000000000046");

    /// `ITextStoreACP`.
    private static final UUID ITEXT_STORE_ACP = UUID.fromString("28888fe3-c2a0-483a-a3ea-8cb1ce51ff3d");

    /// `ITextStoreACPSink`.
    private static final UUID ITEXT_STORE_ACP_SINK = UUID.fromString("22d44c94-a419-4542-a272-ae26093ececf");

    /// `TS_LF_READ`.
    static final int TS_LF_READ = 2;

    /// `TS_LF_READWRITE`.
    static final int TS_LF_READWRITE = 6;

    /// `TS_LF_SYNC`.
    static final int TS_LF_SYNC = 1;

    /// `TS_E_SYNCHRONOUS`.
    private static final int TS_E_SYNCHRONOUS = 0x0004_022A;

    /// `S_OK`.
    private static final int S_OK = 0;

    /// `S_FALSE`.
    private static final int S_FALSE = 1;

    /// `E_NOINTERFACE`.
    private static final int E_NOINTERFACE = 0x8000_4002;

    /// `E_POINTER`.
    private static final int E_POINTER = 0x8000_4003;

    /// `E_INVALIDARG`.
    private static final int E_INVALIDARG = 0x8007_0057;

    /// `E_FAIL`.
    private static final int E_FAIL = 0x8000_4005;

    /// `TS_DEFAULT_VIEW`.
    private static final int TS_DEFAULT_VIEW = 1;

    /// Native bindings.
    private final Win32FfmBindings bindings;

    /// Arena owning the COM object.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue failures = new CallbackFailureQueue();

    /// Editor session that owns surrounding text.
    private final WindowsImeSession ime;

    /// Owning HWND.
    private final MemorySegment hwnd;

    /// COM object.
    private final MemorySegment object;

    /// COM vtable (`IUnknown` plus 26 `ITextStoreACP` slots).
    private final MemorySegment vtable;

    /// Outstanding references.
    private int references = 1;

    /// Advised sink, or `NULL`.
    private MemorySegment sink = MemorySegment.NULL;

    /// Whether a lock is held.
    private boolean locked;

    /// Current lock flags.
    private int lockFlags;

    /// Number of granted locks.
    private int lockCount;

    /// Whether closed.
    private boolean closed;

    /// Creates one store.
    private WindowsTextStore(Win32FfmBindings bindings, WindowsImeSession ime, MemorySegment hwnd) {
        this.bindings = bindings;
        this.ime = ime;
        this.hwnd = hwnd;
        this.arena = Arena.ofConfined();
        this.vtable = arena.allocate(ValueLayout.ADDRESS, 29);
        this.object = arena.allocate(ValueLayout.ADDRESS);
        object.set(ValueLayout.ADDRESS, 0L, vtable);
        vtable.setAtIndex(ValueLayout.ADDRESS, 0L, bindings.createIunknownQueryInterfaceStub(this::queryInterface, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 1L, bindings.createIunknownAddRefStub(this::addRef, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 2L, bindings.createIunknownReleaseStub(this::release, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 3L, bindings.createItextStoreAdviseSinkStub(this::adviseSink, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 4L, bindings.createItextStoreUnadviseSinkStub(this::unadviseSink, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 5L, bindings.createItextStoreRequestLockStub(this::requestLock, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 6L, bindings.createItextStoreGetStatusStub(this::getStatus, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 7L, bindings.createItextStoreQueryInsertStub(this::queryInsert, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 8L, bindings.createItextStoreGetSelectionStub(this::getSelection, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 9L, bindings.createItextStoreSetSelectionStub(this::setSelection, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 10L, bindings.createItextStoreGetTextStub(this::getText, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 11L, bindings.createItextStoreSetTextStub(this::setText, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 12L, bindings.createItextStoreGetFormattedTextStub(this::getFormattedText, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 13L, bindings.createItextStoreGetEmbeddedStub(this::getEmbedded, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 14L, bindings.createItextStoreQueryInsertEmbeddedStub(this::queryInsertEmbedded, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 15L, bindings.createItextStoreInsertEmbeddedStub(this::insertEmbedded, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 16L, bindings.createItextStoreInsertTextAtSelectionStub(this::insertTextAtSelection, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 17L, bindings.createItextStoreInsertEmbeddedAtSelectionStub(this::insertEmbeddedAtSelection, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 18L, bindings.createItextStoreRequestSupportedAttrsStub(this::requestSupportedAttrs, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 19L, bindings.createItextStoreRequestAttrsAtPositionStub(this::requestAttrsAtPosition, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 20L, bindings.createItextStoreRequestAttrsTransitioningAtPositionStub(this::requestAttrsTransitioningAtPosition, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 21L, bindings.createItextStoreFindNextAttrTransitionStub(this::findNextAttrTransition, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 22L, bindings.createItextStoreRetrieveRequestedAttrsStub(this::retrieveRequestedAttrs, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 23L, bindings.createItextStoreGetEndAcpStub(this::getEndAcp, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 24L, bindings.createItextStoreGetActiveViewStub(this::getActiveView, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 25L, bindings.createItextStoreGetAcpFromPointStub(this::getAcpFromPoint, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 26L, bindings.createItextStoreGetTextExtStub(this::getTextExt, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 27L, bindings.createItextStoreGetScreenExtStub(this::getScreenExt, failures, arena));
        vtable.setAtIndex(ValueLayout.ADDRESS, 28L, bindings.createItextStoreGetWndStub(this::getWnd, failures, arena));
    }

    /// Creates a store bound to one IME session and HWND.
    ///
    /// @param libraries the session libraries
    /// @param ime the editor session
    /// @param hwnd the window
    /// @return the store
    public static WindowsTextStore of(WindowsLibraries libraries, WindowsImeSession ime, MemorySegment hwnd) {
        Objects.requireNonNull(libraries, "libraries");
        Objects.requireNonNull(ime, "ime");
        Objects.requireNonNull(hwnd, "hwnd");
        if (hwnd.address() == 0L) {
            throw new IllegalArgumentException("HWND must not be NULL");
        }
        return new WindowsTextStore(libraries.bindings(), ime, hwnd);
    }

    /// Returns the native `ITextStoreACP` pointer.
    ///
    /// @return the COM object
    MemorySegment nativeObject() {
        requireOpen();
        return object;
    }

    /// Returns how many locks were granted.
    ///
    /// @return the count
    public int lockCount() {
        return lockCount;
    }

    /// Invokes `ITextStoreACP::RequestLock` through the generated vtable.
    ///
    /// @param flags the lock flags
    /// @return the session HRESULT written by the method
    public int invokeRequestLock(int flags) {
        requireOpen();
        MemorySegment result = arena.allocate(ValueLayout.JAVA_INT);
        result.set(ValueLayout.JAVA_INT, 0L, 0);
        int hr = Win32FfmBindings.invokeItextStoreRequestLockPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 5L),
                object,
                flags,
                result
        );
        if (hr < 0) {
            throw new IllegalStateException("ITextStoreACP::RequestLock failed with HRESULT " + hr);
        }
        throwContained();
        return result.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextStoreACP::GetText` through the generated vtable.
    ///
    /// @param start the start ACP
    /// @param end the end ACP, or `-1` for the document end
    /// @return the extracted text
    public String invokeGetText(int start, int end) {
        requireOpen();
        MemorySegment written = arena.allocate(ValueLayout.JAVA_INT);
        written.set(ValueLayout.JAVA_INT, 0L, 0);
        MemorySegment next = arena.allocate(ValueLayout.JAVA_INT);
        next.set(ValueLayout.JAVA_INT, 0L, 0);
        int capacity = Math.max(ime.surroundingText().length() + 1, 1);
        MemorySegment buffer = arena.allocate(ValueLayout.JAVA_SHORT, capacity);
        int hr = Win32FfmBindings.invokeItextStoreGetTextPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 10L),
                object,
                start,
                end,
                buffer,
                capacity,
                written,
                MemorySegment.NULL,
                0,
                MemorySegment.NULL,
                next
        );
        if (hr < 0) {
            throw new IllegalStateException("ITextStoreACP::GetText failed with HRESULT " + hr);
        }
        throwContained();
        int count = written.get(ValueLayout.JAVA_INT, 0L);
        return new String(buffer.asSlice(0, (long) count * 2L).toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_16LE);
    }

    /// Invokes `ITextStoreACP::GetACPFromPoint` through the generated vtable.
    ///
    /// The x coordinate is mapped across the candidate rectangle onto `[0, document length]`.
    ///
    /// @param x the screen x coordinate
    /// @param y the screen y coordinate
    /// @return the mapped ACP
    public int invokeGetAcpFromPoint(int x, int y) {
        requireOpen();
        MemorySegment point = arena.allocate(Win32Layouts.POINT);
        point.set(ValueLayout.JAVA_INT, Win32Layouts.POINT_X_OFFSET, x);
        point.set(ValueLayout.JAVA_INT, Win32Layouts.POINT_Y_OFFSET, y);
        MemorySegment acp = arena.allocate(ValueLayout.JAVA_INT);
        acp.set(ValueLayout.JAVA_INT, 0L, 0);
        int hr = Win32FfmBindings.invokeItextStoreGetAcpFromPointPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 25L),
                object,
                TS_DEFAULT_VIEW,
                point,
                0,
                acp
        );
        if (hr < 0) {
            throw new IllegalStateException("ITextStoreACP::GetACPFromPoint failed with HRESULT " + hr);
        }
        throwContained();
        return acp.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextStoreACP::GetScreenExt` through the generated vtable.
    ///
    /// @return the candidate-window rectangle
    public ScreenExtent invokeGetScreenExt() {
        requireOpen();
        MemorySegment rect = arena.allocate(Win32Layouts.RECT);
        rect.fill((byte) 0);
        int hr = Win32FfmBindings.invokeItextStoreGetScreenExtPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 27L),
                object,
                TS_DEFAULT_VIEW,
                rect
        );
        if (hr < 0) {
            throw new IllegalStateException("ITextStoreACP::GetScreenExt failed with HRESULT " + hr);
        }
        throwContained();
        return new ScreenExtent(
                rect.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET),
                rect.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET),
                rect.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET),
                rect.get(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET)
        );
    }

    /// Invokes `ITextStoreACP::QueryInsertEmbedded` through the generated vtable.
    ///
    /// @return whether an embedded object may be inserted
    public boolean invokeQueryInsertEmbedded() {
        requireOpen();
        MemorySegment insertable = arena.allocate(ValueLayout.JAVA_INT);
        insertable.set(ValueLayout.JAVA_INT, 0L, 1);
        int hr = Win32FfmBindings.invokeItextStoreQueryInsertEmbeddedPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 14L),
                object,
                MemorySegment.NULL,
                MemorySegment.NULL,
                insertable
        );
        if (hr < 0) {
            throw new IllegalStateException("ITextStoreACP::QueryInsertEmbedded failed with HRESULT " + hr);
        }
        throwContained();
        return insertable.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Invokes `ITextStoreACP::GetFormattedText` through the generated vtable.
    ///
    /// @return the native HRESULT
    public int invokeGetFormattedText() {
        requireOpen();
        MemorySegment dataObject = arena.allocate(ValueLayout.ADDRESS);
        dataObject.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        int hr = Win32FfmBindings.invokeItextStoreGetFormattedTextPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 12L),
                object,
                0,
                -1,
                dataObject
        );
        throwContained();
        return hr;
    }

    /// Invokes `ITextStoreACP::RetrieveRequestedAttrs` through the generated vtable.
    ///
    /// @return the number of attributes written
    public int invokeRetrieveRequestedAttrs() {
        requireOpen();
        MemorySegment fetched = arena.allocate(ValueLayout.JAVA_INT);
        fetched.set(ValueLayout.JAVA_INT, 0L, -1);
        int hr = Win32FfmBindings.invokeItextStoreRetrieveRequestedAttrsPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 22L),
                object,
                0,
                MemorySegment.NULL,
                fetched
        );
        if (hr < 0) {
            throw new IllegalStateException("ITextStoreACP::RetrieveRequestedAttrs failed with HRESULT " + hr);
        }
        throwContained();
        return fetched.get(ValueLayout.JAVA_INT, 0L);
    }

    /// Invokes `ITextStoreACP::FindNextAttrTransition` through the generated vtable.
    ///
    /// @return whether a transition was reported
    public boolean invokeFindNextAttrTransition() {
        requireOpen();
        MemorySegment acpNext = arena.allocate(ValueLayout.JAVA_INT);
        MemorySegment found = arena.allocate(ValueLayout.JAVA_INT);
        MemorySegment foundOffset = arena.allocate(ValueLayout.JAVA_INT);
        acpNext.set(ValueLayout.JAVA_INT, 0L, -1);
        found.set(ValueLayout.JAVA_INT, 0L, 1);
        foundOffset.set(ValueLayout.JAVA_INT, 0L, -1);
        int hr = Win32FfmBindings.invokeItextStoreFindNextAttrTransitionPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 21L),
                object,
                0,
                ime.surroundingText().length(),
                0,
                MemorySegment.NULL,
                0,
                acpNext,
                found,
                foundOffset
        );
        if (hr < 0) {
            throw new IllegalStateException("ITextStoreACP::FindNextAttrTransition failed with HRESULT " + hr);
        }
        throwContained();
        return found.get(ValueLayout.JAVA_INT, 0L) != 0;
    }

    /// Invokes `ITextStoreACP::SetText` through the generated vtable.
    ///
    /// @param start the start ACP
    /// @param end the end ACP
    /// @param text the replacement
    public void invokeSetText(int start, int end, String text) {
        requireOpen();
        Objects.requireNonNull(text, "text");
        MemorySegment change = arena.allocate(Win32Layouts.TS_TEXTCHANGE);
        change.fill((byte) 0);
        MemorySegment utf16 = arena.allocateFrom(text, StandardCharsets.UTF_16LE);
        int hr = Win32FfmBindings.invokeItextStoreSetTextPointer(
                vtable.getAtIndex(ValueLayout.ADDRESS, 11L),
                object,
                0,
                start,
                end,
                utf16,
                text.length(),
                change
        );
        if (hr < 0) {
            throw new IllegalStateException("ITextStoreACP::SetText failed with HRESULT " + hr);
        }
        throwContained();
    }

    /// Releases this owner's COM reference.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (sink.address() != 0L) {
            releaseUnknown(sink);
            sink = MemorySegment.NULL;
        }
        release(object);
        arena.close();
    }

    /// Implements `IUnknown::QueryInterface`.
    private int queryInterface(MemorySegment self, MemorySegment interfaceId, MemorySegment result) {
        if (result.address() == 0L) {
            return E_POINTER;
        }
        if (WindowsCom.matches(interfaceId, IUNKNOWN) || WindowsCom.matches(interfaceId, ITEXT_STORE_ACP)) {
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

    /// Implements `ITextStoreACP::AdviseSink`.
    private int adviseSink(MemorySegment self, MemorySegment interfaceId, MemorySegment punk, int mask) {
        if (punk.address() == 0L) {
            return E_POINTER;
        }
        if (!WindowsCom.matches(interfaceId, ITEXT_STORE_ACP_SINK) && !WindowsCom.matches(interfaceId, IUNKNOWN)) {
            return E_NOINTERFACE;
        }
        if (sink.address() != 0L) {
            releaseUnknown(sink);
        }
        sink = punk;
        addRefUnknown(punk);
        return S_OK;
    }

    /// Implements `ITextStoreACP::UnadviseSink`.
    private int unadviseSink(MemorySegment self, MemorySegment punk) {
        if (sink.address() == 0L || punk.address() == 0L || sink.address() != punk.address()) {
            return CONNECT_E_NOCONNECTION;
        }
        releaseUnknown(sink);
        sink = MemorySegment.NULL;
        return S_OK;
    }

    /// `CONNECT_E_NOCONNECTION`.
    private static final int CONNECT_E_NOCONNECTION = 0x8004_0200;

    /// Implements `ITextStoreACP::RequestLock`.
    private int requestLock(MemorySegment self, int flags, MemorySegment sessionResult) {
        if (sessionResult.address() == 0L) {
            return E_POINTER;
        }
        if (locked) {
            sessionResult.set(ValueLayout.JAVA_INT, 0L, TS_E_SYNCHRONOUS);
            return (flags & TS_LF_SYNC) != 0 ? S_OK : S_FALSE;
        }
        locked = true;
        lockFlags = flags;
        lockCount++;
        int granted = S_OK;
        if (sink.address() != 0L) {
            MemorySegment vtableAddress = sink.reinterpret(ValueLayout.ADDRESS.byteSize())
                    .get(ValueLayout.ADDRESS, 0L);
            MemorySegment onLock = vtableAddress.reinterpret(ValueLayout.ADDRESS.byteSize() * 4L)
                    .getAtIndex(ValueLayout.ADDRESS, 3L);
            granted = Win32FfmBindings.invokeItfTextStoreAcpSinkOnLockGrantedPointer(onLock, sink, flags);
        }
        locked = false;
        lockFlags = 0;
        sessionResult.set(ValueLayout.JAVA_INT, 0L, granted);
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetStatus`.
    private int getStatus(MemorySegment self, MemorySegment status) {
        if (status.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment record = status.reinterpret(Win32Layouts.TS_STATUS.byteSize());
        record.fill((byte) 0);
        return S_OK;
    }

    /// Implements `ITextStoreACP::QueryInsert`.
    private int queryInsert(
            MemorySegment self,
            int testStart,
            int testEnd,
            int cch,
            MemorySegment resultStart,
            MemorySegment resultEnd
    ) {
        if (resultStart.address() == 0L || resultEnd.address() == 0L) {
            return E_POINTER;
        }
        int length = ime.surroundingText().length();
        if (testStart < 0 || testEnd < testStart || testEnd > length) {
            return E_INVALIDARG;
        }
        resultStart.set(ValueLayout.JAVA_INT, 0L, testStart);
        resultEnd.set(ValueLayout.JAVA_INT, 0L, testStart + cch);
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetSelection`.
    private int getSelection(
            MemorySegment self,
            int index,
            int count,
            MemorySegment selection,
            MemorySegment fetched
    ) {
        if (selection.address() == 0L || fetched.address() == 0L) {
            return E_POINTER;
        }
        if (count == 0) {
            fetched.set(ValueLayout.JAVA_INT, 0L, 0);
            return S_OK;
        }
        MemorySegment record = selection.reinterpret(Win32Layouts.TS_SELECTION_ACP.byteSize());
        record.fill((byte) 0);
        record.set(ValueLayout.JAVA_INT, Win32Layouts.TS_SELECTION_ACP_ACP_START_OFFSET, ime.compositionStart());
        record.set(ValueLayout.JAVA_INT, Win32Layouts.TS_SELECTION_ACP_ACP_END_OFFSET, ime.compositionEnd());
        fetched.set(ValueLayout.JAVA_INT, 0L, 1);
        return S_OK;
    }

    /// Implements `ITextStoreACP::SetSelection`.
    private int setSelection(MemorySegment self, int count, MemorySegment selection) {
        if (selection.address() == 0L || count == 0) {
            return E_INVALIDARG;
        }
        MemorySegment record = selection.reinterpret(Win32Layouts.TS_SELECTION_ACP.byteSize());
        int start = record.get(ValueLayout.JAVA_INT, Win32Layouts.TS_SELECTION_ACP_ACP_START_OFFSET);
        ime.setSurroundingText(ime.surroundingText(), clamp(start, ime.surroundingText().length()));
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetText`.
    private int getText(
            MemorySegment self,
            int acpStart,
            int acpEnd,
            MemorySegment plain,
            int cch,
            MemorySegment written,
            MemorySegment runInfo,
            int cRunInfo,
            MemorySegment runWritten,
            MemorySegment acpNext
    ) {
        if (written.address() == 0L) {
            return E_POINTER;
        }
        String text = ime.surroundingText();
        int end = acpEnd < 0 ? text.length() : acpEnd;
        if (acpStart < 0 || end < acpStart || end > text.length()) {
            return E_INVALIDARG;
        }
        String slice = text.substring(acpStart, end);
        int copy = Math.min(slice.length(), Math.max(cch, 0));
        if (plain.address() != 0L && copy > 0) {
            byte[] utf16 = slice.substring(0, copy).getBytes(StandardCharsets.UTF_16LE);
            plain.reinterpret(utf16.length).copyFrom(MemorySegment.ofArray(utf16));
        }
        written.set(ValueLayout.JAVA_INT, 0L, copy);
        if (runInfo.address() != 0L && cRunInfo > 0) {
            MemorySegment run = runInfo.reinterpret(Win32Layouts.TS_RUNINFO.byteSize());
            run.set(ValueLayout.JAVA_INT, Win32Layouts.TS_RUNINFO_COUNT_OFFSET, copy);
            run.set(ValueLayout.JAVA_INT, Win32Layouts.TS_RUNINFO_TYPE_OFFSET, 0);
            if (runWritten.address() != 0L) {
                runWritten.set(ValueLayout.JAVA_INT, 0L, 1);
            }
        } else if (runWritten.address() != 0L) {
            runWritten.set(ValueLayout.JAVA_INT, 0L, 0);
        }
        if (acpNext.address() != 0L) {
            acpNext.set(ValueLayout.JAVA_INT, 0L, acpStart + copy);
        }
        return S_OK;
    }

    /// Implements `ITextStoreACP::SetText`.
    private int setText(
            MemorySegment self,
            int flags,
            int acpStart,
            int acpEnd,
            MemorySegment text,
            int cch,
            MemorySegment change
    ) {
        if (change.address() == 0L) {
            return E_POINTER;
        }
        String replacement = readUtf16(text, cch);
        try {
            ime.replaceRange(acpStart, acpEnd, replacement);
        } catch (RuntimeException failure) {
            return E_INVALIDARG;
        }
        MemorySegment record = change.reinterpret(Win32Layouts.TS_TEXTCHANGE.byteSize());
        record.set(ValueLayout.JAVA_INT, Win32Layouts.TS_TEXTCHANGE_ACP_START_OFFSET, acpStart);
        record.set(ValueLayout.JAVA_INT, Win32Layouts.TS_TEXTCHANGE_ACP_OLD_END_OFFSET, acpEnd);
        record.set(ValueLayout.JAVA_INT, Win32Layouts.TS_TEXTCHANGE_ACP_NEW_END_OFFSET, acpStart + replacement.length());
        return S_OK;
    }

    /// Implements `ITextStoreACP::InsertTextAtSelection`.
    private int insertTextAtSelection(
            MemorySegment self,
            int flags,
            MemorySegment text,
            int cch,
            MemorySegment acpStart,
            MemorySegment acpEnd,
            MemorySegment change
    ) {
        int start = ime.compositionStart();
        int end = ime.compositionEnd();
        String replacement = readUtf16(text, cch);
        ime.replaceRange(start, end, replacement);
        if (acpStart.address() != 0L) {
            acpStart.set(ValueLayout.JAVA_INT, 0L, start);
        }
        if (acpEnd.address() != 0L) {
            acpEnd.set(ValueLayout.JAVA_INT, 0L, start + replacement.length());
        }
        if (change.address() != 0L) {
            MemorySegment record = change.reinterpret(Win32Layouts.TS_TEXTCHANGE.byteSize());
            record.set(ValueLayout.JAVA_INT, Win32Layouts.TS_TEXTCHANGE_ACP_START_OFFSET, start);
            record.set(ValueLayout.JAVA_INT, Win32Layouts.TS_TEXTCHANGE_ACP_OLD_END_OFFSET, end);
            record.set(ValueLayout.JAVA_INT, Win32Layouts.TS_TEXTCHANGE_ACP_NEW_END_OFFSET, start + replacement.length());
        }
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetEndACP`.
    private int getEndAcp(MemorySegment self, MemorySegment acp) {
        if (acp.address() == 0L) {
            return E_POINTER;
        }
        acp.set(ValueLayout.JAVA_INT, 0L, ime.surroundingText().length());
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetActiveView`.
    private int getActiveView(MemorySegment self, MemorySegment view) {
        if (view.address() == 0L) {
            return E_POINTER;
        }
        view.set(ValueLayout.JAVA_INT, 0L, TS_DEFAULT_VIEW);
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetTextExt`.
    private int getTextExt(
            MemorySegment self,
            int view,
            int acpStart,
            int acpEnd,
            MemorySegment rect,
            MemorySegment clipped
    ) {
        if (rect.address() == 0L || clipped.address() == 0L) {
            return E_POINTER;
        }
        int left = Math.round(ime.candidateX());
        int top = Math.round(ime.candidateY());
        MemorySegment record = rect.reinterpret(Win32Layouts.RECT.byteSize());
        record.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET, left);
        record.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET, top);
        record.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET, left + Math.round(ime.candidateWidth()));
        record.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET, top + Math.round(ime.candidateHeight()));
        clipped.set(ValueLayout.JAVA_INT, 0L, 0);
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetWnd`.
    private int getWnd(MemorySegment self, int view, MemorySegment window) {
        if (window.address() == 0L) {
            return E_POINTER;
        }
        window.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, hwnd);
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetFormattedText`.
    ///
    /// This store exposes plain UTF-16 only, so no `IDataObject` is produced.
    private int getFormattedText(MemorySegment self, int acpStart, int acpEnd, MemorySegment dataObject) {
        if (dataObject.address() == 0L) {
            return E_POINTER;
        }
        dataObject.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return E_FAIL;
    }

    /// Implements `ITextStoreACP::GetEmbedded`.
    private int getEmbedded(
            MemorySegment self,
            int acpPos,
            MemorySegment service,
            MemorySegment interfaceId,
            MemorySegment unknown
    ) {
        if (unknown.address() == 0L) {
            return E_POINTER;
        }
        unknown.reinterpret(ValueLayout.ADDRESS.byteSize()).set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        return E_FAIL;
    }

    /// Implements `ITextStoreACP::QueryInsertEmbedded`.
    private int queryInsertEmbedded(
            MemorySegment self,
            MemorySegment service,
            MemorySegment format,
            MemorySegment insertable
    ) {
        if (insertable.address() == 0L) {
            return E_POINTER;
        }
        insertable.set(ValueLayout.JAVA_INT, 0L, 0);
        return S_OK;
    }

    /// Implements `ITextStoreACP::InsertEmbedded`.
    private int insertEmbedded(
            MemorySegment self,
            int flags,
            int acpStart,
            int acpEnd,
            MemorySegment dataObject,
            MemorySegment change
    ) {
        if (change.address() == 0L) {
            return E_POINTER;
        }
        return E_FAIL;
    }

    /// Implements `ITextStoreACP::InsertEmbeddedAtSelection`.
    private int insertEmbeddedAtSelection(
            MemorySegment self,
            int flags,
            MemorySegment dataObject,
            MemorySegment acpStart,
            MemorySegment acpEnd,
            MemorySegment change
    ) {
        return E_FAIL;
    }

    /// Implements `ITextStoreACP::RequestSupportedAttrs`.
    private int requestSupportedAttrs(MemorySegment self, int flags, int filterCount, MemorySegment filterAttrs) {
        return S_OK;
    }

    /// Implements `ITextStoreACP::RequestAttrsAtPosition`.
    private int requestAttrsAtPosition(
            MemorySegment self,
            int acpPos,
            int filterCount,
            MemorySegment filterAttrs,
            int flags
    ) {
        return S_OK;
    }

    /// Implements `ITextStoreACP::RequestAttrsTransitioningAtPosition`.
    private int requestAttrsTransitioningAtPosition(
            MemorySegment self,
            int acpPos,
            int filterCount,
            MemorySegment filterAttrs,
            int flags
    ) {
        return S_OK;
    }

    /// Implements `ITextStoreACP::FindNextAttrTransition`.
    private int findNextAttrTransition(
            MemorySegment self,
            int acpStart,
            int acpHalt,
            int filterCount,
            MemorySegment filterAttrs,
            int flags,
            MemorySegment acpNext,
            MemorySegment found,
            MemorySegment foundOffset
    ) {
        if (acpNext.address() == 0L || found.address() == 0L || foundOffset.address() == 0L) {
            return E_POINTER;
        }
        acpNext.set(ValueLayout.JAVA_INT, 0L, acpHalt);
        found.set(ValueLayout.JAVA_INT, 0L, 0);
        foundOffset.set(ValueLayout.JAVA_INT, 0L, 0);
        return S_OK;
    }

    /// Implements `ITextStoreACP::RetrieveRequestedAttrs`.
    private int retrieveRequestedAttrs(MemorySegment self, int count, MemorySegment values, MemorySegment fetched) {
        if (fetched.address() == 0L) {
            return E_POINTER;
        }
        fetched.set(ValueLayout.JAVA_INT, 0L, 0);
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetACPFromPoint`.
    private int getAcpFromPoint(
            MemorySegment self,
            int view,
            MemorySegment point,
            int flags,
            MemorySegment acp
    ) {
        if (point.address() == 0L || acp.address() == 0L) {
            return E_POINTER;
        }
        MemorySegment record = point.reinterpret(Win32Layouts.POINT.byteSize());
        int x = record.get(ValueLayout.JAVA_INT, Win32Layouts.POINT_X_OFFSET);
        int left = Math.round(ime.candidateX());
        int width = Math.max(Math.round(ime.candidateWidth()), 1);
        int length = ime.surroundingText().length();
        int mapped = (int) Math.round((double) (x - left) * length / width);
        acp.set(ValueLayout.JAVA_INT, 0L, clamp(mapped, length));
        return S_OK;
    }

    /// Implements `ITextStoreACP::GetScreenExt`.
    private int getScreenExt(MemorySegment self, int view, MemorySegment rect) {
        if (rect.address() == 0L) {
            return E_POINTER;
        }
        int left = Math.round(ime.candidateX());
        int top = Math.round(ime.candidateY());
        MemorySegment record = rect.reinterpret(Win32Layouts.RECT.byteSize());
        record.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_LEFT_OFFSET, left);
        record.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_TOP_OFFSET, top);
        record.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_RIGHT_OFFSET, left + Math.round(ime.candidateWidth()));
        record.set(ValueLayout.JAVA_INT, Win32Layouts.RECT_BOTTOM_OFFSET, top + Math.round(ime.candidateHeight()));
        return S_OK;
    }

    /// Screen rectangle returned by `ITextStoreACP::GetScreenExt`.
    ///
    /// @param left the inclusive left edge
    /// @param top the inclusive top edge
    /// @param right the exclusive right edge
    /// @param bottom the exclusive bottom edge
    public record ScreenExtent(int left, int top, int right, int bottom) {
        /// Accepts any integer rectangle reported by the store.
        public ScreenExtent {
        }
    }

    /// Reads `cch` UTF-16 code units.
    private static String readUtf16(MemorySegment text, int cch) {
        if (text.address() == 0L || cch <= 0) {
            return "";
        }
        MemorySegment mapped = text.reinterpret((long) cch * 2L);
        return new String(mapped.toArray(ValueLayout.JAVA_BYTE), StandardCharsets.UTF_16LE);
    }

    /// Adds a reference on an `IUnknown`.
    private static void addRefUnknown(MemorySegment unknown) {
        MemorySegment vtableAddress = unknown.reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0L);
        MemorySegment addRef = vtableAddress.reinterpret(ValueLayout.ADDRESS.byteSize() * 2L)
                .getAtIndex(ValueLayout.ADDRESS, 1L);
        Win32FfmBindings.invokeIunknownAddRefPointer(addRef, unknown);
    }

    /// Releases an `IUnknown`.
    private static void releaseUnknown(MemorySegment unknown) {
        MemorySegment vtableAddress = unknown.reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0L);
        MemorySegment release = vtableAddress.reinterpret(ValueLayout.ADDRESS.byteSize() * 3L)
                .getAtIndex(ValueLayout.ADDRESS, 2L);
        Win32FfmBindings.invokeIunknownReleasePointer(release, unknown);
    }

    /// Clamps an ACP into `[0, length]`.
    private static int clamp(int value, int length) {
        return Math.max(0, Math.min(value, length));
    }

    /// Throws a contained callback failure.
    private void throwContained() {
        @Nullable Throwable failure = failures.poll();
        if (failure != null) {
            throw new IllegalStateException("ITextStoreACP callback failed", failure);
        }
    }

    /// Verifies the store is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Windows text store is closed");
        }
    }
}
