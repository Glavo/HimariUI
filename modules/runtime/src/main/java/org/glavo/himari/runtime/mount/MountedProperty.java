package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Captures one committed mounted property model target.
///
/// @param name the nonblank property name
/// @param valueType the runtime value type
/// @param phaseImpact the earliest phase impact and required successors
/// @param value the committed non-null model target
@NotNullByDefault
public record MountedProperty(
        String name,
        Class<?> valueType,
        AnimationPhaseImpact phaseImpact,
        Object value
) {
    /// Validates one committed property snapshot.
    public MountedProperty {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(valueType, "valueType");
        Objects.requireNonNull(phaseImpact, "phaseImpact");
        Objects.requireNonNull(value, "value");
        if (!valueType.isInstance(value)) {
            throw new IllegalArgumentException("value is not an instance of " + valueType.getName());
        }
    }
}
