package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Selects the failure detail retained by structural diagnostics.
@NotNullByDefault
public enum StructuralDiagnosticsMode {
    /// Retains callback and cleanup causes for developer diagnostics.
    DEBUG,

    /// Omits callback and cleanup causes while preserving codes, paths, and recovery semantics.
    RELEASE
}
