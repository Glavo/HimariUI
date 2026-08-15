package org.glavo.himari.desktop;

import org.glavo.himari.font.BitmapSfntFont;
import org.glavo.himari.font.GlyphMask;
import org.glavo.himari.font.GlyphRasterizer;
import org.glavo.himari.font.SfntFont;
import org.glavo.himari.graphics.Color;
import org.glavo.himari.graphics.DisplayList;
import org.glavo.himari.graphics.DisplayListOp;
import org.glavo.himari.graphics.SceneEnvelope;
import org.glavo.himari.inspector.Inspector;
import org.glavo.himari.inspector.InspectorSnapshot;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutRect;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.objc.ObjcBlockProbe;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.platform.api.WindowConfiguration;
import org.glavo.himari.platform.api.WindowEvent;
import org.glavo.himari.platform.api.WindowEventType;
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.platform.macos.MacOSProbe;
import org.glavo.himari.platform.wayland.WaylandProbe;
import org.glavo.himari.platform.api.SurfaceRole;
import org.glavo.himari.platform.windows.WindowsBackend;
import org.glavo.himari.platform.windows.WindowsPlatform;
import org.glavo.himari.platform.windows.WindowsPopupHost;
import org.glavo.himari.platform.windows.WindowsWindow;
import org.glavo.himari.rhi.d3d12.D3d12Device;
import org.glavo.himari.rhi.d3d12.D3d12Presentation;
import org.glavo.himari.render.software.SoftwareSurface;
import org.glavo.himari.rhi.metal.MetalProbe;
import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.runtime.structure.StructuralRuntime;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.glavo.himari.text.DefaultShaper;
import org.glavo.himari.text.ShapedGlyph;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/// Launches the first-stable desktop CounterApp against the host window backend.
@NotNullByDefault
public final class DesktopLaunch {
    /// Software surface width.
    private static final int WIDTH = 240;

    /// Software surface height.
    private static final int HEIGHT = 120;

    /// Prevents instantiation.
    private DesktopLaunch() {
    }

