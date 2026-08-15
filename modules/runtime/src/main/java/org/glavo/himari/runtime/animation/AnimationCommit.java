package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

/// Stages scalar model targets for one [AnimationRegistry] commit callback.
///
/// An instance is valid only during the callback supplied to
/// [AnimationRegistry#commit(AnimationTransaction, java.util.function.Consumer)]. Repeated writes
/// to one property coalesce to its last target while preserving first-write property order.
@NotNullByDefault
public final class AnimationCommit {
    /// The registry accepting this staged batch.
    private final AnimationRegistry registry;

    /// The immutable animation transaction propagated with this batch.
    private final AnimationTransaction transaction;

    /// Final staged targets keyed by property identity.
    private final IdentityHashMap<AnimatedScalar, StagedTarget> targets = new IdentityHashMap<>();

    /// Properties in deterministic first-write order.
    private final ArrayList<AnimatedScalar> order = new ArrayList<>();

    /// Whether the commit callback may still stage values.
    private boolean open = true;

    /// Creates an empty callback-scoped commit.
    ///
    /// @param registry the accepting registry
    /// @param transaction the propagated transaction metadata
    AnimationCommit(AnimationRegistry registry, AnimationTransaction transaction) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.transaction = Objects.requireNonNull(transaction, "transaction");
    }

    /// Returns the immutable transaction metadata associated with this commit.
    ///
    /// @return the transaction
    public AnimationTransaction transaction() {
        return transaction;
    }

    /// Stages a final model target without explicit gesture velocity.
    ///
    /// @param property the target property owned by this registry
    /// @param target the finite target in the property's scalar unit
    /// @return this commit for fluent staging
    /// @throws IllegalArgumentException if the property belongs to another registry or the value is
    /// non-finite
    /// @throws IllegalStateException if the callback scope ended or the property is closed
    public AnimationCommit setTarget(AnimatedScalar property, double target) {
        return stage(property, target, false, 0.0);
    }

    /// Stages a final model target with explicit gesture-handoff velocity.
    ///
    /// The velocity uses the property's scalar unit per second and takes precedence over inferred
    /// replacement velocity unless the replacement policy snaps or explicitly restarts. A
    /// preserving policy causes the enclosing commit to reject an effective motion that does not
    /// support velocity retargeting.
    ///
    /// @param property the target property owned by this registry
    /// @param target the finite target in the property's scalar unit
    /// @param initialVelocity the finite scalar-per-second handoff velocity
    /// @return this commit for fluent staging
    /// @throws IllegalArgumentException if ownership or scalar validity fails
    /// @throws IllegalStateException if the callback scope ended or the property is closed
    public AnimationCommit setTargetWithVelocity(
            AnimatedScalar property,
            double target,
            double initialVelocity
    ) {
        return stage(property, target, true, initialVelocity);
    }

    /// Returns the deterministic staged-property order after closing this callback scope.
    ///
    /// @return the stable internal order view
    @UnmodifiableView List<AnimatedScalar> orderedProperties() {
        if (open) {
            throw new IllegalStateException("Animation commit is still open");
        }
        return order;
    }

    /// Returns the final staged target for a property.
    ///
    /// @param property the staged property
    /// @return the staged target
    StagedTarget targetFor(AnimatedScalar property) {
        if (open) {
            throw new IllegalStateException("Animation commit is still open");
        }
        StagedTarget target = targets.get(property);
        if (target == null) {
            throw new IllegalArgumentException("Property is not staged in this animation commit");
        }
        return target;
    }

    /// Ends successful staging while retaining its immutable-by-convention internal records.
    void finish() {
        if (!open) {
            throw new IllegalStateException("Animation commit scope already ended");
        }
        open = false;
    }

    /// Ends failed staging and releases every captured property reference.
    void abort() {
        if (!open) {
            return;
        }
        open = false;
        targets.clear();
        order.clear();
    }

    /// Validates and stages one final property target.
    ///
    /// @param property the property
    /// @param target the target value
    /// @param hasInitialVelocity whether velocity is explicit
    /// @param initialVelocity the explicit velocity or zero
    /// @return this commit
    private AnimationCommit stage(
            AnimatedScalar property,
            double target,
            boolean hasInitialVelocity,
            double initialVelocity
    ) {
        checkOpen();
        Objects.requireNonNull(property, "property");
        registry.checkStagingProperty(property);
        double normalizedTarget = property.adapter.normalize(target);
        double normalizedVelocity = property.adapter.normalizeVelocity(initialVelocity);
        if (!targets.containsKey(property)) {
            order.add(property);
        }
        targets.put(property, new StagedTarget(
                normalizedTarget,
                hasInitialVelocity,
                normalizedVelocity
        ));
        return this;
    }

    /// Verifies that the callback scope remains active.
    ///
    /// @throws IllegalStateException if staging already ended
    private void checkOpen() {
        if (!open) {
            throw new IllegalStateException("Animation commit scope has ended");
        }
    }

    /// Stores one normalized final target and optional handoff velocity.
    ///
    /// @param target the normalized target
    /// @param hasInitialVelocity whether velocity was explicitly supplied
    /// @param initialVelocity the normalized velocity or zero
    @NotNullByDefault
    record StagedTarget(double target, boolean hasInitialVelocity, double initialVelocity) {
        /// Validates finite staged scalars.
        StagedTarget {
            if (!Double.isFinite(target) || !Double.isFinite(initialVelocity)) {
                throw new IllegalArgumentException("Staged animation scalars must be finite");
            }
        }
    }
}
