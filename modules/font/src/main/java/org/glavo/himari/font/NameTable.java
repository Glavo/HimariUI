package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/// Reads a first-stable OpenType `name` table copyright, family, unique-ID, style, full-name,
/// version, PostScript, trademark, manufacturer, designer, description, typographic, and WWS names.
///
/// The first Windows Unicode (`platform 3`, `encoding 1`) `nameID 0`–`14` / `16` / `17` /
/// `18` / `19` / `20` / `21` / `22` / `23` / `24` / `25` record wins. When that record is absent, the first Macintosh Roman
/// (`platform 1`, `encoding 0`) record of the same name ID is used.
@NotNullByDefault
public final class NameTable {
    /// Shared empty table.
    static final NameTable EMPTY = new NameTable(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null);

    /// Decoded copyright, or `null` when no `nameID 0` record is present.
    private final @Nullable String copyright;

    /// Decoded family name, or `null` when no `nameID 1` record is present.
    private final @Nullable String familyName;

    /// Decoded unique font identifier, or `null` when no `nameID 3` record is present.
    private final @Nullable String uniqueId;

    /// Decoded style name, or `null` when no `nameID 2` record is present.
    private final @Nullable String styleName;

    /// Decoded full name, or `null` when no `nameID 4` record is present.
    private final @Nullable String fullName;

    /// Decoded version string, or `null` when no `nameID 5` record is present.
    private final @Nullable String versionString;

    /// Decoded PostScript name, or `null` when no `nameID 6` record is present.
    private final @Nullable String postScriptName;

    /// Decoded trademark, or `null` when no `nameID 7` record is present.
    private final @Nullable String trademark;

    /// Decoded manufacturer, or `null` when no `nameID 8` record is present.
    private final @Nullable String manufacturer;

    /// Decoded designer, or `null` when no `nameID 9` record is present.
    private final @Nullable String designer;

    /// Decoded description, or `null` when no `nameID 10` record is present.
    private final @Nullable String description;

    /// Decoded typographic family, or `null` when no `nameID 16` record is present.
    private final @Nullable String typographicFamily;

    /// Decoded typographic subfamily, or `null` when no `nameID 17` record is present.
    private final @Nullable String typographicSubfamily;

    /// Decoded vendor URL, or `null` when no `nameID 11` record is present.
    private final @Nullable String vendorUrl;

    /// Decoded license, or `null` when no `nameID 13` record is present.
    private final @Nullable String license;

    /// Decoded designer URL, or `null` when no `nameID 12` record is present.
    private final @Nullable String designerUrl;

    /// Decoded license URL, or `null` when no `nameID 14` record is present.
    private final @Nullable String licenseUrl;

    /// Decoded WWS family, or `null` when no `nameID 21` record is present.
    private final @Nullable String wwsFamily;

    /// Decoded WWS subfamily, or `null` when no `nameID 22` record is present.
    private final @Nullable String wwsSubfamily;

    /// Decoded sample text, or `null` when no `nameID 19` record is present.
    private final @Nullable String sampleText;

    /// Decoded compatible full name, or `null` when no `nameID 18` record is present.
    private final @Nullable String compatibleFull;

    /// Decoded PostScript CID findfont name, or `null` when no `nameID 20` record is present.
    private final @Nullable String postScriptCid;

    /// Decoded Variations PostScript prefix, or `null` when no `nameID 25` record is present.
    private final @Nullable String variationsPostScriptPrefix;

    /// Decoded light-background palette name, or `null` when no `nameID 23` record is present.
    private final @Nullable String lightBackgroundPalette;

    /// Decoded dark-background palette name, or `null` when no `nameID 24` record is present.
    private final @Nullable String darkBackgroundPalette;

