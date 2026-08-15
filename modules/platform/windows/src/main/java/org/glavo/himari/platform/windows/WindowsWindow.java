package org.glavo.himari.platform.windows;

import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.platform.api.PlatformWindow;
import org.glavo.himari.platform.api.SurfaceDescriptor;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowId;
import org.glavo.himari.platform.api.WindowSnapshot;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/// Implements [PlatformWindow] for one HWND.
@NotNullByDefault
public final class WindowsWindow implements PlatformWindow {
    /// The owning session.
    private final WindowsPlatform platform;

    /// The native window.
    private final WindowsNativeWindow nativeWindow;

    /// The stable identifier.
    private final WindowId id;

    /// The surface descriptor.
    private final SurfaceDescriptor surface;

    /// Pointer events drained from WndProc and not yet consumed by the application.
    private final ArrayList<PointerEvent> pendingPointers = new ArrayList<>();

    /// Key events drained from WndProc and not yet consumed by the application.
    private final ArrayList<KeyEvent> pendingKeys = new ArrayList<>();

    /// IME session driven by `WM_CHAR` and by explicit test injection.
    private final WindowsImeSession ime = new WindowsImeSession();

    /// The latest snapshot.
    private volatile WindowSnapshot snapshot;

    /// Close completion.
    private final CompletableFuture<Void> closeCompletion = new CompletableFuture<>();

