package org.glavo.himari.rhi.d3d12;

import org.glavo.himari.rhi.d3d12.generated.D3d12FfmBindings;
import org.glavo.himari.rhi.d3d12.generated.D3d12Layouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

/// Copies an upload buffer through a default-heap resource and a readback buffer.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class D3d12GpuCopy {
    /// `D3D12_HEAP_TYPE_DEFAULT`.
    private static final int D3D12_HEAP_TYPE_DEFAULT = 1;

    /// `D3D12_HEAP_TYPE_UPLOAD`.
    private static final int D3D12_HEAP_TYPE_UPLOAD = 2;

    /// `D3D12_HEAP_TYPE_READBACK`.
    private static final int D3D12_HEAP_TYPE_READBACK = 3;

    /// `D3D12_RESOURCE_DIMENSION_BUFFER`.
    private static final int D3D12_RESOURCE_DIMENSION_BUFFER = 1;

    /// `DXGI_FORMAT_UNKNOWN`.
    private static final int DXGI_FORMAT_UNKNOWN = 0;

    /// `D3D12_TEXTURE_LAYOUT_ROW_MAJOR`.
    private static final int D3D12_TEXTURE_LAYOUT_ROW_MAJOR = 1;

    /// `D3D12_RESOURCE_STATE_GENERIC_READ`.
    private static final int D3D12_RESOURCE_STATE_GENERIC_READ = 0x00AC3;

    /// `D3D12_RESOURCE_STATE_COPY_DEST`.
    private static final int D3D12_RESOURCE_STATE_COPY_DEST = 0x400;

    /// `D3D12_RESOURCE_STATE_COPY_SOURCE`.
    private static final int D3D12_RESOURCE_STATE_COPY_SOURCE = 0x800;

    /// `D3D12_COMMAND_LIST_TYPE_DIRECT`.
    private static final int D3D12_COMMAND_LIST_TYPE_DIRECT = 0;

    /// `D3D12_RESOURCE_BARRIER_TYPE_TRANSITION`.
    private static final int D3D12_RESOURCE_BARRIER_TYPE_TRANSITION = 0;

    /// `D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES`.
    private static final int D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES = -1;

    /// `ID3D12Resource`.
    private static final String ID3D12_RESOURCE = "696442be-a72e-4059-bc79-5b5c98040fad";

    /// Fence wait budget.
    private static final long FENCE_TIMEOUT_NANOS = 30_000_000_000L;

    /// Prevents instantiation.
    private D3d12GpuCopy() {
    }

    /// Uploads `payload`, copies it through a default-heap buffer, and reads it back.
    ///
    /// @param device the production device
    /// @param payload the bytes to copy
    /// @return the bytes read from the readback heap
    public static MemorySegment copyThroughDefaultHeap(D3d12Device device, MemorySegment payload) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(payload, "payload");
        if (payload.byteSize() == 0L) {
            throw new IllegalArgumentException("Copy payload must not be empty");
        }
        byte[] bytes = payload.toArray(ValueLayout.JAVA_BYTE);
        Arena arena = device.arena();
        D3d12Native.ComTracker tracker = new D3d12Native.ComTracker();
        try {
            MemorySegment upload = committedBuffer(device, arena, tracker, bytes.length,
                    D3D12_HEAP_TYPE_UPLOAD, D3D12_RESOURCE_STATE_GENERIC_READ);
            mapWrite(upload, arena, bytes);
            MemorySegment def = committedBuffer(device, arena, tracker, bytes.length,
                    D3D12_HEAP_TYPE_DEFAULT, D3D12_RESOURCE_STATE_COPY_DEST);
            MemorySegment readback = committedBuffer(device, arena, tracker, bytes.length,
                    D3D12_HEAP_TYPE_READBACK, D3D12_RESOURCE_STATE_COPY_DEST);
            MemorySegment queue = commandQueue(device, arena, tracker);
            MemorySegment allocator = commandAllocator(device, arena, tracker);
            MemorySegment list = commandList(device, arena, tracker, allocator);
            MemorySegment fence = fence(device, arena, tracker);
            reset(allocator, list);
            copy(list, def, upload, bytes.length);
            barrier(list, arena, def, D3D12_RESOURCE_STATE_COPY_DEST, D3D12_RESOURCE_STATE_COPY_SOURCE);
            copy(list, readback, def, bytes.length);
            closeList(list);
            execute(queue, list, arena);
            signalAndWait(queue, fence, 1L);
            return MemorySegment.ofArray(mapRead(readback, arena, bytes.length)).asReadOnly();
        } finally {
            tracker.close();
        }
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
                "ID3D12Device::CreateCommittedResource(copy)",
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
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommittedResource(copy)"));
    }

    /// Writes CPU bytes into an upload buffer.
    private static void mapWrite(MemorySegment resource, Arena arena, byte[] payload) {
        MemorySegment dataCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Resource::Map(copy write)",
                D3d12FfmBindings.invokeId3d12ResourceMapPointer(
                        D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_MAP_OFFSET),
                        resource,
                        0,
                        MemorySegment.NULL,
                        dataCell
                )
        );
        D3d12Native.requirePointer(dataCell, "Map(copy write)").reinterpret(payload.length)
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
                "ID3D12Resource::Map(copy read)",
                D3d12FfmBindings.invokeId3d12ResourceMapPointer(
                        D3d12Native.functionAt(resource, D3d12Layouts.ID3D12_RESOURCE_VTABLE_MAP_OFFSET),
                        resource,
                        0,
                        range,
                        dataCell
                )
        );
        byte[] copy = D3d12Native.requirePointer(dataCell, "Map(copy read)")
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
                "CreateCommandQueue(copy)",
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
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommandQueue(copy)"));
    }

    /// Creates a command allocator.
    private static MemorySegment commandAllocator(D3d12Device device, Arena arena, D3d12Native.ComTracker tracker) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "CreateCommandAllocator(copy)",
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
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommandAllocator(copy)"));
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
                "CreateCommandList(copy)",
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
        MemorySegment list = tracker.own(D3d12Native.requirePointer(resultCell, "CreateCommandList(copy)"));
        closeList(list);
        return list;
    }

    /// Creates a fence.
    private static MemorySegment fence(D3d12Device device, Arena arena, D3d12Native.ComTracker tracker) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "CreateFence(copy)",
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
        return tracker.own(D3d12Native.requirePointer(resultCell, "CreateFence(copy)"));
    }

    /// Resets the allocator and list.
    private static void reset(MemorySegment allocator, MemorySegment list) {
        D3d12Native.requireSuccess(
                "Allocator::Reset(copy)",
                D3d12FfmBindings.invokeId3d12CommandAllocatorResetPointer(
                        D3d12Native.functionAt(allocator, D3d12Layouts.ID3D12_COMMAND_ALLOCATOR_VTABLE_RESET_OFFSET),
                        allocator
                )
        );
        D3d12Native.requireSuccess(
                "List::Reset(copy)",
                D3d12FfmBindings.invokeId3d12GraphicsCommandListResetPointer(
                        D3d12Native.functionAt(list, D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_RESET_OFFSET),
                        list,
                        allocator,
                        MemorySegment.NULL
                )
        );
    }

    /// Records `CopyBufferRegion`.
    private static void copy(MemorySegment list, MemorySegment destination, MemorySegment source, int byteCount) {
        D3d12FfmBindings.invokeId3d12GraphicsCommandListCopyBufferRegionPointer(
                D3d12Native.functionAt(
                        list,
                        D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_COPY_BUFFER_REGION_OFFSET
                ),
                list,
                destination,
                0L,
                source,
                0L,
                Integer.toUnsignedLong(byteCount)
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
                "List::Close(copy)",
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
                "Queue::Signal(copy)",
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
                throw new IllegalStateException("Timed out waiting for D3D12 copy fence");
            }
            LockSupport.parkNanos(100_000L);
            if (Thread.interrupted()) {
                throw new IllegalStateException("D3D12 copy fence wait was interrupted");
            }
        }
    }
}
