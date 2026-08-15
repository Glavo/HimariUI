package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Identifies one typed inherited value and its immutable default.
///
/// Keys use object identity so independently created keys with the same diagnostic name remain
/// distinct. Values are non-null; applications that need an absent state should model it with an
/// explicit value type.
///
/// @param <T> the inherited value type
@NotNullByDefault
public final class AmbientKey<T> {
    /// The stable human-readable diagnostic name.
    private final String diagnosticName;

    /// The runtime value type.
    private final Class<T> valueType;

    /// The value returned outside an override.
    private final T defaultValue;

    /// Creates one identity key.
    ///
    /// @param diagnosticName the nonblank diagnostic name
    /// @param valueType the accepted runtime value type
    /// @param defaultValue the non-null default value
    private AmbientKey(String diagnosticName, Class<T> valueType, T defaultValue) {
        this.diagnosticName = StructuralContracts.requireName(diagnosticName, "diagnosticName");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.defaultValue = valueType.cast(Objects.requireNonNull(defaultValue, "defaultValue"));
    }

    /// Creates one typed ambient key.
    ///
    /// @param diagnosticName the nonblank diagnostic name
    /// @param valueType the accepted runtime value type
    /// @param defaultValue the non-null default value
    /// @param <T> the inherited value type
    /// @return a new identity key
    public static <T> AmbientKey<T> of(String diagnosticName, Class<T> valueType, T defaultValue) {
        return new AmbientKey<>(diagnosticName, valueType, defaultValue);
    }

    /// Returns the diagnostic name.
    ///
    /// @return the nonblank name
    public String diagnosticName() {
        return diagnosticName;
    }

    /// Returns the accepted runtime value type.
    ///
    /// @return the value class
    public Class<T> valueType() {
        return valueType;
    }

    /// Returns the default value.
    ///
    /// @return the non-null default
    public T defaultValue() {
        return defaultValue;
    }
}
