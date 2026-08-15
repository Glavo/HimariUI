package org.glavo.himari.platform.macos;

import org.glavo.himari.platform.macos.generated.ObjcFfmBindings;
import org.glavo.himari.platform.macos.generated.ObjcLayouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Creates one `NSWindow` and attaches a `CAMetalLayer` through generated `objc_msgSend` variants.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class MacosWindow implements AutoCloseable {
    /// `NSWindowStyleMaskTitled | NSWindowStyleMaskClosable`.
    private static final long NS_WINDOW_STYLE_TITLED_CLOSABLE = 1L | 2L;

    /// `NSBackingStoreBuffered`.
    private static final long NS_BACKING_STORE_BUFFERED = 2L;

    /// Shared libraries.
    private final MacosLibraries libraries;

    /// Temporary selector and rect storage.
    private final Arena arena;

    /// Native window.
    private final MemorySegment window;

    /// Attached Metal layer.
    private final MemorySegment metalLayer;

    /// Whether closed.
    private boolean closed;

    /// Creates one window owner.
    private MacosWindow(
            MacosLibraries libraries,
            Arena arena,
            MemorySegment window,
            MemorySegment metalLayer
    ) {
        this.libraries = libraries;
        this.arena = arena;
        this.window = window;
        this.metalLayer = metalLayer;
    }

    /// Creates an `NSWindow` with a `CAMetalLayer` content layer.
    ///
    /// @return the window
    public static MacosWindow open() {
        MacosLibraries libraries = MacosLibraries.open();
        Arena arena = Arena.ofConfined();
        try {
            ObjcFfmBindings bindings = libraries.bindings();
            requireClass(bindings, arena, "NSApplication");
            requireClass(bindings, arena, "NSWindow");
            requireClass(bindings, arena, "NSString");
            requireClass(bindings, arena, "CAMetalLayer");
            MemorySegment applicationClass = bindings.objcGetClass(arena.allocateFrom("NSApplication"));
            MemorySegment sharedApplication = bindings.selRegisterName(arena.allocateFrom("sharedApplication"));
            MemorySegment application = bindings.objcMsgSendId(applicationClass, sharedApplication);
            if (application.address() == 0L) {
                throw new IllegalStateException("[NSApplication sharedApplication] returned nil");
            }
            MemorySegment windowClass = bindings.objcGetClass(arena.allocateFrom("NSWindow"));
            MemorySegment alloc = bindings.selRegisterName(arena.allocateFrom("alloc"));
            MemorySegment allocated = bindings.objcMsgSendId(windowClass, alloc);
            if (allocated.address() == 0L) {
                throw new IllegalStateException("[NSWindow alloc] returned nil");
            }
            MemorySegment init = bindings.selRegisterName(
                    arena.allocateFrom("initWithContentRect:styleMask:backing:defer:")
            );
            MemorySegment rect = arena.allocate(ObjcLayouts.NS_RECT);
            rect.set(ValueLayout.JAVA_DOUBLE, ObjcLayouts.NS_RECT_X_OFFSET, 80.0);
            rect.set(ValueLayout.JAVA_DOUBLE, ObjcLayouts.NS_RECT_Y_OFFSET, 80.0);
            rect.set(ValueLayout.JAVA_DOUBLE, ObjcLayouts.NS_RECT_WIDTH_OFFSET, 320.0);
            rect.set(ValueLayout.JAVA_DOUBLE, ObjcLayouts.NS_RECT_HEIGHT_OFFSET, 240.0);
            MemorySegment window = bindings.objcMsgSendInitWindow(
                    allocated,
                    init,
                    rect,
                    NS_WINDOW_STYLE_TITLED_CLOSABLE,
                    NS_BACKING_STORE_BUFFERED,
                    (byte) 0
            );
            if (window.address() == 0L) {
                throw new IllegalStateException("NSWindow initWithContentRect returned nil");
            }
            MemorySegment stringClass = bindings.objcGetClass(arena.allocateFrom("NSString"));
            MemorySegment stringWithUtf8 = bindings.selRegisterName(arena.allocateFrom("stringWithUTF8String:"));
            MemorySegment title = bindings.objcMsgSendIdCString(
                    stringClass,
                    stringWithUtf8,
                    arena.allocateFrom("HimariUI")
            );
            bindings.objcMsgSendVoidObject(window, bindings.selRegisterName(arena.allocateFrom("setTitle:")), title);
            MemorySegment contentView = bindings.objcMsgSendId(
                    window,
                    bindings.selRegisterName(arena.allocateFrom("contentView"))
            );
            if (contentView.address() == 0L) {
                throw new IllegalStateException("NSWindow contentView returned nil");
            }
            bindings.objcMsgSendVoidBool(
                    contentView,
                    bindings.selRegisterName(arena.allocateFrom("setWantsLayer:")),
                    (byte) 1
            );
            MemorySegment layerClass = bindings.objcGetClass(arena.allocateFrom("CAMetalLayer"));
            MemorySegment metalLayer = bindings.objcMsgSendId(
                    layerClass,
                    bindings.selRegisterName(arena.allocateFrom("layer"))
            );
            if (metalLayer.address() == 0L) {
                throw new IllegalStateException("[CAMetalLayer layer] returned nil");
            }
            bindings.objcMsgSendVoidObject(
                    contentView,
                    bindings.selRegisterName(arena.allocateFrom("setLayer:")),
                    metalLayer
            );
            bindings.objcMsgSendVoidObject(
                    window,
                    bindings.selRegisterName(arena.allocateFrom("makeKeyAndOrderFront:")),
                    MemorySegment.NULL
            );
            Objects.requireNonNull(application, "application");
            return new MacosWindow(libraries, arena, window, metalLayer);
        } catch (RuntimeException | Error failure) {
            arena.close();
            libraries.close();
            throw failure;
        }
    }

    /// Returns the native `NSWindow`.
    ///
    /// @return the window
    public MemorySegment nativeHandle() {
        requireOpen();
        return window;
    }

    /// Returns the attached `CAMetalLayer`.
    ///
    /// @return the layer
    public MemorySegment metalLayer() {
        requireOpen();
        return metalLayer;
    }

    /// Closes the window and releases the runtime lookup.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException firstFailure = null;
        try {
            libraries.bindings().objcMsgSendVoid(window, libraries.bindings().selRegisterName(arena.allocateFrom("close")));
        } catch (RuntimeException failure) {
            firstFailure = failure;
        }
        try {
            arena.close();
        } catch (RuntimeException failure) {
            if (firstFailure == null) {
                firstFailure = failure;
            } else {
                firstFailure.addSuppressed(failure);
            }
        }
        try {
            libraries.close();
        } catch (RuntimeException failure) {
            if (firstFailure == null) {
                firstFailure = failure;
            } else {
                firstFailure.addSuppressed(failure);
            }
        }
        if (firstFailure != null) {
            throw firstFailure;
        }
    }

    /// Resolves one Objective-C class or fails.
    private static void requireClass(ObjcFfmBindings bindings, Arena arena, String name) {
        MemorySegment type = bindings.objcGetClass(arena.allocateFrom(name));
        if (type.address() == 0L) {
            throw new IllegalStateException("objc_getClass(" + name + ") returned NULL");
        }
    }

    /// Verifies the window is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("macOS window is closed");
        }
    }
}
