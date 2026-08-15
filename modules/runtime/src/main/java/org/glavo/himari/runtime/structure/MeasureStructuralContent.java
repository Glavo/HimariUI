package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Declares semantic-keyed children for one scoped current-measure materialization.
///
/// @param <I> the immutable measure-input type
@FunctionalInterface
@NotNullByDefault
public interface MeasureStructuralContent<I> {
    /// Declares the current viewport's direct semantic-keyed descendants.
    ///
    /// @param scope the restricted current-measure scope
    /// @param input the current constraints or viewport input
    void materialize(MeasureStructuralScope scope, I input);
}
