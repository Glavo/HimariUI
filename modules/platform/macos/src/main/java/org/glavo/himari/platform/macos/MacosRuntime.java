package org.glavo.himari.platform.macos;

import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/// Resolves `NSObject` and the `alloc` selector through generated Objective-C bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class MacosRuntime implements AutoCloseable {
    /// Shared libraries.
    private final MacosLibraries libraries;

    /// Temporary string storage.
    private final Arena arena;

    /// `NSObject` class pointer.
    private final MemorySegment nsObject;

    /// `alloc` selector.
    private final MemorySegment alloc;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates one runtime owner.
    ///
    /// @param libraries the libraries
    /// @param arena the arena
    /// @param nsObject the class
    /// @param alloc the selector
    private MacosRuntime(
            MacosLibraries libraries,
            Arena arena,
            MemorySegment nsObject,
            MemorySegment alloc
    ) {
        this.libraries = libraries;
        this.arena = arena;
        this.nsObject = nsObject;
        this.alloc = alloc;
    }

    /// Opens the Objective-C runtime and resolves `NSObject`/`alloc`.
    ///
    /// @return the runtime
    public static MacosRuntime open() {
        MacosLibraries libraries = MacosLibraries.open();
        Arena arena = Arena.ofConfined();
        try {
            MemorySegment nsObject = libraries.bindings().objcGetClass(arena.allocateFrom("NSObject"));
            if (nsObject.address() == 0L) {
                throw new IllegalStateException("objc_getClass(NSObject) returned NULL");
            }
            MemorySegment alloc = libraries.bindings().selRegisterName(arena.allocateFrom("alloc"));
            if (alloc.address() == 0L) {
                throw new IllegalStateException("sel_registerName(alloc) returned NULL");
            }
            return new MacosRuntime(libraries, arena, nsObject, alloc);
        } catch (RuntimeException | Error failure) {
            arena.close();
            libraries.close();
            throw failure;
        }
    }

    /// Returns the `NSObject` class pointer.
    ///
    /// @return the class
    public MemorySegment nsObjectClass() {
        requireOpen();
        return nsObject;
    }

    /// Returns the `alloc` selector.
    ///
    /// @return the selector
    public MemorySegment allocSelector() {
        requireOpen();
        return alloc;
    }

    /// Closes temporary storage and library lookups.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        arena.close();
        libraries.close();
    }

    /// Verifies the runtime is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("macOS runtime is closed");
        }
    }
}
