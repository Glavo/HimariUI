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

/// Creates an unstyled square button identified by an icon name.
@NotNullByDefault
public final class IconButton {
    /// Default control size.
    private static final Size SIZE = new Size(24.0f, 24.0f);

    /// Icon name used as the accessible label.
    private final String icon;

    /// Activation count.
    private final AtomicInteger activations = new AtomicInteger();

    /// Optional extra activation callback.
    private final Runnable onActivate;

    /// Creates an icon button.
    ///
    /// @param icon the icon name
    /// @param onActivate the activation callback
    public IconButton(String icon, Runnable onActivate) {
        this.icon = Objects.requireNonNull(icon, "icon");
        if (icon.isEmpty()) {
            throw new IllegalArgumentException("Icon name must not be empty");
        }
        this.onActivate = Objects.requireNonNull(onActivate, "onActivate");
    }

    /// Returns the icon name.
    ///
    /// @return the name
    public String icon() {
        return icon;
    }

    /// Returns the activation count.
    ///
    /// @return the count
    public int activations() {
        return activations.get();
    }

    /// Builds the icon-button leaf.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        return factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(2.0f)),
                true,
                SemanticsRole.BUTTON,
                icon,
                Set.of(SemanticsAction.ACTIVATE),
                this::activate
        );
    }

    /// Records one activation and runs the caller callback.
    private void activate() {
        activations.incrementAndGet();
        onActivate.run();
    }
}
