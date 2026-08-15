package org.glavo.himari.runtime.mount;

import org.jetbrains.annotations.NotNullByDefault;

/// Reads one non-null mounted property model target.
///
/// The reader is captured under a binding-owned [org.glavo.himari.state.ReactiveObserver]. Reads
/// performed here invalidate only this binding and its declared phase impact. They do not become
/// structural-group dependencies.
///
/// @param <T> the non-null property type
@NotNullByDefault
@FunctionalInterface
public interface PropertyReader<T> {
    /// Returns the current model target.
    ///
    /// @return the non-null value
    T read();
}
