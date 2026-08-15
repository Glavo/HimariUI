package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Creates a polite live-region status announcement.
@NotNullByDefault
public final class LiveStatus {
    /// Default status size.
    private static final Size SIZE = new Size(180.0f, 20.0f);

    /// Current announcement text.
    private String message;

    /// Creates a status with an initial announcement.
    ///
    /// @param message the initial announcement
    public LiveStatus(String message) {
        this.message = requireMessage(message);
    }

    /// Returns the current announcement.
    ///
    /// @return the message
    public String message() {
        return message;
    }

    /// Replaces the announcement published by the next [#create(LayoutFactory, String)].
    ///
    /// @param message the announcement
    public void announce(String message) {
        this.message = requireMessage(message);
    }

    /// Builds the status leaf as a polite live region.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the leaf
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        LayoutNode node = factory.leaf(
                name,
                SIZE,
                List.of(new LayoutModifier.Padding(2.0f)),
                false,
                SemanticsRole.STATUS,
                message,
                Set.of(),
                null
        );
        node.setLiveRegion(SemanticsLiveRegion.POLITE);
        return node;
    }

    /// Rejects a blank announcement.
    ///
    /// @param message the candidate
    /// @return the message
    private static String requireMessage(String message) {
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Status message must be nonblank");
        }
        return message;
    }
}
