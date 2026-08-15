package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Objects;

/// Exposes binding declarations while one mounted element is being composed.
///
/// The scope is callback-local. Retaining it or invoking it after its callback returns is invalid.
@NotNullByDefault
public final class MountedElementScope {
    /// Declared bindings in first-declaration order.
    private final LinkedHashMap<String, BindingSpec<?>> bindings = new LinkedHashMap<>();

    /// Whether the declaring callback is still active.
    private boolean active = true;

    /// Creates one empty callback-local facade.
    MountedElementScope() {
    }

    /// Declares one typed property binding without a committed-value applier.
    ///
    /// @param name the nonblank element-local property name
    /// @param valueType the runtime value type
    /// @param phaseImpact the earliest phase impact and required successors
    /// @param reader the model-target reader
    /// @param <T> the non-null property type
    public <T> void bind(
            String name,
            Class<T> valueType,
            AnimationPhaseImpact phaseImpact,
            PropertyReader<T> reader
    ) {
        bind(name, valueType, phaseImpact, reader, null);
    }

    /// Declares one typed property binding with a committed-value applier.
    ///
    /// Duplicate names in one composition reject the complete mount declaration.
    ///
    /// @param name the nonblank element-local property name
    /// @param valueType the runtime value type
    /// @param phaseImpact the earliest phase impact and required successors
    /// @param reader the model-target reader
    /// @param applier the optional committed-value receiver
    /// @param <T> the non-null property type
    public <T> void bind(
            String name,
            Class<T> valueType,
            AnimationPhaseImpact phaseImpact,
            PropertyReader<T> reader,
            @Nullable PropertyApplier<T> applier
    ) {
        checkActive();
        String checkedName = requireName(name);
        if (bindings.containsKey(checkedName)) {
            throw new IllegalArgumentException("Duplicate mounted property: " + checkedName);
        }
        bindings.put(checkedName, new BindingSpec<>(
                checkedName,
                Objects.requireNonNull(valueType, "valueType"),
                Objects.requireNonNull(phaseImpact, "phaseImpact"),
                Objects.requireNonNull(reader, "reader"),
                applier
        ));
    }

    /// Returns the bindings declared by this composition.
    ///
    /// @return the binding specifications in declaration order
    LinkedHashMap<String, BindingSpec<?>> bindings() {
        return bindings;
    }

    /// Deactivates this scope after its callback returns.
    void deactivate() {
        active = false;
    }

    /// Verifies callback-local lifetime.
    private void checkActive() {
        if (!active) {
            throw new IllegalStateException("Mounted element scope is no longer active");
        }
    }

    /// Requires a nonblank property name.
    ///
    /// @param name the candidate
    /// @return the unchanged name
    private static String requireName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    /// Stores one declared binding specification.
    ///
    /// @param <T> the non-null property type
    @NotNullByDefault
    record BindingSpec<T>(
            String name,
            Class<T> valueType,
            AnimationPhaseImpact phaseImpact,
            PropertyReader<T> reader,
            @Nullable PropertyApplier<T> applier
    ) {
    }
}
