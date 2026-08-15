package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Captures one deterministic candidate diagnostic for debug-trace quality review.
///
/// @param code the stable lower-kebab-case diagnostic code
/// @param message the deterministic human-readable message
/// @param sourceLocation a stable application source location, or `null` when unavailable
/// @param ownerPath a stable owner, scope, node, or key path, or `null` when unavailable
/// @param dependencyPath a producer-to-consumer path, or `null` when the diagnostic is not dependency-related
/// @param recoveryAction the containment, cleanup, retry, or disposal action, or `null` when none occurred
@NotNullByDefault
public record DiagnosticTrace(
        String code,
        String message,
        @Nullable String sourceLocation,
        @Nullable String ownerPath,
        @Nullable String dependencyPath,
        @Nullable String recoveryAction
) {
    /// Creates a validated diagnostic trace record.
    public DiagnosticTrace {
        code = ComparisonContracts.requireIdentifier(code, "diagnostic code");
        message = ComparisonContracts.requireText(message, "diagnostic message");
        sourceLocation = optionalText(sourceLocation, "sourceLocation");
        ownerPath = optionalText(ownerPath, "ownerPath");
        dependencyPath = optionalText(dependencyPath, "dependencyPath");
        recoveryAction = optionalText(recoveryAction, "recoveryAction");
    }

    /// Returns the fixed zero-to-four quality score defined by the M1 rubric.
    ///
    /// A message alone scores one. Source or owner identity scores two, both score three, and a
    /// trace with both identity forms plus dependency or recovery context scores four.
    ///
    /// @return the trace quality score
    public int qualityScore() {
        boolean source = sourceLocation != null;
        boolean owner = ownerPath != null;
        if (!source && !owner) {
            return 1;
        }
        if (!(source && owner)) {
            return 2;
        }
        return dependencyPath != null || recoveryAction != null ? 4 : 3;
    }

    /// Validates optional text without changing absence.
    ///
    /// @param value the optional text
    /// @param name the diagnostic name
    /// @return `value`
    private static @Nullable String optionalText(@Nullable String value, String name) {
        return value == null ? null : ComparisonContracts.requireText(value, name);
    }
}
