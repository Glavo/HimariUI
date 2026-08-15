package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a versioned reactive producer owned by a [StateDomain].
///
/// Mutable source versions start at zero and advance at most once for each domain epoch in which
/// their semantic values change. A [DerivedState] has an independent semantic version and may pull
/// lazily when [#version()] is requested. [StateSnapshot] captures mutable source values and source
/// versions; it does not snapshot derived caches.
@NotNullByDefault
public interface StateSource {
    /// Returns the domain that owns this source.
    ///
    /// @return the owning domain
    StateDomain domain();

    /// Returns the current semantic version.
    ///
    /// Mutable sources do not expose staged values through this method. A derived implementation may
    /// pull lazily to determine whether its semantic version changed.
    ///
    /// @return the latest published source version
    long version();
}
