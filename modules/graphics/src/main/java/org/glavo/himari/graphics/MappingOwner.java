package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies who applies the last tone or gamut map.
@NotNullByDefault
public enum MappingOwner {
    /// The framework applies the versioned SDR/HDR mapping.
    FRAMEWORK,

    /// The host or display applies the mapping.
    HOST
}
