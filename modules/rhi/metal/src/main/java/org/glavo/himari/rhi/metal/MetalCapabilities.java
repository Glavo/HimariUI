package org.glavo.himari.rhi.metal;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Reports a truthful SDR Metal capability snapshot.
///
/// @param deviceCreated whether `MTLCreateSystemDefaultDevice` succeeded
/// @param commandQueueCreated whether `[device newCommandQueue]` succeeded
/// @param commandBufferCommitted whether a command buffer was created and committed
/// @param hdrPresentationEnabled always `false` for this first-stable backend
/// @param presentationMode the explicit effective presentation mode
@NotNullByDefault
public record MetalCapabilities(
        boolean deviceCreated,
        boolean commandQueueCreated,
        boolean commandBufferCommitted,
        boolean hdrPresentationEnabled,
        String presentationMode
) {
    /// Validates the snapshot.
    public MetalCapabilities {
        Objects.requireNonNull(presentationMode, "presentationMode");
        if (hdrPresentationEnabled) {
            throw new IllegalArgumentException("Production Metal first-stable presentation is SDR only");
        }
    }
}
