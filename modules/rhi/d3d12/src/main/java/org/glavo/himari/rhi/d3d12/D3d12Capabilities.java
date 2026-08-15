package org.glavo.himari.rhi.d3d12;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Reports the truthful SDR capability snapshot of one [D3d12Device].
///
/// @param r8g8b8a8RenderTarget whether `DXGI_FORMAT_R8G8B8A8_UNORM` is a render target
/// @param support1 the raw `D3D12_FEATURE_DATA_FORMAT_SUPPORT.Support1` bits
/// @param hdrPresentationEnabled always `false` for this first-stable backend
/// @param presentationMode the explicit effective presentation mode
@NotNullByDefault
public record D3d12Capabilities(
        boolean r8g8b8a8RenderTarget,
        long support1,
        boolean hdrPresentationEnabled,
        String presentationMode
) {
    /// Validates the snapshot.
    public D3d12Capabilities {
        Objects.requireNonNull(presentationMode, "presentationMode");
        if (hdrPresentationEnabled) {
            throw new IllegalArgumentException("Production D3D12 first-stable presentation is SDR only");
        }
    }
}
