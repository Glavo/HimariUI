package org.glavo.himari.rhi.metal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.MemorySegment;

/// Owns one system-default Metal device created through generated FFM bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class MetalDevice implements AutoCloseable {
    /// Shared libraries.
    private final MetalLibraries libraries;

    /// Native device.
    private final MemorySegment device;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates one device owner.
    ///
    /// @param libraries the libraries
    /// @param device the native device
    private MetalDevice(MetalLibraries libraries, MemorySegment device) {
        this.libraries = libraries;
        this.device = device;
    }

    /// Creates the system-default Metal device.
    ///
    /// @return the device
    public static MetalDevice open() {
        MetalLibraries libraries = MetalLibraries.open();
        try {
            MemorySegment device = libraries.bindings().mtlCreateSystemDefaultDevice();
            if (device.address() == 0L) {
                throw new IllegalStateException("MTLCreateSystemDefaultDevice returned NULL");
            }
            return new MetalDevice(libraries, device);
        } catch (RuntimeException | Error failure) {
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

    /// Releases the device and closes the framework lookups.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException firstFailure = null;
        try {
            libraries.bindings().cfRelease(device);
        } catch (RuntimeException failure) {
            firstFailure = failure;
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
