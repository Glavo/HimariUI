package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Stores a domain-owned mutable `float` value without boxing at its public read and write boundary.
///
/// Semantic equality uses [Float#floatToIntBits(float)]: all NaN encodings compare equal, while
/// positive and negative zero remain distinct.
@NotNullByDefault
public final class FloatState extends AbstractStateSource implements StateSource {
    /// Creates and registers a float state.
    ///
    /// @param domain the owning domain
    /// @param initialValue the initial value
    FloatState(StateDomain domain, float initialValue) {
        super(domain, initialValue);
    }

    /// Returns the value visible to the current execution context.
    ///
    /// @return the staged value for the active owning transaction, or otherwise the latest
    /// published value
    public float get() {
        return (Float) currentValue();
    }

    /// Stages or publishes a replacement value.
    ///
    /// @param value the replacement value
    /// @throws IllegalStateException if called outside the owner thread
    public void set(float value) {
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
    /// @throws IllegalArgumentException if the value is not a `Float`
    @Override
    void validate(@Nullable Object value) {
        if (!(value instanceof Float)) {
            throw new IllegalArgumentException("Float state requires a Float value");
        }
    }

    /// Compares two boxed float values by canonical bits.
    ///
    /// @param first the first value
    /// @param second the second value
    /// @return whether both values have the same canonical bits
    @Override
    boolean valuesEqual(@Nullable Object first, @Nullable Object second) {
        return first instanceof Float firstValue
                && second instanceof Float secondValue
                && Float.floatToIntBits(firstValue) == Float.floatToIntBits(secondValue);
    }
}
