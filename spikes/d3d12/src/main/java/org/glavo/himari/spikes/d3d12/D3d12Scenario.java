package org.glavo.himari.spikes.d3d12;

import org.glavo.himari.spikes.d3d12.generated.D3d12FfmBindings;
import org.glavo.himari.spikes.d3d12.generated.D3d12Layouts;
import org.glavo.himari.spikes.win32.Win32SurfaceWindow;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

/// Creates a D3D12 swapchain, presents deterministic clears, and verifies their GPU readback.
@SuppressWarnings("restricted")
@NotNullByDefault
final class D3d12Scenario {
    /// Fixed swapchain width in physical pixels.
    private static final int WIDTH = 320;

    /// Fixed swapchain height in physical pixels.
    private static final int HEIGHT = 240;

    /// Bytes per `DXGI_FORMAT_R8G8B8A8_UNORM` pixel.
    private static final int BYTES_PER_PIXEL = 4;

    /// D3D12 texture row alignment in bytes.
    private static final int TEXTURE_DATA_PITCH_ALIGNMENT = 256;

    /// Fixed readback row pitch, already aligned to [#TEXTURE_DATA_PITCH_ALIGNMENT].
    private static final int ROW_PITCH = WIDTH * BYTES_PER_PIXEL;

    /// Number of flip-model back buffers.
    private static final int BUFFER_COUNT = 2;

    /// `D3D_FEATURE_LEVEL_11_0`.
    private static final int FEATURE_LEVEL_11_0 = 0xB000;

    /// `DXGI_CREATE_FACTORY_DEBUG`.
    private static final int DXGI_CREATE_FACTORY_DEBUG = 0x1;

    /// `DXGI_MWA_NO_ALT_ENTER`.
    private static final int DXGI_MWA_NO_ALT_ENTER = 0x2;

    /// `DXGI_FORMAT_UNKNOWN`.
    private static final int DXGI_FORMAT_UNKNOWN = 0;

    /// `DXGI_FORMAT_R16G16B16A16_FLOAT`.
    private static final int DXGI_FORMAT_R16G16B16A16_FLOAT = 10;

    /// `DXGI_FORMAT_R10G10B10A2_UNORM`.
    private static final int DXGI_FORMAT_R10G10B10A2_UNORM = 24;

    /// `DXGI_FORMAT_R8G8B8A8_UNORM`.
    private static final int DXGI_FORMAT_R8G8B8A8_UNORM = 28;

    /// `D3D12_FEATURE_FORMAT_SUPPORT`.
    private static final int D3D12_FEATURE_FORMAT_SUPPORT = 3;

    /// `D3D12_FORMAT_SUPPORT1_RENDER_TARGET`.
    private static final int D3D12_FORMAT_SUPPORT1_RENDER_TARGET = 0x4000;

    /// `DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709`.
    private static final int DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709 = 0;

    /// `DXGI_COLOR_SPACE_RGB_FULL_G2084_NONE_P2020`.
    private static final int DXGI_COLOR_SPACE_RGB_FULL_G2084_NONE_P2020 = 12;

    /// `DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P2020`.
    private static final int DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P2020 = 17;

    /// `DXGI_SWAP_CHAIN_COLOR_SPACE_SUPPORT_FLAG_PRESENT`.
    private static final int DXGI_COLOR_SPACE_SUPPORT_PRESENT = 0x1;

    /// `DXGI_USAGE_RENDER_TARGET_OUTPUT`.
    private static final int DXGI_USAGE_RENDER_TARGET_OUTPUT = 0x20;

    /// `DXGI_SCALING_STRETCH`.
    private static final int DXGI_SCALING_STRETCH = 0;

    /// `DXGI_SWAP_EFFECT_FLIP_DISCARD`.
    private static final int DXGI_SWAP_EFFECT_FLIP_DISCARD = 4;

    /// `DXGI_ALPHA_MODE_UNSPECIFIED`.
    private static final int DXGI_ALPHA_MODE_UNSPECIFIED = 0;

    /// `D3D12_COMMAND_LIST_TYPE_DIRECT`.
    private static final int D3D12_COMMAND_LIST_TYPE_DIRECT = 0;

    /// `D3D12_DESCRIPTOR_HEAP_TYPE_RTV`.
    private static final int D3D12_DESCRIPTOR_HEAP_TYPE_RTV = 2;

    /// `D3D12_HEAP_TYPE_READBACK`.
    private static final int D3D12_HEAP_TYPE_READBACK = 3;

    /// `D3D12_RESOURCE_DIMENSION_BUFFER`.
    private static final int D3D12_RESOURCE_DIMENSION_BUFFER = 1;

    /// `D3D12_TEXTURE_LAYOUT_ROW_MAJOR`.
    private static final int D3D12_TEXTURE_LAYOUT_ROW_MAJOR = 1;

    /// `D3D12_RESOURCE_STATE_PRESENT`.
    private static final int D3D12_RESOURCE_STATE_PRESENT = 0;

    /// `D3D12_RESOURCE_STATE_RENDER_TARGET`.
    private static final int D3D12_RESOURCE_STATE_RENDER_TARGET = 0x4;

    /// `D3D12_RESOURCE_STATE_COPY_DEST`.
    private static final int D3D12_RESOURCE_STATE_COPY_DEST = 0x400;

    /// `D3D12_RESOURCE_STATE_COPY_SOURCE`.
    private static final int D3D12_RESOURCE_STATE_COPY_SOURCE = 0x800;

    /// `D3D12_RESOURCE_BARRIER_TYPE_TRANSITION`.
    private static final int D3D12_RESOURCE_BARRIER_TYPE_TRANSITION = 0;

    /// `D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES`.
    private static final int D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES = -1;

    /// `D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX`.
    private static final int D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX = 0;

    /// `D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT`.
    private static final int D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT = 1;

    /// Exact red channel byte in the clear fixture.
    private static final int CLEAR_RED = 17;

    /// Exact green channel byte in the clear fixture.
    private static final int CLEAR_GREEN = 83;

    /// Exact blue channel byte in the clear fixture.
    private static final int CLEAR_BLUE = 149;

    /// Exact alpha channel byte in the clear fixture.
    private static final int CLEAR_ALPHA = 255;

    /// Maximum time allowed for one GPU fence in nanoseconds.
    private static final long FENCE_TIMEOUT_NANOS = 30_000_000_000L;

    /// Maximum message count accepted from the debug queue.
    private static final long MAX_DEBUG_MESSAGES = 4096L;

    /// Maximum bytes accepted for one debug message.
    private static final long MAX_DEBUG_MESSAGE_BYTES = 1024L * 1024L;

    /// Prevents instantiation of this utility class.
    private D3d12Scenario() {
    }

