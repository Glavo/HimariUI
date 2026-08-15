package org.glavo.himari.runtime.mount;

import org.jetbrains.annotations.NotNullByDefault;

/// Receives one committed property model target after a successful [UiCommitTransaction].
///
/// Appliers run only for bindings whose semantic value changed. They must not write application
/// [org.glavo.himari.state.State] or declare structure.
///
/// @param <T> the non-null property type
@NotNullByDefault
@FunctionalInterface
public interface PropertyApplier<T> {
    /// Applies one committed model target.
    ///
    /// @param value the committed non-null value
    void apply(T value);
}
