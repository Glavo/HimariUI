package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.UnaryOperator;

/// Implements a domain-owned mutable object state.
///
/// @param <T> the value type
@NotNullByDefault
final class ObjectMutableState<T> extends AbstractStateSource implements MutableState<T> {
    /// Whether replacement values may be `null`.
    private final boolean nullable;

    /// Creates and registers an object state.
    ///
    /// @param domain the owning domain
    /// @param initialValue the initial value, which may be `null` for a nullable state
    /// @param nullable whether `null` is accepted
    ObjectMutableState(StateDomain domain, @Nullable T initialValue, boolean nullable) {
        super(domain, initialValue);
        this.nullable = nullable;
    }

    /// {@inheritDoc}
    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) currentValue();
    }

    /// {@inheritDoc}
    @Override
    public void set(T value) {
        write(value);
    }

    /// {@inheritDoc}
    @Override
    public void update(UnaryOperator<T> update) {
        Objects.requireNonNull(update, "update");
        StateTransaction.run(domain(), () -> set(update.apply(get())));
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

    /// Verifies this state's runtime null policy.
    ///
    /// @param value the replacement value, which may be `null`
    @Override
    void validate(@Nullable Object value) {
        if (!nullable) {
            Objects.requireNonNull(value, "value");
        }
    }

    /// Compares object values using [Objects#equals(Object, Object)].
    ///
    /// @param first the first value, which may be `null`
    /// @param second the second value, which may be `null`
    /// @return whether the values are equal
    @Override
    boolean valuesEqual(@Nullable Object first, @Nullable Object second) {
        return Objects.equals(first, second);
    }
}
