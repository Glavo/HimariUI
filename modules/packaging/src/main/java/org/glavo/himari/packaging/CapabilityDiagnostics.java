package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Reports requested and effective presentation capability without implying HDR support.
///
/// @param backend the backend identifier
/// @param requestedMode the requested presentation mode
/// @param effectiveMode the effective presentation mode
/// @param hdrEnabled whether HDR output is enabled
/// @param disabledHdrReason the reason HDR is disabled
@NotNullByDefault
public record CapabilityDiagnostics(
        String backend,
        String requestedMode,
        String effectiveMode,
        boolean hdrEnabled,
        String disabledHdrReason
) {
    /// Validates the report.
    public CapabilityDiagnostics {
        Objects.requireNonNull(backend, "backend");
        Objects.requireNonNull(requestedMode, "requestedMode");
        Objects.requireNonNull(effectiveMode, "effectiveMode");
        Objects.requireNonNull(disabledHdrReason, "disabledHdrReason");
        if (hdrEnabled) {
            throw new IllegalArgumentException("First-stable diagnostics must not enable HDR output");
        }
    }

    /// Returns the Windows/D3D12 first-stable SDR fallback report.
    ///
    /// @return the report
    public static CapabilityDiagnostics windowsD3d12Sdr() {
        return new CapabilityDiagnostics(
                "d3d12",
                "sdr",
                "color-managed-sdr",
                false,
                "Production HDR presentation is not a first-stable requirement"
        );
    }

    /// Encodes the report as JSON.
    ///
    /// @return the document
    public String toCanonicalJson() {
        return "{\"backend\":\"" + backend
                + "\",\"requestedMode\":\"" + requestedMode
                + "\",\"effectiveMode\":\"" + effectiveMode
                + "\",\"hdrEnabled\":false,\"disabledHdrReason\":\""
                + disabledHdrReason
                + "\"}";
    }
}
