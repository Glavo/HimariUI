package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Describes a presentation target without exposing a native, browser, or GPU handle.
///
/// Target-specific interop modules may resolve the opaque session-local [SurfaceId]; common modules
/// must not infer a host representation from it.
///
/// @param id the stable session-local surface identifier
/// @param role the host surface role
/// @param kind the target-neutral surface category
@NotNullByDefault
public record SurfaceDescriptor(SurfaceId id, SurfaceRole role, SurfaceKind kind) {
    /// Creates a surface descriptor.
    public SurfaceDescriptor {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(kind, "kind");
    }
}
