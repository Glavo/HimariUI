package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Stores a domain-owned mutable `boolean` value without boxing at its public read and write boundary.
@NotNullByDefault
public final class BooleanState extends AbstractStateSource implements StateSource {
    /// Creates and registers a boolean state.
    ///
    /// @param domain the owning domain
    /// @param initialValue the initial value
    BooleanState(StateDomain domain, boolean initialValue) {
        super(domain, initialValue);
    }

    /// Returns the value visible to the current execution context.
    ///
    /// @return the staged value for the active owning transaction, or otherwise the latest
    /// published value
    public boolean get() {
        return (Boolean) currentValue();
    }

    /// Stages or publishes a replacement value.
    ///
    /// @param value the replacement value
    /// @throws IllegalStateException if called outside the owner thread
    public void set(boolean value) {
        write(value);
    }

    /// {@inheritDoc}
    @Override
    public StateDomain domain() {
        return owningDomain();
    }

    /// {@inheritDoc}
    @Override
    public long version() {
        return publishedVersion();
    }

    /// Verifies the boxed internal value type.
    ///
    /// @param value the replacement value
    /// @throws IllegalArgumentException if the value is not a `Boolean`
    @Override
    void validate(@Nullable Object value) {
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("Boolean state requires a Boolean value");
        }
    }

    /// Compares two boxed boolean values.
    ///
    /// @param first the first value
    /// @param second the second value
    /// @return whether both values represent the same boolean
    @Override
    boolean valuesEqual(@Nullable Object first, @Nullable Object second) {
        return first instanceof Boolean firstValue
                && second instanceof Boolean secondValue
                && firstValue.booleanValue() == secondValue.booleanValue();
    }
}
