package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Provides shared validation for public structural value types.
@NotNullByDefault
final class StructuralContracts {
    /// Prevents construction.
    private StructuralContracts() {
    }

    /// Requires nonblank text without changing its spelling.
    ///
    /// @param value the candidate text
    /// @param name the parameter name
    /// @return the unchanged text
    static String requireName(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
