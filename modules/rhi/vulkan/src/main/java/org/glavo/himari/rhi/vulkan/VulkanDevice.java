package org.glavo.himari.rhi.vulkan;

import org.glavo.himari.rhi.vulkan.generated.VulkanFfmBindings;
import org.glavo.himari.rhi.vulkan.generated.VulkanLayouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Locale;
import java.util.Objects;

/// Owns one Vulkan instance, logical device, and optional Win32 surface.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class VulkanDevice implements AutoCloseable {
    /// `VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO`.
    private static final int VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO = 1;

    /// `VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO`.
    private static final int VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO = 2;

    /// `VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO`.
    private static final int VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO = 3;

    /// `VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR`.
    private static final int VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR = 1_000_009_000;

    /// `VK_QUEUE_GRAPHICS_BIT`.
    private static final int VK_QUEUE_GRAPHICS_BIT = 0x0000_0001;

    /// `VK_SUCCESS`.
    private static final int VK_SUCCESS = 0;

    /// Shared libraries.
    private final VulkanLibraries libraries;

    /// Confined storage for create-info records.
    private final Arena arena;

    /// Native instance.
    private final MemorySegment instance;

    /// Native physical device.
    private final MemorySegment physicalDevice;

    /// Native logical device.
    private final MemorySegment device;

    /// Graphics queue.
    private final MemorySegment queue;

    /// Selected graphics queue family.
    private final int graphicsQueueFamily;

    /// Whether `VK_KHR_swapchain` was enabled on the logical device.
    private final boolean swapchainExtensionEnabled;

    /// Optional Win32 surface handle.
    private long surface;

    /// Queried snapshot.
    private VulkanCapabilities capabilities;

    /// Whether this owner is closed.
    private boolean closed;

    /// Creates one device owner.
    private VulkanDevice(
            VulkanLibraries libraries,
            Arena arena,
            MemorySegment instance,
            MemorySegment physicalDevice,
            MemorySegment device,
            MemorySegment queue,
            int graphicsQueueFamily,
            boolean swapchainExtensionEnabled,
            VulkanCapabilities capabilities
    ) {
        this.libraries = libraries;
        this.arena = arena;
        this.instance = instance;
        this.physicalDevice = physicalDevice;
        this.device = device;
        this.queue = queue;
        this.graphicsQueueFamily = graphicsQueueFamily;
        this.swapchainExtensionEnabled = swapchainExtensionEnabled;
        this.capabilities = capabilities;
    }

    /// Creates a Vulkan instance, logical device, and graphics queue.
    ///
    /// On Windows the instance enables `VK_KHR_surface` and `VK_KHR_win32_surface`.
    ///
    /// @return the device owner
    public static VulkanDevice open() {
        VulkanLibraries libraries = VulkanLibraries.open();
        Arena arena = Arena.ofConfined();
        MemorySegment instance = MemorySegment.NULL;
        try {
            VulkanFfmBindings bindings = libraries.bindings();
            instance = createInstance(bindings, arena);
            PhysicalDevices physicals = enumeratePhysicalDevices(bindings, arena, instance);
            int family = graphicsQueueFamily(bindings, arena, physicals.first());
            boolean swapchain = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");
            MemorySegment device;
            try {
                device = createLogicalDevice(bindings, arena, physicals.first(), family, swapchain);
            } catch (IllegalStateException first) {
                if (!swapchain) {
                    throw first;
                }
                swapchain = false;
                device = createLogicalDevice(bindings, arena, physicals.first(), family, false);
            }
            MemorySegment queueCell = arena.allocate(ValueLayout.ADDRESS);
            queueCell.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
            bindings.vkGetDeviceQueue(device, family, 0, queueCell);
            MemorySegment queue = queueCell.get(ValueLayout.ADDRESS, 0L);
            if (queue.address() == 0L) {
                bindings.vkDestroyDevice(device, MemorySegment.NULL);
                throw new IllegalStateException("vkGetDeviceQueue returned NULL");
            }
            return new VulkanDevice(
                    libraries,
                    arena,
                    instance,
                    physicals.first(),
                    device,
                    queue,
                    family,
                    swapchain,
                    new VulkanCapabilities(
                            physicals.count(),
                            true,
                            family,
                            false,
                            false,
                            "color-managed-sdr"
                    )
            );
        } catch (RuntimeException | Error failure) {
            if (instance.address() != 0L) {
                try {
                    libraries.bindings().vkDestroyInstance(instance, MemorySegment.NULL);
                } catch (RuntimeException ignored) {
                    // Preserve the original failure.
                }
            }
            arena.close();
            libraries.close();
            throw failure;
        }
    }

    /// Creates a `VkSurfaceKHR` for a production HWND.
    ///
    /// @param hinstance the module handle
    /// @param hwnd the window handle
    /// @return the surface handle
    public long createWin32Surface(MemorySegment hinstance, MemorySegment hwnd) {
        requireOpen();
        Objects.requireNonNull(hinstance, "hinstance");
        Objects.requireNonNull(hwnd, "hwnd");
        if (hwnd.address() == 0L) {
            throw new IllegalArgumentException("HWND must not be NULL");
        }
        MemorySegment info = arena.allocate(VulkanLayouts.VK_WIN32_SURFACE_CREATE_INFO_KHR);
        info.fill((byte) 0);
        info.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_WIN32_SURFACE_CREATE_INFO_KHR_S_TYPE_OFFSET,
                VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR);
        info.set(ValueLayout.ADDRESS, VulkanLayouts.VK_WIN32_SURFACE_CREATE_INFO_KHR_HINSTANCE_OFFSET, hinstance);
        info.set(ValueLayout.ADDRESS, VulkanLayouts.VK_WIN32_SURFACE_CREATE_INFO_KHR_HWND_OFFSET, hwnd);
        MemorySegment surfaceCell = arena.allocate(ValueLayout.JAVA_LONG);
        surfaceCell.set(ValueLayout.JAVA_LONG, 0L, 0L);
        int created = libraries.bindings().vkCreateWin32SurfaceKHR(
                instance,
                info,
                MemorySegment.NULL,
                surfaceCell
        );
        if (created != VK_SUCCESS) {
            throw new IllegalStateException("vkCreateWin32SurfaceKHR failed with VkResult " + created);
        }
        long createdSurface = surfaceCell.get(ValueLayout.JAVA_LONG, 0L);
        if (createdSurface == 0L) {
            throw new IllegalStateException("vkCreateWin32SurfaceKHR returned VK_NULL_HANDLE");
        }
        destroySurfaceIfAny();
        surface = createdSurface;
        capabilities = new VulkanCapabilities(
                capabilities.physicalDeviceCount(),
                true,
                graphicsQueueFamily,
                true,
                false,
                "color-managed-sdr"
        );
        return surface;
    }

    /// Creates a swapchain for the current Win32 surface, clears one image, and presents it.
    ///
    /// @param hinstance the module handle
    /// @param hwnd the window handle
    /// @param width the positive width in pixels
    /// @param height the positive height in pixels
    /// @return the present observation
    public VulkanPresentation presentSdr(
            MemorySegment hinstance,
            MemorySegment hwnd,
            int width,
            int height
    ) {
        requireOpen();
        if (!swapchainExtensionEnabled) {
            throw new IllegalStateException("VK_KHR_swapchain was not enabled on the logical device");
        }
        if (surface == 0L) {
            createWin32Surface(hinstance, hwnd);
        }
        return VulkanSwapChain.presentSdr(this, width, height);
    }

    /// Returns the queried SDR snapshot.
    ///
    /// @return the snapshot
    public VulkanCapabilities capabilities() {
        requireOpen();
        return capabilities;
    }

    /// Returns the graphics queue.
    ///
    /// @return the queue
    public MemorySegment queue() {
        requireOpen();
        return queue;
    }

    /// Returns the generated bindings.
    ///
    /// @return the bindings
    VulkanFfmBindings bindings() {
        requireOpen();
        return libraries.bindings();
    }

    /// Returns the confined arena.
    ///
    /// @return the arena
    Arena arena() {
        requireOpen();
        return arena;
    }

    /// Returns the selected physical device.
    ///
    /// @return the physical device
    MemorySegment physicalDevice() {
        requireOpen();
        return physicalDevice;
    }

    /// Returns the logical device.
    ///
    /// @return the device
    MemorySegment logicalDevice() {
        requireOpen();
        return device;
    }

    /// Returns the current Win32 surface handle.
    ///
    /// @return the surface
    long surfaceHandle() {
        requireOpen();
        return surface;
    }

    /// Returns the selected graphics queue family.
    ///
    /// @return the family index
    int graphicsQueueFamily() {
        requireOpen();
        return graphicsQueueFamily;
    }

    /// Destroys the surface, device, and instance.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        @Nullable RuntimeException firstFailure = null;
        try {
            destroySurfaceIfAny();
        } catch (RuntimeException failure) {
            firstFailure = failure;
        }
        try {
            libraries.bindings().vkDestroyDevice(device, MemorySegment.NULL);
        } catch (RuntimeException failure) {
            if (firstFailure == null) {
                firstFailure = failure;
            } else {
                firstFailure.addSuppressed(failure);
            }
        }
        try {
            libraries.bindings().vkDestroyInstance(instance, MemorySegment.NULL);
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

    /// Creates the instance, enabling Win32 WSI extensions on Windows when the loader accepts them.
    private static MemorySegment createInstance(VulkanFfmBindings bindings, Arena arena) {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            try {
                return createInstance(bindings, arena, true);
            } catch (IllegalStateException ignored) {
                return createInstance(bindings, arena, false);
            }
        }
        return createInstance(bindings, arena, false);
    }

    /// Creates one instance, optionally enabling Win32 WSI extensions.
    private static MemorySegment createInstance(VulkanFfmBindings bindings, Arena arena, boolean win32Wsi) {
        MemorySegment createInfo = arena.allocate(VulkanLayouts.VK_INSTANCE_CREATE_INFO);
        createInfo.fill((byte) 0);
        createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_INSTANCE_CREATE_INFO_S_TYPE_OFFSET,
                VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
        if (win32Wsi) {
            MemorySegment names = arena.allocate(ValueLayout.ADDRESS, 2);
            names.setAtIndex(ValueLayout.ADDRESS, 0L, arena.allocateFrom("VK_KHR_surface"));
            names.setAtIndex(ValueLayout.ADDRESS, 1L, arena.allocateFrom("VK_KHR_win32_surface"));
            createInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_INSTANCE_CREATE_INFO_ENABLED_EXTENSION_COUNT_OFFSET, 2);
            createInfo.set(
                    ValueLayout.ADDRESS,
                    VulkanLayouts.VK_INSTANCE_CREATE_INFO_ENABLED_EXTENSION_NAMES_OFFSET,
                    names
            );
        }
        MemorySegment instanceCell = arena.allocate(ValueLayout.ADDRESS);
        instanceCell.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        int created = bindings.vkCreateInstance(createInfo, MemorySegment.NULL, instanceCell);
        if (created != VK_SUCCESS) {
            throw new IllegalStateException("vkCreateInstance failed with VkResult " + created);
        }
        MemorySegment instance = instanceCell.get(ValueLayout.ADDRESS, 0L);
        if (instance.address() == 0L) {
            throw new IllegalStateException("vkCreateInstance returned a NULL instance");
        }
        return instance;
    }

    /// Enumerates physical devices and returns the first handle plus the count.
    private static PhysicalDevices enumeratePhysicalDevices(
            VulkanFfmBindings bindings,
            Arena arena,
            MemorySegment instance
    ) {
        MemorySegment countCell = arena.allocate(ValueLayout.JAVA_INT);
        countCell.set(ValueLayout.JAVA_INT, 0L, 0);
        int counted = bindings.vkEnumeratePhysicalDevices(instance, countCell, MemorySegment.NULL);
        if (counted != VK_SUCCESS) {
            throw new IllegalStateException("vkEnumeratePhysicalDevices failed with VkResult " + counted);
        }
        int count = countCell.get(ValueLayout.JAVA_INT, 0L);
        if (count <= 0) {
            throw new IllegalStateException("Vulkan instance enumerated no physical devices");
        }
        MemorySegment devices = arena.allocate(ValueLayout.ADDRESS, count);
        int enumerated = bindings.vkEnumeratePhysicalDevices(instance, countCell, devices);
        if (enumerated != VK_SUCCESS) {
            throw new IllegalStateException("vkEnumeratePhysicalDevices(handles) failed with VkResult " + enumerated);
        }
        MemorySegment physical = devices.getAtIndex(ValueLayout.ADDRESS, 0L);
        if (physical.address() == 0L) {
            throw new IllegalStateException("vkEnumeratePhysicalDevices returned a NULL physical device");
        }
        return new PhysicalDevices(count, physical);
    }

    /// Holds the enumerated physical-device count and the first handle.
    ///
    /// @param count the enumerated count
    /// @param first the first physical device
    private record PhysicalDevices(int count, MemorySegment first) {
    }

    /// Selects the first graphics-capable queue family.
    private static int graphicsQueueFamily(
            VulkanFfmBindings bindings,
            Arena arena,
            MemorySegment physical
    ) {
        MemorySegment countCell = arena.allocate(ValueLayout.JAVA_INT);
        countCell.set(ValueLayout.JAVA_INT, 0L, 0);
        bindings.vkGetPhysicalDeviceQueueFamilyProperties(physical, countCell, MemorySegment.NULL);
        int count = countCell.get(ValueLayout.JAVA_INT, 0L);
        if (count <= 0) {
            throw new IllegalStateException("Physical device reported no queue families");
        }
        MemorySegment families = arena.allocate(VulkanLayouts.VK_QUEUE_FAMILY_PROPERTIES, count);
        bindings.vkGetPhysicalDeviceQueueFamilyProperties(physical, countCell, families);
        long stride = VulkanLayouts.VK_QUEUE_FAMILY_PROPERTIES.byteSize();
        for (int index = 0; index < count; index++) {
            int flags = families.asSlice(index * stride)
                    .get(ValueLayout.JAVA_INT, VulkanLayouts.VK_QUEUE_FAMILY_PROPERTIES_QUEUE_FLAGS_OFFSET);
            if ((flags & VK_QUEUE_GRAPHICS_BIT) != 0) {
                return index;
            }
        }
        throw new IllegalStateException("Physical device has no graphics queue family");
    }

    /// Creates the logical device.
    private static MemorySegment createLogicalDevice(
            VulkanFfmBindings bindings,
            Arena arena,
            MemorySegment physical,
            int family,
            boolean swapchain
    ) {
        MemorySegment priority = arena.allocate(ValueLayout.JAVA_FLOAT);
        priority.set(ValueLayout.JAVA_FLOAT, 0L, 1.0f);
        MemorySegment queueInfo = arena.allocate(VulkanLayouts.VK_DEVICE_QUEUE_CREATE_INFO);
        queueInfo.fill((byte) 0);
        queueInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_DEVICE_QUEUE_CREATE_INFO_S_TYPE_OFFSET,
                VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
        queueInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_DEVICE_QUEUE_CREATE_INFO_QUEUE_FAMILY_INDEX_OFFSET, family);
        queueInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_DEVICE_QUEUE_CREATE_INFO_QUEUE_COUNT_OFFSET, 1);
        queueInfo.set(ValueLayout.ADDRESS, VulkanLayouts.VK_DEVICE_QUEUE_CREATE_INFO_QUEUE_PRIORITIES_OFFSET, priority);
        MemorySegment deviceInfo = arena.allocate(VulkanLayouts.VK_DEVICE_CREATE_INFO);
        deviceInfo.fill((byte) 0);
        deviceInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_DEVICE_CREATE_INFO_S_TYPE_OFFSET,
                VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
        deviceInfo.set(ValueLayout.JAVA_INT, VulkanLayouts.VK_DEVICE_CREATE_INFO_QUEUE_CREATE_INFO_COUNT_OFFSET, 1);
        deviceInfo.set(ValueLayout.ADDRESS, VulkanLayouts.VK_DEVICE_CREATE_INFO_QUEUE_CREATE_INFOS_OFFSET, queueInfo);
        if (swapchain) {
            MemorySegment names = arena.allocate(ValueLayout.ADDRESS, 1);
            names.setAtIndex(ValueLayout.ADDRESS, 0L, arena.allocateFrom("VK_KHR_swapchain"));
            deviceInfo.set(
                    ValueLayout.JAVA_INT,
                    VulkanLayouts.VK_DEVICE_CREATE_INFO_ENABLED_EXTENSION_COUNT_OFFSET,
                    1
            );
            deviceInfo.set(
                    ValueLayout.ADDRESS,
                    VulkanLayouts.VK_DEVICE_CREATE_INFO_ENABLED_EXTENSION_NAMES_OFFSET,
                    names
            );
        }
        MemorySegment deviceCell = arena.allocate(ValueLayout.ADDRESS);
        deviceCell.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
        int created = bindings.vkCreateDevice(physical, deviceInfo, MemorySegment.NULL, deviceCell);
        if (created != VK_SUCCESS) {
            throw new IllegalStateException("vkCreateDevice failed with VkResult " + created);
        }
        MemorySegment device = deviceCell.get(ValueLayout.ADDRESS, 0L);
        if (device.address() == 0L) {
            throw new IllegalStateException("vkCreateDevice returned a NULL device");
        }
        return device;
    }

    /// Destroys a previously created surface.
    private void destroySurfaceIfAny() {
        if (surface != 0L) {
            libraries.bindings().vkDestroySurfaceKHR(instance, surface, MemorySegment.NULL);
            surface = 0L;
        }
    }

    /// Verifies the device is open.
    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Vulkan device is closed");
        }
    }
}
