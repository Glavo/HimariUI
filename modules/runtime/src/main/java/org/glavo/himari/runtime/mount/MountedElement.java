package org.glavo.himari.runtime.mount;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Captures one committed mounted element and its property model targets.
///
/// @param identity the stable mount identity
/// @param ownerPath the deterministic group-plus-mount path
/// @param properties committed properties in declaration order
@NotNullByDefault
public record MountedElement(
        MountIdentity identity,
        String ownerPath,
        @Unmodifiable List<MountedProperty> properties
) {
    /// Validates one committed element snapshot.
    public MountedElement {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(ownerPath, "ownerPath");
        if (ownerPath.isBlank()) {
            throw new IllegalArgumentException("ownerPath must not be blank");
        }
        properties = List.copyOf(properties);
    }

    /// Returns the committed property with the given name.
    ///
    /// @param name the property name
    /// @return the property
    /// @throws IllegalArgumentException if the name is not committed
    public MountedProperty property(String name) {
        Objects.requireNonNull(name, "name");
        for (MountedProperty property : properties) {
            if (property.name().equals(name)) {
                return property;
            }
        }
        throw new IllegalArgumentException("Unknown mounted property: " + name);
    }
}