    /// Executes one complete device, swapchain, presentation, and teardown lifecycle.
    ///
    /// @param repetitions the positive frame count
    /// @param soakSeconds the non-negative minimum presentation duration
    /// @return the immutable scenario observation after every COM reference has been released
    static D3d12ScenarioResult run(int repetitions, int soakSeconds) {
        if (repetitions <= 0) {
            throw new IllegalArgumentException("repetitions must be positive");
        }
        if (soakSeconds < 0) {
            throw new IllegalArgumentException("soakSeconds must be non-negative");
        }
        if (ROW_PITCH % TEXTURE_DATA_PITCH_ALIGNMENT != 0) {
            throw new AssertionError("Fixed D3D12 readback pitch is not 256-byte aligned");
        }

        try (D3d12Libraries libraries = D3d12Libraries.open();
             Arena arena = Arena.ofConfined();
             Win32SurfaceWindow window = Win32SurfaceWindow.open("HimariUI D3D12 conformance", WIDTH, HEIGHT)) {
            Session session = new Session(libraries.bindings(), arena, window);
            try {
                return session.execute(repetitions, soakSeconds);
            } finally {
                session.closeComReferences();
            }
        }
    }

    /// Owns the native objects and reusable records for a single scenario execution.
    @NotNullByDefault
    private static final class Session {
        /// Generated native bindings.
        private final D3d12FfmBindings bindings;

        /// Confined storage for every native descriptor and pointer cell.
        private final Arena arena;

        /// Window whose borrowed handle backs the swapchain.
        private final Win32SurfaceWindow window;

        /// Reverse-order COM ownership tracker.
        private final D3d12Native.ComTracker comReferences;

        /// Mutable format-support observations accumulated during initialization.
        private final List<D3d12FormatSupport> formats;

        /// Mutable color-space observations accumulated during initialization.
        private final List<D3d12ColorSpaceSupport> colorSpaces;

        /// Owned `IDXGIFactory4` interface.
        private MemorySegment factory = MemorySegment.NULL;

        /// Owned `ID3D12Device` interface.
        private MemorySegment device = MemorySegment.NULL;

        /// Owned optional `ID3D12InfoQueue` interface.
        private MemorySegment infoQueue = MemorySegment.NULL;

        /// Owned direct `ID3D12CommandQueue` interface.
        private MemorySegment commandQueue = MemorySegment.NULL;

        /// Owned `IDXGISwapChain3` interface.
        private MemorySegment swapChain = MemorySegment.NULL;

        /// Owned render-target descriptor heap.
        private MemorySegment descriptorHeap = MemorySegment.NULL;

        /// Owned first swapchain resource.
        private MemorySegment backBuffer0 = MemorySegment.NULL;

        /// Owned second swapchain resource.
        private MemorySegment backBuffer1 = MemorySegment.NULL;

        /// Owned direct command allocator.
        private MemorySegment commandAllocator = MemorySegment.NULL;

        /// Owned graphics command list.
        private MemorySegment commandList = MemorySegment.NULL;

        /// Owned synchronization fence.
        private MemorySegment fence = MemorySegment.NULL;

        /// Owned readback buffer.
        private MemorySegment readback = MemorySegment.NULL;

        /// First render-target CPU descriptor handle value.
        private MemorySegment renderTargetHandle0 = MemorySegment.NULL;

        /// Second render-target CPU descriptor handle value.
        private MemorySegment renderTargetHandle1 = MemorySegment.NULL;

        /// Reusable transition barrier.
        private MemorySegment barrier = MemorySegment.NULL;

        /// Reusable source texture-copy location.
        private MemorySegment sourceCopyLocation = MemorySegment.NULL;

        /// Reusable destination texture-copy location.
        private MemorySegment destinationCopyLocation = MemorySegment.NULL;

        /// One-element array passed to `ExecuteCommandLists`.
        private MemorySegment commandListArray = MemorySegment.NULL;

        /// Exact four-float clear value.
        private MemorySegment clearColor = MemorySegment.NULL;

        /// Read range covering the complete readback buffer.
        private MemorySegment readRange = MemorySegment.NULL;

        /// Whether the D3D12 debug layer was enabled before device creation.
        private boolean debugLayerEnabled;

        /// Whether the DXGI factory was created with its debug flag.
        private boolean dxgiFactoryDebugEnabled;

        /// Last fence value signalled by the direct queue.
        private long fenceValue;

        /// Greatest observed clear-fixture channel delta.
        private int maximumChannelDelta;

        /// Creates the uninitialized native session.
        ///
        /// @param bindings generated native bindings
        /// @param arena confined native storage
        /// @param window initialized Win32 surface window
        private Session(D3d12FfmBindings bindings, Arena arena, Win32SurfaceWindow window) {
            this.bindings = bindings;
            this.arena = arena;
            this.window = window;
            this.comReferences = new D3d12Native.ComTracker();
            this.formats = new ArrayList<>();
            this.colorSpaces = new ArrayList<>();
        }

        /// Initializes all objects, renders the requested frames, collects diagnostics, and releases COM ownership.
        ///
        /// @param repetitions the requested frame count
        /// @param soakSeconds the requested minimum presentation duration
        /// @return the immutable completed observation
        private D3d12ScenarioResult execute(int repetitions, int soakSeconds) {
            initialize();
            long startedAt = System.nanoTime();
            int presentedFrames = renderFrames(repetitions, soakSeconds, startedAt);
            List<D3d12DebugMessage> debugMessages = readDebugMessages();
            int deviceRemovedReason = deviceRemovedReason();
            int ownedReferences = comReferences.ownedCount();
            closeComReferences();
            int releasedReferences = comReferences.releasedCount();
            if (releasedReferences != ownedReferences) {
                throw new IllegalStateException("COM ownership imbalance: owned=" + ownedReferences
                        + ", released=" + releasedReferences);
            }
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
            long pixelsPerFrame = (long) WIDTH * HEIGHT;
            long declaredResourceBytes = Math.addExact(
                    Math.multiplyExact((long) BUFFER_COUNT * WIDTH * HEIGHT, BYTES_PER_PIXEL),
                    (long) ROW_PITCH * HEIGHT
            );
            return new D3d12ScenarioResult(
                    repetitions,
                    soakSeconds,
                    elapsedMillis,
                    WIDTH,
                    HEIGHT,
                    ROW_PITCH,
                    presentedFrames,
                    presentedFrames,
                    Math.multiplyExact(pixelsPerFrame, presentedFrames),
                    maximumChannelDelta,
                    fenceValue,
                    deviceRemovedReason,
                    debugLayerEnabled,
                    dxgiFactoryDebugEnabled,
                    infoQueue.address() != 0L,
                    formats,
                    colorSpaces,
                    debugMessages,
                    ownedReferences,
                    releasedReferences,
                    declaredResourceBytes
            );
        }

