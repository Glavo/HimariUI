package org.glavo.himari.runtime.mount;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Identifies one mounted element by its owning structural group and local key.
///
/// @param groupId the positive structural group identity
/// @param mountKey the nonblank group-local mount key
@NotNullByDefault
public record MountIdentity(long groupId, String mountKey) {
    /// Validates the identity.
    public MountIdentity {
        if (groupId <= 0L) {
            throw new IllegalArgumentException("groupId must be positive");
        }
        Objects.requireNonNull(mountKey, "mountKey");
        if (mountKey.isBlank()) {
            throw new IllegalArgumentException("mountKey must not be blank");
        }
    }

    /// Returns the deterministic owner path suffix for this identity.
    ///
    /// @param groupPath the owning group's path
    /// @return the mount path
    public String ownerPath(String groupPath) {
        Objects.requireNonNull(groupPath, "groupPath");
        return groupPath + "#mount[" + mountKey + ']';
    }
}
