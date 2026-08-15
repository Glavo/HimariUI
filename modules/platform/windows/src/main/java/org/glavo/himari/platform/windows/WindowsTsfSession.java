package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Reaches the Text Services Framework thread manager through `CoCreateInstance`.
///
/// This is the production TSF entry, not a full `ITextStoreACP` implementation. A later adapter
/// will attach document locks and composition sinks to [WindowsImeSession].
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WindowsTsfSession implements AutoCloseable {
    /// `CLSID_TF_ThreadMgr`.
    private static final String CLSID_TF_THREAD_MGR = "529a9e6b-6587-4f23-ab9e-9c7d683e3c50";

    /// `IID_ITfThreadMgr`.
    private static final String IID_ITF_THREAD_MGR = "aa80e801-2021-11d2-93e0-0060b067b86e";

    /// `CLSCTX_INPROC_SERVER`.
    private static final int CLSCTX_INPROC_SERVER = 1;

    /// Native thread-manager pointer, or `NULL` when TSF is unavailable.
    private final MemorySegment threadManager;

    /// `HRESULT` from `CoCreateInstance`.
    private final int createResult;

    /// Arena owning GUID cells.
    private final Arena arena;

    /// `HRESULT` from `ITfThreadMgr::Activate`, or `Integer.MIN_VALUE` when not invoked.
    private int activateResult = Integer.MIN_VALUE;

    /// Assigned TSF client identifier after a successful activate.
    private int clientId;

    /// Whether `Activate` succeeded.
    private boolean activated;

    /// Created document manager, or `NULL`.
    private MemorySegment documentMgr = MemorySegment.NULL;

    /// Pushed context, or `NULL`.
    private MemorySegment context = MemorySegment.NULL;

    /// Whether a text store was pushed.
    private boolean documentAttached;

    /// Whether closed.
    private boolean closed;

    /// Creates one session record.
    private WindowsTsfSession(Arena arena, MemorySegment threadManager, int createResult) {
        this.arena = arena;
        this.threadManager = threadManager;
        this.createResult = createResult;
    }

    /// Attempts to create the in-process TSF thread manager.
    ///
    /// @param libraries the session libraries
    /// @return the session
    public static WindowsTsfSession open(WindowsLibraries libraries) {
        Objects.requireNonNull(libraries, "libraries");
        Arena arena = Arena.ofConfined();
        try {
            MemorySegment resultCell = arena.allocate(ValueLayout.ADDRESS);
            resultCell.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            int created = libraries.bindings().coCreateInstance(
                    WindowsCom.guid(arena, CLSID_TF_THREAD_MGR),
                    MemorySegment.NULL,
                    CLSCTX_INPROC_SERVER,
                    WindowsCom.guid(arena, IID_ITF_THREAD_MGR),
                    resultCell
            );
            return new WindowsTsfSession(arena, resultCell.get(ValueLayout.ADDRESS, 0L), created);
        } catch (RuntimeException | Error failure) {
            arena.close();
            throw failure;
        }
    }

    /// Returns whether `ITfThreadMgr` was created.
    ///
    /// @return whether the COM object is non-null and `HRESULT` succeeded
    public boolean available() {
        return createResult >= 0 && threadManager.address() != 0L;
    }

    /// Returns the `CoCreateInstance` result.
    ///
    /// @return the HRESULT
    public int createResult() {
        return createResult;
    }

    /// Invokes `ITfThreadMgr::Activate` when the thread manager is available.
    ///
    /// @return whether activation succeeded
    public boolean activate() {
        requireOpen();
        if (!available()) {
            return false;
        }
        MemorySegment vtable = threadManager.reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0L);
        MemorySegment activate = vtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 4L)
                .getAtIndex(ValueLayout.ADDRESS, 3L);
        MemorySegment clientCell = arena.allocate(ValueLayout.JAVA_INT);
        clientCell.set(ValueLayout.JAVA_INT, 0L, 0);
        activateResult = Win32FfmBindings.invokeItfThreadMgrActivatePointer(
                activate,
                threadManager,
                clientCell
        );
        if (activateResult >= 0) {
            clientId = clientCell.get(ValueLayout.JAVA_INT, 0L);
            activated = true;
            return true;
        }
        return false;
    }

    /// Returns whether `ITfThreadMgr::Activate` succeeded.
    ///
    /// @return whether the thread is activated
    public boolean activated() {
        return activated;
    }

    /// Returns the TSF client identifier assigned by `Activate`.
    ///
    /// @return the identifier, or `0` when inactive
    public int clientId() {
        return clientId;
    }

    /// Returns the `Activate` HRESULT, or `Integer.MIN_VALUE` when it was not invoked.
    ///
    /// @return the HRESULT
    public int activateResult() {
        return activateResult;
    }

    /// Creates a document manager, attaches `store`, and pushes the context.
    ///
    /// @param store the `ITextStoreACP` implementation
    /// @return whether the document was pushed
    public boolean attach(WindowsTextStore store) {
        requireOpen();
        Objects.requireNonNull(store, "store");
        if (!activated && !activate()) {
            return false;
        }
        MemorySegment vtable = threadManager.reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0L);
        MemorySegment createDocument = vtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 6L)
                .getAtIndex(ValueLayout.ADDRESS, 5L);
        MemorySegment documentCell = arena.allocate(ValueLayout.ADDRESS);
        documentCell.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        int created = Win32FfmBindings.invokeItfThreadMgrCreateDocumentMgrPointer(
                createDocument,
                threadManager,
                documentCell
        );
        if (created < 0 || documentCell.get(ValueLayout.ADDRESS, 0L).address() == 0L) {
            return false;
        }
        documentMgr = documentCell.get(ValueLayout.ADDRESS, 0L);
        MemorySegment documentVtable = documentMgr.reinterpret(ValueLayout.ADDRESS.byteSize())
                .get(ValueLayout.ADDRESS, 0L);
        MemorySegment createContext = documentVtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 4L)
                .getAtIndex(ValueLayout.ADDRESS, 3L);
        MemorySegment contextCell = arena.allocate(ValueLayout.ADDRESS);
        contextCell.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        MemorySegment cookie = arena.allocate(ValueLayout.JAVA_INT);
        cookie.set(ValueLayout.JAVA_INT, 0L, 0);
        int contextCreated = Win32FfmBindings.invokeItfDocumentMgrCreateContextPointer(
                createContext,
                documentMgr,
                clientId,
                0,
                store.nativeObject(),
                contextCell,
                cookie
        );
        if (contextCreated < 0 || contextCell.get(ValueLayout.ADDRESS, 0L).address() == 0L) {
            return false;
        }
        context = contextCell.get(ValueLayout.ADDRESS, 0L);
        MemorySegment push = documentVtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 5L)
                .getAtIndex(ValueLayout.ADDRESS, 4L);
        int pushed = Win32FfmBindings.invokeItfDocumentMgrPushPointer(push, documentMgr, context);
        if (pushed < 0) {
            return false;
        }
        MemorySegment setFocus = vtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 9L)
                .getAtIndex(ValueLayout.ADDRESS, 8L);
        Win32FfmBindings.invokeItfThreadMgrSetFocusPointer(setFocus, threadManager, documentMgr);
        documentAttached = true;
        return true;
    }

    /// Returns whether a document context was pushed.
    ///
    /// @return whether the store is attached
    public boolean documentAttached() {
        return documentAttached;
    }

    /// Releases the thread manager if one was created.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (threadManager.address() != 0L) {
            MemorySegment vtable = threadManager.reinterpret(ValueLayout.ADDRESS.byteSize())
                    .get(ValueLayout.ADDRESS, 0L);
            if (documentMgr.address() != 0L) {
                MemorySegment documentVtable = documentMgr.reinterpret(ValueLayout.ADDRESS.byteSize())
                        .get(ValueLayout.ADDRESS, 0L);
                if (documentAttached) {
                    MemorySegment pop = documentVtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 6L)
                            .getAtIndex(ValueLayout.ADDRESS, 5L);
                    Win32FfmBindings.invokeItfDocumentMgrPopPointer(pop, documentMgr, 1);
                    documentAttached = false;
                }
                MemorySegment releaseDocument = documentVtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 3L)
                        .getAtIndex(ValueLayout.ADDRESS, 2L);
                Win32FfmBindings.invokeIunknownReleasePointer(releaseDocument, documentMgr);
                if (context.address() != 0L) {
                    MemorySegment contextVtable = context.reinterpret(ValueLayout.ADDRESS.byteSize())
                            .get(ValueLayout.ADDRESS, 0L);
                    MemorySegment releaseContext = contextVtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 3L)
                            .getAtIndex(ValueLayout.ADDRESS, 2L);
                    Win32FfmBindings.invokeIunknownReleasePointer(releaseContext, context);
                }
            }
            if (activated) {
                MemorySegment deactivate = vtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 5L)
                        .getAtIndex(ValueLayout.ADDRESS, 4L);
                Win32FfmBindings.invokeItfThreadMgrDeactivatePointer(deactivate, threadManager);
                activated = false;
            }
            MemorySegment release = vtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 3L)
                    .getAtIndex(ValueLayout.ADDRESS, 2L);
            Win32FfmBindings.invokeIunknownReleasePointer(release, threadManager);
        }
        arena.close();
    }

    /// Verifies the session is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Windows TSF session is closed");
        }
    }
}
