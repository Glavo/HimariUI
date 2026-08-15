package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Captures the latest published state of one platform window.
///
/// `configuration.frame()` retains owner-relative popup coordinates, while `effectiveFrame` always
/// uses global logical desktop coordinates. A snapshot remains immutable after later configuration,
/// display migration, or closure.
///
/// @param id the stable session-local window identifier
/// @param role the host surface role
/// @param ownerId the popup owner, or `null` for a top-level window
/// @param configuration the latest requested application properties
/// @param effectiveFrame the effective global logical frame
/// @param effectivelyVisible whether the window is currently eligible for presentation after owner
/// visibility and minimization are applied
/// @param surfaceSize the current software or host surface size in physical pixels
/// @param scaleFactor the current finite positive physical-pixels-per-logical-pixel scale
/// @param displayId the current display selected for the window
/// @param surface the stable target-neutral surface descriptor
/// @param configurationGeneration the nonnegative generation advanced for each semantic snapshot
/// change
/// @param lifecycle whether the window remains open
@NotNullByDefault
public record WindowSnapshot(
        WindowId id,
        SurfaceRole role,
        @Nullable WindowId ownerId,
        WindowConfiguration configuration,
        LogicalRect effectiveFrame,
        boolean effectivelyVisible,
        PhysicalSize surfaceSize,
        double scaleFactor,
        DisplayId displayId,
        SurfaceDescriptor surface,
        long configurationGeneration,
        WindowLifecycle lifecycle
) {
    /// Creates a validated window snapshot.
    ///
    /// @throws IllegalArgumentException if role ownership, scale, surface role, or generation is
    /// invalid
    public WindowSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(effectiveFrame, "effectiveFrame");
        Objects.requireNonNull(surfaceSize, "surfaceSize");
        Objects.requireNonNull(displayId, "displayId");
        Objects.requireNonNull(surface, "surface");
        Objects.requireNonNull(lifecycle, "lifecycle");
        if ((role == SurfaceRole.TOPLEVEL) != (ownerId == null)) {
            throw new IllegalArgumentException("Window role and owner presence are inconsistent");
        }
        if (role == SurfaceRole.POPUP && configuration.state() != WindowState.NORMAL) {
            throw new IllegalArgumentException("A popup window must use the normal state");
        }
        if (effectivelyVisible && !configuration.visible()) {
            throw new IllegalArgumentException("An effectively visible window must request visibility");
        }
        if (lifecycle == WindowLifecycle.CLOSED && effectivelyVisible) {
            throw new IllegalArgumentException("A closed window cannot be effectively visible");
        }
        if (!Double.isFinite(scaleFactor) || scaleFactor <= 0.0) {
            throw new IllegalArgumentException("Window scale factor must be finite and positive");
        }
        if (surface.role() != role) {
            throw new IllegalArgumentException("Surface and window roles must match");
        }
        if (configurationGeneration < 0L) {
            throw new IllegalArgumentException("Window configuration generation must be nonnegative");
        }
    }
}
