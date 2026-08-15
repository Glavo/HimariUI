package org.glavo.himari.runtime.mount;

import org.jetbrains.annotations.NotNullByDefault;

/// Declares the typed property bindings of one mounted element.
@NotNullByDefault
@FunctionalInterface
public interface MountedElementContent {
    /// Declares bindings on the callback-local element scope.
    ///
    /// @param element the callback-local binding facade
    void compose(MountedElementScope element);
}