    /// Creates a table.
    ///
    /// @param copyright the copyright, or `null`
    /// @param familyName the family, or `null`
    /// @param uniqueId the unique identifier, or `null`
    /// @param styleName the style, or `null`
    /// @param fullName the full name, or `null`
    /// @param versionString the version, or `null`
    /// @param postScriptName the PostScript name, or `null`
    /// @param trademark the trademark, or `null`
    /// @param manufacturer the manufacturer, or `null`
    /// @param designer the designer, or `null`
    /// @param description the description, or `null`
    /// @param typographicFamily the typographic family, or `null`
    /// @param typographicSubfamily the typographic subfamily, or `null`
    /// @param vendorUrl the vendor URL, or `null`
    /// @param license the license, or `null`
    /// @param designerUrl the designer URL, or `null`
    /// @param licenseUrl the license URL, or `null`
    /// @param wwsFamily the WWS family, or `null`
    /// @param wwsSubfamily the WWS subfamily, or `null`
    /// @param sampleText the sample text, or `null`
    /// @param compatibleFull the compatible full name, or `null`
    /// @param postScriptCid the PostScript CID findfont name, or `null`
    /// @param variationsPostScriptPrefix the Variations PostScript prefix, or `null`
    /// @param lightBackgroundPalette the light-background palette name, or `null`
    /// @param darkBackgroundPalette the dark-background palette name, or `null`
    private NameTable(
            @Nullable String copyright,
            @Nullable String familyName,
            @Nullable String uniqueId,
            @Nullable String styleName,
            @Nullable String fullName,
            @Nullable String versionString,
            @Nullable String postScriptName,
            @Nullable String trademark,
            @Nullable String manufacturer,
            @Nullable String designer,
            @Nullable String description,
            @Nullable String typographicFamily,
            @Nullable String typographicSubfamily,
            @Nullable String vendorUrl,
            @Nullable String license,
            @Nullable String designerUrl,
            @Nullable String licenseUrl,
            @Nullable String wwsFamily,
            @Nullable String wwsSubfamily,
            @Nullable String sampleText,
            @Nullable String compatibleFull,
            @Nullable String postScriptCid,
            @Nullable String variationsPostScriptPrefix,
            @Nullable String lightBackgroundPalette,
            @Nullable String darkBackgroundPalette
    ) {
        this.copyright = copyright;
        this.familyName = familyName;
        this.uniqueId = uniqueId;
        this.styleName = styleName;
        this.fullName = fullName;
        this.versionString = versionString;
        this.postScriptName = postScriptName;
        this.trademark = trademark;
        this.manufacturer = manufacturer;
        this.designer = designer;
        this.description = description;
        this.typographicFamily = typographicFamily;
        this.typographicSubfamily = typographicSubfamily;
        this.vendorUrl = vendorUrl;
        this.license = license;
        this.designerUrl = designerUrl;
        this.licenseUrl = licenseUrl;
        this.wwsFamily = wwsFamily;
        this.wwsSubfamily = wwsSubfamily;
        this.sampleText = sampleText;
        this.compatibleFull = compatibleFull;
        this.postScriptCid = postScriptCid;
        this.variationsPostScriptPrefix = variationsPostScriptPrefix;
        this.lightBackgroundPalette = lightBackgroundPalette;
        this.darkBackgroundPalette = darkBackgroundPalette;
    }

