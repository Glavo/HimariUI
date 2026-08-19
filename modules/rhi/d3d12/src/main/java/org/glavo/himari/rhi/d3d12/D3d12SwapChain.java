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

/// Owns one flip-model SDR swapchain, command objects, and fence for a production HWND.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class D3d12SwapChain implements AutoCloseable {
    /// Flip-model buffer count.
    private static final int BUFFER_COUNT = 2;

    /// `DXGI_FORMAT_R8G8B8A8_UNORM`.
    private static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;

    /// `DXGI_USAGE_RENDER_TARGET_OUTPUT`.
    private static final int DXGI_USAGE_RENDER_TARGET_OUTPUT = 0x20;

    /// `DXGI_SCALING_STRETCH`.
    private static final int DXGI_SCALING_STRETCH = 0;

    /// `DXGI_SWAP_EFFECT_FLIP_DISCARD`.
    private static final int DXGI_SWAP_EFFECT_FLIP_DISCARD = 4;

    /// `DXGI_ALPHA_MODE_UNSPECIFIED`.
    private static final int DXGI_ALPHA_MODE_UNSPECIFIED = 0;

    /// `DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709`.
    private static final int DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709 = 0;

    /// `DXGI_COLOR_SPACE_RGB_FULL_G2084_NONE_P2020`.
    private static final int DXGI_COLOR_SPACE_RGB_FULL_G2084_NONE_P2020 = 12;

    /// `D3D12_COMMAND_LIST_TYPE_DIRECT`.
    private static final int D3D12_COMMAND_LIST_TYPE_DIRECT = 0;

    /// `D3D12_DESCRIPTOR_HEAP_TYPE_RTV`.
    private static final int D3D12_DESCRIPTOR_HEAP_TYPE_RTV = 2;

    /// `D3D12_RESOURCE_STATE_PRESENT`.
    private static final int D3D12_RESOURCE_STATE_PRESENT = 0;

    /// `D3D12_RESOURCE_STATE_RENDER_TARGET`.
    private static final int D3D12_RESOURCE_STATE_RENDER_TARGET = 0x4;

    /// `D3D12_RESOURCE_STATE_COPY_DEST`.
    private static final int D3D12_RESOURCE_STATE_COPY_DEST = 0x400;

    /// `D3D12_RESOURCE_BARRIER_TYPE_TRANSITION`.
    private static final int D3D12_RESOURCE_BARRIER_TYPE_TRANSITION = 0;

    /// `D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES`.
    private static final int D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES = -1;

    /// Maximum fence wait in nanoseconds.
    private static final long FENCE_TIMEOUT_NANOS = 30_000_000_000L;

    /// COM ownership for swapchain objects.
    private final D3d12Native.ComTracker references;

    /// Direct command queue.
    private final MemorySegment commandQueue;

    /// `IDXGISwapChain3`.
    private final MemorySegment swapChain;

    /// RTV heap.
    private final MemorySegment descriptorHeap;

    /// Back buffer 0.
    private final MemorySegment backBuffer0;

    /// Back buffer 1.
    private final MemorySegment backBuffer1;

    /// RTV handle 0.
    private final MemorySegment renderTargetHandle0;

    /// RTV handle 1.
    private final MemorySegment renderTargetHandle1;

    /// Command allocator.
    private final MemorySegment commandAllocator;

    /// Graphics command list.
    private final MemorySegment commandList;

    /// Fence.
    private final MemorySegment fence;

    /// Reusable transition barrier.
    private final MemorySegment barrier;

    /// Command-list pointer array for `ExecuteCommandLists`.
    private final MemorySegment commandListArray;

    /// Clear-color vector.
    private final MemorySegment clearColor;

    /// Next fence value.
    private long fenceValue;

    /// Whether this owner is closed.
    private boolean closed;

    /// HRESULT from `CheckColorSpaceSupport(P709)`.
    private final int p709CheckHresult;

    /// Present/overlay bits for P709.
    private final int p709Support;

    /// HRESULT from `CheckColorSpaceSupport(P2020 PQ)`.
    private final int p2020PqCheckHresult;

    /// Present/overlay bits for P2020 PQ. First-stable present never selects this space.
    private final int p2020PqSupport;

    /// Creates one swapchain owner.
    ///
    /// @param p709CheckHresult HRESULT from `CheckColorSpaceSupport(P709)`
    /// @param p709Support P709 support flags
    /// @param p2020PqCheckHresult HRESULT from `CheckColorSpaceSupport(P2020 PQ)`
    /// @param p2020PqSupport P2020 PQ support flags
    private D3d12SwapChain(
            D3d12Native.ComTracker references,
            MemorySegment commandQueue,
            MemorySegment swapChain,
            MemorySegment descriptorHeap,
            MemorySegment backBuffer0,
            MemorySegment backBuffer1,
            MemorySegment renderTargetHandle0,
            MemorySegment renderTargetHandle1,
            MemorySegment commandAllocator,
            MemorySegment commandList,
            MemorySegment fence,
            MemorySegment barrier,
            MemorySegment commandListArray,
            MemorySegment clearColor,
            int p709CheckHresult,
            int p709Support,
            int p2020PqCheckHresult,
            int p2020PqSupport
    ) {
        this.references = references;
        this.commandQueue = commandQueue;
        this.swapChain = swapChain;
        this.descriptorHeap = descriptorHeap;
        this.backBuffer0 = backBuffer0;
        this.backBuffer1 = backBuffer1;
        this.renderTargetHandle0 = renderTargetHandle0;
        this.renderTargetHandle1 = renderTargetHandle1;
        this.commandAllocator = commandAllocator;
        this.commandList = commandList;
        this.fence = fence;
        this.barrier = barrier;
        this.commandListArray = commandListArray;
        this.clearColor = clearColor;
        this.p709CheckHresult = p709CheckHresult;
        this.p709Support = p709Support;
        this.p2020PqCheckHresult = p2020PqCheckHresult;
        this.p2020PqSupport = p2020PqSupport;
    }

    /// Creates a flip-model SDR swapchain attached to `hwnd`.
    ///
    /// @param device the production device
    /// @param hwnd the native window
    /// @param width the positive width
    /// @param height the positive height
    /// @return the swapchain
    public static D3d12SwapChain attach(D3d12Device device, MemorySegment hwnd, int width, int height) {
        Objects.requireNonNull(device, "device");
        Objects.requireNonNull(hwnd, "hwnd");
        if (hwnd.address() == 0L) {
            throw new IllegalArgumentException("HWND must not be NULL");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Swapchain dimensions must be positive");
        }
        Arena arena = device.arena();
        D3d12Native.ComTracker tracker = new D3d12Native.ComTracker();
        try {
            MemorySegment queue = createCommandQueue(device, arena, tracker);
            MemorySegment swapChain = createSwapChain(device, arena, tracker, queue, hwnd, width, height);
            ColorSpaceProbe p709 = checkColorSpaceSupport(
                    swapChain,
                    arena,
                    DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709
            );
            ColorSpaceProbe p2020Pq = checkColorSpaceSupport(
                    swapChain,
                    arena,
                    DXGI_COLOR_SPACE_RGB_FULL_G2084_NONE_P2020
            );
            D3d12Native.requireSuccess(
                    "IDXGISwapChain3::SetColorSpace1(SDR)",
                    D3d12FfmBindings.invokeIdxgiSwapChain3SetColorSpace1Pointer(
                            D3d12Native.functionAt(
                                    swapChain,
                                    D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_SET_COLOR_SPACE1_OFFSET
                            ),
                            swapChain,
                            DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709
                    )
            );
            MemorySegment heap = createDescriptorHeap(device, arena, tracker);
            MemorySegment handle0 = arena.allocate(D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE);
            MemorySegment returned = D3d12FfmBindings
                    .invokeId3d12DescriptorHeapGetCpuDescriptorHandleForHeapStartPointer(
                            D3d12Native.functionAt(
                                    heap,
                                    D3d12Layouts.ID3D12_DESCRIPTOR_HEAP_VTABLE_GET_CPU_DESCRIPTOR_HANDLE_FOR_HEAP_START_OFFSET
                            ),
                            heap,
                            handle0
                    );
            if (returned.address() != handle0.address()) {
                throw new IllegalStateException("GetCPUDescriptorHandleForHeapStart returned an unexpected pointer");
            }
            int increment = D3d12FfmBindings.invokeId3d12DeviceGetDescriptorHandleIncrementSizePointer(
                    D3d12Native.functionAt(
                            device.device(),
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_GET_DESCRIPTOR_HANDLE_INCREMENT_SIZE_OFFSET
                    ),
                    device.device(),
                    D3D12_DESCRIPTOR_HEAP_TYPE_RTV
            );
            MemorySegment handle1 = arena.allocate(D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE);
            long base = handle0.get(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE_POINTER_OFFSET);
            handle1.set(
                    ValueLayout.JAVA_LONG,
                    D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE_POINTER_OFFSET,
                    Math.addExact(base, Integer.toUnsignedLong(increment))
            );
            MemorySegment buffer0 = acquireBackBuffer(arena, tracker, swapChain, 0);
            MemorySegment buffer1 = acquireBackBuffer(arena, tracker, swapChain, 1);
            createRenderTargetView(device, buffer0, handle0);
            createRenderTargetView(device, buffer1, handle1);
            MemorySegment allocator = createCommandAllocator(device, arena, tracker);
            MemorySegment list = createCommandList(device, arena, tracker, allocator);
            MemorySegment fence = createFence(device, arena, tracker);
            MemorySegment barrier = arena.allocate(D3d12Layouts.D3D12_RESOURCE_BARRIER);
            MemorySegment listArray = arena.allocate(ValueLayout.ADDRESS);
            listArray.set(ValueLayout.ADDRESS, 0L, list);
            MemorySegment clearColor = arena.allocate(MemoryLayout.sequenceLayout(4, ValueLayout.JAVA_FLOAT));
            return new D3d12SwapChain(
                    tracker,
                    queue,
                    swapChain,
                    heap,
                    buffer0,
                    buffer1,
                    handle0,
                    handle1,
                    allocator,
                    list,
                    fence,
                    barrier,
                    listArray,
                    clearColor,
                    p709.hresult(),
                    p709.support(),
                    p2020Pq.hresult(),
                    p2020Pq.support()
            );
        } catch (RuntimeException | Error failure) {
            tracker.close();
            throw failure;
        }
    }

    /// Clears the current back buffer and presents one SDR frame.
    ///
    /// @param red the red channel in `[0, 1]`
    /// @param green the green channel in `[0, 1]`
    /// @param blue the blue channel in `[0, 1]`
    /// @param alpha the alpha channel in `[0, 1]`
    /// @return the present observation
    public D3d12Presentation clearAndPresent(float red, float green, float blue, float alpha) {
        requireOpen();
        requireChannel(red, "red");
        requireChannel(green, "green");
        requireChannel(blue, "blue");
        requireChannel(alpha, "alpha");
        int bufferIndex = D3d12FfmBindings.invokeIdxgiSwapChain3GetCurrentBackBufferIndexPointer(
                D3d12Native.functionAt(
                        swapChain,
                        D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_GET_CURRENT_BACK_BUFFER_INDEX_OFFSET
                ),
                swapChain
        );
        if (bufferIndex < 0 || bufferIndex >= BUFFER_COUNT) {
            throw new IllegalStateException("Invalid back-buffer index " + bufferIndex);
        }
        MemorySegment backBuffer = bufferIndex == 0 ? backBuffer0 : backBuffer1;
        MemorySegment handle = bufferIndex == 0 ? renderTargetHandle0 : renderTargetHandle1;
        clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 0L, red);
        clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 1L, green);
        clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 2L, blue);
        clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 3L, alpha);
        D3d12Native.requireSuccess(
                "ID3D12CommandAllocator::Reset",
                D3d12FfmBindings.invokeId3d12CommandAllocatorResetPointer(
                        D3d12Native.functionAt(
                                commandAllocator,
                                D3d12Layouts.ID3D12_COMMAND_ALLOCATOR_VTABLE_RESET_OFFSET
                        ),
                        commandAllocator
                )
        );
        D3d12Native.requireSuccess(
                "ID3D12GraphicsCommandList::Reset",
                D3d12FfmBindings.invokeId3d12GraphicsCommandListResetPointer(
                        D3d12Native.functionAt(
                                commandList,
                                D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_RESET_OFFSET
                        ),
                        commandList,
                        commandAllocator,
                        MemorySegment.NULL
                )
        );
        transition(backBuffer, D3D12_RESOURCE_STATE_PRESENT, D3D12_RESOURCE_STATE_RENDER_TARGET);
        D3d12FfmBindings.invokeId3d12GraphicsCommandListClearRenderTargetViewPointer(
                D3d12Native.functionAt(
                        commandList,
                        D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_CLEAR_RENDER_TARGET_VIEW_OFFSET
                ),
                commandList,
                handle,
                clearColor,
                0,
                MemorySegment.NULL
        );
        transition(backBuffer, D3D12_RESOURCE_STATE_RENDER_TARGET, D3D12_RESOURCE_STATE_PRESENT);
        D3d12Native.requireSuccess(
                "ID3D12GraphicsCommandList::Close",
                D3d12FfmBindings.invokeId3d12GraphicsCommandListClosePointer(
                        D3d12Native.functionAt(
                                commandList,
                                D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_CLOSE_OFFSET
                        ),
                        commandList
                )
        );
        D3d12FfmBindings.invokeId3d12CommandQueueExecuteCommandListsPointer(
                D3d12Native.functionAt(
                        commandQueue,
                        D3d12Layouts.ID3D12_COMMAND_QUEUE_VTABLE_EXECUTE_COMMAND_LISTS_OFFSET
                ),
                commandQueue,
                1,
                commandListArray
        );
        D3d12Native.requireSuccess(
                "IDXGISwapChain::Present",
                D3d12FfmBindings.invokeIdxgiSwapChainPresentPointer(
                        D3d12Native.functionAt(
                                swapChain,
                                D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_PRESENT_OFFSET
                        ),
                        swapChain,
                        0,
                        0
                )
        );
        fenceValue = Math.incrementExact(fenceValue);
        D3d12Native.requireSuccess(
                "ID3D12CommandQueue::Signal",
                D3d12FfmBindings.invokeId3d12CommandQueueSignalPointer(
                        D3d12Native.functionAt(
                                commandQueue,
                                D3d12Layouts.ID3D12_COMMAND_QUEUE_VTABLE_SIGNAL_OFFSET
                        ),
                        commandQueue,
                        fence,
                        fenceValue
                )
        );
        awaitFence(fenceValue);
        return presentation(bufferIndex, true);
    }

    /// Copies a `COPY_SOURCE` texture onto the current back buffer and presents it.
    ///
    /// @param sourceTexture the default-heap texture in `D3D12_RESOURCE_STATE_COPY_SOURCE`
    /// @return the present observation
    public D3d12Presentation copyAndPresent(MemorySegment sourceTexture) {
        requireOpen();
        Objects.requireNonNull(sourceTexture, "sourceTexture");
        if (sourceTexture.address() == 0L) {
            throw new IllegalArgumentException("Source texture must not be NULL");
        }
        int bufferIndex = D3d12FfmBindings.invokeIdxgiSwapChain3GetCurrentBackBufferIndexPointer(
                D3d12Native.functionAt(
                        swapChain,
                        D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_GET_CURRENT_BACK_BUFFER_INDEX_OFFSET
                ),
                swapChain
        );
        if (bufferIndex < 0 || bufferIndex >= BUFFER_COUNT) {
            throw new IllegalStateException("Invalid back-buffer index " + bufferIndex);
        }
        MemorySegment backBuffer = bufferIndex == 0 ? backBuffer0 : backBuffer1;
        D3d12Native.requireSuccess(
                "ID3D12CommandAllocator::Reset(copy)",
                D3d12FfmBindings.invokeId3d12CommandAllocatorResetPointer(
                        D3d12Native.functionAt(
                                commandAllocator,
                                D3d12Layouts.ID3D12_COMMAND_ALLOCATOR_VTABLE_RESET_OFFSET
                        ),
                        commandAllocator
                )
        );
        D3d12Native.requireSuccess(
                "ID3D12GraphicsCommandList::Reset(copy)",
                D3d12FfmBindings.invokeId3d12GraphicsCommandListResetPointer(
                        D3d12Native.functionAt(
                                commandList,
                                D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_RESET_OFFSET
                        ),
                        commandList,
                        commandAllocator,
                        MemorySegment.NULL
                )
        );
        transition(backBuffer, D3D12_RESOURCE_STATE_PRESENT, D3D12_RESOURCE_STATE_COPY_DEST);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment destination = D3d12GpuTexture.textureLocation(arena, backBuffer);
            MemorySegment source = D3d12GpuTexture.textureLocation(arena, sourceTexture);
            D3d12FfmBindings.invokeId3d12GraphicsCommandListCopyTextureRegionPointer(
                    D3d12Native.functionAt(
                            commandList,
                            D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_COPY_TEXTURE_REGION_OFFSET
                    ),
                    commandList,
                    destination,
                    0,
                    0,
                    0,
                    source,
                    MemorySegment.NULL
            );
        }
        transition(backBuffer, D3D12_RESOURCE_STATE_COPY_DEST, D3D12_RESOURCE_STATE_PRESENT);
        D3d12Native.requireSuccess(
                "ID3D12GraphicsCommandList::Close(copy)",
                D3d12FfmBindings.invokeId3d12GraphicsCommandListClosePointer(
                        D3d12Native.functionAt(
                                commandList,
                                D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_CLOSE_OFFSET
                        ),
                        commandList
                )
        );
        D3d12FfmBindings.invokeId3d12CommandQueueExecuteCommandListsPointer(
                D3d12Native.functionAt(
                        commandQueue,
                        D3d12Layouts.ID3D12_COMMAND_QUEUE_VTABLE_EXECUTE_COMMAND_LISTS_OFFSET
                ),
                commandQueue,
                1,
                commandListArray
        );
        D3d12Native.requireSuccess(
                "IDXGISwapChain::Present(copy)",
                D3d12FfmBindings.invokeIdxgiSwapChainPresentPointer(
                        D3d12Native.functionAt(
                                swapChain,
                                D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_PRESENT_OFFSET
                        ),
                        swapChain,
                        0,
                        0
                )
        );
        fenceValue = Math.incrementExact(fenceValue);
        D3d12Native.requireSuccess(
                "ID3D12CommandQueue::Signal(copy)",
                D3d12FfmBindings.invokeId3d12CommandQueueSignalPointer(
                        D3d12Native.functionAt(
                                commandQueue,
                                D3d12Layouts.ID3D12_COMMAND_QUEUE_VTABLE_SIGNAL_OFFSET
                        ),
                        commandQueue,
                        fence,
                        fenceValue
                )
        );
        awaitFence(fenceValue);
        return presentation(bufferIndex, false);
    }

    /// Builds a present observation that includes the attach-time color-space probes.
    private D3d12Presentation presentation(int bufferIndex, boolean cleared) {
        return new D3d12Presentation(
                true,
                "DXGI_FORMAT_R8G8B8A8_UNORM",
                "DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709",
                false,
                references.ownedCount(),
                0,
                bufferIndex,
                cleared,
                p709CheckHresult,
                p709Support,
                p2020PqCheckHresult,
                p2020PqSupport
        );
    }

    /// Invokes generated `IDXGISwapChain3::CheckColorSpaceSupport`.
    private static ColorSpaceProbe checkColorSpaceSupport(
            MemorySegment swapChain,
            Arena arena,
            int colorSpace
    ) {
        MemorySegment support = arena.allocate(ValueLayout.JAVA_INT);
        support.set(ValueLayout.JAVA_INT, 0L, 0);
        int hresult = D3d12FfmBindings.invokeIdxgiSwapChain3CheckColorSpaceSupportPointer(
                D3d12Native.functionAt(
                        swapChain,
                        D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_CHECK_COLOR_SPACE_SUPPORT_OFFSET
                ),
                swapChain,
                colorSpace,
                support
        );
        return new ColorSpaceProbe(hresult, support.get(ValueLayout.JAVA_INT, 0L));
    }

    /// One `CheckColorSpaceSupport` result.
    ///
    /// @param hresult the COM status
    /// @param support the DXGI support flags
    private record ColorSpaceProbe(int hresult, int support) {
    }

    /// Releases swapchain COM objects. The parent [D3d12Device] remains open.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        references.close();
    }

    /// Creates the direct command queue.
    private static MemorySegment createCommandQueue(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker
    ) {
        MemorySegment description = arena.allocate(D3d12Layouts.D3D12_COMMAND_QUEUE_DESC);
        description.fill((byte) 0);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_COMMAND_QUEUE_DESC_TYPE_OFFSET,
                D3D12_COMMAND_LIST_TYPE_DIRECT);
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Device::CreateCommandQueue",
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
        return tracker.own(D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateCommandQueue"));
    }

    /// Creates the flip-model swapchain.
    private static MemorySegment createSwapChain(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker,
            MemorySegment commandQueue,
            MemorySegment hwnd,
            int width,
            int height
    ) {
        MemorySegment description = arena.allocate(D3d12Layouts.DXGI_SWAP_CHAIN_DESC1);
        description.fill((byte) 0);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_WIDTH_OFFSET, width);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_HEIGHT_OFFSET, height);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_FORMAT_OFFSET,
                DXGI_FORMAT_R8G8B8A8_UNORM);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_SAMPLE_DESC_OFFSET
                + D3d12Layouts.DXGI_SAMPLE_DESC_COUNT_OFFSET, 1);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_BUFFER_USAGE_OFFSET,
                DXGI_USAGE_RENDER_TARGET_OUTPUT);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_BUFFER_COUNT_OFFSET,
                BUFFER_COUNT);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_SCALING_OFFSET,
                DXGI_SCALING_STRETCH);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_SWAP_EFFECT_OFFSET,
                DXGI_SWAP_EFFECT_FLIP_DISCARD);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_ALPHA_MODE_OFFSET,
                DXGI_ALPHA_MODE_UNSPECIFIED);
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "IDXGIFactory2::CreateSwapChainForHwnd",
                D3d12FfmBindings.invokeIdxgiFactory2CreateSwapChainForHwndPointer(
                        D3d12Native.functionAt(
                                device.factory(),
                                D3d12Layouts.IDXGI_FACTORY4_VTABLE_CREATE_SWAP_CHAIN_FOR_HWND_OFFSET
                        ),
                        device.factory(),
                        commandQueue,
                        hwnd,
                        description,
                        MemorySegment.NULL,
                        MemorySegment.NULL,
                        resultCell
                )
        );
        MemorySegment swapChain1 = tracker.own(
                D3d12Native.requirePointer(resultCell, "IDXGIFactory2::CreateSwapChainForHwnd")
        );
        MemorySegment swapChain3Cell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "IUnknown::QueryInterface(IDXGISwapChain3)",
                D3d12FfmBindings.invokeIunknownQueryInterfacePointer(
                        D3d12Native.functionAt(swapChain1, D3d12Native.QUERY_INTERFACE_OFFSET),
                        swapChain1,
                        D3d12Native.guid(arena, "94d99bdb-f1f8-4ab0-b236-7da0170edab1"),
                        swapChain3Cell
                )
        );
        return tracker.own(D3d12Native.requirePointer(swapChain3Cell, "QueryInterface(IDXGISwapChain3)"));
    }

    /// Creates the RTV heap.
    private static MemorySegment createDescriptorHeap(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker
    ) {
        MemorySegment description = arena.allocate(D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC);
        description.fill((byte) 0);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_TYPE_OFFSET,
                D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
        description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_NUM_DESCRIPTORS_OFFSET,
                BUFFER_COUNT);
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Device::CreateDescriptorHeap",
                D3d12FfmBindings.invokeId3d12DeviceCreateDescriptorHeapPointer(
                        D3d12Native.functionAt(
                                device.device(),
                                D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_DESCRIPTOR_HEAP_OFFSET
                        ),
                        device.device(),
                        description,
                        D3d12Native.guid(arena, "8efb471d-616c-4f49-90f7-127bb763fa51"),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateDescriptorHeap"));
    }

    /// Acquires one swapchain buffer.
    private static MemorySegment acquireBackBuffer(
            Arena arena,
            D3d12Native.ComTracker tracker,
            MemorySegment swapChain,
            int index
    ) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "IDXGISwapChain::GetBuffer",
                D3d12FfmBindings.invokeIdxgiSwapChainGetBufferPointer(
                        D3d12Native.functionAt(
                                swapChain,
                                D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_GET_BUFFER_OFFSET
                        ),
                        swapChain,
                        index,
                        D3d12Native.guid(arena, "696442be-a72e-4059-bc79-5b5c98040fad"),
                        resultCell
                )
        );
        return tracker.own(D3d12Native.requirePointer(resultCell, "IDXGISwapChain::GetBuffer"));
    }

    /// Creates one implicit-format RTV.
    private static void createRenderTargetView(D3d12Device device, MemorySegment resource, MemorySegment handle) {
        D3d12FfmBindings.invokeId3d12DeviceCreateRenderTargetViewPointer(
                D3d12Native.functionAt(
                        device.device(),
                        D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_RENDER_TARGET_VIEW_OFFSET
                ),
                device.device(),
                resource,
                MemorySegment.NULL,
                handle
        );
    }

    /// Creates the command allocator.
    private static MemorySegment createCommandAllocator(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker
    ) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Device::CreateCommandAllocator",
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
        return tracker.own(D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateCommandAllocator"));
    }

    /// Creates and initially closes the command list.
    private static MemorySegment createCommandList(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker,
            MemorySegment allocator
    ) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Device::CreateCommandList",
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
        MemorySegment list = tracker.own(D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateCommandList"));
        D3d12Native.requireSuccess(
                "ID3D12GraphicsCommandList::Close(initial)",
                D3d12FfmBindings.invokeId3d12GraphicsCommandListClosePointer(
                        D3d12Native.functionAt(
                                list,
                                D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_CLOSE_OFFSET
                        ),
                        list
                )
        );
        return list;
    }

    /// Creates the GPU fence.
    private static MemorySegment createFence(
            D3d12Device device,
            Arena arena,
            D3d12Native.ComTracker tracker
    ) {
        MemorySegment resultCell = D3d12Native.pointerCell(arena);
        D3d12Native.requireSuccess(
                "ID3D12Device::CreateFence",
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
        return tracker.own(D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateFence"));
    }

    /// Records one resource-state transition.
    private void transition(MemorySegment resource, int stateBefore, int stateAfter) {
        barrier.fill((byte) 0);
        barrier.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_BARRIER_TYPE_OFFSET,
                D3D12_RESOURCE_BARRIER_TYPE_TRANSITION);
        long transitionOffset = D3d12Layouts.D3D12_RESOURCE_BARRIER_TRANSITION_OFFSET;
        barrier.set(
                ValueLayout.ADDRESS,
                transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_RESOURCE_OFFSET,
                resource
        );
        barrier.set(
                ValueLayout.JAVA_INT,
                transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_SUBRESOURCE_OFFSET,
                D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES
        );
        barrier.set(
                ValueLayout.JAVA_INT,
                transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_STATE_BEFORE_OFFSET,
                stateBefore
        );
        barrier.set(
                ValueLayout.JAVA_INT,
                transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_STATE_AFTER_OFFSET,
                stateAfter
        );
        D3d12FfmBindings.invokeId3d12GraphicsCommandListResourceBarrierPointer(
                D3d12Native.functionAt(
                        commandList,
                        D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_RESOURCE_BARRIER_OFFSET
                ),
                commandList,
                1,
                barrier
        );
    }

    /// Waits until the queue completes `expectedValue`.
    private void awaitFence(long expectedValue) {
        long startedAt = System.nanoTime();
        while (true) {
            long completed = D3d12FfmBindings.invokeId3d12FenceGetCompletedValuePointer(
                    D3d12Native.functionAt(
                            fence,
                            D3d12Layouts.ID3D12_FENCE_VTABLE_GET_COMPLETED_VALUE_OFFSET
                    ),
                    fence
            );
            if (Long.compareUnsigned(completed, expectedValue) >= 0) {
                return;
            }
            if (System.nanoTime() - startedAt >= FENCE_TIMEOUT_NANOS) {
                throw new IllegalStateException("Timed out waiting for D3D12 fence");
            }
            LockSupport.parkNanos(100_000L);
            if (Thread.interrupted()) {
                throw new IllegalStateException("D3D12 fence wait was interrupted");
            }
        }
    }

    /// Requires a finite channel in `[0, 1]`.
    private static void requireChannel(float value, String name) {
        if (!Float.isFinite(value) || value < 0.0f || value > 1.0f) {
            throw new IllegalArgumentException(name + " must be finite and in [0, 1]");
        }
    }

    /// Verifies the swapchain is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("D3D12 swapchain is closed");
        }
    }
}
