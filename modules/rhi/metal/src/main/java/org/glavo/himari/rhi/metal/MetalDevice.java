package org.glavo.himari.rhi.metal;

import org.glavo.himari.rhi.metal.generated.MetalFfmBindings;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

/// Owns one system-default Metal device and a command-queue present path.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class MetalDevice implements AutoCloseable {
    /// Shared libraries.
    private final MetalLibraries libraries;

    /// Selector arena.
    private final Arena arena;

    /// Native device.
    private final MemorySegment device;

    /// Command queue.
    private final MemorySegment commandQueue;

    /// Queried snapshot.
    private final MetalCapabilities capabilities;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates one device owner.
    private MetalDevice(
            MetalLibraries libraries,
            Arena arena,
            MemorySegment device,
            MemorySegment commandQueue,
            MetalCapabilities capabilities
    ) {
        this.libraries = libraries;
        this.arena = arena;
        this.device = device;
        this.commandQueue = commandQueue;
        this.capabilities = capabilities;
    }

    /// Creates the system-default Metal device, command queue, and a committed command buffer.
    ///
    /// A drawable is not presented until [`#presentDrawable(MemorySegment)`] or
    /// [`#presentLayer(MemorySegment)`] is called with a live Metal object.
    ///
    /// @return the device
    public static MetalDevice open() {
        MetalLibraries libraries = MetalLibraries.open();
        Arena arena = Arena.ofConfined();
        try {
            MetalFfmBindings bindings = libraries.bindings();
            MemorySegment device = bindings.mtlCreateSystemDefaultDevice();
            if (device.address() == 0L) {
                throw new IllegalStateException("MTLCreateSystemDefaultDevice returned NULL");
            }
            MemorySegment newCommandQueue = bindings.selRegisterName(arena.allocateFrom("newCommandQueue"));
            MemorySegment commandQueue = bindings.objcMsgSendId(device, newCommandQueue);
            if (commandQueue.address() == 0L) {
                bindings.cfRelease(device);
                throw new IllegalStateException("[MTLDevice newCommandQueue] returned nil");
            }
            MemorySegment commandBufferSel = bindings.selRegisterName(arena.allocateFrom("commandBuffer"));
            MemorySegment commandBuffer = bindings.objcMsgSendId(commandQueue, commandBufferSel);
            if (commandBuffer.address() == 0L) {
                bindings.cfRelease(commandQueue);
                bindings.cfRelease(device);
                throw new IllegalStateException("[MTLCommandQueue commandBuffer] returned nil");
            }
            bindings.objcMsgSendVoid(commandBuffer, bindings.selRegisterName(arena.allocateFrom("commit")));
            return new MetalDevice(
                    libraries,
                    arena,
                    device,
                    commandQueue,
                    new MetalCapabilities(true, true, true, false, "color-managed-sdr")
            );
        } catch (RuntimeException | Error failure) {
            arena.close();
            libraries.close();
            throw failure;
        }
    }

    /// Returns the native device pointer.
    ///
    /// @return the pointer
    public MemorySegment nativeHandle() {
        requireOpen();
        return device;
    }

    /// Returns the native command queue.
    ///
    /// @return the queue
    public MemorySegment commandQueue() {
        requireOpen();
        return commandQueue;
    }

    /// Returns the queried SDR snapshot.
    ///
    /// @return the snapshot
    public MetalCapabilities capabilities() {
        requireOpen();
        return capabilities;
    }

    /// Presents one Metal drawable with a newly created command buffer and commits it.
    ///
    /// This path does not enable EDR or write HDR metadata.
    ///
    /// @param drawable the `MTLDrawable`
    public void presentDrawable(MemorySegment drawable) {
        requireOpen();
        Objects.requireNonNull(drawable, "drawable");
        if (drawable.address() == 0L) {
            throw new IllegalArgumentException("drawable must not be NULL");
        }
        MetalFfmBindings bindings = libraries.bindings();
        MemorySegment commandBuffer = bindings.objcMsgSendId(
                commandQueue,
                bindings.selRegisterName(arena.allocateFrom("commandBuffer"))
        );
        if (commandBuffer.address() == 0L) {
            throw new IllegalStateException("[MTLCommandQueue commandBuffer] returned nil");
        }
        bindings.objcMsgSendVoidObject(
                commandBuffer,
                bindings.selRegisterName(arena.allocateFrom("presentDrawable:")),
                drawable
        );
        bindings.objcMsgSendVoid(commandBuffer, bindings.selRegisterName(arena.allocateFrom("commit")));
    }

    /// Acquires `nextDrawable` from a `CAMetalLayer` and presents it when a drawable is available.
    ///
    /// @param layer the `CAMetalLayer`
    /// @return whether a drawable was presented
    public boolean presentLayer(MemorySegment layer) {
        requireOpen();
        Objects.requireNonNull(layer, "layer");
        if (layer.address() == 0L) {
            throw new IllegalArgumentException("layer must not be NULL");
        }
        MetalFfmBindings bindings = libraries.bindings();
        MemorySegment drawable = bindings.objcMsgSendId(
                layer,
                bindings.selRegisterName(arena.allocateFrom("nextDrawable"))
        );
        if (drawable.address() == 0L) {
            return false;
        }
        presentDrawable(drawable);
        return true;
    }

    /// Releases the queue, device, and framework lookups.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException firstFailure = null;
        try {
            libraries.bindings().cfRelease(commandQueue);
        } catch (RuntimeException failure) {
            firstFailure = failure;
        }
        try {
            libraries.bindings().cfRelease(device);
        } catch (RuntimeException failure) {
            if (firstFailure == null) {
                firstFailure = failure;
            } else {
                firstFailure.addSuppressed(failure);
            }
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

    /// Verifies the device is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Metal device is closed");
        }
    }
}
