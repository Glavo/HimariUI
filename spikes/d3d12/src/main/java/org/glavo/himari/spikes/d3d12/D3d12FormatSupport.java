package org.glavo.himari.spikes.d3d12;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes one candidate DXGI format queried through `ID3D12Device::CheckFeatureSupport`.
///
/// @param name the DXGI format name
/// @param code the native `DXGI_FORMAT` value
/// @param support1 the unsigned `D3D12_FORMAT_SUPPORT1` mask
/// @param support2 the unsigned `D3D12_FORMAT_SUPPORT2` mask
/// @param renderTarget whether `D3D12_FORMAT_SUPPORT1_RENDER_TARGET` is present
@NotNullByDefault
record D3d12FormatSupport(String name, int code, long support1, long support2, boolean renderTarget) {
    /// Encodes this observation as deterministic-key-order JSON.
    ///
    /// @return the JSON object
    String toJson() {
        return "{\"name\":" + JsonSupport.quote(name)
                + ",\"code\":" + code
                + ",\"support1\":" + support1
                + ",\"support2\":" + support2
                + ",\"renderTarget\":" + renderTarget + '}';
    }
}
