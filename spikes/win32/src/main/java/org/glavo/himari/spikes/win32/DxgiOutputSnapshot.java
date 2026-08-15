package org.glavo.himari.spikes.win32;

import org.jetbrains.annotations.NotNullByDefault;

/// Describes the dynamic `IDXGIOutput6::GetDesc1` result matched to the spike window.
///
/// @param adapterIndex the enumerated adapter index
/// @param outputIndex the enumerated output index within the adapter
/// @param factoryCurrent whether the factory reported itself current after enumeration
/// @param deviceName the DXGI output device name
/// @param monitorAddress the unsigned native `HMONITOR` value in hexadecimal
/// @param desktopLeft the output's left desktop coordinate
/// @param desktopTop the output's top desktop coordinate
/// @param desktopRight the output's right desktop coordinate
/// @param desktopBottom the output's bottom desktop coordinate
/// @param attachedToDesktop whether the output is attached to the desktop
/// @param rotation the raw `DXGI_MODE_ROTATION` value
/// @param bitsPerColor the output's current bits per color channel
/// @param colorSpaceCode the raw `DXGI_COLOR_SPACE_TYPE` value
/// @param colorSpaceName the symbolic color-space name known to this profile
/// @param effectivePresentationMode the current mode derived from the reported color space, not a support promise
/// @param advancedColor the matching active-path DisplayConfig state
/// @param redX the reported red-primary x coordinate
/// @param redY the reported red-primary y coordinate
/// @param greenX the reported green-primary x coordinate
/// @param greenY the reported green-primary y coordinate
/// @param blueX the reported blue-primary x coordinate
/// @param blueY the reported blue-primary y coordinate
/// @param whiteX the reported white-point x coordinate
/// @param whiteY the reported white-point y coordinate
/// @param minimumLuminance the reported minimum luminance in nits
/// @param maximumLuminance the reported peak luminance in nits
/// @param maximumFullFrameLuminance the reported full-frame luminance in nits
@NotNullByDefault
record DxgiOutputSnapshot(
        int adapterIndex,
        int outputIndex,
        boolean factoryCurrent,
        String deviceName,
        String monitorAddress,
        int desktopLeft,
        int desktopTop,
        int desktopRight,
        int desktopBottom,
        boolean attachedToDesktop,
        int rotation,
        int bitsPerColor,
        int colorSpaceCode,
        String colorSpaceName,
        String effectivePresentationMode,
        DisplayConfigAdvancedColor advancedColor,
        float redX,
        float redY,
        float greenX,
        float greenY,
        float blueX,
        float blueY,
        float whiteX,
        float whiteY,
        float minimumLuminance,
        float maximumLuminance,
        float maximumFullFrameLuminance
) {
    /// Returns whether every chromaticity and luminance measurement is finite.
    ///
    /// @return whether every floating-point measurement is finite
    boolean measurementsFinite() {
        return Float.isFinite(redX)
                && Float.isFinite(redY)
                && Float.isFinite(greenX)
                && Float.isFinite(greenY)
                && Float.isFinite(blueX)
                && Float.isFinite(blueY)
                && Float.isFinite(whiteX)
                && Float.isFinite(whiteY)
                && Float.isFinite(minimumLuminance)
                && Float.isFinite(maximumLuminance)
                && Float.isFinite(maximumFullFrameLuminance);
    }

    /// Encodes this snapshot as deterministic JSON.
    ///
    /// @return a complete JSON document ending in a line feed
    String toJson() {
        return """
                {
                  "profileId": "m0-win32-window",
                  "profileVersion": 1,
                  "target": {"operatingSystem": "windows", "architecture": "x86_64", "runtime": "jvm"},
                  "sources": ["IDXGIOutput6::GetDesc1", "QueryDisplayConfig", "DisplayConfigGetDeviceInfo"],
                  "dynamicSnapshot": true,
                  "factoryCurrent": %s,
                  "adapterIndex": %d,
                  "outputIndex": %d,
                  "deviceName": %s,
                  "monitorAddress": %s,
                  "desktopCoordinates": {"left": %d, "top": %d, "right": %d, "bottom": %d},
                  "attachedToDesktop": %s,
                  "rotation": %d,
                  "bitsPerColor": %d,
                  "colorSpace": {"code": %d, "name": %s},
                  "effectivePresentationMode": %s,
                  "advancedColor": %s,
                  "primaries": {
                    "red": [%s, %s],
                    "green": [%s, %s],
                    "blue": [%s, %s],
                    "white": [%s, %s]
                  },
                  "luminanceNits": {"minimum": %s, "maximum": %s, "maximumFullFrame": %s},
                  "measurementsFinite": %s,
                  "assertions": {
                    "windowMonitorMatchedExactly": true,
                    "output6DescriptionSucceeded": true,
                    "displayConfigAdvancedColorQueried": true,
                    "matchingActivePathsConsistent": true,
                    "capabilityNotInferredFromFormatAlone": true
                  }
                }
                """.formatted(
                factoryCurrent,
                adapterIndex,
                outputIndex,
                JsonSupport.quote(deviceName),
                JsonSupport.quote(monitorAddress),
                desktopLeft,
                desktopTop,
                desktopRight,
                desktopBottom,
                attachedToDesktop,
                rotation,
                bitsPerColor,
                colorSpaceCode,
                JsonSupport.quote(colorSpaceName),
                JsonSupport.quote(effectivePresentationMode),
                advancedColor.toJsonObject(),
                JsonSupport.number(redX),
                JsonSupport.number(redY),
                JsonSupport.number(greenX),
                JsonSupport.number(greenY),
                JsonSupport.number(blueX),
                JsonSupport.number(blueY),
                JsonSupport.number(whiteX),
                JsonSupport.number(whiteY),
                JsonSupport.number(minimumLuminance),
                JsonSupport.number(maximumLuminance),
                JsonSupport.number(maximumFullFrameLuminance),
                measurementsFinite()
        );
    }
}
