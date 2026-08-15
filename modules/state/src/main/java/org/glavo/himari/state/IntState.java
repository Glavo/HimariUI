package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Stores a domain-owned mutable `int` value without boxing at its public read and write boundary.
@NotNullByDefault
public final class IntState extends AbstractStateSource implements StateSource {
    /// Creates and registers an integer state.
    ///
    /// @param domain the owning domain
    /// @param initialValue the initial value
    IntState(StateDomain domain, int initialValue) {
        super(domain, initialValue);
    }

    /// Returns the value visible to the current execution context.
    ///
    /// @return the staged value for the active owning transaction, or otherwise the latest
    /// published value
    public int get() {
        return (Integer) currentValue();
    }

    /// Stages or publishes a replacement value.
    ///
    /// @param value the replacement value
    /// @throws IllegalStateException if called outside the owner thread
    public void set(int value) {
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
    /// @throws IllegalArgumentException if the value is not an `Integer`
    @Override
    void validate(@Nullable Object value) {
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Integer state requires an Integer value");
        }
    }

    /// Compares two boxed integer values.
    ///
    /// @param first the first value
    /// @param second the second value
    /// @return whether both values represent the same integer
    @Override
    boolean valuesEqual(@Nullable Object first, @Nullable Object second) {
        return first instanceof Integer firstValue
                && second instanceof Integer secondValue
                && firstValue.intValue() == secondValue.intValue();
    }
}
