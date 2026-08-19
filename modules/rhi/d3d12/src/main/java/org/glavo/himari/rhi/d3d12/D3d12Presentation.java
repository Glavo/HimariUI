package org.glavo.himari.rhi.d3d12;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Records one flip-model SDR present through a production swapchain.
///
/// @param presented whether `IDXGISwapChain::Present` succeeded
/// @param format the DXGI format name
/// @param colorSpace the DXGI color-space name actually selected by `SetColorSpace1`
/// @param hdrMetadataApplied always `false`
/// @param ownedReferences COM references acquired for the swapchain
/// @param releasedReferences COM references released after close, or `0` while the chain is live
/// @param backBufferIndex the presented back-buffer index
/// @param cleared whether a render-target clear was recorded
/// @param p709CheckHresult HRESULT from `CheckColorSpaceSupport(P709)`
/// @param p709Support `DXGI_SWAP_CHAIN_COLOR_SPACE_SUPPORT_FLAG` bits for P709
/// @param p2020PqCheckHresult HRESULT from `CheckColorSpaceSupport(P2020 PQ)`
/// @param p2020PqSupport `DXGI_SWAP_CHAIN_COLOR_SPACE_SUPPORT_FLAG` bits for P2020 PQ
@NotNullByDefault
public record D3d12Presentation(
        boolean presented,
        String format,
        String colorSpace,
        boolean hdrMetadataApplied,
        int ownedReferences,
        int releasedReferences,
        int backBufferIndex,
        boolean cleared,
        int p709CheckHresult,
        int p709Support,
        int p2020PqCheckHresult,
        int p2020PqSupport
) {
    /// `DXGI_SWAP_CHAIN_COLOR_SPACE_SUPPORT_FLAG_PRESENT`.
    public static final int SUPPORT_PRESENT = 0x1;

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

    /// Returns whether `CheckColorSpaceSupport` reported present support for P709.
    ///
    /// @return whether the PRESENT bit is set
    public boolean p709PresentSupported() {
        return p709CheckHresult == 0 && (p709Support & SUPPORT_PRESENT) != 0;
    }
}