    /// Parses a `name` table, or returns [`#EMPTY`].
    ///
    /// @param table the table bytes, or `null`
    /// @return the table
    static NameTable parse(@Nullable ByteBuffer table) {
        if (table == null || table.remaining() < 6) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.getShort();
        int count = Short.toUnsignedInt(buffer.getShort());
        int stringOffset = Short.toUnsignedInt(buffer.getShort());
        if (count < 1 || buffer.remaining() < count * 12L) {
            return EMPTY;
        }
        byte[] whole = new byte[table.capacity()];
        table.duplicate().order(ByteOrder.BIG_ENDIAN).rewind().get(whole);
        @Nullable String windowsCopyright = null;
        @Nullable String macintoshCopyright = null;
        @Nullable String windowsFamily = null;
        @Nullable String macintoshFamily = null;
        @Nullable String windowsUnique = null;
        @Nullable String macintoshUnique = null;
        @Nullable String windowsStyle = null;
        @Nullable String macintoshStyle = null;
        @Nullable String windowsFull = null;
        @Nullable String macintoshFull = null;
        @Nullable String windowsVersion = null;
        @Nullable String macintoshVersion = null;
        @Nullable String windowsPostScript = null;
        @Nullable String macintoshPostScript = null;
        @Nullable String windowsTrademark = null;
        @Nullable String macintoshTrademark = null;
        @Nullable String windowsManufacturer = null;
        @Nullable String macintoshManufacturer = null;
        @Nullable String windowsDesigner = null;
        @Nullable String macintoshDesigner = null;
        @Nullable String windowsDescription = null;
        @Nullable String macintoshDescription = null;
        @Nullable String windowsTypoFamily = null;
        @Nullable String macintoshTypoFamily = null;
        @Nullable String windowsTypoSubfamily = null;
        @Nullable String macintoshTypoSubfamily = null;
        @Nullable String windowsVendorUrl = null;
        @Nullable String macintoshVendorUrl = null;
        @Nullable String windowsLicense = null;
        @Nullable String macintoshLicense = null;
        @Nullable String windowsDesignerUrl = null;
        @Nullable String macintoshDesignerUrl = null;
        @Nullable String windowsLicenseUrl = null;
        @Nullable String macintoshLicenseUrl = null;
        @Nullable String windowsWwsFamily = null;
        @Nullable String macintoshWwsFamily = null;
        @Nullable String windowsWwsSubfamily = null;
        @Nullable String macintoshWwsSubfamily = null;
        @Nullable String windowsSample = null;
        @Nullable String macintoshSample = null;
        @Nullable String windowsCompatible = null;
        @Nullable String macintoshCompatible = null;
        @Nullable String windowsCid = null;
        @Nullable String macintoshCid = null;
        @Nullable String windowsVarPs = null;
        @Nullable String macintoshVarPs = null;
        @Nullable String windowsLightBg = null;
        @Nullable String macintoshLightBg = null;
        @Nullable String windowsDarkBg = null;
        @Nullable String macintoshDarkBg = null;
        for (int index = 0; index < count; index++) {
            int platform = Short.toUnsignedInt(buffer.getShort());
            int encoding = Short.toUnsignedInt(buffer.getShort());
            buffer.getShort();
            int nameId = Short.toUnsignedInt(buffer.getShort());
            int length = Short.toUnsignedInt(buffer.getShort());
            int offset = Short.toUnsignedInt(buffer.getShort());
            if ((nameId != 0 && nameId != 1 && nameId != 2 && nameId != 3 && nameId != 4 && nameId != 5 && nameId != 6
                    && nameId != 7 && nameId != 8 && nameId != 9 && nameId != 10 && nameId != 11 && nameId != 12
                    && nameId != 13 && nameId != 14 && nameId != 16 && nameId != 17
                    && nameId != 18 && nameId != 19 && nameId != 20 && nameId != 21 && nameId != 22
                    && nameId != 23 && nameId != 24 && nameId != 25)
                    || length < 1) {
                continue;
            }
            long start = (long) stringOffset + (long) offset;
            if (start < 0 || start + (long) length > whole.length) {
                continue;
            }
            int at = (int) start;
            if (platform == 3 && encoding == 1) {
                if (nameId == 0 && windowsCopyright == null) {
                    windowsCopyright = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 1 && windowsFamily == null) {
                    windowsFamily = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 3 && windowsUnique == null) {
                    windowsUnique = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 2 && windowsStyle == null) {
                    windowsStyle = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 4 && windowsFull == null) {
                    windowsFull = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 5 && windowsVersion == null) {
                    windowsVersion = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 6 && windowsPostScript == null) {
                    windowsPostScript = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 7 && windowsTrademark == null) {
                    windowsTrademark = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 8 && windowsManufacturer == null) {
                    windowsManufacturer = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 9 && windowsDesigner == null) {
                    windowsDesigner = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 10 && windowsDescription == null) {
                    windowsDescription = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 16 && windowsTypoFamily == null) {
                    windowsTypoFamily = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 17 && windowsTypoSubfamily == null) {
                    windowsTypoSubfamily = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 11 && windowsVendorUrl == null) {
                    windowsVendorUrl = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 13 && windowsLicense == null) {
                    windowsLicense = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 12 && windowsDesignerUrl == null) {
                    windowsDesignerUrl = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 14 && windowsLicenseUrl == null) {
                    windowsLicenseUrl = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 21 && windowsWwsFamily == null) {
                    windowsWwsFamily = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 22 && windowsWwsSubfamily == null) {
                    windowsWwsSubfamily = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 19 && windowsSample == null) {
                    windowsSample = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 18 && windowsCompatible == null) {
                    windowsCompatible = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 20 && windowsCid == null) {
                    windowsCid = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 25 && windowsVarPs == null) {
                    windowsVarPs = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 23 && windowsLightBg == null) {
                    windowsLightBg = new String(whole, at, length, StandardCharsets.UTF_16BE);
                } else if (nameId == 24 && windowsDarkBg == null) {
                    windowsDarkBg = new String(whole, at, length, StandardCharsets.UTF_16BE);
                }
            } else if (platform == 1 && encoding == 0) {
                if (nameId == 0 && macintoshCopyright == null) {
                    macintoshCopyright = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 1 && macintoshFamily == null) {
                    macintoshFamily = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 3 && macintoshUnique == null) {
                    macintoshUnique = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 2 && macintoshStyle == null) {
                    macintoshStyle = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 4 && macintoshFull == null) {
                    macintoshFull = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 5 && macintoshVersion == null) {
                    macintoshVersion = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 6 && macintoshPostScript == null) {
                    macintoshPostScript = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 7 && macintoshTrademark == null) {
                    macintoshTrademark = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 8 && macintoshManufacturer == null) {
                    macintoshManufacturer = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 9 && macintoshDesigner == null) {
                    macintoshDesigner = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 10 && macintoshDescription == null) {
                    macintoshDescription = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 16 && macintoshTypoFamily == null) {
                    macintoshTypoFamily = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 17 && macintoshTypoSubfamily == null) {
                    macintoshTypoSubfamily = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 11 && macintoshVendorUrl == null) {
                    macintoshVendorUrl = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 13 && macintoshLicense == null) {
                    macintoshLicense = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 12 && macintoshDesignerUrl == null) {
                    macintoshDesignerUrl = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 14 && macintoshLicenseUrl == null) {
                    macintoshLicenseUrl = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 21 && macintoshWwsFamily == null) {
                    macintoshWwsFamily = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 22 && macintoshWwsSubfamily == null) {
                    macintoshWwsSubfamily = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 19 && macintoshSample == null) {
                    macintoshSample = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 18 && macintoshCompatible == null) {
                    macintoshCompatible = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 20 && macintoshCid == null) {
                    macintoshCid = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 25 && macintoshVarPs == null) {
                    macintoshVarPs = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 23 && macintoshLightBg == null) {
                    macintoshLightBg = new String(whole, at, length, StandardCharsets.US_ASCII);
                } else if (nameId == 24 && macintoshDarkBg == null) {
                    macintoshDarkBg = new String(whole, at, length, StandardCharsets.US_ASCII);
                }
            }
        }
        @Nullable String copyright = windowsCopyright != null ? windowsCopyright : macintoshCopyright;
        @Nullable String family = windowsFamily != null ? windowsFamily : macintoshFamily;
        @Nullable String unique = windowsUnique != null ? windowsUnique : macintoshUnique;
        @Nullable String style = windowsStyle != null ? windowsStyle : macintoshStyle;
        @Nullable String full = windowsFull != null ? windowsFull : macintoshFull;
        @Nullable String version = windowsVersion != null ? windowsVersion : macintoshVersion;
        @Nullable String postScript = windowsPostScript != null ? windowsPostScript : macintoshPostScript;
        @Nullable String trademark = windowsTrademark != null ? windowsTrademark : macintoshTrademark;
        @Nullable String manufacturer = windowsManufacturer != null ? windowsManufacturer : macintoshManufacturer;
        @Nullable String designer = windowsDesigner != null ? windowsDesigner : macintoshDesigner;
        @Nullable String description = windowsDescription != null ? windowsDescription : macintoshDescription;
        @Nullable String typoFamily = windowsTypoFamily != null ? windowsTypoFamily : macintoshTypoFamily;
        @Nullable String typoSubfamily = windowsTypoSubfamily != null ? windowsTypoSubfamily : macintoshTypoSubfamily;
        @Nullable String vendorUrl = windowsVendorUrl != null ? windowsVendorUrl : macintoshVendorUrl;
        @Nullable String license = windowsLicense != null ? windowsLicense : macintoshLicense;
        @Nullable String designerUrl = windowsDesignerUrl != null ? windowsDesignerUrl : macintoshDesignerUrl;
        @Nullable String licenseUrl = windowsLicenseUrl != null ? windowsLicenseUrl : macintoshLicenseUrl;
        @Nullable String wwsFamily = windowsWwsFamily != null ? windowsWwsFamily : macintoshWwsFamily;
        @Nullable String wwsSubfamily = windowsWwsSubfamily != null ? windowsWwsSubfamily : macintoshWwsSubfamily;
        @Nullable String sampleText = windowsSample != null ? windowsSample : macintoshSample;
        @Nullable String compatibleFull = windowsCompatible != null ? windowsCompatible : macintoshCompatible;
        @Nullable String postScriptCid = windowsCid != null ? windowsCid : macintoshCid;
        @Nullable String variationsPostScriptPrefix = windowsVarPs != null ? windowsVarPs : macintoshVarPs;
        @Nullable String lightBackgroundPalette = windowsLightBg != null ? windowsLightBg : macintoshLightBg;
        @Nullable String darkBackgroundPalette = windowsDarkBg != null ? windowsDarkBg : macintoshDarkBg;
        return copyright == null && family == null && unique == null && style == null && full == null
                && version == null && postScript == null && trademark == null && manufacturer == null
                && designer == null && description == null && typoFamily == null && typoSubfamily == null
                && vendorUrl == null && license == null && designerUrl == null && licenseUrl == null
                && wwsFamily == null && wwsSubfamily == null && sampleText == null && compatibleFull == null
                && postScriptCid == null && variationsPostScriptPrefix == null
                && lightBackgroundPalette == null && darkBackgroundPalette == null
                ? EMPTY
                : new NameTable(
                        copyright,
                        family,
                        unique,
                        style,
                        full,
                        version,
                        postScript,
                        trademark,
                        manufacturer,
                        designer,
                        description,
                        typoFamily,
                        typoSubfamily,
                        vendorUrl,
                        license,
                        designerUrl,
                        licenseUrl,
                        wwsFamily,
                        wwsSubfamily,
                        sampleText,
                        compatibleFull,
                        postScriptCid,
                        variationsPostScriptPrefix,
                        lightBackgroundPalette,
                        darkBackgroundPalette
                );
    }