        /// Creates the debug layer, device, swapchain, command objects, descriptors, fence, and readback resource.
        private void initialize() {
            debugLayerEnabled = enableDebugLayer();
            createFactory();
            createDevice();
            acquireInfoQueue();
            createCommandQueue();
            createSwapChain();
            queryCapabilities();
            createRenderTargets();
            createCommandObjects();
            createFence();
            createReadbackResource();
            allocateReusableRecords();
        }

        /// Enables the D3D12 debug layer when the optional system component is available.
        ///
        /// @return whether the layer was enabled
        private boolean enableDebugLayer() {
            MemorySegment resultCell = D3d12Native.pointerCell(arena);
            int result = bindings.d3d12GetDebugInterface(
                    D3d12Native.guid(arena, "344488b7-6846-474b-b989-f027448245e0"),
                    resultCell
            );
            if (result < 0) {
                return false;
            }
            MemorySegment debug = comReferences.own(D3d12Native.requirePointer(resultCell, "D3D12GetDebugInterface"));
            D3d12FfmBindings.invokeId3d12DebugEnableDebugLayerPointer(
                    D3d12Native.functionAt(debug, D3d12Layouts.ID3D12_DEBUG_VTABLE_ENABLE_DEBUG_LAYER_OFFSET),
                    debug
            );
            return true;
        }

        /// Creates `IDXGIFactory4`, retrying without factory diagnostics if that optional flag is unavailable.
        private void createFactory() {
            MemorySegment resultCell = D3d12Native.pointerCell(arena);
            MemorySegment interfaceId = D3d12Native.guid(arena, "1bc6ea02-ef36-464f-bf0c-21ca39e5168a");
            int requestedFlags = debugLayerEnabled ? DXGI_CREATE_FACTORY_DEBUG : 0;
            int result = bindings.createDxgiFactory2(requestedFlags, interfaceId, resultCell);
            if (result < 0 && requestedFlags != 0) {
                resultCell.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
                result = bindings.createDxgiFactory2(0, interfaceId, resultCell);
                dxgiFactoryDebugEnabled = false;
            } else {
                dxgiFactoryDebugEnabled = requestedFlags != 0;
            }
            D3d12Native.requireSuccess("CreateDXGIFactory2", result);
            factory = comReferences.own(D3d12Native.requirePointer(resultCell, "CreateDXGIFactory2"));
            D3d12Native.requireSuccess(
                    "IDXGIFactory::MakeWindowAssociation",
                    D3d12FfmBindings.invokeIdxgiFactoryMakeWindowAssociationPointer(
                            D3d12Native.functionAt(
                                    factory,
                                    D3d12Layouts.IDXGI_FACTORY4_VTABLE_MAKE_WINDOW_ASSOCIATION_OFFSET
                            ),
                            factory,
                            window.handle(),
                            DXGI_MWA_NO_ALT_ENTER
                    )
            );
        }

        /// Creates the default-adapter `ID3D12Device` at feature level 11.0 or newer.
        private void createDevice() {
            MemorySegment resultCell = D3d12Native.pointerCell(arena);
            int result = bindings.d3d12CreateDevice(
                    MemorySegment.NULL,
                    FEATURE_LEVEL_11_0,
                    D3d12Native.guid(arena, "189819f1-1db6-4b57-be54-1821339b85f7"),
                    resultCell
            );
            D3d12Native.requireSuccess("D3D12CreateDevice", result);
            device = comReferences.own(D3d12Native.requirePointer(resultCell, "D3D12CreateDevice"));
        }

        /// Acquires and clears `ID3D12InfoQueue` when the debug layer exposed it.
        private void acquireInfoQueue() {
            if (!debugLayerEnabled) {
                return;
            }
            infoQueue = queryInterface(
                    device,
                    "0742a90b-c387-483f-b946-30a7e4e61458",
                    "ID3D12InfoQueue",
                    false
            );
            if (infoQueue.address() != 0L) {
                D3d12FfmBindings.invokeId3d12InfoQueueClearStoredMessagesPointer(
                        D3d12Native.functionAt(
                                infoQueue,
                                D3d12Layouts.ID3D12_INFO_QUEUE_VTABLE_CLEAR_STORED_MESSAGES_OFFSET
                        ),
                        infoQueue
                );
            }
        }

        /// Creates the direct command queue supplied to the DXGI swapchain.
        private void createCommandQueue() {
            MemorySegment description = arena.allocate(D3d12Layouts.D3D12_COMMAND_QUEUE_DESC);
            description.fill((byte) 0);
            description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_COMMAND_QUEUE_DESC_TYPE_OFFSET,
                    D3D12_COMMAND_LIST_TYPE_DIRECT);
            MemorySegment resultCell = D3d12Native.pointerCell(arena);
            int result = D3d12FfmBindings.invokeId3d12DeviceCreateCommandQueuePointer(
                    D3d12Native.functionAt(
                            device,
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMAND_QUEUE_OFFSET
                    ),
                    device,
                    description,
                    D3d12Native.guid(arena, "0ec870a6-5d7e-4c22-8cfc-5baae07616ed"),
                    resultCell
            );
            D3d12Native.requireSuccess("ID3D12Device::CreateCommandQueue", result);
            commandQueue = comReferences.own(
                    D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateCommandQueue")
            );
        }

