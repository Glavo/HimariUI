package org.glavo.himari.rhi.d3d12;

import org.glavo.himari.rhi.d3d12.generated.D3d12FfmBindings;
import org.glavo.himari.rhi.d3d12.generated.D3d12Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Owns one upload-heap committed buffer and a shader-visible CBV/SRV/UAV descriptor heap.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class D3d12GpuResource implements AutoCloseable {
    /// `D3D12_HEAP_TYPE_UPLOAD`.
    private static final int D3D12_HEAP_TYPE_UPLOAD = 2;

    /// `D3D12_RESOURCE_DIMENSION_BUFFER`.
    private static final int D3D12_RESOURCE_DIMENSION_BUFFER = 1;

    /// `DXGI_FORMAT_UNKNOWN`.
    private static final int DXGI_FORMAT_UNKNOWN = 0;

    /// `D3D12_TEXTURE_LAYOUT_ROW_MAJOR`.
    private static final int D3D12_TEXTURE_LAYOUT_ROW_MAJOR = 1;

    /// `D3D12_RESOURCE_STATE_GENERIC_READ`.
    private static final int D3D12_RESOURCE_STATE_GENERIC_READ = 0x00AC3;

    /// `D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV`.
    private static final int D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV = 0;

    /// `D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE`.
    private static final int D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE = 0x1;

    /// `ID3D12Resource`.
    private static final String ID3D12_RESOURCE = "696442be-a72e-4059-bc79-5b5c98040fad";

    /// `ID3D12DescriptorHeap`.
    private static final String ID3D12_DESCRIPTOR_HEAP = "8efb471d-616c-4f49-90f7-127bb763fa51";

    /// COM ownership.
    private final D3d12Native.ComTracker references;

    /// Upload-heap buffer.
    private final MemorySegment resource;

    /// Shader-visible CBV/SRV/UAV heap.
    private final MemorySegment descriptorHeap;

    /// Payload copied through `Map`.
    private final byte[] payload;

    /// Descriptor increment for the CBV/SRV/UAV heap.
    private final int descriptorIncrement;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates one resource owner.
    private D3d12GpuResource(
            D3d12Native.ComTracker references,
            MemorySegment resource,
            MemorySegment descriptorHeap,
            byte[] payload,
            int descriptorIncrement
    ) {
        this.references = references;
        this.resource = resource;
        this.descriptorHeap = descriptorHeap;
        this.payload = payload;
        this.descriptorIncrement = descriptorIncrement;
    }

    /// Creates an upload buffer, writes `payload`, and allocates a shader-visible descriptor heap.
    ///
    /// @param device the production device
    /// @param payload the bytes written through `ID3D12Resource::Map`
    /// @return the resource owner
    public static D3d12GpuResource createUpload(D3d12Device device, byte[] payload) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(payload, "payload");
        if (payload.length == 0) {
            throw new IllegalArgumentException("Upload payload must not be empty");
        }
        Arena arena = device.arena();
        D3d12Native.ComTracker tracker = new D3d12Native.ComTracker();
        try {
            MemorySegment resource = createUploadBuffer(device, arena, tracker, payload.length);
            write(resource, arena, payload);
            MemorySegment heap = createShaderVisibleHeap(device, arena, tracker);
            int increment = D3d12FfmBindings.invokeId3d12DeviceGetDescriptorHandleIncrementSizePointer(
                    D3d12Native.functionAt(
                            device.device(),
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_GET_DESCRIPTOR_HANDLE_INCREMENT_SIZE_OFFSET
                    ),
                    device.device(),
                    D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV
            );
            if (increment <= 0) {
                throw new IllegalStateException("CBV/SRV/UAV descriptor increment must be positive");
            }
            return new D3d12GpuResource(tracker, resource, heap, payload.clone(), increment);
        } catch (RuntimeException | Error failure) {
            tracker.close();
            throw failure;
        }
    }

    /// Returns a copy of the bytes written through `Map`.
    ///
    /// @return the payload
    public byte @Unmodifiable [] payload() {
        return payload.clone();
    }

    /// Returns the CBV/SRV/UAV descriptor increment in bytes.
    ///
    /// @return the increment
    public int descriptorIncrement() {
        return descriptorIncrement;
    }

    /// Returns the number of owned COM references.
    ///
    /// @return the count
    public int ownedReferences() {
        return references.ownedCount();
    }

    /// Reads the upload buffer back through `Map`.
    ///
    /// @param device the device that owns the arena used to allocate the map range
    /// @return the bytes currently visible to the CPU
    public byte[] readBack(D3d12Device device) {
        requireOpen();
        Objects.requireNonNull(device, "device");
        Arena arena = device.arena();
        MemorySegment range = arena.allocate(D3d12Layouts.D3D12_RANGE);
        range.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RANGE_BEGIN_OFFSET, 0L);
        range.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RANGE_END_OFFSET, payload.length);
        MemorySegment dataCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Resource::Map(read)",
                D3d12FfmBindings.invokeId3d12ResourceMapPointer(
                        D3d12Native.functionAt(
                                resource,
                                D3d12Layouts.ID3D12_RESOURCE_VTABLE_MAP_OFFSET
                        ),
                        resource,
                        0,
                        range,
                        dataCell
                )
        );
        MemorySegment mapped = D3d12Native.requirePointer(dataCell, "ID3D12Resource::Map(read)")
                .reinterpret(payload.length);
        byte[] copy = mapped.toArray(ValueLayout.JAVA_BYTE);
        D3d12FfmBindings.invokeId3d12ResourceUnmapPointer(
                D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_UNMAP_OFFSET),
                resource,
                0,
                range
        );
        return copy;
    }

    /// Releases the committed resource and descriptor heap.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        references.close();
    }

    /// Creates the upload-heap buffer.
    private static MemorySegment createUploadBuffer(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker,
            int byteSize
    ) {
        MemorySegment heapProperties = arena.allocate(D3d12Layouts.D3D12_HEAP_PROPERTIES);
        heapProperties.fill((byte) 0);
        heapProperties.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_HEAP_PROPERTIES_TYPE_OFFSET,
                D3D12_HEAP_TYPE_UPLOAD);
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
                "ID3D12Device::CreateCommittedResource",
                D3d12FfmBindings.invokeId3d12DeviceCreateCommittedResourcePointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMITTED_RESOURCE_OFFSET
                        ),
                        device.device(),
                        heapProperties,
                        0,
                        description,
                        D3D12_RESOURCE_STATE_GENERIC_READ,
                        MemorySegment.NULL,
                        D3d12Native.guid(arena, ID3D12_RESOURCE),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateCommittedResource"));
    }

    /// Maps the upload buffer and copies `payload`.
    private static void write(MemorySegment resource, Arena arena, byte[] payload) {
        MemorySegment dataCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Resource::Map(write)",
                D3d12FfmBindings.invokeId3d12ResourceMapPointer(
                        D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_MAP_OFFSET),
                        resource,
                        0,
                        MemorySegment.NULL,
                        dataCell
                )
        );
        MemorySegment mapped = D3d12Native.requirePointer(dataCell, "ID3D12Resource::Map(write)")
                .reinterpret(payload.length);
        mapped.copyFrom(MemorySegment.ofArray(payload));
        MemorySegment written = arena.allocate(D3d12Layouts.D3D12_RANGE);
        written.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RANGE_BEGIN_OFFSET, 0L);
        written.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RANGE_END_OFFSET, payload.length);
        D3d12FfmBindings.invokeId3d12ResourceUnmapPointer(
                D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_UNMAP_OFFSET),
                resource,
                0,
                written
        );
    }

    /// Creates a one-slot shader-visible CBV/SRV/UAV heap.
    private static MemorySegment createShaderVisibleHeap(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker
    ) {
        MemorySegment description = arena.allocate(D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC);
        description.fill((byte) 0);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_TYPE_OFFSET,
                D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_NUM_DESCRIPTORS_OFFSET, 1);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_FLAGS_OFFSET,
                D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE);
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Device::CreateDescriptorHeap(CBV_SRV_UAV)",
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
        return tracker.own(D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateDescriptorHeap(CBV_SRV_UAV)"));
    }

    /// Verifies the resource is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("D3D12 GPU resource is closed");
        }
    }
}
