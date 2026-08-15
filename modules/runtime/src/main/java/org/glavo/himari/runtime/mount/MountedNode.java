package org.glavo.himari.runtime.mount;

import org.glavo.himari.state.ReactiveOwner;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Stores one committed or staged mounted element and its bindings.
@NotNullByDefault
final class MountedNode {
    /// The stable mount identity.
    private final MountIdentity identity;

    /// The deterministic owner path.
    private final String ownerPath;

    /// The reactive owner that bounds this element's bindings.
    private final ReactiveOwner owner;

    /// Active bindings in declaration order.
    private final LinkedHashMap<String, PropertyBinding<?>> bindings = new LinkedHashMap<>();

    /// Whether this node has released its bindings.
    private boolean disposed;

    /// Creates one empty node.
    ///
    /// @param identity the mount identity
    /// @param ownerPath the owner path
    /// @param owner the reactive owner
    MountedNode(MountIdentity identity, String ownerPath, ReactiveOwner owner) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.ownerPath = Objects.requireNonNull(ownerPath, "ownerPath");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    /// Returns the identity.
    ///
    /// @return the identity
    MountIdentity identity() {
        return identity;
    }

    /// Returns the owner path.
    ///
    /// @return the path
    String ownerPath() {
        return ownerPath;
    }

    /// Returns the bindings in declaration order.
    ///
    /// @return the bindings
    LinkedHashMap<String, PropertyBinding<?>> bindings() {
        return bindings;
    }

    /// Replaces binding specifications while preserving compatible observers.
    ///
    /// A type or reader identity change disposes the previous binding and creates a replacement.
    ///
    /// @param specs the next specifications
    /// @param disposed the bindings removed by this reconciliation
    void reconcile(
            LinkedHashMap<String, MountedElementScope.BindingSpec<?>> specs,
            List<PropertyBinding<?>> disposed
    ) {
        LinkedHashMap<String, PropertyBinding<?>> next = new LinkedHashMap<>();
        for (Map.Entry<String, MountedElementScope.BindingSpec<?>> entry : specs.entrySet()) {
            MountedElementScope.BindingSpec<?> spec = entry.getValue();
            @Nullable PropertyBinding<?> previous = bindings.get(entry.getKey());
            if (previous != null && sameContract(previous, spec)) {
                next.put(entry.getKey(), previous);
            } else {
                if (previous != null) {
                    disposed.add(previous);
                    previous.dispose();
                }
                next.put(entry.getKey(), createBinding(spec));
            }
        }
        for (Map.Entry<String, PropertyBinding<?>> entry : bindings.entrySet()) {
            if (!next.containsKey(entry.getKey())) {
                disposed.add(entry.getValue());
                entry.getValue().dispose();
            }
        }
        bindings.clear();
        bindings.putAll(next);
    }

    /// Returns a committed snapshot of this element.
    ///
    /// @return the snapshot
    MountedElement snapshot() {
        ArrayList<MountedProperty> properties = new ArrayList<>();
        for (PropertyBinding<?> binding : bindings.values()) {
            @Nullable MountedProperty property = binding.snapshot();
            if (property != null) {
                properties.add(property);
            }
        }
        return new MountedElement(identity, ownerPath, List.copyOf(properties));
    }

    /// Disposes every binding. Repeated calls are permitted.
    void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (PropertyBinding<?> binding : bindings.values()) {
            binding.dispose();
        }
        bindings.clear();
        owner.close();
    }

    /// Creates one binding from a specification.
    ///
    /// @param spec the specification
    /// @param <T> the property type
    /// @return the binding
    private <T> PropertyBinding<T> createBinding(MountedElementScope.BindingSpec<T> spec) {
        return new PropertyBinding<>(
                spec.name(),
                spec.valueType(),
                spec.phaseImpact(),
                spec.reader(),
                spec.applier(),
                owner
        );
    }

    /// Returns whether an existing binding can keep its observer.
    ///
    /// @param previous the committed binding
    /// @param spec the next specification
    /// @return whether the contract is unchanged
    private static boolean sameContract(
            PropertyBinding<?> previous,
            MountedElementScope.BindingSpec<?> spec
    ) {
        return previous.valueType().equals(spec.valueType())
                && previous.phaseImpact().equals(spec.phaseImpact())
                && previous.reader() == spec.reader()
                && previous.applier() == spec.applier();
    }
}
