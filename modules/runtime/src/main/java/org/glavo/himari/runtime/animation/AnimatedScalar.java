package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Represents one registry-owned scalar with separate model and presentation values.
///
/// Model and presentation reads are thread-safe snapshots of the latest atomic registry
/// publication. Mutation and closure remain confined to the registry's owner context.
@NotNullByDefault
public final class AnimatedScalar implements AutoCloseable {
    /// The registry owning this property and all mutable fields below.
    final AnimationRegistry registry;

    /// The zero-based stable registry slot.
    final int slot;

    /// The positive stable registry-local identity.
    final long propertyId;

    /// The stable developer-facing diagnostic name.
    final String debugName;

    /// The allocation-free scalar normalization and equality policy.
    final ScalarAnimationAdapter adapter;

    /// The UI phases invalidated when presentation value changes.
    final AnimationPhaseImpact phaseImpact;

    /// The authoritative committed model target, guarded by the registry monitor.
    double modelTarget;

    /// The latest atomically published presentation value, guarded by the registry monitor.
    double presentationValue;

    /// The latest scalar-per-second velocity, guarded by the registry monitor.
    double velocity;

    /// The active effective motion, or `null`, guarded by the registry monitor.
    @Nullable MotionSpec effectiveMotion;

    /// The monotonic timestamp at which the active motion began.
    long startTimestampNanos;

    /// The active motion's starting presentation value.
    double startValue;

    /// The active motion's initial scalar-per-second velocity.
    double initialVelocity;

    /// The transaction owning the active motion, or zero when inactive.
    long transactionId;

    /// The number of semantically changed committed model targets.
    long replacementGeneration;

    /// Whether this property permanently stopped accepting targets.
    boolean closed;

    /// Creates one live scalar property in its initial model and presentation state.
    ///
    /// @param registry the owning registry
    /// @param slot the stable zero-based registry slot
    /// @param propertyId the positive stable registry-local identity
    /// @param debugName the diagnostic name
    /// @param adapter the scalar adapter
    /// @param phaseImpact the presentation phase impact
    /// @param initialValue the normalized initial value
    AnimatedScalar(
            AnimationRegistry registry,
            int slot,
            long propertyId,
            String debugName,
            ScalarAnimationAdapter adapter,
            AnimationPhaseImpact phaseImpact,
            double initialValue
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.slot = slot;
        this.propertyId = propertyId;
        this.debugName = Objects.requireNonNull(debugName, "debugName");
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.phaseImpact = Objects.requireNonNull(phaseImpact, "phaseImpact");
        this.modelTarget = initialValue;
        this.presentationValue = initialValue;
    }

    /// Returns the stable registry-local identity.
    ///
    /// @return the positive property identity
    public long propertyId() {
        return propertyId;
    }

    /// Returns the stable diagnostic name.
    ///
    /// @return the diagnostic name
    public String debugName() {
        return debugName;
    }

    /// Returns the scalar adapter.
    ///
    /// @return the immutable adapter
    public ScalarAnimationAdapter adapter() {
        return adapter;
    }

    /// Returns the phase impact of presentation changes.
    ///
    /// @return the immutable phase impact
    public AnimationPhaseImpact phaseImpact() {
        return phaseImpact;
    }

    /// Returns the authoritative committed model target.
    ///
    /// @return the latest model target
    public double modelTarget() {
        return registry.modelTarget(this);
    }

    /// Returns the latest atomically published presentation value.
    ///
    /// @return the latest presentation value
    public double presentationValue() {
        return registry.presentationValue(this);
    }

    /// Returns the latest scalar-per-second presentation velocity.
    ///
    /// @return the current velocity
    public double velocity() {
        return registry.velocity(this);
    }

    /// Returns whether this property has an active timeline.
    ///
    /// @return whether motion is active
    public boolean isActive() {
        return registry.isActive(this);
    }

    /// Returns whether this property stopped accepting target changes.
    ///
    /// @return whether the property is closed
    public boolean isClosed() {
        return registry.isPropertyClosed(this);
    }

    /// Captures this property at one registry publication boundary.
    ///
    /// @return the immutable property snapshot
    public AnimatedScalarSnapshot snapshot() {
        return registry.snapshot(this);
    }

    /// Cancels active motion, releases the registry slot, and stops accepting targets.
    ///
    /// Closure is idempotent and must run on the registry owner context. It does not alter the
    /// authoritative model state outside this presentation property.
    ///
    /// @throws IllegalStateException if called outside the owner context, during commit staging, if
    /// the still-live property has a closed event loop, or after presentation epochs are exhausted
    @Override
    public void close() {
        registry.closeProperty(this);
    }
}
