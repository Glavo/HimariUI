package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Implements callback-local current-measure operations for [MeasureStructuralScope].
@NotNullByDefault
interface MeasureStructuralScopeSession {
    /// Declares one direct semantic-keyed child.
    void keyedGroup(String sourceIdentity, Object semanticKey, StructuralContent content);

    /// Reads one ambient value.
    <T> T ambient(AmbientKey<T> key);

    /// Checks cooperative cancellation.
    void checkpoint();

    /// Rejects the current materialization draft.
    void fail(String code);
}
