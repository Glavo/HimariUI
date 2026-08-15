package org.glavo.himari.rhi.d3d12;

import org.glavo.himari.rhi.d3d12.generated.D3d12FfmBindings;
import org.glavo.himari.rhi.d3d12.generated.D3d12Layouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/// Uploads, clears, reads back, and presents 2D SDR textures through generated D3D12 bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class D3d12GpuTexture {
    /// `D3D12_HEAP_TYPE_DEFAULT`.
    private static final int D3D12_HEAP_TYPE_DEFAULT = 1;

    /// `D3D12_HEAP_TYPE_UPLOAD`.
    private static final int D3D12_HEAP_TYPE_UPLOAD = 2;

    /// `D3D12_HEAP_TYPE_READBACK`.
    private static final int D3D12_HEAP_TYPE_READBACK = 3;

    /// `D3D12_RESOURCE_DIMENSION_BUFFER`.
    private static final int D3D12_RESOURCE_DIMENSION_BUFFER = 1;

    /// `D3D12_RESOURCE_DIMENSION_TEXTURE2D`.
    private static final int D3D12_RESOURCE_DIMENSION_TEXTURE2D = 3;

    /// `DXGI_FORMAT_UNKNOWN`.
    private static final int DXGI_FORMAT_UNKNOWN = 0;

    /// `DXGI_FORMAT_R8G8B8A8_UNORM`.
    private static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;

    /// `D3D12_TEXTURE_LAYOUT_ROW_MAJOR`.
    private static final int D3D12_TEXTURE_LAYOUT_ROW_MAJOR = 1;

    /// `D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET`.
    private static final int D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET = 0x1;

    /// `D3D12_RESOURCE_STATE_GENERIC_READ`.
    private static final int D3D12_RESOURCE_STATE_GENERIC_READ = 0x00AC3;

    /// `D3D12_RESOURCE_STATE_COPY_DEST`.
    private static final int D3D12_RESOURCE_STATE_COPY_DEST = 0x400;

    /// `D3D12_RESOURCE_STATE_COPY_SOURCE`.
    private static final int D3D12_RESOURCE_STATE_COPY_SOURCE = 0x800;

    /// `D3D12_RESOURCE_STATE_RENDER_TARGET`.
    private static final int D3D12_RESOURCE_STATE_RENDER_TARGET = 0x4;

    /// `D3D12_COMMAND_LIST_TYPE_DIRECT`.
    private static final int D3D12_COMMAND_LIST_TYPE_DIRECT = 0;

    /// `D3D12_DESCRIPTOR_HEAP_TYPE_RTV`.
    private static final int D3D12_DESCRIPTOR_HEAP_TYPE_RTV = 2;

    /// `D3D12_RESOURCE_BARRIER_TYPE_TRANSITION`.
    private static final int D3D12_RESOURCE_BARRIER_TYPE_TRANSITION = 0;

    /// `D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES`.
    private static final int D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES = -1;

    /// `D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX`.
    private static final int D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX = 0;

    /// `D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT`.
    private static final int D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT = 1;

    /// `D3D12_TEXTURE_DATA_PITCH_ALIGNMENT`.
    private static final int D3D12_TEXTURE_DATA_PITCH_ALIGNMENT = 256;

    /// `ID3D12Resource`.
    private static final String ID3D12_RESOURCE = "696442be-a72e-4059-bc79-5b5c98040fad";

    /// `ID3D12DescriptorHeap`.
    private static final String ID3D12_DESCRIPTOR_HEAP = "8efb471d-616c-4f49-90f7-127bb763fa51";

    /// Fence wait budget.
    private static final long FENCE_TIMEOUT_NANOS = 30_000_000_000L;

    /// Prevents instantiation.
    private D3d12GpuTexture() {
    }

    /// Uploads row-major RGBA into a default-heap texture and copies it back to the CPU.
    ///
    /// @param device the production device
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the read-back observation
    public static D3d12TextureRoundTrip roundTripSdrRgba(
            D3d12Device device,
            byte[] rgba,
            int width,
            int height
    ) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(rgba, "rgba");
        requireExtent(width, height, rgba);
        int rowPitch = rowPitch(width);
        int packedSize = Math.multiplyExact(rowPitch, height);
        Arena arena = device.arena();
        D3d12Native.ComTracker tracker = new D3d12Native.ComTracker();
        try {
            MemorySegment upload = committedBuffer(device, arena, tracker, packedSize,
                    D3D12_HEAP_TYPE_UPLOAD, D3D12_RESOURCE_STATE_GENERIC_READ);
            mapWrite(upload, arena, pack(rgba, width, height, rowPitch));
            MemorySegment texture = committedTexture(device, arena, tracker, width, height, 0,
                    D3D12_RESOURCE_STATE_COPY_DEST);
            MemorySegment readback = committedBuffer(device, arena, tracker, packedSize,
                    D3D12_HEAP_TYPE_READBACK, D3D12_RESOURCE_STATE_COPY_DEST);
            MemorySegment queue = commandQueue(device, arena, tracker);
            MemorySegment allocator = commandAllocator(device, arena, tracker);
            MemorySegment list = commandList(device, arena, tracker, allocator);
            MemorySegment fence = fence(device, arena, tracker);
            reset(allocator, list);
            copyPlacedToTexture(list, arena, texture, upload, width, height, rowPitch);
            barrier(list, arena, texture, D3D12_RESOURCE_STATE_COPY_DEST, D3D12_RESOURCE_STATE_COPY_SOURCE);
            copyTextureToPlaced(list, arena, readback, texture, width, height, rowPitch);
            closeList(list);
            execute(queue, list, arena);
            signalAndWait(queue, fence, 1L);
            return new D3d12TextureRoundTrip(
                    width,
                    height,
                    unpack(mapRead(readback, arena, packedSize), width, height, rowPitch),
                    true
            );
        } finally {
            tracker.close();
        }
    }

    /// Clears an offscreen R8G8B8A8 render target and copies the result back to the CPU.
    ///
    /// @param device the production device
    /// @param red the red channel in `[0, 1]`
    /// @param green the green channel in `[0, 1]`
    /// @param blue the blue channel in `[0, 1]`
    /// @param alpha the alpha channel in `[0, 1]`
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the read-back observation
    public static D3d12TextureRoundTrip clearSdrAndReadback(
            D3d12Device device,
            float red,
            float green,
            float blue,
            float alpha,
            int width,
            int height
    ) {
        Objects.requireNonNull(device, "device");
        requireChannel(red, "red");
        requireChannel(green, "green");
        requireChannel(blue, "blue");
        requireChannel(alpha, "alpha");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        int rowPitch = rowPitch(width);
        int packedSize = Math.multiplyExact(rowPitch, height);
        Arena arena = device.arena();
        D3d12Native.ComTracker tracker = new D3d12Native.ComTracker();
        try {
            MemorySegment texture = committedTexture(
                    device,
                    arena,
                    tracker,
                    width,
                    height,
                    D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET,
                    D3D12_RESOURCE_STATE_RENDER_TARGET
            );
            MemorySegment heap = rtvHeap(device, arena, tracker);
            MemorySegment handle = arena.allocate(D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE);
            MemorySegment returned = D3d12FfmBindings
                    .invokeId3d12DescriptorHeapGetCpuDescriptorHandleForHeapStartPointer(
                            D3d12Native.functionAt(
                                    heap,
                                    D3d12Layouts.ID3D12_DESCRIPTOR_HEAP_VTABLE_GET_CPU_DESCRIPTOR_HANDLE_FOR_HEAP_START_OFFSET
                            ),
                            heap,
                            handle
                    );
            if (returned.address() != handle.address()) {
                throw new IllegalStateException("GetCPUDescriptorHandleForHeapStart returned an unexpected pointer");
            }
            D3d12FfmBindings.invokeId3d12DeviceCreateRenderTargetViewPointer(
                    D3d12Native.functionAt(
                            device.device(),
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_RENDER_TARGET_VIEW_OFFSET
                    ),
                    device.device(),
                    texture,
                    MemorySegment.NULL,
                    handle
            );
            MemorySegment readback = committedBuffer(device, arena, tracker, packedSize,
                    D3D12_HEAP_TYPE_READBACK, D3D12_RESOURCE_STATE_COPY_DEST);
            MemorySegment queue = commandQueue(device, arena, tracker);
            MemorySegment allocator = commandAllocator(device, arena, tracker);
            MemorySegment list = commandList(device, arena, tracker, allocator);
            MemorySegment fence = fence(device, arena, tracker);
            MemorySegment clearColor = arena.allocate(MemoryLayout.sequenceLayout(4, ValueLayout.JAVA_FLOAT));
            clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 0L, red);
            clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 1L, green);
            clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 2L, blue);
            clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 3L, alpha);
            reset(allocator, list);
            D3d12FfmBindings.invokeId3d12GraphicsCommandListClearRenderTargetViewPointer(
                    D3d12Native.functionAt(
                            list,
                            D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_CLEAR_RENDER_TARGET_VIEW_OFFSET
                    ),
                    list,
                    handle,
                    clearColor,
                    0,
                    MemorySegment.NULL
            );
            barrier(list, arena, texture, D3D12_RESOURCE_STATE_RENDER_TARGET, D3D12_RESOURCE_STATE_COPY_SOURCE);
            copyTextureToPlaced(list, arena, readback, texture, width, height, rowPitch);
            closeList(list);
            execute(queue, list, arena);
            signalAndWait(queue, fence, 1L);
            return new D3d12TextureRoundTrip(
                    width,
                    height,
                    unpack(mapRead(readback, arena, packedSize), width, height, rowPitch),
                    true
            );
        } finally {
            tracker.close();
        }
    }

    /// Uploads row-major RGBA and copies it onto a flip-model SDR swapchain.
    ///
    /// @param device the production device
    /// @param hwnd the native window
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the present observation
    public static D3d12Presentation presentSdrRgba(
            D3d12Device device,
            MemorySegment hwnd,
            byte[] rgba,
            int width,
            int height
    ) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(hwnd, "hwnd");
        Objects.requireNonNull(rgba, "rgba");
        if (hwnd.address() == 0L) {
            throw new IllegalArgumentException("HWND must not be NULL");
        }
        requireExtent(width, height, rgba);
        int rowPitch = rowPitch(width);
        int packedSize = Math.multiplyExact(rowPitch, height);
        Arena arena = device.arena();
        D3d12Native.ComTracker tracker = new D3d12Native.ComTracker();
        try {
            MemorySegment upload = committedBuffer(device, arena, tracker, packedSize,
                    D3D12_HEAP_TYPE_UPLOAD, D3D12_RESOURCE_STATE_GENERIC_READ);
            mapWrite(upload, arena, pack(rgba, width, height, rowPitch));
            MemorySegment texture = committedTexture(device, arena, tracker, width, height, 0,
                    D3D12_RESOURCE_STATE_COPY_DEST);
            MemorySegment queue = commandQueue(device, arena, tracker);
            MemorySegment allocator = commandAllocator(device, arena, tracker);
            MemorySegment list = commandList(device, arena, tracker, allocator);
            MemorySegment fence = fence(device, arena, tracker);
            reset(allocator, list);
            copyPlacedToTexture(list, arena, texture, upload, width, height, rowPitch);
            barrier(list, arena, texture, D3D12_RESOURCE_STATE_COPY_DEST, D3D12_RESOURCE_STATE_COPY_SOURCE);
            closeList(list);
            execute(queue, list, arena);
            signalAndWait(queue, fence, 1L);
            try (D3d12SwapChain swapChain = D3d12SwapChain.attach(device, hwnd, width, height)) {
                return swapChain.copyAndPresent(texture);
            }
        } finally {
            tracker.close();
        }
    }

    /// Aligns a row stride to `D3D12_TEXTURE_DATA_PITCH_ALIGNMENT`.
    ///
    /// @param width the pixel width
    /// @return the row pitch in bytes
    static int rowPitch(int width) {
        int raw = Math.multiplyExact(width, 4);
        return (raw + D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1) & ~(D3D12_TEXTURE_DATA_PITCH_ALIGNMENT - 1);
    }

    /// Packs tightly packed RGBA into a pitch-aligned upload buffer.
    private static byte[] pack(byte[] rgba, int width, int height, int rowPitch) {
        byte[] packed = new byte[Math.multiplyExact(rowPitch, height)];
        int rowBytes = Math.multiplyExact(width, 4);
        for (int row = 0; row < height; row++) {
            System.arraycopy(rgba, row * rowBytes, packed, row * rowPitch, rowBytes);
        }
        return packed;
    }

    /// Unpacks a pitch-aligned readback buffer into tightly packed RGBA.
    private static byte[] unpack(byte[] packed, int width, int height, int rowPitch) {
        byte[] rgba = new byte[Math.multiplyExact(Math.multiplyExact(width, height), 4)];
        int rowBytes = Math.multiplyExact(width, 4);
        for (int row = 0; row < height; row++) {
            System.arraycopy(packed, row * rowPitch, rgba, row * rowBytes, rowBytes);
        }
        return rgba;
    }

    /// Creates a committed 2D texture.
    private static MemorySegment committedTexture(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker,
            int width,
            int height,
            int flags,
            int initialState
    ) {
        MemorySegment heapProperties = arena.allocate(D3d12Layouts.D3D12_HEAP_PROPERTIES);
        heapProperties.fill((byte) 0);
        heapProperties.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_HEAP_PROPERTIES_TYPE_OFFSET,
                D3D12_HEAP_TYPE_DEFAULT);
        heapProperties.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_HEAP_PROPERTIES_CREATION_NODE_MASK_OFFSET, 1);
        heapProperties.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_HEAP_PROPERTIES_VISIBLE_NODE_MASK_OFFSET, 1);
        MemorySegment description = arena.allocate(D3d12Layouts.D3D12_RESOURCE_DESC);
        description.fill((byte) 0);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_DIMENSION_OFFSET,
                D3D12_RESOURCE_DIMENSION_TEXTURE2D);
        description.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RESOURCE_DESC_WIDTH_OFFSET, width);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_HEIGHT_OFFSET, height);
        description.set(ValueLayout.JAVA_SHORT, D3d12Layouts.D3D12_RESOURCE_DESC_DEPTH_OR_ARRAY_SIZE_OFFSET, (short) 1);
        description.set(ValueLayout.JAVA_SHORT, D3d12Layouts.D3D12_RESOURCE_DESC_MIP_LEVELS_OFFSET, (short) 1);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_FORMAT_OFFSET,
                DXGI_FORMAT_R8G8B8A8_UNORM);
        description.set(
                ValueLayout.JAVA_INT,
                D3d12Layouts.D3D12_RESOURCE_DESC_SAMPLE_DESC_OFFSET + D3d12Layouts.DXGI_SAMPLE_DESC_COUNT_OFFSET,
                1
        );
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_FLAGS_OFFSET, flags);
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Device::CreateCommittedResource(texture)",
                D3d12FfmBindings.invokeId3d12DeviceCreateCommittedResourcePointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMITTED_RESOURCE_OFFSET
                        ),
                        device.device(),
                        heapProperties,
                        0,
                        description,
                        initialState,
                        MemorySegment.NULL,
                        D3d12Native.guid(arena, ID3D12_RESOURCE),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommittedResource(texture)"));
    }

    /// Creates a committed buffer.
    private static MemorySegment committedBuffer(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker,
            int byteSize,
            int heapType,
            int initialState
    ) {
        MemorySegment heapProperties = arena.allocate(D3d12Layouts.D3D12_HEAP_PROPERTIES);
        heapProperties.fill((byte) 0);
        heapProperties.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_HEAP_PROPERTIES_TYPE_OFFSET, heapType);
        heapProperties.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_HEAP_PROPERTIES_CREATION_NODE_MASK_OFFSET, 1);
        heapProperties.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_HEAP_PROPERTIES_VISIBLE_NODE_MASK_OFFSET, 1);
        MemorySegment description = arena.allocate(D3d12Layouts.D3D12_RESOURCE_DESC);
        description.fill((byte) 0);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_DIMENSION_OFFSET,
                D3D12_RESOURCE_DIMENSION_BUFFER);
        description.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RESOURCE_DESC_WIDTH_OFFSET, byteSize);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_HEIGHT_OFFSET, 1);
        description.set(ValueLayout.JAVA_SHORT, D3d12Layouts.D3D12_RESOURCE_DESC_DEPTH_OR_ARRAY_SIZE_OFFSET, (short) 1);
        description.set(ValueLayout.JAVA_SHORT, D3d12Layouts.D3D12_RESOURCE_DESC_MIP_LEVELS_OFFSET, (short) 1);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_FORMAT_OFFSET, DXGI_FORMAT_UNKNOWN);
        description.set(
                ValueLayout.JAVA_INT,
                D3d12Layouts.D3D12_RESOURCE_DESC_SAMPLE_DESC_OFFSET + D3d12Layouts.DXGI_SAMPLE_DESC_COUNT_OFFSET,
                1
        );
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_LAYOUT_OFFSET,
                D3D12_TEXTURE_LAYOUT_ROW_MAJOR);
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Device::CreateCommittedResource(texture-buffer)",
                D3d12FfmBindings.invokeId3d12DeviceCreateCommittedResourcePointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMITTED_RESOURCE_OFFSET
                        ),
                        device.device(),
                        heapProperties,
                        0,
                        description,
                        initialState,
                        MemorySegment.NULL,
                        D3d12Native.guid(arena, ID3D12_RESOURCE),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommittedResource(texture-buffer)"));
    }

    /// Creates a one-slot RTV heap.
    private static MemorySegment rtvHeap(D3d12Device device, Arena arena, D3d12Native.ComTracker tracker) {
        MemorySegment description = arena.allocate(D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC);
        description.fill((byte) 0);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_TYPE_OFFSET,
                D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_NUM_DESCRIPTORS_OFFSET, 1);
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "CreateDescriptorHeap(texture RTV)",
                D3d12FfmBindings.invokeId3d12DeviceCreateDescriptorHeapPointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_DESCRIPTOR_HEAP_OFFSET
                        ),
                        device.device(),
                        description,
                        D3d12Native.guid(arena, ID3D12_DESCRIPTOR_HEAP),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateDescriptorHeap(texture RTV)"));
    }

    /// Records a placed-footprint to texture copy.
    private static void copyPlacedToTexture(
            MemorySegment list,
            Arena arena,
            MemorySegment texture,
            MemorySegment buffer,
            int width,
            int height,
            int rowPitch
    ) {
        MemorySegment destination = textureLocation(arena, texture);
        MemorySegment source = placedLocation(arena, buffer, width, height, rowPitch);
        D3d12FfmBindings.invokeId3d12GraphicsCommandListCopyTextureRegionPointer(
                D3d12Native.functionAt(
                        list,
                        D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_COPY_TEXTURE_REGION_OFFSET
                ),
                list,
                destination,
                0,
                0,
                0,
                source,
                MemorySegment.NULL
        );
    }

    /// Records a texture to placed-footprint copy.
    private static void copyTextureToPlaced(
            MemorySegment list,
            Arena arena,
            MemorySegment buffer,
            MemorySegment texture,
            int width,
            int height,
            int rowPitch
    ) {
        MemorySegment destination = placedLocation(arena, buffer, width, height, rowPitch);
        MemorySegment source = textureLocation(arena, texture);
        D3d12FfmBindings.invokeId3d12GraphicsCommandListCopyTextureRegionPointer(
                D3d12Native.functionAt(
                        list,
                        D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_COPY_TEXTURE_REGION_OFFSET
                ),
                list,
                destination,
                0,
                0,
                0,
                source,
                MemorySegment.NULL
        );
    }

    /// Fills a `D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX` location.
    static MemorySegment textureLocation(Arena arena, MemorySegment texture) {
        MemorySegment location = arena.allocate(D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION);
        location.fill((byte) 0);
        location.set(ValueLayout.ADDRESS, D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_RESOURCE_OFFSET, texture);
        location.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_TYPE_OFFSET,
                D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX);
        location.set(
                ValueLayout.JAVA_INT,
                D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_PAYLOAD_OFFSET
                        + D3d12Layouts.D3D12_TEXTURE_COPY_PAYLOAD_SUBRESOURCE_INDEX_OFFSET,
                0
        );
        return location;
    }

    /// Fills a `D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT` location.
    private static MemorySegment placedLocation(
            Arena arena,
            MemorySegment buffer,
            int width,
            int height,
            int rowPitch
    ) {
        MemorySegment location = arena.allocate(D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION);
        location.fill((byte) 0);
        location.set(ValueLayout.ADDRESS, D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_RESOURCE_OFFSET, buffer);
        location.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_TYPE_OFFSET,
                D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT);
        long footprint = D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_PAYLOAD_OFFSET
                + D3d12Layouts.D3D12_PLACED_SUBRESOURCE_FOOTPRINT_FOOTPRINT_OFFSET;
        location.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_PAYLOAD_OFFSET, 0L);
        location.set(ValueLayout.JAVA_INT, footprint + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_FORMAT_OFFSET,
                DXGI_FORMAT_R8G8B8A8_UNORM);
        location.set(ValueLayout.JAVA_INT, footprint + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_WIDTH_OFFSET, width);
        location.set(ValueLayout.JAVA_INT, footprint + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_HEIGHT_OFFSET, height);
        location.set(ValueLayout.JAVA_INT, footprint + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_DEPTH_OFFSET, 1);
        location.set(ValueLayout.JAVA_INT, footprint + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_ROW_PITCH_OFFSET,
                rowPitch);
        return location;
    }

    /// Writes CPU bytes into an upload buffer.
    private static void mapWrite(MemorySegment resource, Arena arena, byte[] payload) {
        MemorySegment dataCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Resource::Map(texture write)",
                D3d12FfmBindings.invokeId3d12ResourceMapPointer(
                        D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_MAP_OFFSET),
                        resource,
                        0,
                        MemorySegment.NULL,
                        dataCell
                )
        );
        D3d12Native.requirePointer(dataCell, "Map(texture write)").reinterpret(payload.length)
                .copyFrom(MemorySegment.ofArray(payload));
        D3d12FfmBindings.invokeId3d12ResourceUnmapPointer(
                D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_UNMAP_OFFSET),
                resource,
                0,
                MemorySegment.NULL
        );
    }

    /// Reads CPU bytes from a readback buffer.
    private static byte[] mapRead(MemorySegment resource, Arena arena, int byteSize) {
        MemorySegment range = arena.allocate(D3d12Layouts.D3D12_RANGE);
        range.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RANGE_BEGIN_OFFSET, 0L);
        range.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RANGE_END_OFFSET, byteSize);
        MemorySegment dataCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Resource::Map(texture read)",
                D3d12FfmBindings.invokeId3d12ResourceMapPointer(
                        D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_MAP_OFFSET),
                        resource,
                        0,
                        range,
                        dataCell
                )
        );
        byte[] copy = D3d12Native.requirePointer(dataCell, "Map(texture read)")
                .reinterpret(byteSize)
                .toArray(ValueLayout.JAVA_BYTE);
        D3d12FfmBindings.invokeId3d12ResourceUnmapPointer(
                D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_UNMAP_OFFSET),
                resource,
                0,
                range
        );
        return copy;
    }

    /// Creates a direct command queue.
    private static MemorySegment commandQueue(D3d12Device device, Arena arena, D3d12Native.ComTracker tracker) {
        MemorySegment description = arena.allocate(D3d12Layouts.D3D12_COMMAND_QUEUE_DESC);
        description.fill((byte) 0);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_COMMAND_QUEUE_DESC_TYPE_OFFSET,
                D3D12_COMMAND_LIST_TYPE_DIRECT);
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "CreateCommandQueue(texture)",
                D3d12FfmBindings.invokeId3d12DeviceCreateCommandQueuePointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMAND_QUEUE_OFFSET
                        ),
                        device.device(),
                        description,
                        D3d12Native.guid(arena, "0ec870a6-5d7e-4c22-8cfc-5baae07616ed"),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommandQueue(texture)"));
    }

    /// Creates a command allocator.
    private static MemorySegment commandAllocator(D3d12Device device, Arena arena, D3d12Native.ComTracker tracker) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "CreateCommandAllocator(texture)",
                D3d12FfmBindings.invokeId3d12DeviceCreateCommandAllocatorPointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMAND_ALLOCATOR_OFFSET
                        ),
                        device.device(),
                        D3D12_COMMAND_LIST_TYPE_DIRECT,
                        D3d12Native.guid(arena, "6102dee4-af59-4b09-b999-b44d73f09b24"),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommandAllocator(texture)"));
    }

    /// Creates and initially closes a command list.
    private static MemorySegment commandList(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker,
            MemorySegment allocator
    ) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "CreateCommandList(texture)",
                D3d12FfmBindings.invokeId3d12DeviceCreateCommandListPointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMAND_LIST_OFFSET
                        ),
                        device.device(),
                        0,
                        D3D12_COMMAND_LIST_TYPE_DIRECT,
                        allocator,
                        MemorySegment.NULL,
                        D3d12Native.guid(arena, "5b160d0f-ac1b-4185-8ba8-b3ae42a5a455"),
                        resultCell
                )
        );
        MemorySegment list = tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommandList(texture)"));
        closeList(list);
        return list;
    }

    /// Creates a fence.
    private static MemorySegment fence(D3d12Device device, Arena arena, D3d12Native.ComTracker tracker) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "CreateFence(texture)",
                D3d12FfmBindings.invokeId3d12DeviceCreateFencePointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_FENCE_OFFSET
                        ),
                        device.device(),
                        0L,
                        0,
                        D3d12Native.guid(arena, "0a753dcf-c4d8-4b91-adf6-be5a60d95a76"),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateFence(texture)"));
    }

    /// Resets the allocator and list.
    private static void reset(MemorySegment allocator, MemorySegment list) {
        D3d12Native.requireSuccess(
                "Allocator::Reset(texture)",
                D3d12FfmBindings.invokeId3d12CommandAllocatorResetPointer(
                        D3d12Native.functionAt(allocator, D3d12Layouts.ID3D12_COMMAND_ALLOCATOR_VTABLE_RESET_OFFSET),
                        allocator
                )
        );
        D3d12Native.requireSuccess(
                "List::Reset(texture)",
                D3d12FfmBindings.invokeId3d12GraphicsCommandListResetPointer(
                        D3d12Native.functionAt(list, D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_RESET_OFFSET),
                        list,
                        allocator,
                        MemorySegment.NULL
                )
        );
    }

    /// Records one transition barrier.
    private static void barrier(
            MemorySegment list,
            Arena arena,
            MemorySegment resource,
            int before,
            int after
    ) {
        MemorySegment barrier = arena.allocate(D3d12Layouts.D3D12_RESOURCE_BARRIER);
        barrier.fill((byte) 0);
        barrier.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_BARRIER_TYPE_OFFSET,
                D3D12_RESOURCE_BARRIER_TYPE_TRANSITION);
        long transitionOffset = D3d12Layouts.D3D12_RESOURCE_BARRIER_TRANSITION_OFFSET;
        barrier.set(ValueLayout.ADDRESS, transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_RESOURCE_OFFSET,
                resource);
        barrier.set(ValueLayout.JAVA_INT, transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_SUBRESOURCE_OFFSET,
                D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES);
        barrier.set(ValueLayout.JAVA_INT, transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_STATE_BEFORE_OFFSET,
                before);
        barrier.set(ValueLayout.JAVA_INT, transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_STATE_AFTER_OFFSET,
                after);
        D3d12FfmBindings.invokeId3d12GraphicsCommandListResourceBarrierPointer(
                D3d12Native.functionAt(
                        list,
                        D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_RESOURCE_BARRIER_OFFSET
                ),
                list,
                1,
                barrier
        );
    }

    /// Closes the command list.
    private static void closeList(MemorySegment list) {
        D3d12Native.requireSuccess(
                "List::Close(texture)",
                D3d12FfmBindings.invokeId3d12GraphicsCommandListClosePointer(
                        D3d12Native.functionAt(
                                list,
                                D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_CLOSE_OFFSET
                        ),
                        list
                )
        );
    }

    /// Submits the list.
    private static void execute(MemorySegment queue, MemorySegment list, Arena arena) {
        MemorySegment array = arena.allocate(ValueLayout.ADDRESS);
        array.set(ValueLayout.ADDRESS, 0L, list);
        D3d12FfmBindings.invokeId3d12CommandQueueExecuteCommandListsPointer(
                D3d12Native.functionAt(
                        queue,
                        D3d12Layouts.ID3D12_COMMAND_QUEUE_VTABLE_EXECUTE_COMMAND_LISTS_OFFSET
                ),
                queue,
                1,
                array
        );
    }

    /// Signals and spin-waits for the fence.
    private static void signalAndWait(MemorySegment queue, MemorySegment fence, long value) {
        D3d12Native.requireSuccess(
                "Queue::Signal(texture)",
                D3d12FfmBindings.invokeId3d12CommandQueueSignalPointer(
                        D3d12Native.functionAt(queue, D3d12Layouts.ID3D12_COMMAND_QUEUE_VTABLE_SIGNAL_OFFSET),
                        queue,
                        fence,
                        value
                )
        );
        long startedAt = System.nanoTime();
        while (true) {
            long completed = D3d12FfmBindings.invokeId3d12FenceGetCompletedValuePointer(
                    D3d12Native.functionAt(fence, D3d12Layouts.ID3D12_FENCE_VTABLE_GET_COMPLETED_VALUE_OFFSET),
                    fence
            );
            if (Long.compareUnsigned(completed, value) >= 0) {
                return;
            }
            if (System.nanoTime() - startedAt >= FENCE_TIMEOUT_NANOS) {
                throw new IllegalStateException("Timed out waiting for D3D12 texture fence");
            }
            LockSupport.parkNanos(100_000L);
            if (Thread.interrupted()) {
                throw new IllegalStateException("D3D12 texture fence wait was interrupted");
            }
        }
    }

    /// Requires a finite channel in `[0, 1]`.
    private static void requireChannel(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }

    /// Requires a tightly packed RGBA buffer for `width` by `height`.
    private static void requireExtent(int width, int height, byte[] rgba) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rgba.length != expected) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
    }
}
