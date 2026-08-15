package org.glavo.himari.platform.wayland;

import org.glavo.himari.platform.wayland.generated.WaylandClientLayouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/// Owns the constructed `wl_interface` records required to marshal xdg-shell requests.
@NotNullByDefault
final class WaylandInterfaces {
    /// `wl_registry`.
    final MemorySegment registry;

    /// `wl_compositor`.
    final MemorySegment compositor;

    /// `wl_surface`.
    final MemorySegment surface;

    /// `xdg_wm_base`.
    final MemorySegment xdgWmBase;

    /// `xdg_surface`.
    final MemorySegment xdgSurface;

    /// `xdg_toplevel`.
    final MemorySegment xdgToplevel;

    /// `wl_shm`.
    final MemorySegment shm;

    /// `wl_seat`.
    final MemorySegment seat;

    /// `zxdg_decoration_manager_v1`.
    final MemorySegment decorationManager;

    /// Builds every interface record in `arena`.
    ///
    /// @param arena the owning arena
    WaylandInterfaces(Arena arena) {
        this.registry = arena.allocate(WaylandClientLayouts.WL_INTERFACE);
        this.compositor = arena.allocate(WaylandClientLayouts.WL_INTERFACE);
        this.surface = arena.allocate(WaylandClientLayouts.WL_INTERFACE);
        this.xdgWmBase = arena.allocate(WaylandClientLayouts.WL_INTERFACE);
        this.xdgSurface = arena.allocate(WaylandClientLayouts.WL_INTERFACE);
        this.xdgToplevel = arena.allocate(WaylandClientLayouts.WL_INTERFACE);
        this.shm = arena.allocate(WaylandClientLayouts.WL_INTERFACE);
        this.seat = arena.allocate(WaylandClientLayouts.WL_INTERFACE);
        this.decorationManager = arena.allocate(WaylandClientLayouts.WL_INTERFACE);

        fill(
                registry,
                arena,
                "wl_registry",
                1,
                messages(arena, message(arena, "bind", "usun", null)),
                1,
                messages(
                        arena,
                        message(arena, "global", "usu", null),
                        message(arena, "global_remove", "u", null)
                ),
                2
        );
        fill(
                surface,
                arena,
                "wl_surface",
                6,
                messages(
                        arena,
                        message(arena, "destroy", "", null),
                        message(arena, "attach", "?oii", null),
                        message(arena, "damage", "iiii", null),
                        message(arena, "frame", "n", null),
                        message(arena, "set_opaque_region", "?o", null),
                        message(arena, "set_input_region", "?o", null),
                        message(arena, "commit", "", null)
                ),
                7,
                MemorySegment.NULL,
                0
        );
        fill(
                compositor,
                arena,
                "wl_compositor",
                6,
                messages(
                        arena,
                        message(arena, "create_surface", "n", types(arena, surface)),
                        message(arena, "create_region", "n", null)
                ),
                2,
                MemorySegment.NULL,
                0
        );
        fill(
                xdgToplevel,
                arena,
                "xdg_toplevel",
                6,
                messages(
                        arena,
                        message(arena, "destroy", "", null),
                        message(arena, "set_parent", "?o", null),
                        message(arena, "set_title", "s", null),
                        message(arena, "set_app_id", "s", null)
                ),
                4,
                messages(
                        arena,
                        message(arena, "configure", "iia", null),
                        message(arena, "close", "", null)
                ),
                2
        );
        fill(
                xdgSurface,
                arena,
                "xdg_surface",
                6,
                messages(
                        arena,
                        message(arena, "destroy", "", null),
                        message(arena, "get_toplevel", "n", types(arena, xdgToplevel)),
                        message(arena, "get_popup", "n?oo", null),
                        message(arena, "set_window_geometry", "iiii", null),
                        message(arena, "ack_configure", "u", null)
                ),
                5,
                messages(arena, message(arena, "configure", "u", null)),
                1
        );
        fill(
                shm,
                arena,
                "wl_shm",
                1,
                messages(arena, message(arena, "create_pool", "nhi", null)),
                1,
                messages(arena, message(arena, "format", "u", null)),
                1
        );
        fill(
                seat,
                arena,
                "wl_seat",
                7,
                messages(
                        arena,
                        message(arena, "get_pointer", "n", null),
                        message(arena, "get_keyboard", "n", null),
                        message(arena, "get_touch", "n", null),
                        message(arena, "release", "", null)
                ),
                4,
                messages(
                        arena,
                        message(arena, "capabilities", "u", null),
                        message(arena, "name", "s", null)
                ),
                2
        );
        fill(
                decorationManager,
                arena,
                "zxdg_decoration_manager_v1",
                1,
                messages(
                        arena,
                        message(arena, "destroy", "", null),
                        message(arena, "get_toplevel_decoration", "no", null)
                ),
                2,
                MemorySegment.NULL,
                0
        );
        fill(
                xdgWmBase,
                arena,
                "xdg_wm_base",
                6,
                messages(
                        arena,
                        message(arena, "destroy", "", null),
                        message(arena, "create_positioner", "n", null),
                        message(arena, "get_xdg_surface", "no", types(arena, xdgSurface, surface)),
                        message(arena, "pong", "u", null)
                ),
                4,
                messages(arena, message(arena, "ping", "u", null)),
                1
        );
    }

