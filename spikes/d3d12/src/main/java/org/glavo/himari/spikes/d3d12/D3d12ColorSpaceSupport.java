package org.glavo.himari.spikes.d3d12;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes one color-space candidate queried against the selected SDR swapchain.
///
/// @param name the DXGI color-space name
/// @param code the native `DXGI_COLOR_SPACE_TYPE` value
/// @param supportFlags the unsigned `DXGI_SWAP_CHAIN_COLOR_SPACE_SUPPORT_FLAG` mask
/// @param present whether the color space carries the `PRESENT` support flag
@NotNullByDefault
record D3d12ColorSpaceSupport(String name, int code, long supportFlags, boolean present) {
    /// Encodes this observation as deterministic-key-order JSON.
    ///
    /// @return the JSON object
    String toJson() {
        return "{\"name\":" + JsonSupport.quote(name)
                + ",\"code\":" + code
                + ",\"supportFlags\":" + supportFlags
                + ",\"present\":" + present + '}';
    }
}
