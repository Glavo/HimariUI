package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Requests asynchronous creation of a top-level or popup platform window.
///
/// @param role the requested host surface role
/// @param ownerId the owner window for a popup, or `null` for a top-level window
/// @param configuration the initial application-controlled properties
@NotNullByDefault
public record WindowRequest(
        SurfaceRole role,
        @Nullable WindowId ownerId,
        WindowConfiguration configuration
) {
    /// Creates and validates a window request.
    ///
    /// @throws IllegalArgumentException if owner presence does not match `role` or a popup requests
    /// a non-normal state
    public WindowRequest {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(configuration, "configuration");
        if (role == SurfaceRole.TOPLEVEL && ownerId != null) {
            throw new IllegalArgumentException("A top-level window must not declare an owner");
        }
        if (role == SurfaceRole.POPUP && ownerId == null) {
            throw new IllegalArgumentException("A popup window must declare an owner");
        }
        if (role == SurfaceRole.POPUP && configuration.state() != WindowState.NORMAL) {
            throw new IllegalArgumentException("A popup window must use the normal state");
        }
    }

    /// Creates a top-level window request.
    ///
    /// @param configuration the initial configuration
    /// @return the request
    public static WindowRequest toplevel(WindowConfiguration configuration) {
        return new WindowRequest(SurfaceRole.TOPLEVEL, null, configuration);
    }

    /// Creates an owner-relative popup window request.
    ///
    /// @param ownerId the popup owner
    /// @param configuration the initial popup configuration
    /// @return the request
    public static WindowRequest popup(WindowId ownerId, WindowConfiguration configuration) {
        return new WindowRequest(SurfaceRole.POPUP, ownerId, configuration);
    }
}
