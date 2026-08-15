package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Decides whether two values are semantically interchangeable for reactive publication.
///
/// Implementations must be deterministic, synchronous, and free of externally observable side
/// effects. A policy used by [DerivedState] runs without dependency tracking, and state writes are
/// rejected while it executes. If it throws, the derivation remains invalidated and the failure is
/// propagated to the caller.
///
/// @param <T> the compared value type
@FunctionalInterface
@NotNullByDefault
public interface EqualityPolicy<T> {
    /// Returns whether `next` has the same reactive meaning as `previous`.
    ///
    /// @param previous the previously cached value
    /// @param next the newly computed value
    /// @return whether the semantic version must remain unchanged
    boolean equivalent(T previous, T next);

    /// Returns a policy based on [Objects#equals(Object, Object)].
    ///
    /// @param <T> the compared value type
    /// @return a structural equality policy
    static <T> EqualityPolicy<T> structural() {
        return Objects::equals;
    }

    /// Returns a policy that compares object identity.
    ///
    /// @param <T> the compared value type
    /// @return an identity equality policy
    static <T> EqualityPolicy<T> identity() {
        return (previous, next) -> previous == next;
    }

    /// Returns a policy that treats every recomputed value as semantically new.
    ///
    /// @param <T> the compared value type
    /// @return a policy that always returns `false`
    static <T> EqualityPolicy<T> neverEqual() {
        return (previous, next) -> false;
    }
}
