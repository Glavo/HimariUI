package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/// Parses a bounded ICC `deviceSettingsType` (`devs`) table.
///
/// Profiles are untrusted input. Each platform stores one or more setting combinations.
/// A Microsoft `rsln` value is an 8-byte integer whose most-significant 32 bits are the Y
/// resolution in dpi and whose least-significant 32 bits are the X resolution. Media and
/// halftone settings use 4-byte codes.
///
/// @param platforms the platform entries in table order
@NotNullByDefault
public record IccDeviceSettings(@Unmodifiable List<Platform> platforms) {
    /// Type and tag `'devs'`.
    public static final int SIGNATURE = 0x6465_7673;

    /// Microsoft platform signature (`MSFT`).
    public static final String PLATFORM_MSFT = "MSFT";

    /// Microsoft resolution setting (`rsln`).
    public static final String SETTING_RESOLUTION = "rsln";

    /// Microsoft media-type setting (`mtyp`).
    public static final String SETTING_MEDIA = "mtyp";

    /// Microsoft halftone setting (`hftn`).
    public static final String SETTING_HALFTONE = "hftn";

    /// `DMMEDIA_GLOSSY`.
    public static final int MEDIA_GLOSSY = 3;

    /// Maximum accepted platform, combination, or setting counts.
    public static final int MAX_ENTRIES = 16;

    /// Maximum accepted payload bytes for one setting.
    public static final int MAX_SETTING_BYTES = 64;

    /// Bytes in the tag header before the first platform.
    private static final int HEADER_BYTES = 12;

    /// Bytes in one platform header.
    private static final int PLATFORM_HEADER_BYTES = 12;

    /// Bytes in one combination header.
    private static final int COMBINATION_HEADER_BYTES = 8;

    /// Bytes in one setting header.
    private static final int SETTING_HEADER_BYTES = 12;

    /// Validates and copies the platforms.
    public IccDeviceSettings {
        Objects.requireNonNull(platforms, "platforms");
        if (platforms.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("ICC device-settings platform count exceeds the accepted bound");
        }
        platforms = List.copyOf(platforms);
        for (Platform platform : platforms) {
            Objects.requireNonNull(platform, "platform");
        }
    }

