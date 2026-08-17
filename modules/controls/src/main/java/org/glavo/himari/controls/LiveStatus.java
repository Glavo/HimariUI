package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsLiveRegion;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

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

    /// Live-region politeness published by [#create(LayoutFactory, String)].
    private SemanticsLiveRegion liveRegion = SemanticsLiveRegion.POLITE;

    /// Last leaf published by [#create(LayoutFactory, String)], or `null` before the first create.
    private @Nullable LayoutNode published;

    /// Whether the status is disabled.
    private boolean disabled;

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

    /// Replaces the announcement and updates the last published leaf label when one exists.
    ///
    /// [`LayoutNode#setLabel(String)`] notifies live-region listeners so a host accessibility
    /// bridge can raise `UIA_LiveRegionChangedEventId`.
    ///
    /// @param message the announcement
    public void announce(String message) {
        this.message = requireMessage(message);
        if (published != null) {
            published.setLabel(this.message);
        }
    }

    /// Replaces the politeness published by the next [#create(LayoutFactory, String)].
    ///
    /// @param liveRegion the politeness
    public void setLiveRegion(SemanticsLiveRegion liveRegion) {
        this.liveRegion = Objects.requireNonNull(liveRegion, "liveRegion");
    }

    /// Returns the published politeness.
    ///
    /// @return the politeness
    public SemanticsLiveRegion liveRegion() {
        return liveRegion;
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
        node.setLiveRegion(liveRegion);
        node.setDisabled(disabled);
        this.published = node;
        return node;
    }

    /// Returns whether the status is disabled.
    ///
    /// @return whether the status is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the last created leaf when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (published != null) {
            published.setDisabled(disabled);
        }
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
