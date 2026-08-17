package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;

/// Content-light metadata in nits, or zeros when the source publishes none.
///
/// @param maxCll the maximum content light level
/// @param maxFall the maximum frame-average light level
@NotNullByDefault
public record ContentLightMetadata(float maxCll, float maxFall) {
    /// Absent metadata.
    public static final ContentLightMetadata NONE = new ContentLightMetadata(0.0f, 0.0f);

    /// Validates finite nonnegative values.
    public ContentLightMetadata {
        if (!Float.isFinite(maxCll) || !Float.isFinite(maxFall) || maxCll < 0.0f || maxFall < 0.0f) {
            throw new IllegalArgumentException("Content-light values must be finite and nonnegative");
        }
    }
}
