package org.glavo.himari.spikes.win32;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes the active-path Advanced Color state returned by `DisplayConfigGetDeviceInfo`.
///
/// @param sourceDeviceName the matched GDI source name
/// @param matchingActivePathCount the number of active target paths sharing the source name
/// @param advancedColorSupported whether the target reports support for an Advanced Color mode
/// @param advancedColorEnabled whether an Advanced Color mode is currently enabled
/// @param wideColorEnforced whether wide color gamut is currently enforced
/// @param advancedColorForceDisabled whether system policy currently disables Advanced Color
/// @param colorEncodingCode the raw `DISPLAYCONFIG_COLOR_ENCODING` value
/// @param colorEncodingName the symbolic encoding name known to this profile
/// @param bitsPerColorChannel the current DisplayConfig bits per color channel
@NotNullByDefault
record DisplayConfigAdvancedColor(
        String sourceDeviceName,
        int matchingActivePathCount,
        boolean advancedColorSupported,
        boolean advancedColorEnabled,
        boolean wideColorEnforced,
        boolean advancedColorForceDisabled,
        int colorEncodingCode,
        String colorEncodingName,
        int bitsPerColorChannel
) {
    /// Returns a copy with the final number of matching active paths.
    ///
    /// @param count the positive matching-path count
    /// @return the updated immutable state
    DisplayConfigAdvancedColor withMatchingActivePathCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        return new DisplayConfigAdvancedColor(
                sourceDeviceName,
                count,
                advancedColorSupported,
                advancedColorEnabled,
                wideColorEnforced,
                advancedColorForceDisabled,
                colorEncodingCode,
                colorEncodingName,
                bitsPerColorChannel
        );
    }

    /// Returns whether another path reports the same Advanced Color state.
    ///
    /// The source name and path count are intentionally excluded because cloned targets share the source and are
    /// counted only after all active paths have been examined.
    ///
    /// @param other the state from another active target path
    /// @return whether all target-dependent fields agree
    boolean hasSameTargetState(DisplayConfigAdvancedColor other) {
        return advancedColorSupported == other.advancedColorSupported
                && advancedColorEnabled == other.advancedColorEnabled
                && wideColorEnforced == other.wideColorEnforced
                && advancedColorForceDisabled == other.advancedColorForceDisabled
                && colorEncodingCode == other.colorEncodingCode
                && bitsPerColorChannel == other.bitsPerColorChannel;
    }

    /// Encodes this state as a deterministic indented JSON object.
    ///
    /// @return a JSON object without a trailing line feed
    String toJsonObject() {
        return """
                {
                      "sourceDeviceName": %s,
                      "matchingActivePathCount": %d,
                      "advancedColorSupported": %s,
                      "advancedColorEnabled": %s,
                      "wideColorEnforced": %s,
                      "advancedColorForceDisabled": %s,
                      "colorEncoding": {"code": %d, "name": %s},
                      "bitsPerColorChannel": %s,
                      "matchingPathsConsistent": true
                    }""".formatted(
                JsonSupport.quote(sourceDeviceName),
                matchingActivePathCount,
                advancedColorSupported,
                advancedColorEnabled,
                wideColorEnforced,
                advancedColorForceDisabled,
                colorEncodingCode,
                JsonSupport.quote(colorEncodingName),
                Integer.toUnsignedString(bitsPerColorChannel)
        );
    }
}
