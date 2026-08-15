package org.glavo.himari.platform.wayland;

import org.glavo.himari.ffi.CallbackFailureQueue;
import org.glavo.himari.platform.wayland.generated.WaylandClientFfmBindings;
import org.glavo.himari.platform.wayland.generated.WaylandClientLayouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Binds `xdg_wm_base`, `wl_shm`, `wl_seat`, and `zxdg_decoration_manager_v1` when advertised,
/// creates a `wl_surface`/`xdg_toplevel`, and answers `xdg_wm_base.ping`.
@SuppressWarnings("restricted")
@NotNullByDefault
public final class WaylandXdgSession implements AutoCloseable {
    /// `wl_display.get_registry`.
    private static final int WL_DISPLAY_GET_REGISTRY = 1;

    /// `wl_registry.bind`.
    private static final int WL_REGISTRY_BIND = 0;

    /// `wl_compositor.create_surface`.
    private static final int WL_COMPOSITOR_CREATE_SURFACE = 0;

    /// `wl_surface.commit`.
    private static final int WL_SURFACE_COMMIT = 6;

    /// `wl_surface.destroy`.
    private static final int WL_SURFACE_DESTROY = 0;

    /// `xdg_wm_base.get_xdg_surface`.
    private static final int XDG_WM_BASE_GET_XDG_SURFACE = 2;

    /// `xdg_wm_base.pong`.
    private static final int XDG_WM_BASE_PONG = 3;

    /// `xdg_wm_base.destroy`.
    private static final int XDG_WM_BASE_DESTROY = 0;

    /// `xdg_surface.get_toplevel`.
    private static final int XDG_SURFACE_GET_TOPLEVEL = 1;

    /// `xdg_surface.ack_configure`.
    private static final int XDG_SURFACE_ACK_CONFIGURE = 4;

    /// `xdg_surface.destroy`.
    private static final int XDG_SURFACE_DESTROY = 0;

    /// `xdg_toplevel.destroy`.
    private static final int XDG_TOPLEVEL_DESTROY = 0;

    /// `WL_MARSHAL_FLAG_DESTROY`.
    private static final int WL_MARSHAL_FLAG_DESTROY = 1;

    /// Display that owns the connection.
    private final WaylandDisplay display;

    /// Generated bindings.
    private final WaylandClientFfmBindings bindings;

    /// Arena for interfaces, listeners, and argument cells.
    private final Arena arena;

    /// Contained callback failures.
    private final CallbackFailureQueue failures = new CallbackFailureQueue();

    /// Constructed protocol interfaces.
    private final WaylandInterfaces interfaces;

    /// Registry proxy.
    private final MemorySegment registry;

    /// Advertised `wl_compositor` name, or `-1`.
    private int compositorName = -1;

    /// Advertised compositor version.
    private int compositorVersion;

    /// Advertised `xdg_wm_base` name, or `-1`.
    private int xdgWmBaseName = -1;

    /// Advertised `xdg_wm_base` version.
    private int xdgWmBaseVersion;

    /// Advertised `wl_shm` name, or `-1`.
    private int shmName = -1;

    /// Advertised `wl_shm` version.
    private int shmVersion;

    /// Advertised `wl_seat` name, or `-1`.
    private int seatName = -1;

    /// Advertised `wl_seat` version.
    private int seatVersion;

    /// Advertised `zxdg_decoration_manager_v1` name, or `-1`.
    private int decorationName = -1;

    /// Advertised decoration-manager version.
    private int decorationVersion;

    /// Bound compositor, or `NULL`.
    private MemorySegment compositor = MemorySegment.NULL;

    /// Bound `xdg_wm_base`, or `NULL`.
    private MemorySegment xdgWmBase = MemorySegment.NULL;

    /// Bound `wl_shm`, or `NULL`.
    private MemorySegment shm = MemorySegment.NULL;

    /// Bound `wl_seat`, or `NULL`.
    private MemorySegment seat = MemorySegment.NULL;

    /// Bound `zxdg_decoration_manager_v1`, or `NULL`.
    private MemorySegment decorationManager = MemorySegment.NULL;

    /// Created `wl_surface`, or `NULL`.
    private MemorySegment surface = MemorySegment.NULL;

    /// Created `xdg_surface`, or `NULL`.
    private MemorySegment xdgSurface = MemorySegment.NULL;

    /// Created `xdg_toplevel`, or `NULL`.
    private MemorySegment toplevel = MemorySegment.NULL;

    /// Whether closed.
    private boolean closed;