    /// Returns the copyright string.
    ///
    /// @return the copyright, or `null` when absent
    @Nullable String copyright() {
        return copyright;
    }

    /// Returns the unique font identifier.
    ///
    /// @return the unique identifier, or `null` when absent
    @Nullable String uniqueId() {
        return uniqueId;
    }

    /// Returns the family name.
    ///
    /// @return the family, or `null` when absent
    @Nullable String familyName() {
        return familyName;
    }

    /// Returns the style name.
    ///
    /// @return the style, or `null` when absent
    @Nullable String styleName() {
        return styleName;
    }

    /// Returns the full name.
    ///
    /// @return the full name, or `null` when absent
    @Nullable String fullName() {
        return fullName;
    }

    /// Returns the version string.
    ///
    /// @return the version, or `null` when absent
    @Nullable String versionString() {
        return versionString;
    }

    /// Returns the PostScript name.
    ///
    /// @return the PostScript name, or `null` when absent
    @Nullable String postScriptName() {
        return postScriptName;
    }

    /// Returns the trademark string.
    ///
    /// @return the trademark, or `null` when absent
    @Nullable String trademark() {
        return trademark;
    }

    /// Returns the manufacturer string.
    ///
    /// @return the manufacturer, or `null` when absent
    @Nullable String manufacturer() {
        return manufacturer;
    }