    /// Writes one `wl_interface` record.
    private static void fill(
            MemorySegment interfaceType,
            Arena arena,
            String name,
            int version,
            MemorySegment methods,
            int methodCount,
            MemorySegment events,
            int eventCount
    ) {
        interfaceType.fill((byte) 0);
        interfaceType.set(ValueLayout.ADDRESS, WaylandClientLayouts.WL_INTERFACE_NAME_OFFSET, arena.allocateFrom(name));
        interfaceType.set(ValueLayout.JAVA_INT, WaylandClientLayouts.WL_INTERFACE_VERSION_OFFSET, version);
        interfaceType.set(ValueLayout.JAVA_INT, WaylandClientLayouts.WL_INTERFACE_METHOD_COUNT_OFFSET, methodCount);
        interfaceType.set(ValueLayout.ADDRESS, WaylandClientLayouts.WL_INTERFACE_METHODS_OFFSET, methods);
        interfaceType.set(ValueLayout.JAVA_INT, WaylandClientLayouts.WL_INTERFACE_EVENT_COUNT_OFFSET, eventCount);
        interfaceType.set(ValueLayout.ADDRESS, WaylandClientLayouts.WL_INTERFACE_EVENTS_OFFSET, events);
    }

    /// Allocates one `wl_message`.
    private static MemorySegment message(Arena arena, String name, String signature, @Nullable MemorySegment types) {
        MemorySegment message = arena.allocate(WaylandClientLayouts.WL_MESSAGE);
        message.fill((byte) 0);
        message.set(ValueLayout.ADDRESS, WaylandClientLayouts.WL_MESSAGE_NAME_OFFSET, arena.allocateFrom(name));
        message.set(
                ValueLayout.ADDRESS,
                WaylandClientLayouts.WL_MESSAGE_SIGNATURE_OFFSET,
                arena.allocateFrom(signature)
        );
        message.set(
                ValueLayout.ADDRESS,
                WaylandClientLayouts.WL_MESSAGE_TYPES_OFFSET,
                types == null ? MemorySegment.NULL : types
        );
        return message;
    }

    /// Copies `messages` into a contiguous `wl_message` array.
    private static MemorySegment messages(Arena arena, MemorySegment... messages) {
        if (messages.length == 0) {
            return MemorySegment.NULL;
        }
        long stride = WaylandClientLayouts.WL_MESSAGE.byteSize();
        MemorySegment array = arena.allocate(WaylandClientLayouts.WL_MESSAGE, messages.length);
        for (int index = 0; index < messages.length; index++) {
            array.asSlice(index * stride, stride).copyFrom(messages[index]);
        }
        return array;
    }

    /// Allocates one `const struct wl_interface *` array.
    private static MemorySegment types(Arena arena, MemorySegment... interfaces) {
        MemorySegment array = arena.allocate(ValueLayout.ADDRESS, interfaces.length);
        for (int index = 0; index < interfaces.length; index++) {
            array.setAtIndex(ValueLayout.ADDRESS, index, interfaces[index]);
        }
        return array;
    }
}
