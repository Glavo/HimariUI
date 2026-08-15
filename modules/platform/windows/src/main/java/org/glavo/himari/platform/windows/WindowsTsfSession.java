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
            MemorySegment release = vtable.reinterpret(ValueLayout.ADDRESS.byteSize() * 3L)
                    .getAtIndex(ValueLayout.ADDRESS, 2L);
            Win32FfmBindings.invokeIunknownReleasePointer(release, threadManager);
        }
        arena.close();
    }
}
