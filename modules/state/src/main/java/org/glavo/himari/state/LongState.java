package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Stores a domain-owned mutable `long` value without boxing at its public read and write boundary.
@NotNullByDefault
public final class LongState extends AbstractStateSource implements StateSource {
    /// Creates and registers a long state.
    ///
    /// @param domain the owning domain
    /// @param initialValue the initial value
    LongState(StateDomain domain, long initialValue) {
        super(domain, initialValue);
    }

    /// Returns the value visible to the current execution context.
    ///
    /// @return the staged value for the active owning transaction, or otherwise the latest
    /// published value
    public long get() {
        return (Long) currentValue();
    }

    /// Stages or publishes a replacement value.
    ///
    /// @param value the replacement value
    /// @throws IllegalStateException if called outside the owner thread
    public void set(long value) {
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
    /// @throws IllegalArgumentException if the value is not a `Long`
    @Override
    void validate(@Nullable Object value) {
        if (!(value instanceof Long)) {
            throw new IllegalArgumentException("Long state requires a Long value");
        }
    }

    /// Compares two boxed long values.
    ///
    /// @param first the first value
    /// @param second the second value
    /// @return whether both values represent the same long
    @Override
    boolean valuesEqual(@Nullable Object first, @Nullable Object second) {
        return first instanceof Long firstValue
                && second instanceof Long secondValue
                && firstValue.longValue() == secondValue.longValue();
    }
}
