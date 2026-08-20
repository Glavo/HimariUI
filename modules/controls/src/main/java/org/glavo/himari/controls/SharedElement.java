package org.glavo.himari.controls;

import org.glavo.himari.layout.Alignment;
import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutRect;
import org.glavo.himari.layout.Size;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.glavo.himari.platform.api.LogicalRect;
import org.glavo.himari.runtime.animation.TweenSpec;
import org.glavo.himari.runtime.transition.MatchedGeometryKey;
import org.glavo.himari.runtime.transition.MatchedGeometryLink;
import org.glavo.himari.runtime.transition.RetainedPresentation;
import org.glavo.himari.runtime.transition.TransitionIdentity;
import org.glavo.himari.runtime.transition.TransitionLifetime;
import org.glavo.himari.runtime.transition.TransitionParticipation;
import org.glavo.himari.runtime.transition.TransitionPhase;
import org.glavo.himari.runtime.transition.TransitionRegistry;
import org.glavo.himari.runtime.transition.TransitionSpec;
import org.glavo.himari.runtime.transition.VisibilityTransition;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Hosts one first-stable matched-geometry pair as a public control overlay.
///
/// After both leaves are placed, [#overlay(double)] captures source and destination bounds
/// through [`TransitionRegistry`] and interpolates the shared-element rectangle. Geometry matching
/// links presentation only and does not transfer application state.
///
/// [#detach(long)] keeps the element's owner and does not retain an exit overlay.
/// [#remove(long)] disposes the owner and retains presentation data for the active exit.
@NotNullByDefault
public final class SharedElement {
    /// Source leaf size.
    private static final Size SOURCE_SIZE = new Size(16.0f, 16.0f);

    /// Destination leaf size.
    private static final Size DESTINATION_SIZE = new Size(32.0f, 24.0f);

    /// Exit duration used so a removal can retain presentation while exiting.
    private static final long EXIT_NANOS = 1_000_000_000L;

    /// Matched-geometry key.
    private final MatchedGeometryKey key;

    /// Registry that owns the current-frame captures.
    private final TransitionRegistry registry = new TransitionRegistry();

    /// Visibility state machine that owns detach versus remove lifetimes.
    private final VisibilityTransition visibility;

    /// Source leaf after [#create(LayoutFactory, String)].
    private @Nullable LayoutNode sourceNode;

    /// Destination leaf after [#create(LayoutFactory, String)].
    private @Nullable LayoutNode destinationNode;

    /// Last accepted pair, or `null` before a successful capture.
    private @Nullable MatchedGeometryLink link;

    /// Creates a pair in `namespace` with identity `id`.
    ///
    /// @param namespace the geometry namespace
    /// @param id the stable identity
    public SharedElement(String namespace, String id) {
        this.key = new MatchedGeometryKey(namespace, id);
        this.visibility = new VisibilityTransition(
                new TransitionIdentity(namespace, id),
                id,
                TransitionSpec.symmetric(
                        TweenSpec.linear(EXIT_NANOS),
                        TransitionParticipation.OVERLAY
                )
        );
    }

    /// Returns the geometry key.
    ///
    /// @return the key
    public MatchedGeometryKey key() {
        return key;
    }

    /// Returns the last accepted pair, or `null` before [#overlay(double)] succeeds.
    ///
    /// @return the link
    public @Nullable MatchedGeometryLink link() {
        return link;
    }

    /// Builds the source and destination leaves.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @return the column that hosts both leaves
    public LayoutNode create(LayoutFactory factory, String name) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        this.sourceNode = factory.leaf(
                name + "-source",
                SOURCE_SIZE,
                List.of(),
                false,
                SemanticsRole.IMAGE,
                key.id(),
                Set.of(),
                null
        );
        this.destinationNode = factory.leaf(
                name + "-destination",
                DESTINATION_SIZE,
                List.of(new LayoutModifier.Padding(4.0f)),
                false,
                SemanticsRole.IMAGE,
                key.id(),
                Set.of(),
                null
        );
        this.link = null;
        return factory.column(
                name,
                Alignment.START,
                List.of(),
                sourceNode,
                destinationNode
        );
    }

    /// Captures placed source and destination bounds and interpolates the overlay.
    ///
    /// @param progress the unit progress in `[0, 1]`
    /// @return the overlay rectangle
    public LogicalRect overlay(double progress) {
        if (sourceNode == null || destinationNode == null) {
            throw new IllegalStateException("Shared element must be created before overlay capture");
        }
        registry.beginFrame();
        registry.captureSource(key, logical(sourceNode.bounds()));
        registry.captureDestination(key, logical(destinationNode.bounds()));
        @Nullable MatchedGeometryLink next = registry.link(key);
        if (next == null) {
            throw new IllegalStateException("Matched-geometry pair was incomplete or conflicted");
        }
        this.link = next;
        captureBounds();
        return next.interpolate(progress);
    }

    /// Begins or continues becoming visible and remounts a disposed owner.
    ///
    /// @param nowNanos the monotonic timestamp
    public void show(long nowNanos) {
        captureBounds();
        visibility.show(nowNanos);
    }

    /// Detaches the element while retaining local state and the element's owner.
    ///
    /// @param nowNanos the monotonic timestamp
    public void detach(long nowNanos) {
        captureBounds();
        visibility.detach(nowNanos);
    }

    /// Removes the element, disposes its owner, and retains exit presentation.
    ///
    /// @param nowNanos the monotonic timestamp
    public void remove(long nowNanos) {
        captureBounds();
        visibility.remove(nowNanos);
    }

    /// Samples presentation progress at one timestamp.
    ///
    /// @param nowNanos the monotonic timestamp
    /// @return the phase after sampling
    public TransitionPhase sample(long nowNanos) {
        return visibility.sample(nowNanos);
    }

    /// Returns the current lifetime.
    ///
    /// @return the lifetime
    public TransitionLifetime lifetime() {
        return visibility.lifetime();
    }

    /// Returns whether the element's owner is disposed.
    ///
    /// @return whether the owner is gone
    public boolean ownerDisposed() {
        return visibility.ownerDisposed();
    }

    /// Returns the retained exit presentation, or `null`.
    ///
    /// @return the retained presentation
    public @Nullable RetainedPresentation retainedPresentation() {
        return visibility.retainedPresentation();
    }

    /// Publishes placed source bounds onto the visibility owner when available.
    private void captureBounds() {
        if (sourceNode != null) {
            visibility.setBounds(logical(sourceNode.bounds()));
        }
    }

    /// Converts a placed layout rectangle into a logical rectangle.
    private static LogicalRect logical(LayoutRect bounds) {
        return new LogicalRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }
}
