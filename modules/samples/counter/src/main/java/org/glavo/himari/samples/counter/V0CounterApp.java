package org.glavo.himari.samples.counter;

import org.glavo.himari.font.BitmapSfntFont;
import org.glavo.himari.font.GlyphMask;
import org.glavo.himari.font.GlyphRasterizer;
import org.glavo.himari.font.SfntFont;
import org.glavo.himari.graphics.Color;
import org.glavo.himari.graphics.DisplayList;
import org.glavo.himari.graphics.DisplayListOp;
import org.glavo.himari.graphics.SceneEnvelope;
import org.glavo.himari.layout.Constraints;
import org.glavo.himari.layout.LayoutRect;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.bootstrap.BootstrapCounterPane;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.platform.headless.HeadlessEventLoop;
import org.glavo.himari.render.software.SoftwareSurface;
import org.glavo.himari.runtime.animation.AnimatedScalar;
import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.runtime.animation.AnimationRegistry;
import org.glavo.himari.runtime.animation.AnimationReplacementPolicy;
import org.glavo.himari.runtime.animation.AnimationTransaction;
import org.glavo.himari.runtime.animation.ScalarAnimationAdapter;
import org.glavo.himari.runtime.animation.TweenSpec;
import org.glavo.himari.runtime.structure.StructuralRuntime;
import org.glavo.himari.state.IntState;
import org.glavo.himari.state.StateDomain;
import org.glavo.himari.text.DefaultShaper;
import org.glavo.himari.text.ShapedGlyph;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/// Runs the V0 Headless architecture seed: structure, layout, input, text, and software rendering.
@NotNullByDefault
public final class V0CounterApp {
    /// Surface width.
    private static final int WIDTH = 240;

    /// Surface height.
    private static final int HEIGHT = 120;

    /// Prevents instantiation.
    private V0CounterApp() {
    }

    /// Executes two injected activations and writes PNG, extended-linear, and scene artifacts.
    ///
    /// @param arguments optional output directory
    /// @throws IOException if artifacts cannot be written
    public static void main(String[] arguments) throws IOException {
        Path output = arguments.length == 0
                ? Path.of("build/conformance/v0-counter")
                : Path.of(arguments[0]);
        Files.createDirectories(output);
        Result result = run(2);
        Files.write(output.resolve("counter.png"), result.png().toArray(ValueLayout.JAVA_BYTE));
        Files.write(output.resolve("counter.extlin"), result.extendedLinear().toArray(ValueLayout.JAVA_BYTE));
        Files.writeString(output.resolve("scene.json"), result.sceneJson(), StandardCharsets.UTF_8);
        Files.writeString(output.resolve("results.json"), result.summaryJson(), StandardCharsets.UTF_8);
        System.out.println("V0 CounterApp count=" + result.count() + " png=" + result.png().byteSize());
    }

