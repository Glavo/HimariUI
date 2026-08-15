package org.glavo.himari.runtime.structure;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.IdentityHashMap;
import java.util.Objects;

/// Stores one non-null local reactive value owned by a remembered structural slot.
///
/// Reads made by active structural callbacks are tracked at the reading group. A semantic write
/// invalidates only those active readers. Writes are owner-thread operations and are rejected while
/// any structural attempt or application-state transaction is active. Disposal follows the
/// remembered slot's branch and group lifetime.
///
/// @param <T> the value type
@NotNullByDefault
public final class StructuralLocal<T> {
    /// The runtime enforcing thread and attempt rules.
    final StructuralRuntime runtime;

    /// The group that owns the remembered slot.
    final StructuralRuntime.GroupNode owner;

    /// The accepted runtime value type.
    final Class<T> valueType;

    /// Active structural readers using identity semantics.
    final IdentityHashMap<StructuralRuntime.GroupNode, Boolean> consumers = new IdentityHashMap<>();

    /// The current non-null value.
    T value;

    /// Whether the owning slot was disposed.
    boolean disposed;

    /// Creates one runtime-owned local cell.
    ///
    /// @param runtime the owning runtime
    /// @param owner the owning group
    /// @param valueType the accepted value type
    /// @param initialValue the first value
    StructuralLocal(StructuralRuntime runtime, StructuralRuntime.GroupNode owner, Class<T> valueType, T initialValue) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.owner = Objects.requireNonNull(owner, "owner");
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        this.value = valueType.cast(Objects.requireNonNull(initialValue, "initialValue"));
    }

    /// Returns the current value and records a structural dependency when read from a callback.
    ///
    /// @return the non-null current value
    /// @throws IllegalStateException if called off the runtime owner thread or after disposal
    public T get() {
        return runtime.readLocal(this);
    }

    /// Replaces the value and invalidates active structural readers when it changes by equality.
    /// Value equality must be synchronous and free of application-state writes and runtime reentry.
    ///
    /// @param newValue the non-null replacement
    /// @throws IllegalStateException if called off the owner thread, during a structural attempt or
    /// state transaction, or after disposal
    public void set(T newValue) {
        runtime.writeLocal(this, valueType.cast(Objects.requireNonNull(newValue, "newValue")));
    }

    /// Returns whether the owning remembered slot has been disposed.
    ///
    /// @return whether no further read or write is permitted
    public boolean isDisposed() {
        runtime.checkOwnerThread();
        return disposed;
    }
}
