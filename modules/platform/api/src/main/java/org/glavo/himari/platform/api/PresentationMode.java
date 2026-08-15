package org.glavo.himari.platform.api;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies a display presentation mode using an extensible stable identifier.
///
/// A mode states how a surface may present values; it does not by itself identify RGB primaries,
/// precision, a pixel format, luminance metadata, or who performs tone mapping.
///
/// @param identifier the nonblank, trimmed stable identifier
@NotNullByDefault
public record PresentationMode(String identifier) implements Comparable<PresentationMode> {
    /// Standard-dynamic-range display-referred presentation.
    public static final PresentationMode SDR = new PresentationMode("sdr");

    /// Extended-linear presentation relative to an explicit reference white.
    public static final PresentationMode EXTENDED_LINEAR = new PresentationMode("extended-linear");

    /// Perceptual-quantizer presentation with separately declared luminance metadata.
    public static final PresentationMode PQ = new PresentationMode("pq");

    /// Hybrid-log-gamma presentation with separately declared luminance metadata.
    public static final PresentationMode HLG = new PresentationMode("hlg");

    /// Creates a presentation-mode identifier.
    ///
    /// @throws IllegalArgumentException if `identifier` is blank or has surrounding whitespace
    public PresentationMode {
        if (identifier.isBlank() || !identifier.equals(identifier.strip())) {
            throw new IllegalArgumentException("Presentation-mode identifier must be nonblank and trimmed");
        }
    }

    /// Compares modes lexicographically by stable identifier.
    ///
    /// @param other the other mode
    /// @return the comparison result
    @Override
    public int compareTo(PresentationMode other) {
        return identifier.compareTo(other.identifier);
    }

    /// Returns the stable identifier.
    ///
    /// @return the identifier
    @Override
    public String toString() {
        return identifier;
    }
}
