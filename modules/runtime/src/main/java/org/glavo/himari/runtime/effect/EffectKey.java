package org.glavo.himari.runtime.effect;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Identifies one keyed effect independently of mounted topology.
///
/// @param ownerPath the deterministic owner path
/// @param localKey the nonblank owner-local effect key
@NotNullByDefault
public record EffectKey(String ownerPath, String localKey) {
    /// Validates the key.
    public EffectKey {
        Objects.requireNonNull(ownerPath, "ownerPath");
        if (ownerPath.isBlank()) {
            throw new IllegalArgumentException("ownerPath must not be blank");
        }
        Objects.requireNonNull(localKey, "localKey");
        if (localKey.isBlank()) {
            throw new IllegalArgumentException("localKey must not be blank");
        }
    }

    /// Returns the deterministic diagnostic path.
    ///
    /// @return the path
    public String path() {
        return ownerPath + "#effect[" + localKey + ']';
    }
}
