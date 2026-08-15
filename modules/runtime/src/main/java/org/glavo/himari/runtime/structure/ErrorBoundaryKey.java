package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies one explicitly resettable application error boundary.
///
/// Keys use object identity. The same key must not be declared at multiple committed positions in
/// one runtime.
@NotNullByDefault
public final class ErrorBoundaryKey {
    /// The stable human-readable diagnostic name.
    private final String diagnosticName;

    /// Creates one boundary key.
    ///
    /// @param diagnosticName the nonblank diagnostic name
    private ErrorBoundaryKey(String diagnosticName) {
        this.diagnosticName = StructuralContracts.requireName(diagnosticName, "diagnosticName");
    }

    /// Creates a new identity key.
    ///
    /// @param diagnosticName the nonblank diagnostic name
    /// @return the new key
    public static ErrorBoundaryKey create(String diagnosticName) {
        return new ErrorBoundaryKey(diagnosticName);
    }

    /// Returns the diagnostic name.
    ///
    /// @return the nonblank name
    public String diagnosticName() {
        return diagnosticName;
    }
}
