package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Describes one cleanup callback that failed while logical disposal continued.
///
/// @param ownerPath stable owner path of the disposed effect or resource
/// @param boundaryPath nearest declared boundary path, or `root`
/// @param cause the callback failure in debug mode, or `null` in release mode
@NotNullByDefault
public record StructuralCleanupFailure(
        String ownerPath,
        String boundaryPath,
        @Nullable Throwable cause
) {
    /// Validates the stable owner and boundary paths.
    public StructuralCleanupFailure {
        ownerPath = StructuralContracts.requireName(ownerPath, "ownerPath");
        boundaryPath = StructuralContracts.requireName(boundaryPath, "boundaryPath");
    }
}
