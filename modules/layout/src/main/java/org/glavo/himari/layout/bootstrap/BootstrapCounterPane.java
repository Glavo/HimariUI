package org.glavo.himari.layout.bootstrap;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/// Builds the private V0 counter interaction tree from M2 contracts only.
@NotNullByDefault
public final class BootstrapCounterPane {
    /// Prevents instantiation.
    private BootstrapCounterPane() {
    }

    /// Creates a column containing a count label and an increment button.
    ///
    /// @param tree the layout tree
    /// @param count the counter cell
    /// @return the pane root
    public static LayoutNode create(LayoutTree tree, AtomicInteger count) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(count, "count");
        LayoutFactory factory = new LayoutFactory(tree);
        LayoutNode label = BootstrapLabel.create(factory, "count-label", "Count: " + count.get());
        LayoutNode button = BootstrapButton.create(factory, "increment", "Increment", count::incrementAndGet);
        return factory.column(
                "counter-pane",
                Alignment.START,
                List.of(new LayoutModifier.Padding(8.0f)),
                label,
                button
        );
    }
}
