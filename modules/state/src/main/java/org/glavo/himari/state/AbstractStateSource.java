package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Shares domain ownership and storage-slot behavior between source-state implementations.
@NotNullByDefault
abstract class AbstractStateSource {
    /// The domain that owns this source.
    private final StateDomain domain;

    /// The source's stable slot in domain publications.
    private final int slot;

    /// The producer node used for dependency tracking and invalidation.
    private final SourceReactiveNode reactiveNode;

    /// Registers a source and stores its stable publication slot.
    ///
    /// @param domain the owning domain
    /// @param initialValue the initial value, which may be `null`
    AbstractStateSource(StateDomain domain, @Nullable Object initialValue) {
        this.domain = domain;
        this.slot = domain.registerSource(initialValue);
        this.reactiveNode = new SourceReactiveNode(this);
    }

    /// Returns the owning domain.
    ///
    /// @return the owning domain
    final StateDomain owningDomain() {
        return domain;
    }

    /// Returns the stable publication slot.
    ///
    /// @return the slot
    final int slot() {
        return slot;
    }

    /// Returns the producer node representing this source.
    ///
    /// @return the reactive producer node
    final SourceReactiveNode reactiveNode() {
        return reactiveNode;
    }

    /// Returns the value visible to this thread.
    ///
    /// @return the staged value for an active owning transaction, or the latest published value
    final @Nullable Object currentValue() {
        @Nullable Object value = StateTransaction.read(this);
        ReactiveTracking.recordRead(reactiveNode);
        return value;
    }

    /// Stages a validated value in the active transaction or an implicit single-write transaction.
    ///
    /// @param value the replacement value, which may be `null` if accepted by this source
    final void write(@Nullable Object value) {
        validate(value);
        StateTransaction.write(this, value);
    }

    /// Returns the latest published source version.
    ///
    /// @return the latest version
    final long publishedVersion() {
        return domain.currentPublication().version(slot);
    }

    /// Validates a replacement value before it is staged.
    ///
    /// @param value the replacement value, which may be `null`
    abstract void validate(@Nullable Object value);

    /// Returns whether two values are semantically equal for this source.
    ///
    /// @param first the first value, which may be `null`
    /// @param second the second value, which may be `null`
    /// @return whether publication treats the values as equal
    abstract boolean valuesEqual(@Nullable Object first, @Nullable Object second);
}
