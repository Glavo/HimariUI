package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Declares one restartable structural group through ordinary Java code.
@FunctionalInterface
@NotNullByDefault
public interface StructuralContent {
    /// Declares the current group's children, memory, effects, resources, and ambient reads.
    ///
    /// The callback must be synchronous, non-reentrant, and free of unmanaged side effects. Reactive
    /// source reads are captured for this group. State writes are rejected while it executes.
    ///
    /// @param scope the callback-local structural scope
    void compose(StructuralScope scope);
}
