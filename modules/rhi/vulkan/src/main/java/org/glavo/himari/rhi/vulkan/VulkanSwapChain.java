package org.glavo.himari.rhi.vulkan;

import org.glavo.himari.rhi.vulkan.generated.VulkanFfmBindings;
import org.glavo.himari.rhi.vulkan.generated.VulkanLayouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Creates a swapchain, clears one image, and presents once through generated FFM bindings.
@SuppressWarnings("restricted")
@NotNullByDefault
final class VulkanSwapChain {
    /// `VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR`.
    private static final int VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR = 1_000_001_000;

    /// `VK_STRUCTURE_TYPE_PRESENT_INFO_KHR`.
    private static final int VK_STRUCTURE_TYPE_PRESENT_INFO_KHR = 1_000_001_001;

    /// `VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO`.
    private static final int VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO = 39;

    /// `VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO`.
    private static final int VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO = 40;

    /// `VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO`.
    private static final int VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO = 42;

    /// `VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER`.
    private static final int VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER = 45;

    /// `VK_STRUCTURE_TYPE_SUBMIT_INFO`.
    private static final int VK_STRUCTURE_TYPE_SUBMIT_INFO = 4;

    /// `VK_STRUCTURE_TYPE_FENCE_CREATE_INFO`.
    private static final int VK_STRUCTURE_TYPE_FENCE_CREATE_INFO = 8;

    /// `VK_FORMAT_B8G8R8A8_UNORM`.
    private static final int VK_FORMAT_B8G8R8A8_UNORM = 44;

    /// `VK_FORMAT_R8G8B8A8_UNORM`.
    private static final int VK_FORMAT_R8G8B8A8_UNORM = 37;

    /// `VK_COLOR_SPACE_SRGB_NONLINEAR_KHR`.
    private static final int VK_COLOR_SPACE_SRGB_NONLINEAR_KHR = 0;

    /// `VK_PRESENT_MODE_FIFO_KHR`.
    private static final int VK_PRESENT_MODE_FIFO_KHR = 2;

    /// `VK_PRESENT_MODE_IMMEDIATE_KHR`.
    private static final int VK_PRESENT_MODE_IMMEDIATE_KHR = 0;

    /// `VK_IMAGE_USAGE_TRANSFER_DST_BIT`.
    private static final int VK_IMAGE_USAGE_TRANSFER_DST_BIT = 0x0000_0002;

    /// `VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR`.
    private static final int VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR = 0x0000_0001;

    /// `VK_IMAGE_LAYOUT_UNDEFINED`.
    private static final int VK_IMAGE_LAYOUT_UNDEFINED = 0;

    /// `VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL`.
    private static final int VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL = 7;

    /// `VK_IMAGE_LAYOUT_PRESENT_SRC_KHR`.
    private static final int VK_IMAGE_LAYOUT_PRESENT_SRC_KHR = 1_000_001_002;

    /// `VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT`.
    private static final int VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT = 0x0000_0001;

    /// `VK_PIPELINE_STAGE_TRANSFER_BIT`.
    private static final int VK_PIPELINE_STAGE_TRANSFER_BIT = 0x0000_1000;

    /// `VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT`.
    private static final int VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT = 0x0000_2000;

    /// `VK_ACCESS_TRANSFER_WRITE_BIT`.
    private static final int VK_ACCESS_TRANSFER_WRITE_BIT = 0x0000_1000;

    /// `VK_ACCESS_MEMORY_READ_BIT`.
    private static final int VK_ACCESS_MEMORY_READ_BIT = 0x0000_8000;

    /// `VK_IMAGE_ASPECT_COLOR_BIT`.
    private static final int VK_IMAGE_ASPECT_COLOR_BIT = 0x0000_0001;

    /// `VK_QUEUE_FAMILY_IGNORED`.
    private static final int VK_QUEUE_FAMILY_IGNORED = 0xFFFF_FFFF;

    /// `VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT`.
    private static final int VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT = 0x0000_0001;

