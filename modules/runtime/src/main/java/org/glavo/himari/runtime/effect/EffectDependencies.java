package org.glavo.himari.runtime.effect;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Stores the comparable dependency identity that decides whether a keyed effect updates.
///
/// @param values immutable comparable tokens in declaration order
@NotNullByDefault
public record EffectDependencies(@Unmodifiable List<Object> values) {
    /// An empty dependency set that never compares unequal to another empty set.
    public static final EffectDependencies NONE = new EffectDependencies(List.of());

    /// Validates and copies the tokens.
    public EffectDependencies {
        values = List.copyOf(values);
        for (Object value : values) {
            Objects.requireNonNull(value, "dependency");
        }
    }

    /// Creates a dependency set from zero or more tokens.
    ///
    /// @param values the tokens
    /// @return the dependency set
    public static EffectDependencies of(Object... values) {
        Objects.requireNonNull(values, "values");
        if (values.length == 0) {
            return NONE;
        }
        return new EffectDependencies(List.of(values));
    }
}