    /// Creates one session after the registry listener is attached.
    private WaylandXdgSession(
            WaylandDisplay display,
            WaylandClientFfmBindings bindings,
            Arena arena,
            WaylandInterfaces interfaces,
            MemorySegment registry
    ) {
        this.display = display;
        this.bindings = bindings;
        this.arena = arena;
        this.interfaces = interfaces;
        this.registry = registry;
    }

    /// Binds xdg-shell objects on an open display.
    ///
    /// @param display the connected display
    /// @return the session
    public static WaylandXdgSession bind(WaylandDisplay display) {
        Objects.requireNonNull(display, "display");
        Arena arena = Arena.ofConfined();
        WaylandClientFfmBindings bindings = display.bindings();
        WaylandInterfaces interfaces = new WaylandInterfaces(arena);
        MemorySegment args = arena.allocate(WaylandClientLayouts.WL_ARGUMENT);
        args.fill((byte) 0);
        int version = bindings.wlProxyGetVersion(display.nativeHandle());
        MemorySegment registry = bindings.wlProxyMarshalArrayFlags(
                display.nativeHandle(),
                WL_DISPLAY_GET_REGISTRY,
                interfaces.registry,
                version,
                0,
                args
        );
        if (registry.address() == 0L) {
            arena.close();
            throw new IllegalStateException("wl_display.get_registry returned NULL");
        }
        WaylandXdgSession session = new WaylandXdgSession(display, bindings, arena, interfaces, registry);
        try {
            MemorySegment listener = arena.allocate(ValueLayout.ADDRESS, 2);
            listener.setAtIndex(
                    ValueLayout.ADDRESS,
                    0L,
                    bindings.createWlRegistryGlobalStub(session::onGlobal, session.failures, arena)
            );
            listener.setAtIndex(
                    ValueLayout.ADDRESS,
                    1L,
                    bindings.createWlRegistryGlobalRemoveStub(session::onGlobalRemove, session.failures, arena)
            );
            int added = bindings.wlProxyAddListener(registry, listener, MemorySegment.NULL);
            if (added != 0) {
                throw new IllegalStateException("wl_proxy_add_listener(registry) failed with " + added);
            }
            session.roundtrip();
            session.bindAdvertised();
            session.createToplevelIfPossible();
            return session;
        } catch (RuntimeException | Error failure) {
            session.close();
            throw failure;
        }
    }

    /// Returns whether the compositor advertised `xdg_wm_base`.
    ///
    /// @return whether the global was seen
    public boolean xdgWmBaseAdvertised() {
        return xdgWmBaseName >= 0;
    }

    /// Returns whether `xdg_wm_base` was bound.
    ///
    /// @return whether the object exists
    public boolean xdgWmBaseBound() {
        return xdgWmBase.address() != 0L;
    }

    /// Returns whether an `xdg_toplevel` was created.
    ///
    /// @return whether the toplevel exists
    public boolean toplevelCreated() {
        return toplevel.address() != 0L;
    }

    /// Returns whether the compositor advertised `wl_shm`.
    ///
    /// @return whether the global was seen
    public boolean shmAdvertised() {
        return shmName >= 0;
    }

    /// Returns whether the compositor advertised `wl_seat`.
    ///
    /// @return whether the global was seen
    public boolean seatAdvertised() {
        return seatName >= 0;
    }

    /// Returns whether the compositor advertised `zxdg_decoration_manager_v1`.
    ///
    /// @return whether the global was seen
    public boolean decorationManagerAdvertised() {
        return decorationName >= 0;
    }

    /// Returns whether `wl_shm` was bound.
    ///
    /// @return whether the object exists
    public boolean shmBound() {
        return shm.address() != 0L;
    }

    /// Returns whether `wl_seat` was bound.
    ///
    /// @return whether the object exists
    public boolean seatBound() {
        return seat.address() != 0L;
    }

    /// Returns whether `zxdg_decoration_manager_v1` was bound.
    ///
    /// @return whether the object exists
    public boolean decorationManagerBound() {
        return decorationManager.address() != 0L;
    }

