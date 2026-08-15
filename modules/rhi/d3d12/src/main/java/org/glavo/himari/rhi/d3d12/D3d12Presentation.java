package org.glavo.himari.rhi.d3d12;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one flip-model SDR present through a production swapchain.
///
/// @param presented whether `IDXGISwapChain::Present` succeeded
/// @param format the DXGI format name
/// @param colorSpace the DXGI color-space name
/// @param hdrMetadataApplied always `false`
/// @param ownedReferences COM references acquired for the swapchain
/// @param releasedReferences COM references released after close, or `0` while the chain is live
/// @param backBufferIndex the presented back-buffer index
/// @param cleared whether a render-target clear was recorded
@NotNullByDefault
public record D3d12Presentation(
        boolean presented,
        String format,
        String colorSpace,
        boolean hdrMetadataApplied,
        int ownedReferences,
        int releasedReferences,
        int backBufferIndex,
        boolean cleared
) {
    /// Validates the observation.
    public D3d12Presentation {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(colorSpace, "colorSpace");
        if (ownedReferences < 0 || releasedReferences < 0) {
            throw new IllegalArgumentException("COM reference counts must be nonnegative");
        }
        if (backBufferIndex < 0) {
            throw new IllegalArgumentException("backBufferIndex must be nonnegative");
        }
        if (hdrMetadataApplied) {
            throw new IllegalArgumentException("Production D3D12 first-stable present must not apply HDR metadata");
        }
    }
}
