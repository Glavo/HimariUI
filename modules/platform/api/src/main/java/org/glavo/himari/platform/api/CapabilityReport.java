package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;

/// Session-level color capability and SDR-fallback report for `DIAG-001`.
///
/// @param requested the presentation mode the application asked for
/// @param effective the first mode the host actually advertised
/// @param description the first display's color volume
/// @param mappingOwner who performs gamut/tone mapping (`application` or `host`)
/// @param disabledHdrReason why HDR is not enabled, or empty when HDR modes are advertised
@NotNullByDefault
public record CapabilityReport(
        PresentationMode requested,
        PresentationMode effective,
        DisplayColorDescription description,
        String mappingOwner,
        String disabledHdrReason
) {
    /// Validates one report.
    public CapabilityReport {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(effective, "effective");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(mappingOwner, "mappingOwner");
        Objects.requireNonNull(disabledHdrReason, "disabledHdrReason");
        if (mappingOwner.isBlank()) {
            throw new IllegalArgumentException("mappingOwner must be nonblank");
        }
    }

    /// Builds a report from the session's first display.
    ///
    /// Requested presentation is SDR. Effective presentation is the first advertised mode.
    /// Mapping stays application-owned. When the host lists only SDR, `disabledHdrReason`
    /// is `host advertised only SDR`.
    ///
    /// @param session the open platform session
    /// @return the report
    public static CapabilityReport from(PlatformSession<?> session) {
        Objects.requireNonNull(session, "session");
        List<DisplaySnapshot> displays = session.displays();
        if (displays.isEmpty()) {
            throw new IllegalStateException("session has no displays");
        }
        DisplayColorDescription description = displays.getFirst().colorCapabilities().description();
        PresentationMode effective = description.presentationModes().getFirst();
        boolean hdrAdvertised = false;
        for (PresentationMode mode : description.presentationModes()) {
            if (mode.equals(PresentationMode.EXTENDED_LINEAR)
                    || mode.equals(PresentationMode.PQ)
                    || mode.equals(PresentationMode.HLG)) {
                hdrAdvertised = true;
                break;
            }
        }
        String reason = hdrAdvertised ? "" : "host advertised only SDR";
        return new CapabilityReport(PresentationMode.SDR, effective, description, "application", reason);
    }
}