    /// Destroys created objects. The display remains open.
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        destroy(toplevel, XDG_TOPLEVEL_DESTROY);
        toplevel = MemorySegment.NULL;
        destroy(xdgSurface, XDG_SURFACE_DESTROY);
        xdgSurface = MemorySegment.NULL;
        destroy(surface, WL_SURFACE_DESTROY);
        surface = MemorySegment.NULL;
        destroy(xdgWmBase, XDG_WM_BASE_DESTROY);
        xdgWmBase = MemorySegment.NULL;
        destroy(decorationManager, 0);
        decorationManager = MemorySegment.NULL;
        if (seat.address() != 0L) {
            bindings.wlProxyDestroy(seat);
            seat = MemorySegment.NULL;
        }
        if (shm.address() != 0L) {
            bindings.wlProxyDestroy(shm);
            shm = MemorySegment.NULL;
        }
        if (compositor.address() != 0L) {
            bindings.wlProxyDestroy(compositor);
            compositor = MemorySegment.NULL;
        }
        if (registry.address() != 0L) {
            bindings.wlProxyDestroy(registry);
        }
        arena.close();
    }

    /// Records advertised globals.
    private void onGlobal(
            MemorySegment data,
            MemorySegment registryProxy,
            int name,
            MemorySegment interfaceName,
            int version
    ) {
        String advertised = readCString(interfaceName);
        if ("wl_compositor".equals(advertised)) {
            compositorName = name;
            compositorVersion = version;
        } else if ("xdg_wm_base".equals(advertised)) {
            xdgWmBaseName = name;
            xdgWmBaseVersion = version;
        } else if ("wl_shm".equals(advertised)) {
            shmName = name;
            shmVersion = version;
        } else if ("wl_seat".equals(advertised)) {
            seatName = name;
            seatVersion = version;
        } else if ("zxdg_decoration_manager_v1".equals(advertised)) {
            decorationName = name;
            decorationVersion = version;
        }
    }

    /// Ignores global removals during this first-stable bind.
    private void onGlobalRemove(MemorySegment data, MemorySegment registryProxy, int name) {
    }

    /// Answers `xdg_wm_base.ping`.
    private void onPing(MemorySegment data, MemorySegment wmBase, int serial) {
        MemorySegment args = arena.allocate(WaylandClientLayouts.WL_ARGUMENT);
        args.set(ValueLayout.JAVA_INT, WaylandClientLayouts.WL_ARGUMENT_U_OFFSET, serial);
        bindings.wlProxyMarshalArrayFlags(
                wmBase,
                XDG_WM_BASE_PONG,
                MemorySegment.NULL,
                bindings.wlProxyGetVersion(wmBase),
                0,
                args
        );
    }

    /// Acknowledges `xdg_surface.configure`.
    private void onConfigure(MemorySegment data, MemorySegment xdg, int serial) {
        MemorySegment args = arena.allocate(WaylandClientLayouts.WL_ARGUMENT);
        args.set(ValueLayout.JAVA_INT, WaylandClientLayouts.WL_ARGUMENT_U_OFFSET, serial);
        bindings.wlProxyMarshalArrayFlags(
                xdg,
                XDG_SURFACE_ACK_CONFIGURE,
                MemorySegment.NULL,
                bindings.wlProxyGetVersion(xdg),
                0,
                args
        );
    }

    /// Binds advertised compositor and `xdg_wm_base` objects.
    private void bindAdvertised() {
        if (compositorName >= 0) {
            compositor = bind(compositorName, interfaces.compositor, Math.min(compositorVersion, 6));
        }
        if (xdgWmBaseName >= 0) {
            xdgWmBase = bind(xdgWmBaseName, interfaces.xdgWmBase, Math.min(xdgWmBaseVersion, 6));
            if (xdgWmBase.address() != 0L) {
                MemorySegment listener = arena.allocate(ValueLayout.ADDRESS, 1);
                listener.setAtIndex(
                        ValueLayout.ADDRESS,
                        0L,
                        bindings.createXdgWmBasePingStub(this::onPing, failures, arena)
                );
                bindings.wlProxyAddListener(xdgWmBase, listener, MemorySegment.NULL);
            }
        }
        if (shmName >= 0) {
            shm = bind(shmName, interfaces.shm, Math.min(shmVersion, 1));
        }
        if (seatName >= 0) {
            seat = bind(seatName, interfaces.seat, Math.min(seatVersion, 7));
        }
        if (decorationName >= 0) {
            decorationManager = bind(decorationName, interfaces.decorationManager, Math.min(decorationVersion, 1));
        }
    }

    /// Creates a surface and toplevel when both globals were bound.
    private void createToplevelIfPossible() {
        if (compositor.address() == 0L || xdgWmBase.address() == 0L) {
            return;
        }
        MemorySegment newId = arena.allocate(WaylandClientLayouts.WL_ARGUMENT);
        newId.fill((byte) 0);
        surface = bindings.wlProxyMarshalArrayFlags(
                compositor,
                WL_COMPOSITOR_CREATE_SURFACE,
                interfaces.surface,
                bindings.wlProxyGetVersion(compositor),
                0,
                newId
        );
        if (surface.address() == 0L) {
            throw new IllegalStateException("wl_compositor.create_surface returned NULL");
        }
        MemorySegment surfaceArgs = arena.allocate(WaylandClientLayouts.WL_ARGUMENT, 2);
        surfaceArgs.asSlice(0, WaylandClientLayouts.WL_ARGUMENT.byteSize()).fill((byte) 0);
        surfaceArgs.asSlice(WaylandClientLayouts.WL_ARGUMENT.byteSize())
                .set(ValueLayout.ADDRESS, WaylandClientLayouts.WL_ARGUMENT_O_OFFSET, surface);
        xdgSurface = bindings.wlProxyMarshalArrayFlags(
                xdgWmBase,
                XDG_WM_BASE_GET_XDG_SURFACE,
                interfaces.xdgSurface,
                bindings.wlProxyGetVersion(xdgWmBase),
                0,
                surfaceArgs
        );
        if (xdgSurface.address() == 0L) {
            throw new IllegalStateException("xdg_wm_base.get_xdg_surface returned NULL");
        }
        MemorySegment configure = arena.allocate(ValueLayout.ADDRESS, 1);
        configure.setAtIndex(
                ValueLayout.ADDRESS,
                0L,
                bindings.createXdgSurfaceConfigureStub(this::onConfigure, failures, arena)
        );
        bindings.wlProxyAddListener(xdgSurface, configure, MemorySegment.NULL);
        MemorySegment toplevelArgs = arena.allocate(WaylandClientLayouts.WL_ARGUMENT);
        toplevelArgs.fill((byte) 0);
        toplevel = bindings.wlProxyMarshalArrayFlags(
                xdgSurface,
                XDG_SURFACE_GET_TOPLEVEL,
                interfaces.xdgToplevel,
                bindings.wlProxyGetVersion(xdgSurface),
                0,
                toplevelArgs
        );
        if (toplevel.address() == 0L) {
            throw new IllegalStateException("xdg_surface.get_toplevel returned NULL");
        }
        bindings.wlProxyMarshalArrayFlags(
                surface,
                WL_SURFACE_COMMIT,
                MemorySegment.NULL,
                bindings.wlProxyGetVersion(surface),
                0,
                MemorySegment.NULL
        );
        roundtrip();
    }

    /// Binds one registry global.
    private MemorySegment bind(int name, MemorySegment interfaceType, int version) {
        MemorySegment args = arena.allocate(WaylandClientLayouts.WL_ARGUMENT, 4);
        long stride = WaylandClientLayouts.WL_ARGUMENT.byteSize();
        args.asSlice(0, stride).set(ValueLayout.JAVA_INT, WaylandClientLayouts.WL_ARGUMENT_U_OFFSET, name);
        args.asSlice(stride, stride).set(
                ValueLayout.ADDRESS,
                WaylandClientLayouts.WL_ARGUMENT_O_OFFSET,
                interfaceType.get(ValueLayout.ADDRESS, WaylandClientLayouts.WL_INTERFACE_NAME_OFFSET)
        );
        args.asSlice(2 * stride, stride)
                .set(ValueLayout.JAVA_INT, WaylandClientLayouts.WL_ARGUMENT_U_OFFSET, version);
        args.asSlice(3 * stride, stride).fill((byte) 0);
        MemorySegment bound = bindings.wlProxyMarshalArrayFlags(
                registry,
                WL_REGISTRY_BIND,
                interfaceType,
                version,
                0,
                args
        );
        if (bound.address() == 0L) {
            throw new IllegalStateException("wl_registry.bind returned NULL");
        }
        return bound;
    }

    /// Sends a destructor request and destroys the proxy.
    private void destroy(MemorySegment proxy, int opcode) {
        if (proxy.address() == 0L) {
            return;
        }
        bindings.wlProxyMarshalArrayFlags(
                proxy,
                opcode,
                MemorySegment.NULL,
                bindings.wlProxyGetVersion(proxy),
                WL_MARSHAL_FLAG_DESTROY,
                MemorySegment.NULL
        );
    }

    /// Runs one display roundtrip and rethrows contained callback failures.
    private void roundtrip() {
        display.roundtrip();
        @Nullable Throwable failure = failures.poll();
        if (failure != null) {
            throw new IllegalStateException("Wayland listener failed", failure);
        }
    }

    /// Reads a NUL-terminated UTF-8 C string.
    private static String readCString(MemorySegment pointer) {
        if (pointer.address() == 0L) {
            return "";
        }
        return pointer.reinterpret(4096).getString(0);
    }
}
