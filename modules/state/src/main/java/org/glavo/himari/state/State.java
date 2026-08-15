package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

/// Provides typed reads of an object state value.
///
/// A mutable-state read performed by the owner thread inside a [StateTransaction] observes the most
/// recent value staged for that state. [DerivedState] reads pull the fine-grained graph lazily and
/// use an ephemeral, non-publishing evaluation inside transactions. Use [StateSnapshot] when several
/// mutable sources and their versions must come from one published epoch.
///
/// @param <T> the value type
@NotNullByDefault
public interface State<T> extends StateSource {
    /// Returns the value visible to the current execution context.
    ///
    /// @return the value defined by the implementation's source or derived-state contract
    T get();
}