    /// Parses one `devs` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the device settings
    public static IccDeviceSettings parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER_BYTES || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC devs tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC device-settings tag is not devs");
        }
        int platformCount = u32(bytes, offset + 8);
        if (platformCount < 0 || platformCount > MAX_ENTRIES) {
            throw new IllegalArgumentException("ICC device-settings platform count is outside the accepted bounds");
        }
        ArrayList<Platform> platforms = new ArrayList<>(platformCount);
        int cursor = HEADER_BYTES;
        for (int index = 0; index < platformCount; index++) {
            if (cursor + PLATFORM_HEADER_BYTES > size) {
                throw new IllegalArgumentException("ICC device-settings platform is truncated");
            }
            String id = signature(bytes, offset + cursor);
            int platformSize = u32(bytes, offset + cursor + 4);
            int combinationCount = u32(bytes, offset + cursor + 8);
            if (platformSize < PLATFORM_HEADER_BYTES || cursor + platformSize > size) {
                throw new IllegalArgumentException("ICC device-settings platform size is outside the tag");
            }
            if (combinationCount < 0 || combinationCount > MAX_ENTRIES) {
                throw new IllegalArgumentException("ICC device-settings combination count is outside the accepted bounds");
            }
            platforms.add(readPlatform(
                    bytes,
                    offset + cursor + PLATFORM_HEADER_BYTES,
                    platformSize - PLATFORM_HEADER_BYTES,
                    id,
                    combinationCount
            ));
            cursor += platformSize;
        }
        return new IccDeviceSettings(platforms);
    }

    /// Finds the first platform whose ID equals `id`.
    ///
    /// @param id the four-character platform signature
    /// @return the platform, or `null` when absent
    public @Nullable Platform platform(String id) {
        Objects.requireNonNull(id, "id");
        for (Platform platform : platforms) {
            if (platform.id().equals(id)) {
                return platform;
            }
        }
        return null;
    }

    /// Reads one platform's combinations from `relative` bytes of payload.
    private static Platform readPlatform(
            byte[] bytes,
            int start,
            int remaining,
            String id,
            int combinationCount
    ) {
        ArrayList<Combination> combinations = new ArrayList<>(combinationCount);
        int cursor = 0;
        for (int index = 0; index < combinationCount; index++) {
            if (cursor + COMBINATION_HEADER_BYTES > remaining) {
                throw new IllegalArgumentException("ICC device-settings combination is truncated");
            }
            int combinationSize = u32(bytes, start + cursor);
            int settingCount = u32(bytes, start + cursor + 4);
            if (combinationSize < COMBINATION_HEADER_BYTES || cursor + combinationSize > remaining) {
                throw new IllegalArgumentException("ICC device-settings combination size is outside the platform");
            }
            if (settingCount < 0 || settingCount > MAX_ENTRIES) {
                throw new IllegalArgumentException("ICC device-settings setting count is outside the accepted bounds");
            }
            combinations.add(readCombination(
                    bytes,
                    start + cursor + COMBINATION_HEADER_BYTES,
                    combinationSize - COMBINATION_HEADER_BYTES,
                    settingCount
            ));
            cursor += combinationSize;
        }
        return new Platform(id, combinations);
    }

    /// Reads one combination's settings from `remaining` payload bytes.
    private static Combination readCombination(byte[] bytes, int start, int remaining, int settingCount) {
        ArrayList<Setting> settings = new ArrayList<>(settingCount);
        int cursor = 0;
        for (int index = 0; index < settingCount; index++) {
            if (cursor + SETTING_HEADER_BYTES > remaining) {
                throw new IllegalArgumentException("ICC device-settings setting is truncated");
            }
            String id = signature(bytes, start + cursor);
            int valueSize = u32(bytes, start + cursor + 4);
            int valueCount = u32(bytes, start + cursor + 8);
            if (valueSize < 1 || valueCount < 1 || valueCount > MAX_ENTRIES) {
                throw new IllegalArgumentException("ICC device-settings value geometry is outside the accepted bounds");
            }
            long payload = (long) valueSize * valueCount;
            if (payload > MAX_SETTING_BYTES || cursor + SETTING_HEADER_BYTES + payload > remaining) {
                throw new IllegalArgumentException("ICC device-settings values exceed the combination");
            }
            byte[] values = Arrays.copyOfRange(
                    bytes,
                    start + cursor + SETTING_HEADER_BYTES,
                    start + cursor + SETTING_HEADER_BYTES + (int) payload
            );
            settings.add(new Setting(id, valueSize, values));
            cursor += SETTING_HEADER_BYTES + (int) payload;
        }
        return new Combination(settings);
    }

    /// Reads a 4-character signature.
    private static String signature(byte[] bytes, int offset) {
        return new String(bytes, offset, 4, StandardCharsets.US_ASCII);
    }

    /// Reads a big-endian unsigned 32-bit integer as a signed Java `int`.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// One platform's setting combinations.
    ///
    /// @param id the four-character platform signature
    /// @param combinations the setting combinations in table order
    public record Platform(String id, @Unmodifiable List<Combination> combinations) {
        /// Validates the platform.
        public Platform {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(combinations, "combinations");
            if (id.length() != 4) {
                throw new IllegalArgumentException("ICC device-settings platform ID must be four characters");
            }
            if (combinations.size() > MAX_ENTRIES) {
                throw new IllegalArgumentException("ICC device-settings combination count exceeds the accepted bound");
            }
            combinations = List.copyOf(combinations);
            for (Combination combination : combinations) {
                Objects.requireNonNull(combination, "combination");
            }
        }
    }

    /// One valid combination of device settings.
    ///
    /// @param settings the settings in table order
    public record Combination(@Unmodifiable List<Setting> settings) {
        /// Validates the combination.
        public Combination {
            Objects.requireNonNull(settings, "settings");
            if (settings.size() > MAX_ENTRIES) {
                throw new IllegalArgumentException("ICC device-settings setting count exceeds the accepted bound");
            }
            settings = List.copyOf(settings);
            for (Setting setting : settings) {
                Objects.requireNonNull(setting, "setting");
            }
        }

        /// Finds the first setting whose ID equals `id`.
        ///
        /// @param id the four-character setting signature
        /// @return the setting, or `null` when absent
        public @Nullable Setting setting(String id) {
            Objects.requireNonNull(id, "id");
            for (Setting setting : settings) {
                if (setting.id().equals(id)) {
                    return setting;
                }
            }
            return null;
        }
    }

    /// One setting and its packed values.
    ///
    /// @param id the four-character setting signature
    /// @param valueSize bytes per value
    /// @param values packed values of length `valueSize × count`
    public record Setting(String id, int valueSize, byte @Unmodifiable [] values) {
        /// Validates and copies the values.
        public Setting {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(values, "values");
            if (id.length() != 4) {
                throw new IllegalArgumentException("ICC device-settings ID must be four characters");
            }
            if (valueSize < 1 || values.length < valueSize || values.length % valueSize != 0) {
                throw new IllegalArgumentException("ICC device-settings values must be a multiple of the value size");
            }
            if (values.length > MAX_SETTING_BYTES) {
                throw new IllegalArgumentException("ICC device-settings values exceed the accepted bound");
            }
            values = Arrays.copyOf(values, values.length);
        }

        /// Returns the number of packed values.
        ///
        /// @return the value count
        public int valueCount() {
            return values.length / valueSize;
        }

        /// Returns the first 32-bit value as an unsigned quantity in a Java `int` bit pattern.
        ///
        /// @return the first value
        public int firstU32() {
            if (valueSize < 4 || values.length < 4) {
                throw new IllegalStateException("ICC device-settings value is smaller than 4 bytes");
            }
            return u32(values, 0);
        }

        /// Returns the Microsoft `rsln` X resolution in dots per inch.
        ///
        /// @return the X dpi stored in the least-significant 32 bits
        public int resolutionX() {
            requireResolution();
            return u32(values, 4);
        }

        /// Returns the Microsoft `rsln` Y resolution in dots per inch.
        ///
        /// @return the Y dpi stored in the most-significant 32 bits
        public int resolutionY() {
            requireResolution();
            return u32(values, 0);
        }

        /// Rejects a setting that is not an 8-byte `rsln` value.
        private void requireResolution() {
            if (!SETTING_RESOLUTION.equals(id) || values.length < 8) {
                throw new IllegalStateException("ICC device-settings resolution requires an 8-byte rsln value");
            }
        }
    }
}
