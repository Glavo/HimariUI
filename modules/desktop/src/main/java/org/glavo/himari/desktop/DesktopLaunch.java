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
import org.glavo.himari.platform.api.WindowRequest;
import org.glavo.himari.platform.api.WindowState;
import org.glavo.himari.platform.macos.MacosProbe;
import org.glavo.himari.platform.wayland.WaylandProbe;
import org.glavo.himari.platform.windows.WindowsBackend;
import org.glavo.himari.platform.windows.WindowsPlatform;
import org.glavo.himari.platform.windows.WindowsWindow;
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

import java.nio.ByteBuffer;
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
    /// On Windows a real HWND is created, pumped, and closed when `smoke` is true. Other hosts
    /// still execute the Headless CounterApp tree and record environment-blocked backend probes.
    ///
    /// @param smoke whether to close host windows before returning
    /// @param output the optional artifact directory
    /// @return the launch result
    /// @throws Exception if the host window or artifact write fails
    public static DesktopLaunchResult run(boolean smoke, @Nullable Path output) throws Exception {
        DesktopHost host = DesktopHost.detect();
        WaylandProbe wayland = WaylandProbe.run();
        MacosProbe macos = MacosProbe.run();
        MetalProbe metal = MetalProbe.run();
        ObjcBlockProbe objc = ObjcBlockProbe.run();
        StateDomain domain = new StateDomain();
        IntState count = domain.intState(0);
        AtomicInteger clicks = new AtomicInteger();
        boolean windowCreated = false;
        int windowCount = 0;
        String inspectorJson;
        String label;
        byte[] png;
        byte[] extendedLinear;
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
                    WindowsWindow primary = platform.createWindow(
                            WindowRequest.toplevel(new WindowConfiguration(
                                    "HimariUI",
                                    new LogicalRect(48.0, 48.0, WIDTH, HEIGHT),
                                    true,
                                    WindowState.NORMAL
                            )),
                            event -> { }
                    ).toCompletableFuture().get();
                    WindowsWindow secondary = platform.createWindow(
                            WindowRequest.toplevel(new WindowConfiguration(
                                    "HimariUI-2",
                                    new LogicalRect(320.0, 48.0, WIDTH, HEIGHT),
                                    true,
                                    WindowState.NORMAL
                            )),
                            event -> { }
                    ).toCompletableFuture().get();
                    platform.pump();
                    windowCreated = !primary.isClosed() && !secondary.isClosed();
                    windowCount = platform.openWindowCount();
                    int x = Math.round(bounds.x() + 2.0f);
                    int y = Math.round(bounds.y() + 2.0f);
                    primary.postPointer(PointerEventType.DOWN, x, y);
                    platform.pump();
                    primary.postPointer(PointerEventType.UP, x, y);
                    platform.pump();
                    for (PointerEvent event : primary.takePointerEvents()) {
                        layout.dispatch(event);
                    }
                    count.set(clicks.get());
                    runtime.update();
                    runtime.applyMountedProperties();
                    layout.dispatch(new PointerEvent(PointerEventType.DOWN, bounds.x() + 2.0f, bounds.y() + 2.0f));
                    layout.dispatch(new PointerEvent(PointerEventType.UP, bounds.x() + 2.0f, bounds.y() + 2.0f));
                    count.set(clicks.get());
                    runtime.update();
                    runtime.applyMountedProperties();
                    if (smoke) {
                        primary.closeAsync().toCompletableFuture().get();
                        secondary.closeAsync().toCompletableFuture().get();
                        platform.pump();
                    }
                } finally {
                    if (smoke) {
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
            }
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
        DesktopLaunchResult result = new DesktopLaunchResult(
                host,
                windowCreated,
                windowCount,
                smoke,
                count.get(),
                label,
                inspectorNodes,
                png.length,
                extendedLinear.length,
                wayland.status(),
                macos.status(),
                metal.status(),
                objc.status()
        );
        if (output != null) {
            Files.createDirectories(output);
            Files.write(output.resolve("counter.png"), png);
            Files.write(output.resolve("counter.extlin"), extendedLinear);
            Files.writeString(output.resolve("inspector.json"), inspectorJson, StandardCharsets.UTF_8);
            Files.writeString(output.resolve("results.json"), result.toJson(), StandardCharsets.UTF_8);
        }
        return result;
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

    /// Encodes floats as little-endian bytes.
    ///
    /// @param values the components
    /// @return the bytes
    private static byte[] floats(float[] values) {
        Objects.requireNonNull(values, "values");
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : values) {
            buffer.putFloat(value);
        }
        return buffer.array();
    }
}
