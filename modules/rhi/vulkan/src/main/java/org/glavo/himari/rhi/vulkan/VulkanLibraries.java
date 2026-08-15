package org.glavo.himari.rhi.vulkan;

import org.glavo.himari.ffi.NativeLibraryLoadAudit;
import org.glavo.himari.rhi.vulkan.generated.VulkanFfmBindings;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.SymbolLookup;
import java.util.Locale;

/// Owns the Vulkan loader lookup for one device session.
@SuppressWarnings("restricted")
@NotNullByDefault
final class VulkanLibraries implements AutoCloseable {
    /// The arena controlling library lookups.
    private final Arena arena;

    /// The generated bindings.
    private final VulkanFfmBindings bindings;

    /// Creates one owner.
    ///
    /// @param arena the lookup arena
    /// @param bindings the bindings
    private VulkanLibraries(Arena arena, VulkanFfmBindings bindings) {
        this.arena = arena;
        this.bindings = bindings;
    }

    /// Returns the loader name for the current host, or `null` when Vulkan is not a first-stable target.
    ///
    /// @return the library name
    static String loaderName() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("windows")) {
            return "vulkan-1.dll";
        }
        if (os.contains("linux")) {
            return "libvulkan.so.1";
        }
        return "";
    }

    /// Opens the system Vulkan loader.
    ///
    /// @return the library owner
    static VulkanLibraries open() {
        String loader = loaderName();
        if (loader.isEmpty()) {
            throw new IllegalStateException("Vulkan loader is not a first-stable target on "
                    + System.getProperty("os.name", ""));
        }
        Arena arena = Arena.ofConfined();
        try {
            SymbolLookup symbols = SymbolLookup.libraryLookup(loader, arena);
            NativeLibraryLoadAudit.recordSuccessfulLoad(loader);
            return new VulkanLibraries(arena, new VulkanFfmBindings(symbols));
        } catch (RuntimeException | Error failure) {
            arena.close();
            throw failure;
        }
    }

    /// Returns the generated bindings.
    ///
    /// @return the bindings
    VulkanFfmBindings bindings() {
        return bindings;
    }

    /// Closes the lookups.
    @Override
    public void close() {
        arena.close();
    }
}
