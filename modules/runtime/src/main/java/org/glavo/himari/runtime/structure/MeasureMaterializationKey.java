package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Identifies one layout-owned current-measure materialization group.
///
/// Keys use object identity and may be declared at only one committed position in a runtime.
///
/// @param <I> the immutable measure-input type
@NotNullByDefault
public final class MeasureMaterializationKey<I> {
    /// The stable human-readable diagnostic name.
    private final String diagnosticName;

    /// The runtime input type.
    private final Class<I> inputType;

    /// Creates one materialization key.
    ///
    /// @param diagnosticName the nonblank diagnostic name
    /// @param inputType the accepted input type
    private MeasureMaterializationKey(String diagnosticName, Class<I> inputType) {
        this.diagnosticName = StructuralContracts.requireName(diagnosticName, "diagnosticName");
        this.inputType = Objects.requireNonNull(inputType, "inputType");
    }

    /// Creates a new typed identity key.
    ///
    /// @param diagnosticName the nonblank diagnostic name
    /// @param inputType the accepted input type
    /// @param <I> the immutable measure-input type
    /// @return the new key
    public static <I> MeasureMaterializationKey<I> create(String diagnosticName, Class<I> inputType) {
        return new MeasureMaterializationKey<>(diagnosticName, inputType);
    }

    /// Returns the diagnostic name.
    ///
    /// @return the nonblank name
    public String diagnosticName() {
        return diagnosticName;
    }

    /// Returns the accepted input type.
    ///
    /// @return the input class
    public Class<I> inputType() {
        return inputType;
    }
}
