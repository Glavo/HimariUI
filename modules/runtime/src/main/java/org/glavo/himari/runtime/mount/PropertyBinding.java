package org.glavo.himari.runtime.mount;

import org.glavo.himari.runtime.animation.AnimationPhaseImpact;
import org.glavo.himari.state.ReactiveObservation;
import org.glavo.himari.state.ReactiveObserver;
import org.glavo.himari.state.ReactiveOwner;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/// Owns one fine-grained property consumer and its committed model target.
///
/// @param <T> the non-null property type
@NotNullByDefault
final class PropertyBinding<T> {
    /// The element-local property name.
    private final String name;

    /// The runtime value type.
    private final Class<T> valueType;

    /// The declared phase impact.
    private final AnimationPhaseImpact phaseImpact;

    /// Reads the current model target.
    private final PropertyReader<T> reader;

    /// Optional committed-value receiver.
    private final @Nullable PropertyApplier<T> applier;

    /// The observer that captures reader dependencies.
    private final ReactiveObserver observer;

    /// The latest committed value, or `null` before the first publication.
    private @Nullable T committed;

    /// Creates one binding.
    ///
    /// @param name the property name
    /// @param valueType the value type
    /// @param phaseImpact the phase impact
    /// @param reader the model-target reader
    /// @param applier the optional applier
    /// @param owner the reactive owner that bounds this observer
    PropertyBinding(
            String name,
            Class<T> valueType,
            AnimationPhaseImpact phaseImpact,
            PropertyReader<T> reader,
            @Nullable PropertyApplier<T> applier,
            ReactiveOwner owner
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.phaseImpact = Objects.requireNonNull(phaseImpact, "phaseImpact");
        this.reader = Objects.requireNonNull(reader, "reader");
        this.applier = applier;
        this.observer = Objects.requireNonNull(owner, "owner").createObserver();
    }

    /// Returns the property name.
    ///
    /// @return the name
    String name() {
        return name;
    }

    /// Returns the value type.
    ///
    /// @return the type
    Class<T> valueType() {
        return valueType;
    }

    /// Returns the phase impact.
    ///
    /// @return the impact
    AnimationPhaseImpact phaseImpact() {
        return phaseImpact;
    }

    /// Returns the committed-value applier, or `null`.
    ///
    /// @return the applier
    @Nullable PropertyApplier<T> applier() {
        return applier;
    }

    /// Returns the model-target reader.
    ///
    /// @return the reader
    PropertyReader<T> reader() {
        return reader;
    }

    /// Returns whether the reader must execute.
    ///
    /// @return whether the binding is invalidated
    boolean isInvalidated() {
        return observer.isInvalidated();
    }

    /// Captures the current reader value without publishing it.
    ///
    /// @return the captured observation and value
    CapturedValue<T> capture() {
        AtomicReference<T> holder = new AtomicReference<>();
        ReactiveObservation observation = observer.capture(() -> {
            T value = Objects.requireNonNull(reader.read(), name);
            if (!valueType.isInstance(value)) {
                throw new IllegalStateException(name + " produced " + value.getClass().getName());
            }
            holder.set(value);
        });
        return new CapturedValue<>(observation, Objects.requireNonNull(holder.get(), name));
    }

    /// Returns whether `next` is semantically equal to the committed value.
    ///
    /// @param next the candidate
    /// @return whether the committed value is unchanged
    boolean hasSameValue(T next) {
        return committed != null && Objects.equals(committed, next);
    }

    /// Publishes a new committed value and returns the previous value.
    ///
    /// @param value the next value
    /// @return the previous value, or `null` on first publication
    @Nullable T publish(T value) {
        @Nullable T previous = committed;
        committed = value;
        return previous;
    }

    /// Restores a previously committed value after a failed transaction.
    ///
    /// @param previous the value to restore, or `null` to clear the first publication
    void restore(@Nullable T previous) {
        committed = previous;
    }

    /// Returns the committed snapshot, or `null` before the first publication.
    ///
    /// @return the committed property
    @Nullable MountedProperty snapshot() {
        if (committed == null) {
            return null;
        }
        return new MountedProperty(name, valueType, phaseImpact, committed);
    }

    /// Disposes the observer.
    void dispose() {
        observer.close();
        committed = null;
    }

    /// Holds one captured reader observation.
    ///
    /// @param observation the detached observation
    /// @param value the captured value
    /// @param <T> the property type
    @NotNullByDefault
    record CapturedValue<T>(ReactiveObservation observation, T value) {
    }
}
