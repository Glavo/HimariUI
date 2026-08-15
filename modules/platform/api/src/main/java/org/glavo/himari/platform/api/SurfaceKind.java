package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a target-neutral presentation-surface category using an extensible stable identifier.
///
/// @param identifier the nonblank, trimmed category identifier
@NotNullByDefault
public record SurfaceKind(String identifier) {
    /// A framework-owned software presentation surface.
    public static final SurfaceKind SOFTWARE = new SurfaceKind("software");

    /// A host-presented surface whose target handle remains outside the platform-neutral API.
    public static final SurfaceKind HOST_PRESENTED = new SurfaceKind("host-presented");

    /// Creates a surface-kind identifier.
    ///
    /// @throws IllegalArgumentException if `identifier` is blank or has surrounding whitespace
    public SurfaceKind {
        if (identifier.isBlank() || !identifier.equals(identifier.strip())) {
            throw new IllegalArgumentException("Surface-kind identifier must be nonblank and trimmed");
        }
    }

    /// Returns the stable identifier.
    ///
    /// @return the identifier
    @Override
    public String toString() {
        return identifier;
    }
}
