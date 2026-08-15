package org.glavo.himari.runtime.transition;

import org.glavo.himari.platform.api.LogicalRect;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Owns structural transitions and matched-geometry captures for one update frame.
///
/// Duplicate identities and duplicate geometry sources are recorded as diagnostics. The registry
/// never selects a winner by traversal or allocation order.
@NotNullByDefault
public final class TransitionRegistry {
    /// Live transitions keyed by structural identity.
    private final LinkedHashMap<TransitionIdentity, VisibilityTransition> transitions = new LinkedHashMap<>();

    /// Identities claimed in the current frame.
    private final LinkedHashSet<TransitionIdentity> claimed = new LinkedHashSet<>();

    /// Identities claimed more than once in the current frame.
    private final LinkedHashSet<TransitionIdentity> identityConflicts = new LinkedHashSet<>();

    /// First source rectangle captured for each key in the current frame.
    private final LinkedHashMap<MatchedGeometryKey, LogicalRect> sources = new LinkedHashMap<>();

    /// First destination rectangle captured for each key in the current frame.
    private final LinkedHashMap<MatchedGeometryKey, LogicalRect> destinations = new LinkedHashMap<>();

    /// Source keys captured more than once in the current frame.
    private final LinkedHashSet<MatchedGeometryKey> sourceConflicts = new LinkedHashSet<>();

    /// Destination keys captured more than once in the current frame.
    private final LinkedHashSet<MatchedGeometryKey> destinationConflicts = new LinkedHashSet<>();

    /// Creates an empty registry.
    public TransitionRegistry() {
    }

    /// Clears per-frame identity claims and geometry captures.
    public void beginFrame() {
        claimed.clear();
        identityConflicts.clear();
        sources.clear();
        destinations.clear();
        sourceConflicts.clear();
        destinationConflicts.clear();
    }

    /// Returns an existing presentation or creates one for a new or previously removed identity.
    ///
    /// A gone, removed presentation is replaced so the next show mounts a new owner. Hidden or
    /// detached identities retarget the retained presentation.
    ///
    /// @param identity the structural identity
    /// @param debugName the diagnostic name used when creating
    /// @param spec the specification used when creating
    /// @return the transition
    public VisibilityTransition open(
            TransitionIdentity identity,
            String debugName,
            TransitionSpec spec
    ) {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(debugName, "debugName");
        Objects.requireNonNull(spec, "spec");
        claim(identity);
        VisibilityTransition existing = transitions.get(identity);
        if (existing != null
                && !(existing.phase() == TransitionPhase.GONE
                && existing.lifetime() == TransitionLifetime.REMOVED)) {
            existing.setSpec(spec);
            return existing;
        }
        VisibilityTransition created = new VisibilityTransition(identity, debugName, spec);
        transitions.put(identity, created);
        return created;
    }

    /// Records one identity claim for the current frame.
    ///
    /// @param identity the identity
    public void claim(TransitionIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        if (!claimed.add(identity)) {
            identityConflicts.add(identity);
        }
    }

    /// Returns the transition for `identity`, or `null`.
    ///
    /// @param identity the identity
    /// @return the transition
    public @Nullable VisibilityTransition get(TransitionIdentity identity) {
        return transitions.get(Objects.requireNonNull(identity, "identity"));
    }

    /// Samples every live transition at one timestamp.
    ///
    /// @param nowNanos the monotonic timestamp
    public void sample(long nowNanos) {
        for (VisibilityTransition transition : transitions.values()) {
            transition.sample(nowNanos);
        }
    }

    /// Captures a source rectangle after its layout pass.
    ///
    /// A second capture of the same key records a conflict and keeps the first rectangle.
    ///
    /// @param key the geometry key
    /// @param bounds the source bounds
    public void captureSource(MatchedGeometryKey key, LogicalRect bounds) {
        capture(sources, sourceConflicts, key, bounds);
    }

    /// Captures a destination rectangle after its layout pass.
    ///
    /// A second capture of the same key records a conflict and keeps the first rectangle.
    ///
    /// @param key the geometry key
    /// @param bounds the destination bounds
    public void captureDestination(MatchedGeometryKey key, LogicalRect bounds) {
        capture(destinations, destinationConflicts, key, bounds);
    }

    /// Returns an accepted source and destination pair, or `null` when incomplete or conflicted.
    ///
    /// @param key the geometry key
    /// @return the link
    public @Nullable MatchedGeometryLink link(MatchedGeometryKey key) {
        Objects.requireNonNull(key, "key");
        if (sourceConflicts.contains(key) || destinationConflicts.contains(key)) {
            return null;
        }
        LogicalRect source = sources.get(key);
        LogicalRect destination = destinations.get(key);
        if (source == null || destination == null) {
            return null;
        }
        return new MatchedGeometryLink(key, source, destination);
    }

    /// Returns identities claimed more than once in the current frame.
    ///
    /// @return the conflicts
    public @Unmodifiable Set<TransitionIdentity> identityConflicts() {
        return Set.copyOf(identityConflicts);
    }

    /// Returns source keys captured more than once in the current frame.
    ///
    /// @return the conflicts
    public @Unmodifiable Set<MatchedGeometryKey> sourceConflicts() {
        return Set.copyOf(sourceConflicts);
    }

    /// Returns destination keys captured more than once in the current frame.
    ///
    /// @return the conflicts
    public @Unmodifiable Set<MatchedGeometryKey> destinationConflicts() {
        return Set.copyOf(destinationConflicts);
    }

    /// Returns a snapshot of live transitions.
    ///
    /// @return the transitions
    public @Unmodifiable Map<TransitionIdentity, VisibilityTransition> transitions() {
        return Map.copyOf(transitions);
    }

    /// Stores the first rectangle and records later duplicates as conflicts.
    ///
    /// @param table the first-capture table
    /// @param conflicts the conflict set
    /// @param key the geometry key
    /// @param bounds the rectangle
    private static void capture(
            LinkedHashMap<MatchedGeometryKey, LogicalRect> table,
            LinkedHashSet<MatchedGeometryKey> conflicts,
            MatchedGeometryKey key,
            LogicalRect bounds
    ) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(bounds, "bounds");
        if (table.containsKey(key)) {
            conflicts.add(key);
            return;
        }
        table.put(key, bounds);
    }
}
