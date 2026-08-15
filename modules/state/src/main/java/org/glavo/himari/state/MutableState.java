package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.function.UnaryOperator;

/// Provides owner-thread mutation of an object [State].
///
/// Writes made inside a [StateTransaction] are staged until its outermost successful return. A
/// write outside a transaction is published as a single-write transaction. A state created by
/// [StateDomain#mutableState(Object)] rejects `null`; one created by
/// [StateDomain#nullableState(Object)] accepts it according to its annotated type argument.
///
/// @param <T> the value type
@NotNullByDefault
public interface MutableState<T> extends State<T> {
    /// Stages or publishes a replacement value.
    ///
    /// Replacing a value with an equal value does not advance the source version or domain epoch.
    ///
    /// @param value the replacement value
    /// @throws IllegalStateException if called outside the domain owner thread or while a commit is
    /// comparing or publishing values
    /// @throws NullPointerException if this state was created by
    /// [StateDomain#mutableState(Object)] and `value` is `null`
    void set(T value);

    /// Replaces the current transaction-visible value with a computed value.
    ///
    /// The operator executes immediately on the domain owner thread. It executes once and is not a
    /// reactive computation.
    ///
    /// @param update the non-null replacement function
    /// @throws IllegalStateException if called outside the domain owner thread or while a commit is
    /// comparing or publishing values
    /// @throws NullPointerException if `update` is `null`, or if a non-null state receives a `null`
    /// result
    void update(UnaryOperator<T> update);
}