    /// Executes the injected sequence and returns the rendered artifacts.
    ///
    /// @param activations the number of increment activations
    /// @return the result
    public static Result run(int activations) {
        if (activations < 0) {
            throw new IllegalArgumentException("activations must be nonnegative");
        }
        StateDomain domain = new StateDomain();
        IntState count = domain.intState(0);
        try (HeadlessEventLoop eventLoop = new HeadlessEventLoop();
             AnimationRegistry registry = new AnimationRegistry(eventLoop);
             StructuralRuntime runtime = new StructuralRuntime(domain, scope -> scope.mount(
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
            AtomicInteger clicks = new AtomicInteger();
            layout.setRoot(BootstrapCounterPane.create(layout, clicks));
            layout.measure(Constraints.loose(WIDTH, HEIGHT));
            layout.place();
            SemanticsNode button = layout.semantics().nodeWith(SemanticsAction.ACTIVATE);
            LayoutRect bounds = button.bounds();
            boolean keyboardObserved = false;
            for (int index = 0; index < activations; index++) {
                if (index == 0 || activations == 1) {
                    layout.dispatch(new PointerEvent(PointerEventType.DOWN, bounds.x() + 2.0f, bounds.y() + 2.0f));
                    layout.dispatch(new PointerEvent(PointerEventType.UP, bounds.x() + 2.0f, bounds.y() + 2.0f));
                } else {
                    layout.dispatch(new KeyEvent(KeyEventType.DOWN, LogicalKey.ENTER));
                    keyboardObserved = true;
                }
                count.set(clicks.get());
                runtime.update();
                runtime.applyMountedProperties();
            }
            AnimatedScalar highlight = registry.createScalar(
                    "highlight",
                    0.0,
                    ScalarAnimationAdapter.UNIT_INTERVAL,
                    AnimationPhaseImpact.COMPOSITE
            );
            long epochBeforeMotion = domain.epoch();
            registry.commit(
                    AnimationTransaction.standard(
                            1L,
                            1L,
                            1L,
                            TweenSpec.linear(1_000_000_000L),
                            AnimationReplacementPolicy.PRESERVE_VELOCITY
                    ),
                    commit -> commit.setTarget(highlight, 1.0)
            );
            if (domain.epoch() != epochBeforeMotion) {
                throw new IllegalStateException("Animation commit wrote application state");
            }
            double modelTarget = highlight.modelTarget();
            double startPresentation = highlight.presentationValue();
            eventLoop.clock().advanceBy(500_000_000L);
            registry.sample();
            double midPresentation = highlight.presentationValue();
            eventLoop.clock().advanceBy(500_000_000L);
            registry.sample();
            double finalPresentation = highlight.presentationValue();
            if (modelTarget != 1.0 || startPresentation != 0.0 || midPresentation <= startPresentation
                    || midPresentation >= modelTarget || finalPresentation != modelTarget) {
                throw new IllegalStateException(
                        "Model/presentation did not separate on the Headless manual clock"
                );
            }
            String label = String.valueOf(runtime.mounts().snapshot().elements().getFirst().property("text").value());
            SfntFont font = BitmapSfntFont.create();
            DisplayList displayList = record(label, font);
            String encoded = new SceneEnvelope(SceneEnvelope.CURRENT_SCHEMA, WIDTH, HEIGHT, displayList)
                    .toCanonicalJson();
            if (!encoded.equals(SceneEnvelope.parse(encoded).toCanonicalJson())) {
                throw new IllegalStateException("Scene envelope did not round-trip");
            }
            SoftwareSurface surface = new SoftwareSurface(WIDTH, HEIGHT);
            surface.clear(Color.srgb(0.95f, 0.95f, 0.97f, 1.0f));
            surface.replay(displayList);
            String sceneJson = new SceneEnvelope(SceneEnvelope.CURRENT_SCHEMA, WIDTH, HEIGHT, displayList)
                    .toCanonicalJson();
            return new Result(
                    count.get(),
                    label,
                    layout.focus().focusedId() != null,
                    button.actions().contains(SemanticsAction.ACTIVATE),
                    keyboardObserved,
                    modelTarget,
                    midPresentation,
                    finalPresentation,
                    surface.toSdrPng(),
                    floats(surface.extendedLinearPremultiplied()),
                    sceneJson
            );
        }
    }

    /// Records the label as a display list of filled chrome and grayscale glyphs.
    ///
    /// @param label the label
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
    /// @param values the floats
    /// @return the bytes
    private static MemorySegment floats(float[] values) {
        byte[] bytes = new byte[values.length * 4];
        MemorySegment segment = MemorySegment.ofArray(bytes);
        ValueLayout.OfFloat layout = ValueLayout.JAVA_FLOAT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
        for (int index = 0; index < values.length; index++) {
            segment.setAtIndex(layout, index, values[index]);
        }
        return segment.asReadOnly();
    }

    /// Stores one V0 run.
    ///
    /// @param count the counter
    /// @param label the mounted label
    /// @param focusObserved whether focus was assigned
    /// @param semanticsExposeActivate whether semantics expose ACTIVATE
    /// @param keyboardObserved whether an injected Enter activated the button
    /// @param animationModelTarget the committed highlight model target
    /// @param animationMidPresentation the highlight presentation at 500 ms
    /// @param animationFinalPresentation the highlight presentation after the tween
    /// @param png the SDR PNG
    /// @param extendedLinear the extended-linear capture
    /// @param sceneJson the canonical scene
    @NotNullByDefault
    public record Result(
            int count,
            String label,
            boolean focusObserved,
            boolean semanticsExposeActivate,
            boolean keyboardObserved,
            double animationModelTarget,
            double animationMidPresentation,
            double animationFinalPresentation,
            MemorySegment png,
            MemorySegment extendedLinear,
            String sceneJson
    ) {
        /// Encodes a machine-readable summary.
        ///
        /// @return the JSON
        public String summaryJson() {
            return """
                    {
                      "profile": "v0-counter",
                      "status": "passed",
                      "count": %d,
                      "label": "%s",
                      "focusObserved": %s,
                      "semanticsExposeActivate": %s,
                      "keyboardObserved": %s,
                      "animationModelTarget": %s,
                      "animationMidPresentation": %s,
                      "animationFinalPresentation": %s,
                      "pngBytes": %d,
                      "extendedLinearBytes": %d,
                      "nativeLibraryLoaded": false
                    }
                    """.formatted(
                    count,
                    label,
                    focusObserved,
                    semanticsExposeActivate,
                    keyboardObserved,
                    animationModelTarget,
                    animationMidPresentation,
                    animationFinalPresentation,
                    png.byteSize(),
                    extendedLinear.byteSize()
            );
        }
    }
}