    /// `VK_SUCCESS`.
    private static final int VK_SUCCESS = 0;

    /// `VK_SUBOPTIMAL_KHR`.
    private static final int VK_SUBOPTIMAL_KHR = 1_000_001_003;

    /// Prevents instantiation.
    private VulkanSwapChain() {
    }

    /// Clears one swapchain image to the deterministic SDR color and presents it.
    ///
    /// @param owner the device owner
    /// @param width the requested width in pixels
    /// @param height the requested height in pixels
    /// @return the present observation
    static VulkanPresentation presentSdr(VulkanDevice owner, int width, int height) {
        Objects.requireNonNull(owner, "owner");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Swapchain dimensions must be positive");
        }
        VulkanFfmBindings bindings = owner.bindings();
        Arena arena = owner.arena();
        long surface = owner.surfaceHandle();
        MemorySegment physical = owner.physicalDevice();
        MemorySegment device = owner.logicalDevice();
        MemorySegment queue = owner.queue();
        int family = owner.graphicsQueueFamily();
        MemorySegment supported = writeInt(arena, 0);
        requireSuccess(
                "vkGetPhysicalDeviceSurfaceSupportKHR",
                bindings.vkGetPhysicalDeviceSurfaceSupportKHR(physical, family, surface, supported)
        );
        if (supported.get(ValueLayout.JAVA_INT, 0L) == 0) {
            throw new IllegalStateException("Queue family " + family + " cannot present to the Win32 surface");
        }
        MemorySegment capabilities = arena.allocate(VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR);
        requireSuccess(
                "vkGetPhysicalDeviceSurfaceCapabilitiesKHR",
                bindings.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physical, surface, capabilities)
        );
        int currentWidth = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_CURRENT_EXTENT_OFFSET
                        + VulkanLayouts.VK_EXTENT2D_WIDTH_OFFSET
        );
        int currentHeight = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_CURRENT_EXTENT_OFFSET
                        + VulkanLayouts.VK_EXTENT2D_HEIGHT_OFFSET
        );
        int minWidth = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_MIN_IMAGE_EXTENT_OFFSET
                        + VulkanLayouts.VK_EXTENT2D_WIDTH_OFFSET
        );
        int minHeight = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_MIN_IMAGE_EXTENT_OFFSET
                        + VulkanLayouts.VK_EXTENT2D_HEIGHT_OFFSET
        );
        int maxWidth = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_MAX_IMAGE_EXTENT_OFFSET
                        + VulkanLayouts.VK_EXTENT2D_WIDTH_OFFSET
        );
        int maxHeight = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_MAX_IMAGE_EXTENT_OFFSET
                        + VulkanLayouts.VK_EXTENT2D_HEIGHT_OFFSET
        );
        int extentWidth = chooseExtent(currentWidth, minWidth, maxWidth, width);
        int extentHeight = chooseExtent(currentHeight, minHeight, maxHeight, height);
        int minImages = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_MIN_IMAGE_COUNT_OFFSET
        );
        int maxImages = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_MAX_IMAGE_COUNT_OFFSET
        );
        int imageCount = minImages + 1;
        if (maxImages > 0 && imageCount > maxImages) {
            imageCount = maxImages;
        }
        int usage = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_SUPPORTED_USAGE_FLAGS_OFFSET
        );
        if ((usage & VK_IMAGE_USAGE_TRANSFER_DST_BIT) == 0) {
            throw new IllegalStateException("Surface does not support TRANSFER_DST usage required for SDR clear");
        }
        int transform = capabilities.get(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SURFACE_CAPABILITIES_KHR_CURRENT_TRANSFORM_OFFSET
        );
        SurfaceFormat format = chooseFormat(bindings, arena, physical, surface);
        int presentMode = choosePresentMode(bindings, arena, physical, surface);
        MemorySegment createInfo = arena.allocate(VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR);
        createInfo.fill((byte) 0);
        createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_S_TYPE_OFFSET,
                VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
        createInfo.set(ValueLayout.JAVA_LONG, VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_SURFACE_OFFSET, surface);
        createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_MIN_IMAGE_COUNT_OFFSET, imageCount);
        createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_IMAGE_FORMAT_OFFSET, format.format());
        createInfo.set(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_IMAGE_COLOR_SPACE_OFFSET,
                format.colorSpace()
        );
        createInfo.set(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_IMAGE_EXTENT_OFFSET + VulkanLayouts.VK_EXTENT2D_WIDTH_OFFSET,
                extentWidth
        );
        createInfo.set(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_IMAGE_EXTENT_OFFSET + VulkanLayouts.VK_EXTENT2D_HEIGHT_OFFSET,
                extentHeight
        );
        createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_IMAGE_ARRAY_LAYERS_OFFSET, 1);
        createInfo.set(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_IMAGE_USAGE_OFFSET,
                VK_IMAGE_USAGE_TRANSFER_DST_BIT
        );
        createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_PRE_TRANSFORM_OFFSET, transform);
        createInfo.set(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_COMPOSITE_ALPHA_OFFSET,
                VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR
        );
        createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_PRESENT_MODE_OFFSET, presentMode);
        createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SWAPCHAIN_CREATE_INFO_KHR_CLIPPED_OFFSET, 1);
        MemorySegment swapchainCell = arena.allocate(ValueLayout.JAVA_LONG);
        swapchainCell.set(ValueLayout.JAVA_LONG, 0L, 0L);
        requireSuccess(
                "vkCreateSwapchainKHR",
                bindings.vkCreateSwapchainKHR(device, createInfo, MemorySegment.NULL, swapchainCell)
        );
        long swapchain = swapchainCell.get(ValueLayout.JAVA_LONG, 0L);
        if (swapchain == 0L) {
            throw new IllegalStateException("vkCreateSwapchainKHR returned VK_NULL_HANDLE");
        }
        long fence = 0L;
        long commandPool = 0L;
        try {
            MemorySegment countCell = writeInt(arena, 0);
            requireSuccess(
                    "vkGetSwapchainImagesKHR",
                    bindings.vkGetSwapchainImagesKHR(device, swapchain, countCell, MemorySegment.NULL)
            );
            int actualCount = countCell.get(ValueLayout.JAVA_INT, 0L);
            if (actualCount <= 0) {
                throw new IllegalStateException("Swapchain reported no images");
            }
            MemorySegment images = arena.allocate(ValueLayout.JAVA_LONG, actualCount);
            requireSuccess(
                    "vkGetSwapchainImagesKHR(handles)",
                    bindings.vkGetSwapchainImagesKHR(device, swapchain, countCell, images)
            );
            MemorySegment fenceInfo = arena.allocate(VulkanLayouts.VK_FENCE_CREATE_INFO);
            fenceInfo.fill((byte) 0);
            fenceInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_FENCE_CREATE_INFO_S_TYPE_OFFSET,
                    VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            MemorySegment fenceCell = arena.allocate(ValueLayout.JAVA_LONG);
            fenceCell.set(ValueLayout.JAVA_LONG, 0L, 0L);
            requireSuccess(
                    "vkCreateFence",
                    bindings.vkCreateFence(device, fenceInfo, MemorySegment.NULL, fenceCell)
            );
            fence = fenceCell.get(ValueLayout.JAVA_LONG, 0L);
            MemorySegment imageIndexCell = writeInt(arena, 0);
            int acquired = bindings.vkAcquireNextImageKHR(
                    device,
                    swapchain,
                    0xFFFF_FFFF_FFFF_FFFFL,
                    0L,
                    fence,
                    imageIndexCell
            );
            if (acquired != VK_SUCCESS && acquired != VK_SUBOPTIMAL_KHR) {
                throw new IllegalStateException("vkAcquireNextImageKHR failed with VkResult " + acquired);
            }
            waitFence(bindings, device, arena, fence);
            requireSuccess("vkResetFences", bindings.vkResetFences(device, 1, fenceCell));
            int imageIndex = imageIndexCell.get(ValueLayout.JAVA_INT, 0L);
            long image = images.getAtIndex(ValueLayout.JAVA_LONG, imageIndex);
            MemorySegment poolInfo = arena.allocate(VulkanLayouts.VK_COMMAND_POOL_CREATE_INFO);
            poolInfo.fill((byte) 0);
            poolInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_COMMAND_POOL_CREATE_INFO_S_TYPE_OFFSET,
                    VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_COMMAND_POOL_CREATE_INFO_FLAGS_OFFSET, 2);
            poolInfo.set(
                    ValueLayout.JAVA_INT,
                    VulkanLayouts.VK_COMMAND_POOL_CREATE_INFO_QUEUE_FAMILY_INDEX_OFFSET,
                    family
            );
            MemorySegment poolCell = arena.allocate(ValueLayout.JAVA_LONG);
            poolCell.set(ValueLayout.JAVA_LONG, 0L, 0L);
            requireSuccess(
                    "vkCreateCommandPool",
                    bindings.vkCreateCommandPool(device, poolInfo, MemorySegment.NULL, poolCell)
            );
            commandPool = poolCell.get(ValueLayout.JAVA_LONG, 0L);
            MemorySegment allocateInfo = arena.allocate(VulkanLayouts.VK_COMMAND_BUFFER_ALLOCATE_INFO);
            allocateInfo.fill((byte) 0);
            allocateInfo.set(
                    ValueLayout.JAVA_INT,
                    VulkanLayouts.VK_COMMAND_BUFFER_ALLOCATE_INFO_S_TYPE_OFFSET,
                    VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO
            );
            allocateInfo.set(
                    ValueLayout.JAVA_LONG,
                    VulkanLayouts.VK_COMMAND_BUFFER_ALLOCATE_INFO_COMMAND_POOL_OFFSET,
                    commandPool
            );
            allocateInfo.set(
                    ValueLayout.JAVA_INT,
                    VulkanLayouts.VK_COMMAND_BUFFER_ALLOCATE_INFO_COMMAND_BUFFER_COUNT_OFFSET,
                    1
            );
            MemorySegment commandCell = arena.allocate(ValueLayout.ADDRESS);
            commandCell.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            requireSuccess(
                    "vkAllocateCommandBuffers",
                    bindings.vkAllocateCommandBuffers(device, allocateInfo, commandCell)
            );
            MemorySegment commandBuffer = commandCell.get(ValueLayout.ADDRESS, 0L);
            MemorySegment beginInfo = arena.allocate(VulkanLayouts.VK_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.fill((byte) 0);
            beginInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_COMMAND_BUFFER_BEGIN_INFO_S_TYPE_OFFSET,
                    VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.set(
                    ValueLayout.JAVA_INT,
                    VulkanLayouts.VK_COMMAND_BUFFER_BEGIN_INFO_FLAGS_OFFSET,
                    VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT
            );
            requireSuccess("vkBeginCommandBuffer", bindings.vkBeginCommandBuffer(commandBuffer, beginInfo));
            barrier(
                    bindings,
                    arena,
                    commandBuffer,
                    image,
                    VK_IMAGE_LAYOUT_UNDEFINED,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    0,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT
            );
            MemorySegment color = arena.allocate(VulkanLayouts.VK_CLEAR_COLOR_VALUE);
            color.set(ValueLayout.JAVA_FLOAT, VulkanLayouts.VK_CLEAR_COLOR_VALUE_FLOAT32_0_OFFSET, 17.0f / 255.0f);
            color.set(ValueLayout.JAVA_FLOAT, VulkanLayouts.VK_CLEAR_COLOR_VALUE_FLOAT32_1_OFFSET, 83.0f / 255.0f);
            color.set(ValueLayout.JAVA_FLOAT, VulkanLayouts.VK_CLEAR_COLOR_VALUE_FLOAT32_2_OFFSET, 149.0f / 255.0f);
            color.set(ValueLayout.JAVA_FLOAT, VulkanLayouts.VK_CLEAR_COLOR_VALUE_FLOAT32_3_OFFSET, 1.0f);
            MemorySegment range = colorRange(arena);
            bindings.vkCmdClearColorImage(
                    commandBuffer,
                    image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    color,
                    1,
                    range
            );
            barrier(
                    bindings,
                    arena,
                    commandBuffer,
                    image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                    VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_ACCESS_MEMORY_READ_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT
            );
            requireSuccess("vkEndCommandBuffer", bindings.vkEndCommandBuffer(commandBuffer));
            MemorySegment submit = arena.allocate(VulkanLayouts.VK_SUBMIT_INFO);
            submit.fill((byte) 0);
            submit.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SUBMIT_INFO_S_TYPE_OFFSET, VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submit.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_SUBMIT_INFO_COMMAND_BUFFER_COUNT_OFFSET, 1);
            submit.set(ValueLayout.ADDRESS, VulkanLayouts.VK_SUBMIT_INFO_COMMAND_BUFFERS_OFFSET, commandCell);
            requireSuccess("vkQueueSubmit", bindings.vkQueueSubmit(queue, 1, submit, fence));
            waitFence(bindings, device, arena, fence);
            MemorySegment swapchainCell2 = arena.allocate(ValueLayout.JAVA_LONG);
            swapchainCell2.set(ValueLayout.JAVA_LONG, 0L, swapchain);
            MemorySegment present = arena.allocate(VulkanLayouts.VK_PRESENT_INFO_KHR);
            present.fill((byte) 0);
            present.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_PRESENT_INFO_KHR_S_TYPE_OFFSET,
                    VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            present.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_PRESENT_INFO_KHR_SWAPCHAIN_COUNT_OFFSET, 1);
            present.set(ValueLayout.ADDRESS, VulkanLayouts.VK_PRESENT_INFO_KHR_SWAPCHAINS_OFFSET, swapchainCell2);
            present.set(ValueLayout.ADDRESS, VulkanLayouts.VK_PRESENT_INFO_KHR_IMAGE_INDICES_OFFSET, imageIndexCell);
            int presented = bindings.vkQueuePresentKHR(queue, present);
            if (presented != VK_SUCCESS && presented != VK_SUBOPTIMAL_KHR) {
                throw new IllegalStateException("vkQueuePresentKHR failed with VkResult " + presented);
            }
            bindings.vkQueueWaitIdle(queue);
            return new VulkanPresentation(
                    true,
                    true,
                    true,
                    imageIndex,
                    actualCount,
                    format.name(),
                    "VK_COLOR_SPACE_SRGB_NONLINEAR_KHR",
                    presentMode == VK_PRESENT_MODE_FIFO_KHR
                            ? "VK_PRESENT_MODE_FIFO_KHR"
                            : "VK_PRESENT_MODE_IMMEDIATE_KHR",
                    false
            );
        } finally {
            if (commandPool != 0L) {
                bindings.vkDestroyCommandPool(device, commandPool, MemorySegment.NULL);
            }
            if (fence != 0L) {
                bindings.vkDestroyFence(device, fence, MemorySegment.NULL);
            }
            bindings.vkDestroySwapchainKHR(device, swapchain, MemorySegment.NULL);
        }
    }

    /// Records one image layout transition.
    private static void barrier(
            VulkanFfmBindings bindings,
            Arena arena,
            MemorySegment commandBuffer,
            long image,
            int oldLayout,
            int newLayout,
            int srcAccess,
            int dstAccess,
            int srcStage,
            int dstStage
    ) {
        MemorySegment barrier = arena.allocate(VulkanLayouts.VK_IMAGE_MEMORY_BARRIER);
        barrier.fill((byte) 0);
        barrier.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_S_TYPE_OFFSET,
                VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
        barrier.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_SRC_ACCESS_MASK_OFFSET, srcAccess);
        barrier.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_DST_ACCESS_MASK_OFFSET, dstAccess);
        barrier.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_OLD_LAYOUT_OFFSET, oldLayout);
        barrier.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_NEW_LAYOUT_OFFSET, newLayout);
        barrier.set(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_SRC_QUEUE_FAMILY_INDEX_OFFSET,
                VK_QUEUE_FAMILY_IGNORED
        );
        barrier.set(
                ValueLayout.JAVA_INT,
                VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_DST_QUEUE_FAMILY_INDEX_OFFSET,
                VK_QUEUE_FAMILY_IGNORED
        );
        barrier.set(ValueLayout.JAVA_LONG, VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_IMAGE_OFFSET, image);
        MemorySegment range = colorRange(arena);
        barrier.asSlice(
                VulkanLayouts.VK_IMAGE_MEMORY_BARRIER_SUBRESOURCE_RANGE_OFFSET,
                VulkanLayouts.VK_IMAGE_SUBRESOURCE_RANGE.byteSize()
        ).copyFrom(range);
        bindings.vkCmdPipelineBarrier(
                commandBuffer,
                srcStage,
                dstStage,
                0,
                0,
                MemorySegment.NULL,
                0,
                MemorySegment.NULL,
                1,
                barrier
        );
    }

    /// Builds a color-aspect subresource range covering the whole image.
    private static MemorySegment colorRange(Arena arena) {
        MemorySegment range = arena.allocate(VulkanLayouts.VK_IMAGE_SUBRESOURCE_RANGE);
        range.fill((byte) 0);
        range.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_IMAGE_SUBRESOURCE_RANGE_ASPECT_MASK_OFFSET,
                VK_IMAGE_ASPECT_COLOR_BIT);
        range.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_IMAGE_SUBRESOURCE_RANGE_LEVEL_COUNT_OFFSET, 1);
        range.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_IMAGE_SUBRESOURCE_RANGE_LAYER_COUNT_OFFSET, 1);
        return range;
    }

    /// Waits for one fence.
    private static void waitFence(VulkanFfmBindings bindings, MemorySegment device, Arena arena, long fence) {
        MemorySegment fenceCell = arena.allocate(ValueLayout.JAVA_LONG);
        fenceCell.set(ValueLayout.JAVA_LONG, 0L, fence);
        requireSuccess(
                "vkWaitForFences",
                bindings.vkWaitForFences(device, 1, fenceCell, 1, 30_000_000_000L)
        );
    }

    /// Selects an SDR UNORM surface format.
    private static SurfaceFormat chooseFormat(
            VulkanFfmBindings bindings,
            Arena arena,
            MemorySegment physical,
            long surface
    ) {
        MemorySegment countCell = writeInt(arena, 0);
        requireSuccess(
                "vkGetPhysicalDeviceSurfaceFormatsKHR",
                bindings.vkGetPhysicalDeviceSurfaceFormatsKHR(physical, surface, countCell, MemorySegment.NULL)
        );
        int count = countCell.get(ValueLayout.JAVA_INT, 0L);
        if (count <= 0) {
            throw new IllegalStateException("Surface reported no formats");
        }
        MemorySegment formats = arena.allocate(VulkanLayouts.VK_SURFACE_FORMAT_KHR, count);
        requireSuccess(
                "vkGetPhysicalDeviceSurfaceFormatsKHR(records)",
                bindings.vkGetPhysicalDeviceSurfaceFormatsKHR(physical, surface, countCell, formats)
        );
        long stride = VulkanLayouts.VK_SURFACE_FORMAT_KHR.byteSize();
        for (int index = 0; index < count; index++) {
            MemorySegment record = formats.asSlice(index * stride);
            int format = record.get(ValueLayout.JAVA_INT, VulkanLayouts.VK_SURFACE_FORMAT_KHR_FORMAT_OFFSET);
            int colorSpace = record.get(ValueLayout.JAVA_INT, VulkanLayouts.VK_SURFACE_FORMAT_KHR_COLOR_SPACE_OFFSET);
            if (colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
                    && (format == VK_FORMAT_B8G8R8A8_UNORM || format == VK_FORMAT_R8G8B8A8_UNORM)) {
                return new SurfaceFormat(format, colorSpace, formatName(format));
            }
        }
        MemorySegment first = formats.asSlice(0);
        int format = first.get(ValueLayout.JAVA_INT, VulkanLayouts.VK_SURFACE_FORMAT_KHR_FORMAT_OFFSET);
        int colorSpace = first.get(ValueLayout.JAVA_INT, VulkanLayouts.VK_SURFACE_FORMAT_KHR_COLOR_SPACE_OFFSET);
        if (colorSpace != VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
            throw new IllegalStateException("Surface has no sRGB-nonlinear SDR color space");
        }
        return new SurfaceFormat(format, colorSpace, formatName(format));
    }

    /// Prefers FIFO, then immediate.
    private static int choosePresentMode(
            VulkanFfmBindings bindings,
            Arena arena,
            MemorySegment physical,
            long surface
    ) {
        MemorySegment countCell = writeInt(arena, 0);
        requireSuccess(
                "vkGetPhysicalDeviceSurfacePresentModesKHR",
                bindings.vkGetPhysicalDeviceSurfacePresentModesKHR(physical, surface, countCell, MemorySegment.NULL)
        );
        int count = countCell.get(ValueLayout.JAVA_INT, 0L);
        if (count <= 0) {
            return VK_PRESENT_MODE_FIFO_KHR;
        }
        MemorySegment modes = arena.allocate(ValueLayout.JAVA_INT, count);
        requireSuccess(
                "vkGetPhysicalDeviceSurfacePresentModesKHR(values)",
                bindings.vkGetPhysicalDeviceSurfacePresentModesKHR(physical, surface, countCell, modes)
        );
        boolean fifo = false;
        boolean immediate = false;
        for (int index = 0; index < count; index++) {
            int mode = modes.getAtIndex(ValueLayout.JAVA_INT, index);
            fifo |= mode == VK_PRESENT_MODE_FIFO_KHR;
            immediate |= mode == VK_PRESENT_MODE_IMMEDIATE_KHR;
        }
        if (fifo) {
            return VK_PRESENT_MODE_FIFO_KHR;
        }
        if (immediate) {
            return VK_PRESENT_MODE_IMMEDIATE_KHR;
        }
        return modes.getAtIndex(ValueLayout.JAVA_INT, 0);
    }

    /// Clamps a requested extent against the surface capabilities.
    private static int chooseExtent(int current, int min, int max, int requested) {
        if (current != 0xFFFF_FFFF) {
            return current;
        }
        int value = requested;
        if (min > 0) {
            value = Math.max(value, min);
        }
        if (max > 0) {
            value = Math.min(value, max);
        }
        return Math.max(value, 1);
    }

    /// Allocates a zeroed `uint32_t` cell.
    private static MemorySegment writeInt(Arena arena, int value) {
        MemorySegment cell = arena.allocate(ValueLayout.JAVA_INT);
        cell.set(ValueLayout.JAVA_INT, 0L, value);
        return cell;
    }

    /// Rejects a failing `VkResult`.
    private static void requireSuccess(String name, int result) {
        if (result != VK_SUCCESS) {
            throw new IllegalStateException(name + " failed with VkResult " + result);
        }
    }

    /// Names a selected UNORM format.
    private static String formatName(int format) {
        if (format == VK_FORMAT_B8G8R8A8_UNORM) {
            return "VK_FORMAT_B8G8R8A8_UNORM";
        }
        if (format == VK_FORMAT_R8G8B8A8_UNORM) {
            return "VK_FORMAT_R8G8B8A8_UNORM";
        }
        return "VkFormat(" + format + ')';
    }

    /// Holds one selected SDR surface format.
    ///
    /// @param format the `VkFormat`
    /// @param colorSpace the `VkColorSpaceKHR`
    /// @param name the format name
    private record SurfaceFormat(int format, int colorSpace, String name) {
    }
}