    /// Returns the designer string.
    ///
    /// @return the designer, or `null` when absent
    @Nullable String designer() {
        return designer;
    }

    /// Returns the description string.
    ///
    /// @return the description, or `null` when absent
    @Nullable String description() {
        return description;
    }

    /// Returns the typographic family name.
    ///
    /// @return the typographic family, or `null` when absent
    @Nullable String typographicFamily() {
        return typographicFamily;
    }

    /// Returns the typographic subfamily name.
    ///
    /// @return the typographic subfamily, or `null` when absent
    @Nullable String typographicSubfamily() {
        return typographicSubfamily;
    }

    /// Returns the vendor URL.
    ///
    /// @return the vendor URL, or `null` when absent
    @Nullable String vendorUrl() {
        return vendorUrl;
    }

    /// Returns the license string.
    ///
    /// @return the license, or `null` when absent
    @Nullable String license() {
        return license;
    }

    /// Returns the designer URL.
    ///
    /// @return the designer URL, or `null` when absent
    @Nullable String designerUrl() {
        return designerUrl;
    }

    /// Returns the license URL.
    ///
    /// @return the license URL, or `null` when absent
    @Nullable String licenseUrl() {
        return licenseUrl;
    }

    /// Returns the WWS family name.
    ///
    /// @return the WWS family, or `null` when absent
    @Nullable String wwsFamily() {
        return wwsFamily;
    }

    /// Returns the WWS subfamily name.
    ///
    /// @return the WWS subfamily, or `null` when absent
    @Nullable String wwsSubfamily() {
        return wwsSubfamily;
    }

    /// Returns the sample text.
    ///
    /// @return the sample text, or `null` when absent
    @Nullable String sampleText() {
        return sampleText;
    }

    /// Returns the compatible full name.
    ///
    /// @return the compatible full name, or `null` when absent
    @Nullable String compatibleFull() {
        return compatibleFull;
    }

    /// Returns the PostScript CID findfont name.
    ///
    /// @return the CID name, or `null` when absent
    @Nullable String postScriptCid() {
        return postScriptCid;
    }

    /// Returns the Variations PostScript name prefix.
    ///
    /// @return the prefix, or `null` when absent
    @Nullable String variationsPostScriptPrefix() {
        return variationsPostScriptPrefix;
    }

    /// Returns the light-background palette name.
    ///
    /// @return the palette name, or `null` when absent
    @Nullable String lightBackgroundPalette() {
        return lightBackgroundPalette;
    }

    /// Returns the dark-background palette name.
    ///
    /// @return the palette name, or `null` when absent
    @Nullable String darkBackgroundPalette() {
        return darkBackgroundPalette;
    }
}