    /// Creates one window.
    ///
    /// @param platform the session
    /// @param nativeWindow the HWND owner
    /// @param snapshot the initial snapshot
    WindowsWindow(WindowsPlatform platform, WindowsNativeWindow nativeWindow, WindowSnapshot snapshot) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.nativeWindow = Objects.requireNonNull(nativeWindow, "nativeWindow");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.id = snapshot.id();
        this.surface = snapshot.surface();
    }

    /// {@inheritDoc}
    @Override
    public WindowId id() {
        return id;
    }

    /// {@inheritDoc}
    @Override
    public SurfaceDescriptor surface() {
        return surface;
    }

    /// {@inheritDoc}
    @Override
    public WindowSnapshot snapshot() {
        return snapshot;
    }

    /// Returns the native HWND for RHI attachment.
    ///
    /// @return the handle
    public MemorySegment nativeHandle() {
        return nativeWindow.handle();
    }

    /// Returns the window DPI last reported by `GetDpiForWindow`.
    ///
    /// @return the DPI
    public int dpi() {
        return nativeWindow.dpi();
    }

    /// Returns the IME session attached to this window.
    ///
    /// @return the session
    public WindowsImeSession ime() {
        return ime;
    }

    /// Returns pointer events delivered through WndProc since the last drain.
    ///
    /// @return the events
    public @Unmodifiable List<PointerEvent> takePointerEvents() {
        List<PointerEvent> copy = List.copyOf(pendingPointers);
        pendingPointers.clear();
        return copy;
    }

    /// Returns key events delivered through WndProc since the last drain.
    ///
    /// @return the events
    public @Unmodifiable List<KeyEvent> takeKeyEvents() {
        List<KeyEvent> copy = List.copyOf(pendingKeys);
        pendingKeys.clear();
        return copy;
    }

    /// Writes Unicode text to the system clipboard through generated FFM bindings.
    ///
    /// @param text the text
    public void writeClipboard(String text) {
        WindowsClipboard.writeUnicode(platform.libraries(), nativeHandle(), text);
    }

    /// Reads Unicode text from the system clipboard.
    ///
    /// @return the text, or `null` when the format is absent
    public @Nullable String readClipboard() {
        return WindowsClipboard.readUnicode(platform.libraries(), nativeHandle());
    }

    /// Registers an OLE drop target on this HWND.
    ///
    /// @return the registered target
    public WindowsDropTarget registerDropTarget() {
        return WindowsDropTarget.register(platform.libraries(), nativeHandle());
    }

    /// Creates a Unicode `IDataObject` owned by this session.
    ///
    /// @param text the payload
    /// @return the data object
    public WindowsDataObject createUnicodeDataObject(String text) {
        return WindowsDataObject.unicode(platform.libraries(), text);
    }

    /// Pushes the IME candidate rectangle through IMM32.
    ///
    /// @return whether `ImmSetCompositionWindow` succeeded
    public boolean applyImeCandidate() {
        return WindowsImmSession.applyCandidateRectangle(platform.libraries(), nativeHandle(), ime);
    }

    /// Creates the in-process TSF thread manager.
    ///
    /// @return the session
    public WindowsTsfSession openTsf() {
        return WindowsTsfSession.open(platform.libraries());
    }

    /// Creates an `ITextStoreACP` bound to this window's IME session.
    ///
    /// @return the store
    public WindowsTextStore createTextStore() {
        return WindowsTextStore.of(platform.libraries(), ime, nativeHandle());
    }

    /// Creates an `IRawElementProviderSimple` for one semantics node.
    ///
    /// @param node the projected node
    /// @return the provider
    public WindowsAutomationProvider automationProvider(SemanticsNode node) {
        return WindowsAutomationProvider.of(platform.libraries(), node);
    }

    /// Returns the module handle used to create this HWND.
    ///
    /// @return the `HINSTANCE`
    public MemorySegment moduleHandle() {
        return nativeWindow.instance();
    }

    /// Returns whether a move/resize modal loop is active.
    ///
    /// @return whether the loop is active
    public boolean modalLoopActive() {
        return nativeWindow.modalLoopActive();
    }

    /// Returns modal-loop timer ticks delivered through WndProc.
    ///
    /// @return the tick count
    public int modalTimerTicks() {
        return nativeWindow.modalTimerTicks();
    }

    /// Blits unassociated 8-bit sRGB RGBA pixels into this HWND and retains them for `WM_PAINT`.
    ///
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the scanline count reported by `SetDIBitsToDevice`
    public int presentSdrRgba(MemorySegment rgba, int width, int height) {
        return nativeWindow.presentSdrRgba(rgba, width, height);
    }

    /// Posts a left-button pointer sequence through the production WndProc.
    ///
    /// @param type the pointer kind
    /// @param x the client x
    /// @param y the client y
    public void postPointer(org.glavo.himari.layout.input.PointerEventType type, int x, int y) {
        int message = switch (type) {
            case MOVE -> 0x0200;
            case DOWN -> 0x0201;
            case UP -> 0x0202;
        };
        nativeWindow.postMessage(message, 0L, WindowsNativeWindow.packPointer(x, y));
    }

    /// Posts a virtual-key event through the production WndProc.
    ///
    /// @param down whether this is `WM_KEYDOWN`
    /// @param virtualKey the virtual-key code
    public void postVirtualKey(boolean down, int virtualKey) {
        nativeWindow.postMessage(down ? 0x0100 : 0x0101, Integer.toUnsignedLong(virtualKey), 0L);
    }

    /// Posts a `WM_CHAR` through the production WndProc.
    ///
    /// @param codeUnit the UTF-16 code unit
    public void postChar(char codeUnit) {
        nativeWindow.postMessage(0x0102, codeUnit, 0L);
    }

    /// {@inheritDoc}
    @Override
    public CompletionStage<WindowSnapshot> configure(WindowConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        if (platform.eventLoop().isOwnerThread()) {
            return CompletableFuture.completedFuture(platform.configureWindow(this, configuration));
        }
        CompletableFuture<WindowSnapshot> future = new CompletableFuture<>();
        platform.eventLoop().post(() -> {
            try {
                future.complete(platform.configureWindow(this, configuration));
            } catch (RuntimeException | Error failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
    }

    /// {@inheritDoc}
    @Override
    public void requestRedraw() {
        platform.eventLoop().post(() -> platform.requestRedraw(this));
    }

    /// {@inheritDoc}
    @Override
    public CompletionStage<Void> closeAsync() {
        if (platform.eventLoop().isOwnerThread()) {
            platform.closeWindow(this);
            return closeCompletion;
        }
        platform.eventLoop().post(() -> platform.closeWindow(this));
        return closeCompletion;
    }

    /// {@inheritDoc}
    @Override
    public boolean isClosed() {
        return snapshot.lifecycle() == org.glavo.himari.platform.api.WindowLifecycle.CLOSED;
    }

    /// Returns the native owner.
    ///
    /// @return the native window
    WindowsNativeWindow nativeWindow() {
        return nativeWindow;
    }

    /// Drains WndProc-normalized input into the Java queues and IME session.
    void consumeNativeInput() {
        pendingPointers.addAll(nativeWindow.takePointerEvents());
        pendingKeys.addAll(nativeWindow.takeKeyEvents());
        String characters = nativeWindow.takeCharacters();
        if (!characters.isEmpty()) {
            ime.updateComposition(characters);
            ime.commit();
        }
    }

    /// Publishes a replacement snapshot.
    ///
    /// @param next the next snapshot
    void publish(WindowSnapshot next) {
        snapshot = next;
    }

    /// Completes close.
    void completeClose() {
        closeCompletion.complete(null);
    }

    /// Returns the close future.
    ///
    /// @return the future
    CompletableFuture<Void> closeFuture() {
        return closeCompletion;
    }
}
