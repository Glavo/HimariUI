package org.glavo.himari.spikes.d3d12;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes one message retrieved from `ID3D12InfoQueue`.
///
/// @param category the native message category
/// @param severity the native message severity
/// @param severityName the stable severity spelling
/// @param id the native D3D12 message identifier
/// @param description the copied UTF-8 description
@NotNullByDefault
record D3d12DebugMessage(int category, int severity, String severityName, int id, String description) {
    /// Returns whether this message is an error or corruption report.
    ///
    /// @return `true` for severity zero or one
    boolean isError() {
        return severity == 0 || severity == 1;
    }

    /// Encodes this message as deterministic-key-order JSON.
    ///
    /// @return the JSON object
    String toJson() {
        return "{\"category\":" + category
                + ",\"severity\":" + severity
                + ",\"severityName\":" + JsonSupport.quote(severityName)
                + ",\"id\":" + id
                + ",\"description\":" + JsonSupport.quote(description) + '}';
    }
}