        /// Creates the flip-model SDR swapchain and acquires its `IDXGISwapChain3` interface.
        private void createSwapChain() {
            MemorySegment description = arena.allocate(D3d12Layouts.DXGI_SWAP_CHAIN_DESC1);
            description.fill((byte) 0);
            description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_WIDTH_OFFSET, WIDTH);
            description.set(ValueLayout.JAVA_INT, D3d12Layouts.DXGI_SWAP_CHAIN_DESC1_HEIGHT_OFFSET, HEIGHT);
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
            int result = D3d12FfmBindings.invokeIdxgiFactory2CreateSwapChainForHwndPointer(
                    D3d12Native.functionAt(
                            factory,
                            D3d12Layouts.IDXGI_FACTORY4_VTABLE_CREATE_SWAP_CHAIN_FOR_HWND_OFFSET
                    ),
                    factory,
                    commandQueue,
                    window.handle(),
                    description,
                    MemorySegment.NULL,
                    MemorySegment.NULL,
                    resultCell
            );
            D3d12Native.requireSuccess("IDXGIFactory2::CreateSwapChainForHwnd", result);
            MemorySegment swapChain1 = comReferences.own(
                    D3d12Native.requirePointer(resultCell, "IDXGIFactory2::CreateSwapChainForHwnd")
            );
            swapChain = queryInterface(
                    swapChain1,
                    "94d99bdb-f1f8-4ab0-b236-7da0170edab1",
                    "IDXGISwapChain3",
                    true
            );
        }

        /// Queries the three planned render-target formats and candidate color spaces, then selects explicit SDR.
        private void queryCapabilities() {
            formats.add(queryFormat("DXGI_FORMAT_R8G8B8A8_UNORM", DXGI_FORMAT_R8G8B8A8_UNORM));
            formats.add(queryFormat("DXGI_FORMAT_R10G10B10A2_UNORM", DXGI_FORMAT_R10G10B10A2_UNORM));
            formats.add(queryFormat("DXGI_FORMAT_R16G16B16A16_FLOAT", DXGI_FORMAT_R16G16B16A16_FLOAT));

            colorSpaces.add(queryColorSpace(
                    "DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709",
                    DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P709
            ));
            colorSpaces.add(queryColorSpace(
                    "DXGI_COLOR_SPACE_RGB_FULL_G2084_NONE_P2020",
                    DXGI_COLOR_SPACE_RGB_FULL_G2084_NONE_P2020
            ));
            colorSpaces.add(queryColorSpace(
                    "DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P2020",
                    DXGI_COLOR_SPACE_RGB_FULL_G22_NONE_P2020
            ));
            if (!colorSpaces.getFirst().present()) {
                throw new IllegalStateException("Selected SDR color space lacks DXGI presentation support");
            }
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
        }

        /// Queries one D3D12 format-support record.
        ///
        /// @param name the stable DXGI format name
        /// @param format the native format value
        /// @return the support observation
        private D3d12FormatSupport queryFormat(String name, int format) {
            MemorySegment data = arena.allocate(D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT);
            data.fill((byte) 0);
            data.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT_FORMAT_OFFSET, format);
            int result = D3d12FfmBindings.invokeId3d12DeviceCheckFeatureSupportPointer(
                    D3d12Native.functionAt(
                            device,
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_CHECK_FEATURE_SUPPORT_OFFSET
                    ),
                    device,
                    D3D12_FEATURE_FORMAT_SUPPORT,
                    data,
                    Math.toIntExact(D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT.byteSize())
            );
            D3d12Native.requireSuccess("ID3D12Device::CheckFeatureSupport(" + name + ')', result);
            int support1 = data.get(
                    ValueLayout.JAVA_INT,
                    D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT_SUPPORT1_OFFSET
            );
            int support2 = data.get(
                    ValueLayout.JAVA_INT,
                    D3d12Layouts.D3D12_FEATURE_DATA_FORMAT_SUPPORT_SUPPORT2_OFFSET
            );
            return new D3d12FormatSupport(
                    name,
                    format,
                    Integer.toUnsignedLong(support1),
                    Integer.toUnsignedLong(support2),
                    (support1 & D3D12_FORMAT_SUPPORT1_RENDER_TARGET) != 0
            );
        }

        /// Queries one color space against the selected SDR swapchain configuration.
        ///
        /// @param name the stable color-space name
        /// @param colorSpace the native color-space value
        /// @return the support observation
        private D3d12ColorSpaceSupport queryColorSpace(String name, int colorSpace) {
            MemorySegment supportCell = arena.allocate(ValueLayout.JAVA_INT);
            supportCell.set(ValueLayout.JAVA_INT, 0L, 0);
            int result = D3d12FfmBindings.invokeIdxgiSwapChain3CheckColorSpaceSupportPointer(
                    D3d12Native.functionAt(
                            swapChain,
                            D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_CHECK_COLOR_SPACE_SUPPORT_OFFSET
                    ),
                    swapChain,
                    colorSpace,
                    supportCell
            );
            D3d12Native.requireSuccess("IDXGISwapChain3::CheckColorSpaceSupport(" + name + ')', result);
            int support = supportCell.get(ValueLayout.JAVA_INT, 0L);
            return new D3d12ColorSpaceSupport(
                    name,
                    colorSpace,
                    Integer.toUnsignedLong(support),
                    (support & DXGI_COLOR_SPACE_SUPPORT_PRESENT) != 0
            );
        }

        /// Creates the RTV heap, acquires both swapchain buffers, and writes their descriptors.
        private void createRenderTargets() {
            MemorySegment heapDescription = arena.allocate(D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC);
            heapDescription.fill((byte) 0);
            heapDescription.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_TYPE_OFFSET,
                    D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
            heapDescription.set(ValueLayout.JAVA_INT,
                    D3d12Layouts.D3D12_DESCRIPTOR_HEAP_DESC_NUM_DESCRIPTORS_OFFSET, BUFFER_COUNT);
            MemorySegment heapCell = D3d12Native.pointerCell(arena);
            int heapResult = D3d12FfmBindings.invokeId3d12DeviceCreateDescriptorHeapPointer(
                    D3d12Native.functionAt(
                            device,
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_DESCRIPTOR_HEAP_OFFSET
                    ),
                    device,
                    heapDescription,
                    D3d12Native.guid(arena, "8efb471d-616c-4f49-90f7-127bb763fa51"),
                    heapCell
            );
            D3d12Native.requireSuccess("ID3D12Device::CreateDescriptorHeap", heapResult);
            descriptorHeap = comReferences.own(
                    D3d12Native.requirePointer(heapCell, "ID3D12Device::CreateDescriptorHeap")
            );

            int descriptorIncrement = D3d12FfmBindings
                    .invokeId3d12DeviceGetDescriptorHandleIncrementSizePointer(
                            D3d12Native.functionAt(
                                    device,
                                    D3d12Layouts.ID3D12_DEVICE_VTABLE_GET_DESCRIPTOR_HANDLE_INCREMENT_SIZE_OFFSET
                            ),
                            device,
                            D3D12_DESCRIPTOR_HEAP_TYPE_RTV
                    );
            if (descriptorIncrement <= 0) {
                throw new IllegalStateException("D3D12 RTV descriptor increment is not positive: "
                        + Integer.toUnsignedString(descriptorIncrement));
            }
            renderTargetHandle0 = arena.allocate(D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE);
            MemorySegment returnedHandle = D3d12FfmBindings
                    .invokeId3d12DescriptorHeapGetCpuDescriptorHandleForHeapStartPointer(
                            D3d12Native.functionAt(
                                    descriptorHeap,
                                    D3d12Layouts
                                            .ID3D12_DESCRIPTOR_HEAP_VTABLE_GET_CPU_DESCRIPTOR_HANDLE_FOR_HEAP_START_OFFSET
                            ),
                            descriptorHeap,
                            renderTargetHandle0
                    );
            if (returnedHandle.address() != renderTargetHandle0.address()) {
                throw new IllegalStateException(
                        "ID3D12DescriptorHeap::GetCPUDescriptorHandleForHeapStart returned an unexpected pointer"
                );
            }
            renderTargetHandle1 = arena.allocate(D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE);
            long descriptorBase = renderTargetHandle0.get(
                    ValueLayout.JAVA_LONG,
                    D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE_POINTER_OFFSET
            );
            renderTargetHandle1.set(
                    ValueLayout.JAVA_LONG,
                    D3d12Layouts.D3D12_CPU_DESCRIPTOR_HANDLE_POINTER_OFFSET,
                    Math.addExact(descriptorBase, Integer.toUnsignedLong(descriptorIncrement))
            );

            backBuffer0 = acquireBackBuffer(0);
            backBuffer1 = acquireBackBuffer(1);
            createRenderTargetView(backBuffer0, renderTargetHandle0);
            createRenderTargetView(backBuffer1, renderTargetHandle1);
        }

        /// Acquires one owned swapchain resource.
        ///
        /// @param index the back-buffer index
        /// @return the owned `ID3D12Resource`
        private MemorySegment acquireBackBuffer(int index) {
            MemorySegment resultCell = D3d12Native.pointerCell(arena);
            int result = D3d12FfmBindings.invokeIdxgiSwapChainGetBufferPointer(
                    D3d12Native.functionAt(
                            swapChain,
                            D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_GET_BUFFER_OFFSET
                    ),
                    swapChain,
                    index,
                    D3d12Native.guid(arena, "696442be-a72e-4059-bc79-5b5c98040fad"),
                    resultCell
            );
            D3d12Native.requireSuccess("IDXGISwapChain::GetBuffer(" + index + ')', result);
            return comReferences.own(D3d12Native.requirePointer(resultCell, "IDXGISwapChain::GetBuffer"));
        }

        /// Creates one implicit-format render-target view.
        ///
        /// @param resource the owned swapchain resource
        /// @param handle the destination CPU descriptor handle value
        private void createRenderTargetView(MemorySegment resource, MemorySegment handle) {
            D3d12FfmBindings.invokeId3d12DeviceCreateRenderTargetViewPointer(
                    D3d12Native.functionAt(
                            device,
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_RENDER_TARGET_VIEW_OFFSET
                    ),
                    device,
                    resource,
                    MemorySegment.NULL,
                    handle
            );
        }

        /// Creates and initially closes one direct allocator/list pair.
        private void createCommandObjects() {
            MemorySegment allocatorCell = D3d12Native.pointerCell(arena);
            int allocatorResult = D3d12FfmBindings.invokeId3d12DeviceCreateCommandAllocatorPointer(
                    D3d12Native.functionAt(
                            device,
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMAND_ALLOCATOR_OFFSET
                    ),
                    device,
                    D3D12_COMMAND_LIST_TYPE_DIRECT,
                    D3d12Native.guid(arena, "6102dee4-af59-4b09-b999-b44d73f09b24"),
                    allocatorCell
            );
            D3d12Native.requireSuccess("ID3D12Device::CreateCommandAllocator", allocatorResult);
            commandAllocator = comReferences.own(
                    D3d12Native.requirePointer(allocatorCell, "ID3D12Device::CreateCommandAllocator")
            );

            MemorySegment listCell = D3d12Native.pointerCell(arena);
            int listResult = D3d12FfmBindings.invokeId3d12DeviceCreateCommandListPointer(
                    D3d12Native.functionAt(
                            device,
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMAND_LIST_OFFSET
                    ),
                    device,
                    0,
                    D3D12_COMMAND_LIST_TYPE_DIRECT,
                    commandAllocator,
                    MemorySegment.NULL,
                    D3d12Native.guid(arena, "5b160d0f-ac1b-4185-8ba8-b3ae42a5a455"),
                    listCell
            );
            D3d12Native.requireSuccess("ID3D12Device::CreateCommandList", listResult);
            commandList = comReferences.own(
                    D3d12Native.requirePointer(listCell, "ID3D12Device::CreateCommandList")
            );
            D3d12Native.requireSuccess(
                    "ID3D12GraphicsCommandList::Close(initial)",
                    D3d12FfmBindings.invokeId3d12GraphicsCommandListClosePointer(
                            D3d12Native.functionAt(
                                    commandList,
                                    D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_CLOSE_OFFSET
                            ),
                            commandList
                    )
            );
        }

        /// Creates a fence initially completed at zero.
        private void createFence() {
            MemorySegment resultCell = D3d12Native.pointerCell(arena);
            int result = D3d12FfmBindings.invokeId3d12DeviceCreateFencePointer(
                    D3d12Native.functionAt(device, D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_FENCE_OFFSET),
                    device,
                    0L,
                    0,
                    D3d12Native.guid(arena, "0a753dcf-c4d8-4b91-adf6-be5a60d95a76"),
                    resultCell
            );
            D3d12Native.requireSuccess("ID3D12Device::CreateFence", result);
            fence = comReferences.own(D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateFence"));
        }

        /// Creates the row-major readback buffer in `COPY_DEST` state.
        private void createReadbackResource() {
            MemorySegment heapProperties = arena.allocate(D3d12Layouts.D3D12_HEAP_PROPERTIES);
            heapProperties.fill((byte) 0);
            heapProperties.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_HEAP_PROPERTIES_TYPE_OFFSET,
                    D3D12_HEAP_TYPE_READBACK);
            heapProperties.set(ValueLayout.JAVA_INT,
                    D3d12Layouts.D3D12_HEAP_PROPERTIES_CREATION_NODE_MASK_OFFSET, 1);
            heapProperties.set(ValueLayout.JAVA_INT,
                    D3d12Layouts.D3D12_HEAP_PROPERTIES_VISIBLE_NODE_MASK_OFFSET, 1);

            MemorySegment description = arena.allocate(D3d12Layouts.D3D12_RESOURCE_DESC);
            description.fill((byte) 0);
            description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_DIMENSION_OFFSET,
                    D3D12_RESOURCE_DIMENSION_BUFFER);
            description.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RESOURCE_DESC_WIDTH_OFFSET,
                    (long) ROW_PITCH * HEIGHT);
            description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_HEIGHT_OFFSET, 1);
            description.set(ValueLayout.JAVA_SHORT,
                    D3d12Layouts.D3D12_RESOURCE_DESC_DEPTH_OR_ARRAY_SIZE_OFFSET, (short) 1);
            description.set(ValueLayout.JAVA_SHORT, D3d12Layouts.D3D12_RESOURCE_DESC_MIP_LEVELS_OFFSET,
                    (short) 1);
            description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_FORMAT_OFFSET,
                    DXGI_FORMAT_UNKNOWN);
            description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_SAMPLE_DESC_OFFSET
                    + D3d12Layouts.DXGI_SAMPLE_DESC_COUNT_OFFSET, 1);
            description.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_DESC_LAYOUT_OFFSET,
                    D3D12_TEXTURE_LAYOUT_ROW_MAJOR);

            MemorySegment resultCell = D3d12Native.pointerCell(arena);
            int result = D3d12FfmBindings.invokeId3d12DeviceCreateCommittedResourcePointer(
                    D3d12Native.functionAt(
                            device,
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_CREATE_COMMITTED_RESOURCE_OFFSET
                    ),
                    device,
                    heapProperties,
                    0,
                    description,
                    D3D12_RESOURCE_STATE_COPY_DEST,
                    MemorySegment.NULL,
                    D3d12Native.guid(arena, "696442be-a72e-4059-bc79-5b5c98040fad"),
                    resultCell
            );
            D3d12Native.requireSuccess("ID3D12Device::CreateCommittedResource(readback)", result);
            readback = comReferences.own(
                    D3d12Native.requirePointer(resultCell, "ID3D12Device::CreateCommittedResource")
            );
        }

        /// Allocates and initializes the native records reused by every frame.
        private void allocateReusableRecords() {
            barrier = arena.allocate(D3d12Layouts.D3D12_RESOURCE_BARRIER);
            sourceCopyLocation = arena.allocate(D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION);
            sourceCopyLocation.fill((byte) 0);
            sourceCopyLocation.set(
                    ValueLayout.JAVA_INT,
                    D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_TYPE_OFFSET,
                    D3D12_TEXTURE_COPY_TYPE_SUBRESOURCE_INDEX
            );
            sourceCopyLocation.set(
                    ValueLayout.JAVA_INT,
                    D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_PAYLOAD_OFFSET
                            + D3d12Layouts.D3D12_TEXTURE_COPY_PAYLOAD_SUBRESOURCE_INDEX_OFFSET,
                    0
            );

            destinationCopyLocation = arena.allocate(D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION);
            destinationCopyLocation.fill((byte) 0);
            destinationCopyLocation.set(
                    ValueLayout.ADDRESS,
                    D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_RESOURCE_OFFSET,
                    readback
            );
            destinationCopyLocation.set(
                    ValueLayout.JAVA_INT,
                    D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_TYPE_OFFSET,
                    D3D12_TEXTURE_COPY_TYPE_PLACED_FOOTPRINT
            );
            long footprintOffset = D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_PAYLOAD_OFFSET
                    + D3d12Layouts.D3D12_TEXTURE_COPY_PAYLOAD_PLACED_FOOTPRINT_OFFSET;
            destinationCopyLocation.set(
                    ValueLayout.JAVA_LONG,
                    footprintOffset + D3d12Layouts.D3D12_PLACED_SUBRESOURCE_FOOTPRINT_OFFSET_OFFSET,
                    0L
            );
            long subresourceFootprintOffset = footprintOffset
                    + D3d12Layouts.D3D12_PLACED_SUBRESOURCE_FOOTPRINT_FOOTPRINT_OFFSET;
            destinationCopyLocation.set(
                    ValueLayout.JAVA_INT,
                    subresourceFootprintOffset + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_FORMAT_OFFSET,
                    DXGI_FORMAT_R8G8B8A8_UNORM
            );
            destinationCopyLocation.set(
                    ValueLayout.JAVA_INT,
                    subresourceFootprintOffset + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_WIDTH_OFFSET,
                    WIDTH
            );
            destinationCopyLocation.set(
                    ValueLayout.JAVA_INT,
                    subresourceFootprintOffset + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_HEIGHT_OFFSET,
                    HEIGHT
            );
            destinationCopyLocation.set(
                    ValueLayout.JAVA_INT,
                    subresourceFootprintOffset + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_DEPTH_OFFSET,
                    1
            );
            destinationCopyLocation.set(
                    ValueLayout.JAVA_INT,
                    subresourceFootprintOffset + D3d12Layouts.D3D12_SUBRESOURCE_FOOTPRINT_ROW_PITCH_OFFSET,
                    ROW_PITCH
            );

            commandListArray = arena.allocate(ValueLayout.ADDRESS);
            commandListArray.set(ValueLayout.ADDRESS, 0L, commandList);
            clearColor = arena.allocate(MemoryLayout.sequenceLayout(4, ValueLayout.JAVA_FLOAT));
            clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 0L, CLEAR_RED / 255.0F);
            clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 1L, CLEAR_GREEN / 255.0F);
            clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 2L, CLEAR_BLUE / 255.0F);
            clearColor.setAtIndex(ValueLayout.JAVA_FLOAT, 3L, 1.0F);
            readRange = arena.allocate(D3d12Layouts.D3D12_RANGE);
            readRange.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RANGE_BEGIN_OFFSET, 0L);
            readRange.set(ValueLayout.JAVA_LONG, D3d12Layouts.D3D12_RANGE_END_OFFSET,
                    (long) ROW_PITCH * HEIGHT);
        }

        /// Presents all frames while distributing them across the requested soak interval.
        ///
        /// @param repetitions the frame count
        /// @param soakSeconds the minimum duration
        /// @param startedAt the monotonic start time
        /// @return the number of successful presentations
        private int renderFrames(int repetitions, int soakSeconds, long startedAt) {
            long soakNanos = Math.multiplyExact((long) soakSeconds, 1_000_000_000L);
            window.show();
            int presented = 0;
            try {
                for (int index = 0; index < repetitions; index++) {
                    long targetOffset = repetitions == 1
                            ? soakNanos
                            : Math.multiplyExact(soakNanos, index) / (repetitions - 1L);
                    waitUntil(Math.addExact(startedAt, targetOffset));
                    renderFrame();
                    presented++;
                    if (index == 0) {
                        window.hide();
                    }
                }
                if (!window.pumpMessages()) {
                    throw new IllegalStateException("Win32 surface message queue terminated during D3D12 run");
                }
                return presented;
            } finally {
                window.hide();
            }
        }

        /// Pumps window messages and parks in bounded slices until one monotonic deadline.
        ///
        /// @param targetNanos the absolute `System.nanoTime` deadline
        private void waitUntil(long targetNanos) {
            while (true) {
                if (!window.pumpMessages()) {
                    throw new IllegalStateException("Win32 surface message queue terminated during D3D12 soak");
                }
                long remaining = targetNanos - System.nanoTime();
                if (remaining <= 0L) {
                    return;
                }
                LockSupport.parkNanos(Math.min(remaining, 10_000_000L));
                if (Thread.interrupted()) {
                    throw new IllegalStateException("D3D12 conformance thread was interrupted");
                }
            }
        }

        /// Records, submits, presents, synchronizes, and reads back one clear frame.
        private void renderFrame() {
            int bufferIndex = D3d12FfmBindings.invokeIdxgiSwapChain3GetCurrentBackBufferIndexPointer(
                    D3d12Native.functionAt(
                            swapChain,
                            D3d12Layouts.IDXGI_SWAP_CHAIN3_VTABLE_GET_CURRENT_BACK_BUFFER_INDEX_OFFSET
                    ),
                    swapChain
            );
            if (bufferIndex < 0 || bufferIndex >= BUFFER_COUNT) {
                throw new IllegalStateException("IDXGISwapChain3 returned invalid back-buffer index "
                        + Integer.toUnsignedString(bufferIndex));
            }
            MemorySegment backBuffer = bufferIndex == 0 ? backBuffer0 : backBuffer1;
            MemorySegment renderTargetHandle = bufferIndex == 0 ? renderTargetHandle0 : renderTargetHandle1;

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
                    renderTargetHandle,
                    clearColor,
                    0,
                    MemorySegment.NULL
            );
            transition(backBuffer, D3D12_RESOURCE_STATE_RENDER_TARGET, D3D12_RESOURCE_STATE_COPY_SOURCE);
            sourceCopyLocation.set(
                    ValueLayout.ADDRESS,
                    D3d12Layouts.D3D12_TEXTURE_COPY_LOCATION_RESOURCE_OFFSET,
                    backBuffer
            );
            D3d12FfmBindings.invokeId3d12GraphicsCommandListCopyTextureRegionPointer(
                    D3d12Native.functionAt(
                            commandList,
                            D3d12Layouts.ID3D12_GRAPHICS_COMMAND_LIST_VTABLE_COPY_TEXTURE_REGION_OFFSET
                    ),
                    commandList,
                    destinationCopyLocation,
                    0,
                    0,
                    0,
                    sourceCopyLocation,
                    MemorySegment.NULL
            );
            transition(backBuffer, D3D12_RESOURCE_STATE_COPY_SOURCE, D3D12_RESOURCE_STATE_PRESENT);
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
            int removedReason = deviceRemovedReason();
            if (removedReason != 0) {
                throw D3d12Native.hresultFailure("ID3D12Device::GetDeviceRemovedReason", removedReason);
            }
            maximumChannelDelta = Math.max(maximumChannelDelta, verifyReadback());
        }

        /// Records one transition barrier in the active command list.
        ///
        /// @param resource the transitioning resource
        /// @param stateBefore the declared current state
        /// @param stateAfter the required next state
        private void transition(MemorySegment resource, int stateBefore, int stateAfter) {
            barrier.fill((byte) 0);
            barrier.set(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_RESOURCE_BARRIER_TYPE_OFFSET,
                    D3D12_RESOURCE_BARRIER_TYPE_TRANSITION);
            long transitionOffset = D3d12Layouts.D3D12_RESOURCE_BARRIER_TRANSITION_OFFSET;
            barrier.set(ValueLayout.ADDRESS,
                    transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_RESOURCE_OFFSET,
                    resource);
            barrier.set(ValueLayout.JAVA_INT,
                    transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_SUBRESOURCE_OFFSET,
                    D3D12_RESOURCE_BARRIER_ALL_SUBRESOURCES);
            barrier.set(ValueLayout.JAVA_INT,
                    transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_STATE_BEFORE_OFFSET,
                    stateBefore);
            barrier.set(ValueLayout.JAVA_INT,
                    transitionOffset + D3d12Layouts.D3D12_RESOURCE_TRANSITION_BARRIER_STATE_AFTER_OFFSET,
                    stateAfter);
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

        /// Waits until the direct queue completes one fence value.
        ///
        /// @param expectedValue the unsigned fence value
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
                if (completed == -1L) {
                    int removedReason = deviceRemovedReason();
                    throw D3d12Native.hresultFailure("D3D12 fence device removal", removedReason);
                }
                if (Long.compareUnsigned(completed, expectedValue) >= 0) {
                    return;
                }
                if (System.nanoTime() - startedAt >= FENCE_TIMEOUT_NANOS) {
                    throw new IllegalStateException("Timed out waiting for D3D12 fence "
                            + Long.toUnsignedString(expectedValue) + "; completed="
                            + Long.toUnsignedString(completed));
                }
                LockSupport.parkNanos(100_000L);
                if (Thread.interrupted()) {
                    throw new IllegalStateException("D3D12 fence wait was interrupted");
                }
            }
        }

        /// Maps and checks every RGBA byte in the readback buffer.
        ///
        /// @return the maximum observed channel delta, always zero on success
        private int verifyReadback() {
            MemorySegment dataCell = D3d12Native.pointerCell(arena);
            int result = D3d12FfmBindings.invokeId3d12ResourceMapPointer(
                    D3d12Native.functionAt(readback, D3d12Layouts.ID3D12_RESOURCE_VTABLE_MAP_OFFSET),
                    readback,
                    0,
                    readRange,
                    dataCell
            );
            D3d12Native.requireSuccess("ID3D12Resource::Map(readback)", result);
            MemorySegment mapped = D3d12Native.requirePointer(dataCell, "ID3D12Resource::Map")
                    .reinterpret((long) ROW_PITCH * HEIGHT);
            int maximumDelta = 0;
            try {
                for (int y = 0; y < HEIGHT; y++) {
                    long rowOffset = (long) y * ROW_PITCH;
                    for (int x = 0; x < WIDTH; x++) {
                        long pixelOffset = rowOffset + (long) x * BYTES_PER_PIXEL;
                        maximumDelta = Math.max(maximumDelta,
                                channelDelta(mapped, pixelOffset, CLEAR_RED));
                        maximumDelta = Math.max(maximumDelta,
                                channelDelta(mapped, pixelOffset + 1L, CLEAR_GREEN));
                        maximumDelta = Math.max(maximumDelta,
                                channelDelta(mapped, pixelOffset + 2L, CLEAR_BLUE));
                        maximumDelta = Math.max(maximumDelta,
                                channelDelta(mapped, pixelOffset + 3L, CLEAR_ALPHA));
                    }
                }
            } finally {
                D3d12FfmBindings.invokeId3d12ResourceUnmapPointer(
                        D3d12Native.functionAt(readback, D3d12Layouts.ID3D12_RESOURCE_VTABLE_UNMAP_OFFSET),
                        readback,
                        0,
                        MemorySegment.NULL
                );
            }
            if (maximumDelta != 0) {
                throw new IllegalStateException("D3D12 clear readback differs from the exact fixture; max delta="
                        + maximumDelta);
            }
            return maximumDelta;
        }

        /// Computes one unsigned-byte delta from its expected channel value.
        ///
        /// @param data the mapped readback memory
        /// @param byteOffset the channel byte offset
        /// @param expected the expected unsigned byte
        /// @return the absolute unsigned-byte delta
        private static int channelDelta(MemorySegment data, long byteOffset, int expected) {
            int observed = Byte.toUnsignedInt(data.get(ValueLayout.JAVA_BYTE, byteOffset));
            return Math.abs(observed - expected);
        }

        /// Returns the current device-removal status.
        ///
        /// @return `S_OK` or the failing removal `HRESULT`
        private int deviceRemovedReason() {
            return D3d12FfmBindings.invokeId3d12DeviceGetDeviceRemovedReasonPointer(
                    D3d12Native.functionAt(
                            device,
                            D3d12Layouts.ID3D12_DEVICE_VTABLE_GET_DEVICE_REMOVED_REASON_OFFSET
                    ),
                    device
            );
        }

        /// Copies all messages currently allowed by the debug queue's retrieval filter.
        ///
        /// @return an immutable message snapshot, or an empty list when no queue is available
        private @Unmodifiable List<D3d12DebugMessage> readDebugMessages() {
            if (infoQueue.address() == 0L) {
                return List.of();
            }
            long count = D3d12FfmBindings
                    .invokeId3d12InfoQueueGetNumStoredMessagesAllowedByRetrievalFilterPointer(
                            D3d12Native.functionAt(
                                    infoQueue,
                                    D3d12Layouts
                                            .ID3D12_INFO_QUEUE_VTABLE_GET_NUM_STORED_MESSAGES_ALLOWED_BY_RETRIEVAL_FILTER_OFFSET
                            ),
                            infoQueue
                    );
            if (Long.compareUnsigned(count, MAX_DEBUG_MESSAGES) > 0) {
                throw new IllegalStateException("D3D12 debug queue exceeds bounded message count: "
                        + Long.toUnsignedString(count));
            }
            ArrayList<D3d12DebugMessage> messages = new ArrayList<>(Math.toIntExact(count));
            try (Arena messageArena = Arena.ofConfined()) {
                for (long index = 0L; Long.compareUnsigned(index, count) < 0; index++) {
                    MemorySegment sizeCell = messageArena.allocate(ValueLayout.JAVA_LONG);
                    sizeCell.set(ValueLayout.JAVA_LONG, 0L, 0L);
                    int sizeResult = D3d12FfmBindings.invokeId3d12InfoQueueGetMessagePointer(
                            D3d12Native.functionAt(
                                    infoQueue,
                                    D3d12Layouts.ID3D12_INFO_QUEUE_VTABLE_GET_MESSAGE_OFFSET
                            ),
                            infoQueue,
                            index,
                            MemorySegment.NULL,
                            sizeCell
                    );
                    D3d12Native.requireSuccess("ID3D12InfoQueue::GetMessage(size)", sizeResult);
                    long messageBytes = sizeCell.get(ValueLayout.JAVA_LONG, 0L);
                    if (messageBytes < D3d12Layouts.D3D12_MESSAGE.byteSize()
                            || Long.compareUnsigned(messageBytes, MAX_DEBUG_MESSAGE_BYTES) > 0) {
                        throw new IllegalStateException("D3D12 debug message has invalid byte size "
                                + Long.toUnsignedString(messageBytes));
                    }
                    MemorySegment message = messageArena.allocate(messageBytes, D3d12Layouts.D3D12_MESSAGE.byteAlignment());
                    int messageResult = D3d12FfmBindings.invokeId3d12InfoQueueGetMessagePointer(
                            D3d12Native.functionAt(
                                    infoQueue,
                                    D3d12Layouts.ID3D12_INFO_QUEUE_VTABLE_GET_MESSAGE_OFFSET
                            ),
                            infoQueue,
                            index,
                            message,
                            sizeCell
                    );
                    D3d12Native.requireSuccess("ID3D12InfoQueue::GetMessage", messageResult);
                    int category = message.get(
                            ValueLayout.JAVA_INT,
                            D3d12Layouts.D3D12_MESSAGE_CATEGORY_OFFSET
                    );
                    int severity = message.get(
                            ValueLayout.JAVA_INT,
                            D3d12Layouts.D3D12_MESSAGE_SEVERITY_OFFSET
                    );
                    int id = message.get(ValueLayout.JAVA_INT, D3d12Layouts.D3D12_MESSAGE_ID_OFFSET);
                    long descriptionBytes = message.get(
                            ValueLayout.JAVA_LONG,
                            D3d12Layouts.D3D12_MESSAGE_DESCRIPTION_BYTE_LENGTH_OFFSET
                    );
                    MemorySegment description = message.get(
                            ValueLayout.ADDRESS,
                            D3d12Layouts.D3D12_MESSAGE_DESCRIPTION_OFFSET
                    );
                    messages.add(new D3d12DebugMessage(
                            category,
                            severity,
                            severityName(severity),
                            id,
                            copyDescription(description, descriptionBytes)
                    ));
                }
            }
            return List.copyOf(messages);
        }

        /// Copies one bounded native UTF-8 debug description.
        ///
        /// @param address the borrowed description address
        /// @param byteLength the native byte length, including a possible trailing zero
        /// @return the detached Java string
        private static String copyDescription(MemorySegment address, long byteLength) {
            if (byteLength == 0L) {
                return "";
            }
            if (address.address() == 0L || Long.compareUnsigned(byteLength, MAX_DEBUG_MESSAGE_BYTES) > 0) {
                throw new IllegalStateException("D3D12 debug description is null or unbounded");
            }
            byte[] bytes = address.reinterpret(byteLength).toArray(ValueLayout.JAVA_BYTE);
            int contentLength = bytes.length;
            if (contentLength > 0 && bytes[contentLength - 1] == 0) {
                contentLength--;
            }
            return new String(bytes, 0, contentLength, StandardCharsets.UTF_8);
        }

        /// Maps one native debug severity to a stable name.
        ///
        /// @param severity the native severity
        /// @return the stable name
        private static String severityName(int severity) {
            return switch (severity) {
                case 0 -> "CORRUPTION";
                case 1 -> "ERROR";
                case 2 -> "WARNING";
                case 3 -> "INFO";
                case 4 -> "MESSAGE";
                default -> "UNKNOWN_" + severity;
            };
        }

        /// Queries one COM interface and registers the returned reference when present.
        ///
        /// @param source the borrowed source interface
        /// @param interfaceId the requested interface UUID
        /// @param name the diagnostic interface name
        /// @param required whether absence must fail the scenario
        /// @return the owned interface or `NULL` when an optional interface is unavailable
        private MemorySegment queryInterface(
                MemorySegment source,
                String interfaceId,
                String name,
                boolean required
        ) {
            MemorySegment resultCell = D3d12Native.pointerCell(arena);
            int result = D3d12FfmBindings.invokeIunknownQueryInterfacePointer(
                    D3d12Native.functionAt(source, D3d12Native.QUERY_INTERFACE_OFFSET),
                    source,
                    D3d12Native.guid(arena, interfaceId),
                    resultCell
            );
            if (result < 0) {
                if (required) {
                    throw D3d12Native.hresultFailure("IUnknown::QueryInterface(" + name + ')', result);
                }
                return MemorySegment.NULL;
            }
            return comReferences.own(D3d12Native.requirePointer(resultCell, "QueryInterface(" + name + ')'));
        }

        /// Releases every currently owned COM reference in reverse acquisition order.
        private void closeComReferences() {
            comReferences.close();
        }
    }
}
