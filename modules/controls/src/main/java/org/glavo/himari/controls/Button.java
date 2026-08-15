package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/// Creates an unstyled activatable button.
@NotNullByDefault
public final class Button {
    /// Approximate glyph cell width used before theme metrics exist.
    private static final float CELL_WIDTH = 8.0f;

    /// Default control height.
    private static final float HEIGHT = 24.0f;

    /// Activation count.
    private final AtomicInteger activations = new AtomicInteger();

    /// Visible and accessible label.
    private final String label;

    /// Optional extra activation callback.
    private final Runnable onActivate;

    /// Creates a button.
    ///
    /// @param label the accessible name
    /// @param onActivate the activation callback
    public Button(String label, Runnable onActivate) {
        this.label = Objects.requireNonNull(label, "label");
        this.onActivate = Objects.requireNonNull(onActivate, "onActivate");
    }

    /// Returns the activation count.
    ///
    /// @return the count
    public int activations() {
        return activations.get();
    }

    /// Returns the accessible name.
    ///
    /// @return the label
    public String label() {
        return label;
    }

    /// Builds the button leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        return factory.leaf(
                name,
                new Size(Math.max(48.0f, label.length() * CELL_WIDTH + 16.0f), HEIGHT),
                List.of(new LayoutModifier.Padding(4.0f)),
                true,
                SemanticsRole.BUTTON,
                label,
                Set.of(SemanticsAction.ACTIVATE),
                this::activate
        );
    }

    /// Activates the button as if it were invoked by input.
    public void press() {
        activate();
    }

    /// Records one activation and runs the caller callback.
    private void activate() {
        activations.incrementAndGet();
        onActivate.run();
    }
}
