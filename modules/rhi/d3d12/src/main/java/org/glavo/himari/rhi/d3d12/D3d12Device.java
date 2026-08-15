package org.glavo.himari.rhi.d3d12;

import org.glavo.himari.rhi.d3d12.generated.D3d12FfmBindings;
import org.glavo.himari.rhi.d3d12.generated.D3d12Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Owns one production D3D12 factory and device created through generated FFM bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class D3d12Device implements AutoCloseable {
    /// `D3D_FEATURE_LEVEL_11_0`.
    private static final int FEATURE_LEVEL_11_0 = 0xB000;

    /// `DXGI_FORMAT_R8G8B8A8_UNORM`.
    private static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;

    /// `D3D12_FEATURE_FORMAT_SUPPORT`.
    private static final int D3D12_FEATURE_FORMAT_SUPPORT = 3;

    /// `D3D12_FORMAT_SUPPORT1_RENDER_TARGET`.
    private static final int D3D12_FORMAT_SUPPORT1_RENDER_TARGET = 0x4000;

    /// Shared system libraries.
    private final D3d12Libraries libraries;

    /// Confined storage for descriptors and GUID cells.
    private final Arena arena;

    /// COM ownership for the factory and device.
    private final D3d12Native.ComTracker deviceReferences = new D3d12Native.ComTracker();

    /// `IDXGIFactory4`.
    private final MemorySegment factory;

    /// `ID3D12Device`.
    private final MemorySegment device;

    /// Queried format-support snapshot.
    private final D3d12Capabilities capabilities;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates one device owner.
    ///
    /// @param libraries the libraries
    /// @param arena the confined arena
    /// @param factory the owned factory
    /// @param device the owned device
    /// @param capabilities the queried snapshot
    private D3d12Device(
            D3d12Libraries libraries,
            Arena arena,
            MemorySegment factory,
            MemorySegment device,
            D3d12Capabilities capabilities
    ) {
        this.libraries = libraries;
        this.arena = arena;
        this.factory = factory;
        this.device = device;
        this.capabilities = capabilities;
    }

    /// Creates a default-adapter device at feature level 11.0 or newer.
    ///
    /// @return the device owner
    /// @throws RuntimeException if DXGI or D3D12 cannot be created
    public static D3d12Device open() {
        D3d12Libraries libraries = D3d12Libraries.open();
        Arena arena = Arena.ofConfined();
        try {
            D3d12FfmBindings bindings = libraries.bindings();
            MemorySegment factoryCell = D3d12Native.pointerCell(arena);
            D3d12Native.requireSuccess(
                    "CreateDXGIFactory2",
                    bindings.createDxgiFactory2(
                            0,
                            D3d12Native.guid(arena, "1bc6ea02-ef36-464f-bf0c-21ca39e5168a"),
                            factoryCell
                    )
            );
            MemorySegment factory = D3d12Native.requirePointer(factoryCell, "CreateDXGIFactory2");
            MemorySegment deviceCell = D3d12Native.pointerCell(arena);
            D3d12Native.requireSuccess(
                    "D3D12CreateDevice",
                    bindings.d3d12CreateDevice(
                            MemorySegment.NULL,
                            FEATURE_LEVEL_11_0,
                            D3d12Native.guid(arena, "189819f1-1db6-4b57-be54-1821339b85f7"),
                            deviceCell
                    )
            );
            MemorySegment device = D3d12Native.requirePointer(deviceCell, "D3D12CreateDevice");
            try {
                D3d12Device opened = new D3d12Device(
                        libraries,
                        arena,
                        factory,
                        device,
                        queryR8G8B8A8(arena, device)
                );
                opened.deviceReferences.own(factory);
                opened.deviceReferences.own(device);
                return opened;
            } catch (RuntimeException | Error failure) {
                releaseQuietly(factory);
                releaseQuietly(device);
                throw failure;
            }
        } catch (RuntimeException | Error failure) {
            arena.close();
            libraries.close();
            throw failure;
        }
    }

    /// Opens a device when D3D12 is available.
    ///
    /// @return the device, or `null` when the adapter or runtime is missing
    public static @Nullable D3d12Device tryOpen() {
        try {
            return open();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /// Returns the queried SDR capability snapshot.
    ///
    /// @return the snapshot
    public D3d12Capabilities capabilities() {
        requireOpen();
        return capabilities;
    }

    /// Returns the owned factory pointer.
    ///
    /// @return the factory
    MemorySegment factory() {
        requireOpen();
        return factory;
    }

    /// Returns the owned device pointer.
    ///
    /// @return the device
    MemorySegment device() {
        requireOpen();
        return device;
    }

    /// Returns the confined arena used by this device.
    ///
    /// @return the arena
    Arena arena() {
        requireOpen();
        return arena;
    }

    /// Creates an upload-heap committed buffer and a shader-visible CBV/SRV/UAV heap.
    ///
    /// @param payload the bytes written through `ID3D12Resource::Map`
    /// @return the resource owner
    public D3d12GpuResource createUploadResource(MemorySegment payload) {
        requireOpen();
        return D3d12GpuResource.createUpload(this, payload);
    }

    /// Copies `payload` through a default-heap buffer and returns the readback bytes.
    ///
    /// @param payload the bytes to copy
    /// @return the GPU-round-tripped bytes
    public MemorySegment copyThroughDefaultHeap(MemorySegment payload) {
        requireOpen();
        return D3d12GpuCopy.copyThroughDefaultHeap(this, payload);
    }

    /// Uploads row-major RGBA into a default-heap texture and copies it back to the CPU.
    ///
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the read-back observation
    public D3d12TextureRoundTrip roundTripSdrRgba(MemorySegment rgba, int width, int height) {
        requireOpen();
        return D3d12GpuTexture.roundTripSdrRgba(this, rgba, width, height);
    }

    /// Clears an offscreen R8G8B8A8 render target and copies the result back to the CPU.
    ///
    /// @param red the red channel in `[0, 1]`
    /// @param green the green channel in `[0, 1]`
    /// @param blue the blue channel in `[0, 1]`
    /// @param alpha the alpha channel in `[0, 1]`
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the read-back observation
    public D3d12TextureRoundTrip clearSdrAndReadback(
            float red,
            float green,
            float blue,
            float alpha,
            int width,
            int height
    ) {
        requireOpen();
        return D3d12GpuTexture.clearSdrAndReadback(this, red, green, blue, alpha, width, height);
    }

    /// Uploads row-major RGBA and presents it through a flip-model SDR swapchain.
    ///
    /// @param hwnd the native window handle
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the present observation
    public D3d12Presentation presentSdrRgba(MemorySegment hwnd, MemorySegment rgba, int width, int height) {
        requireOpen();
        return D3d12GpuTexture.presentSdrRgba(this, hwnd, rgba, width, height);
    }

    /// Creates a flip-model SDR swapchain for `hwnd`, presents once, and releases the present objects.
    ///
    /// The HWND must remain valid for the duration of this call. Hardware HDR metadata is never applied.
    ///
    /// @param hwnd the native window handle
    /// @param width the positive swapchain width in pixels
    /// @param height the positive swapchain height in pixels
    /// @return the present observation
    public D3d12Presentation presentSdr(MemorySegment hwnd, int width, int height) {
        requireOpen();
        Objects.requireNonNull(hwnd, "hwnd");
        if (hwnd.address() == 0L) {
            throw new IllegalArgumentException("HWND must not be NULL");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Swapchain dimensions must be positive");
        }
        try (D3d12SwapChain swapChain = D3d12SwapChain.attach(this, hwnd, width, height)) {
            return swapChain.clearAndPresent(17.0f / 255.0f, 83.0f / 255.0f, 149.0f / 255.0f, 1.0f);
        }
    }

    /// Releases the factory, device, and library lookups.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException firstFailure = null;
        try {
            deviceReferences.close();
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

    /// Queries `DXGI_FORMAT_R8G8B8A8_UNORM` render-target support.
    ///
    /// @param arena the arena
    /// @param device the device
    /// @return the snapshot
    private static D3d12Capabilities queryR8G8B8A8(
            Arena arena,
            MemorySegment device
    ) {
        MemorySegment data = arena.allocate(D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT);
        data.fill((byte) 0);
        data.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT_FORMAT_OFFSET,
                DXGI_FORMAT_R8G8B8A8_UNORM);
        D3d12Native.requireSuccess(
                "ID3D12Device::CheckFeatureSupport(R8G8B8A8)",
                D3d12FfmBindings.invokeId3d12DeviceCheckFeatureSupportPointer(
                        D3d12Native.functionAt(
                                device,
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CHECK_FEATURE_SUPPORT_OFFSET
                        ),
                        device,
                        D3D12_FEATURE_FORMAT_SUPPORT,
                        data,
                        Math.toIntExact(D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT.byteSize())
                )
        );
        int support1 = data.get(
                ValueLayout.JAVA_INT,
                D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT_SUPPORT1_OFFSET
        );
        return new D3d12Capabilities(
                (support1 & D3D12_FORMAT_SUPPORT1_RENDER_TARGET) != 0,
                Integer.toUnsignedLong(support1),
                false,
                "color-managed-sdr"
        );
    }

    /// Releases one COM reference, ignoring a later failure after the first.
    ///
    /// @param reference the interface
    private static void releaseQuietly(MemorySegment reference) {
        try {
            D3d12FfmBindings.invokeIunknownReleasePointer(
                    D3d12Native.functionAt(reference, 16L),
                    reference
            );
        } catch (RuntimeException ignored) {
            // Preserve the original open() failure.
        }
    }

    /// Verifies that this owner is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("D3D12 device is closed");
        }
    }
}
