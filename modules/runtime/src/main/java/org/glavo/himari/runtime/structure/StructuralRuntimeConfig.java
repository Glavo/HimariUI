package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Configures bounded diagnostics and current-measure work.
///
/// @param diagnosticsMode retained failure-detail policy
/// @param maximumRetainedFailures maximum failures kept in the diagnostic queue
/// @param maximumMaterializedChildren maximum direct keyed children in one measure attempt
@NotNullByDefault
public record StructuralRuntimeConfig(
        StructuralDiagnosticsMode diagnosticsMode,
        int maximumRetainedFailures,
        int maximumMaterializedChildren
) {
    /// Validates positive bounds and the diagnostics policy.
    public StructuralRuntimeConfig {
        Objects.requireNonNull(diagnosticsMode, "diagnosticsMode");
        if (maximumRetainedFailures < 1) {
            throw new IllegalArgumentException("maximumRetainedFailures must be positive");
        }
        if (maximumMaterializedChildren < 1) {
            throw new IllegalArgumentException("maximumMaterializedChildren must be positive");
        }
    }

    /// Returns the development defaults.
    ///
    /// @return debug diagnostics, 128 retained failures, and 4096 direct measure children
    public static StructuralRuntimeConfig defaults() {
        return new StructuralRuntimeConfig(StructuralDiagnosticsMode.DEBUG, 128, 4096);
    }
}