    /// Runs the desktop CounterApp and optionally writes artifacts.
    ///
    /// On Windows a real HWND is created, pumped, and presented. Smoke mode closes the session
    /// before returning. Interactive mode honors `CLOSE_REQUESTED` and blocks in `WaitMessage` until
    /// every HWND is closed. Other hosts still execute the Headless CounterApp tree and record
    /// environment-blocked backend probes.
    ///
    /// @param smoke whether to close host windows before returning
    /// @param output the optional artifact directory
    /// @return the launch result
    /// @throws Exception if the host window or artifact write fails
    public static DesktopLaunchResult run(boolean smoke, @Nullable Path output) throws Exception {
        DesktopHost host = DesktopHost.detect();
        WaylandProbe wayland = WaylandProbe.run();
        MacOSProbe macos = MacOSProbe.run();
        MetalProbe metal = MetalProbe.run();
        ObjcBlockProbe objc = ObjcBlockProbe.run();
        StateDomain domain = new StateDomain();
        IntState count = domain.intState(0);
        AtomicInteger clicks = new AtomicInteger();
        boolean windowCreated = false;
        int windowCount = 0;
        int presentedScanlines = 0;
        boolean d3d12Presented = false;
        boolean popupHosted = false;
        boolean messageLoopRan = false;
        String inspectorJson;
        String label;
        MemorySegment png;
        MemorySegment extendedLinear;
        int inspectorNodes;
        try (StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.mount(
                "label",
                element -> element.bind(
                        "text",
                        String.class,
                        AnimationPhaseImpact.MEASURE,
                        () -> "Count: " + count.get()
                )
        ))) {
            runtime.update();
            runtime.applyMountedProperties();
            LayoutTree layout = new LayoutTree();
            layout.setRoot(BootstrapCounterPane.create(layout, clicks));
            layout.measure(Constraints.loose(WIDTH, HEIGHT));
            layout.place();
            SemanticsNode button = layout.semantics().nodeWith(SemanticsAction.ACTIVATE);
            LayoutRect bounds = button.bounds();
            if (host == DesktopHost.WINDOWS) {
                WindowsPlatform platform = new WindowsBackend().open().toCompletableFuture().get();
                try {
                    WindowsWindow[] hosted = new WindowsWindow[2];
                    WindowsWindow primary = platform.createWindow(
                            WindowRequest.toplevel(new WindowConfiguration(
                                    "HimariUI",
                                    new LogicalRect(48.0, 48.0, WIDTH, HEIGHT),
                                    true,
                                    WindowState.NORMAL
                            )),
                            event -> closeOnRequest(hosted, event)
                    ).toCompletableFuture().get();
                    hosted[0] = primary;
                    WindowsWindow secondary = platform.createWindow(
                            WindowRequest.toplevel(new WindowConfiguration(
                                    "HimariUI-2",
                                    new LogicalRect(320.0, 48.0, WIDTH, HEIGHT),
                                    true,
                                    WindowState.NORMAL
                            )),
                            event -> closeOnRequest(hosted, event)
                    ).toCompletableFuture().get();
                    hosted[1] = secondary;
                    platform.pump();
                    primary.takePointerEvents();
                    secondary.takePointerEvents();
                    windowCreated = !primary.isClosed() && !secondary.isClosed();
                    windowCount = platform.openWindowCount();
                    WindowsWindow hostedPopup = WindowsPopupHost.show(
                            platform,
                            primary,
                            "HimariUI-Menu",
                            new LogicalRect(80.0, 80.0, 160.0, 80.0),
                            () -> { }
                    );
                    platform.pump();
                    popupHosted = !hostedPopup.isClosed()
                            && hostedPopup.snapshot().role() == SurfaceRole.POPUP;
                    hostedPopup.closeAsync().toCompletableFuture().get();
                    platform.pump();
                    int x = Math.round(bounds.x() + 2.0f);
                    int y = Math.round(bounds.y() + 2.0f);
                    activateThroughWndProc(platform, primary, layout, clicks, count, runtime, x, y);
                    activateThroughWndProc(platform, primary, layout, clicks, count, runtime, x, y);
                    label = String.valueOf(runtime.mounts().snapshot().elements().getFirst().property("text").value());
                    InspectorSnapshot inspector = Inspector.capture(layout, runtime.trace());
                    inspectorNodes = inspector.nodes().size();
                    inspectorJson = inspector.toCanonicalJson();
                    SfntFont font = BitmapSfntFont.create();
                    DisplayList displayList = record(label, font);
                    SoftwareSurface surface = new SoftwareSurface(WIDTH, HEIGHT);
                    surface.clear(Color.srgb(0.95f, 0.95f, 0.97f, 1.0f));
                    surface.replay(displayList);
                    png = surface.toSdrPng();
                    extendedLinear = floats(surface.extendedLinearPremultiplied());
                    MemorySegment rgba = surface.toSdrRgba();
                    presentedScanlines += primary.presentSdrRgba(rgba, WIDTH, HEIGHT);
                    presentedScanlines += secondary.presentSdrRgba(rgba, WIDTH, HEIGHT);
                    d3d12Presented = presentD3d12(primary.nativeHandle(), rgba, WIDTH, HEIGHT);
                    platform.pump();
                    if (!new SceneEnvelope(SceneEnvelope.CURRENT_SCHEMA, WIDTH, HEIGHT, displayList)
                            .toCanonicalJson()
                            .equals(SceneEnvelope.parse(
                                    new SceneEnvelope(SceneEnvelope.CURRENT_SCHEMA, WIDTH, HEIGHT, displayList)
                                            .toCanonicalJson()
                            ).toCanonicalJson())) {
                        throw new IllegalStateException("Desktop scene envelope did not round-trip");
                    }
                    if (smoke) {
                        primary.closeAsync().toCompletableFuture().get();
                        secondary.closeAsync().toCompletableFuture().get();
                        platform.pump();
                    } else {
                        platform.pumpUntilClosed();
                        messageLoopRan = true;
                    }
                } finally {
                    if (!platform.isClosed()) {
                        platform.close();
                    }
                }
            } else {
                layout.dispatch(new PointerEvent(PointerEventType.DOWN, bounds.x() + 2.0f, bounds.y() + 2.0f));
                layout.dispatch(new PointerEvent(PointerEventType.UP, bounds.x() + 2.0f, bounds.y() + 2.0f));
                layout.dispatch(new PointerEvent(PointerEventType.DOWN, bounds.x() + 2.0f, bounds.y() + 2.0f));
                layout.dispatch(new PointerEvent(PointerEventType.UP, bounds.x() + 2.0f, bounds.y() + 2.0f));
                count.set(clicks.get());
                runtime.update();
                runtime.applyMountedProperties();
                label = String.valueOf(runtime.mounts().snapshot().elements().getFirst().property("text").value());
                InspectorSnapshot inspector = Inspector.capture(layout, runtime.trace());
                inspectorNodes = inspector.nodes().size();
                inspectorJson = inspector.toCanonicalJson();
                SfntFont font = BitmapSfntFont.create();
                DisplayList displayList = record(label, font);
                SoftwareSurface surface = new SoftwareSurface(WIDTH, HEIGHT);
                surface.clear(Color.srgb(0.95f, 0.95f, 0.97f, 1.0f));
                surface.replay(displayList);
                png = surface.toSdrPng();
                extendedLinear = floats(surface.extendedLinearPremultiplied());
                if (!new SceneEnvelope(SceneEnvelope.CURRENT_SCHEMA, WIDTH, HEIGHT, displayList)
                        .toCanonicalJson()
                        .equals(SceneEnvelope.parse(
                                new SceneEnvelope(SceneEnvelope.CURRENT_SCHEMA, WIDTH, HEIGHT, displayList)
                                        .toCanonicalJson()
                        ).toCanonicalJson())) {
                    throw new IllegalStateException("Desktop scene envelope did not round-trip");
                }
            }
        }
        DesktopLaunchResult result = new DesktopLaunchResult(
                host,
                windowCreated,
                windowCount,
                smoke,
                count.get(),
                label,
                inspectorNodes,
                Math.toIntExact(png.byteSize()),
                Math.toIntExact(extendedLinear.byteSize()),
                presentedScanlines,
                d3d12Presented,
                popupHosted,
                messageLoopRan,
                wayland.status(),
                macos.status(),
                metal.status(),
                objc.status()
        );
        if (output != null) {
            Files.createDirectories(output);
            Files.write(output.resolve("counter.png"), png.toArray(ValueLayout.JAVA_BYTE));
            Files.write(output.resolve("counter.extlin"), extendedLinear.toArray(ValueLayout.JAVA_BYTE));
            Files.writeString(output.resolve("inspector.json"), inspectorJson, StandardCharsets.UTF_8);
            Files.writeString(output.resolve("results.json"), result.toJson(), StandardCharsets.UTF_8);
        }
        return result;
    }

    /// Closes the matching hosted HWND when the host asks the application to decide.
    ///
    /// @param hosted the primary and secondary windows
    /// @param event the host event
    private static void closeOnRequest(WindowsWindow[] hosted, WindowEvent event) {
        if (event.type() != WindowEventType.CLOSE_REQUESTED) {
            return;
        }
        for (WindowsWindow candidate : hosted) {
            if (candidate != null
                    && !candidate.isClosed()
                    && candidate.id().equals(event.snapshot().id())) {
                candidate.closeAsync();
            }
        }
    }

    /// Posts one left-button click through WndProc and dispatches the drained events.
    ///
    /// @param platform the session
    /// @param window the HWND that receives the posted messages
    /// @param layout the tree that consumes normalized pointer events
    /// @param clicks the bootstrap activation counter
    /// @param count the mounted label state
    /// @param runtime the structural runtime
    /// @param x the client x
    /// @param y the client y
    private static void activateThroughWndProc(
            WindowsPlatform platform,
            WindowsWindow window,
            LayoutTree layout,
            AtomicInteger clicks,
            IntState count,
            StructuralRuntime runtime,
            int x,
            int y
    ) {
        window.postPointer(PointerEventType.DOWN, x, y);
        platform.pump();
        window.postPointer(PointerEventType.UP, x, y);
        platform.pump();
        for (PointerEvent event : window.takePointerEvents()) {
            layout.dispatch(event);
        }
        count.set(clicks.get());
        runtime.update();
        runtime.applyMountedProperties();
    }

    /// Records the label as filled chrome and grayscale glyphs.
    ///
    /// @param label the mounted counter text
    /// @param font the sample font
    /// @return the display list
    private static DisplayList record(String label, SfntFont font) {
        ArrayList<DisplayListOp> ops = new ArrayList<>();
        ops.add(new DisplayListOp.FillRect(8.0f, 8.0f, WIDTH - 16.0f, 32.0f, Color.srgb(0.2f, 0.4f, 0.8f, 1.0f)));
        float x = 16.0f;
        float y = 12.0f;
        for (ShapedGlyph glyph : DefaultShaper.shape(font, label)) {
            GlyphMask mask = GlyphRasterizer.rasterize(font, glyph.glyphId(), 16);
            if (mask.width() > 0) {
                ops.add(new DisplayListOp.DrawGlyph(
                        x,
                        y,
                        mask.width(),
                        mask.height(),
                        mask.coverage(),
                        Color.SRGB_WHITE
                ));
            }
            x += glyph.xAdvance() * (16.0f / font.unitsPerEm());
        }
        return new DisplayList(List.copyOf(ops));
    }

    /// Presents RGBA through D3D12 when a device is available.
    ///
    /// Missing adapters or present failures leave the GDI blit as the visible frame.
    ///
    /// @param hwnd the native window
    /// @param rgba the software pixels
    /// @param width the width
    /// @param height the height
    /// @return whether D3D12 presented without applying HDR metadata
    static boolean presentD3d12(MemorySegment hwnd, MemorySegment rgba, int width, int height) {
        @Nullable D3d12Device device = D3d12Device.tryOpen();
        if (device == null) {
            return false;
        }
        try (device) {
            D3d12Presentation gpu = device.presentSdrRgba(hwnd, rgba, width, height);
            return gpu.presented() && !gpu.hdrMetadataApplied();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /// Encodes floats as little-endian bytes.
    ///
    /// @param values the components
    /// @return the bytes
    private static MemorySegment floats(float[] values) {
        Objects.requireNonNull(values, "values");
        byte[] bytes = new byte[values.length * 4];
        MemorySegment segment = MemorySegment.ofArray(bytes);
        ValueLayout.OfFloat layout = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
        for (int index = 0; index < values.length; index++) {
            segment.setAtIndex(layout, index, values[index]);
        }
        return segment.asReadOnly();
    }
}
