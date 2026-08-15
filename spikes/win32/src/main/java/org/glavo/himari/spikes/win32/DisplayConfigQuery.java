package org.glavo.himari.spikes.win32;

import org.glavo.himari.spikes.win32.generated.Win32FfmBindings;
import org.glavo.himari.spikes.win32.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/// Queries the Windows DisplayConfig state associated with a DXGI source name.
@NotNullByDefault
final class DisplayConfigQuery {
    /// Successful Win32 status code.
    private static final int ERROR_SUCCESS = 0;

    /// Status returned when display topology changes between buffer sizing and enumeration.
    private static final int ERROR_INSUFFICIENT_BUFFER = 122;

    /// Requests only paths active in the current desktop topology.
    private static final int QDC_ONLY_ACTIVE_PATHS = 0x00000002;

    /// Device-information request for a path source's GDI device name.
    private static final int DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME = 1;

    /// Device-information request for a path target's Advanced Color state.
    private static final int DISPLAYCONFIG_DEVICE_INFO_GET_ADVANCED_COLOR_INFO = 9;

    /// Maximum topology restart count when concurrent display changes invalidate sized buffers.
    private static final int MAX_QUERY_ATTEMPTS = 4;

    /// Defensive upper bound for either active-path or mode records in this conformance process.
    private static final int MAX_RECORD_COUNT = 4096;

    /// Prevents instantiation of this utility class.
    private DisplayConfigQuery() {
    }

    /// Returns the active Advanced Color state associated with `dxgiDeviceName`.
    ///
    /// Cloned active paths are accepted only when every matching target reports identical state. A topology change
    /// that produces insufficient buffers restarts the complete size-and-query operation.
    ///
    /// @param bindings the generated User32 bindings
    /// @param dxgiDeviceName the bounded device name returned by `IDXGIOutput6::GetDesc1`
    /// @return the matching dynamic state
    /// @throws IllegalStateException if enumeration fails, no active path matches, or cloned targets disagree
    static DisplayConfigAdvancedColor query(Win32FfmBindings bindings, String dxgiDeviceName) {
        for (int attempt = 0; attempt < MAX_QUERY_ATTEMPTS; attempt++) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment pathCountPointer = arena.allocate(ValueLayout.JAVA_INT);
                MemorySegment modeCountPointer = arena.allocate(ValueLayout.JAVA_INT);
                int result = bindings.getDisplayConfigBufferSizes(
                        QDC_ONLY_ACTIVE_PATHS,
                        pathCountPointer,
                        modeCountPointer
                );
                requireWin32Success(result, "GetDisplayConfigBufferSizes");

                int pathCapacity = boundedCount(pathCountPointer, "display path");
                int modeCapacity = boundedCount(modeCountPointer, "display mode");
                if (pathCapacity == 0) {
                    throw new IllegalStateException("GetDisplayConfigBufferSizes returned no active display paths");
                }
                MemorySegment paths = arena.allocate(MemoryLayout.sequenceLayout(
                        pathCapacity,
                        Win32Layouts.DISPLAYCONFIG_PATH_INFO
                ));
                MemorySegment modes = arena.allocate(MemoryLayout.sequenceLayout(
                        Math.max(1, modeCapacity),
                        Win32Layouts.DISPLAYCONFIG_MODE_INFO
                ));
                result = bindings.queryDisplayConfig(
                        QDC_ONLY_ACTIVE_PATHS,
                        pathCountPointer,
                        paths,
                        modeCountPointer,
                        modes,
                        MemorySegment.NULL
                );
                if (result == ERROR_INSUFFICIENT_BUFFER) {
                    continue;
                }
                requireWin32Success(result, "QueryDisplayConfig");
                int pathCount = boundedCount(pathCountPointer, "returned display path");
                if (pathCount > pathCapacity) {
                    throw new IllegalStateException("QueryDisplayConfig returned " + pathCount
                            + " paths into a capacity of " + pathCapacity);
                }
                return decodeMatchingPaths(bindings, arena, paths, pathCount, dxgiDeviceName);
            }
        }
        throw new IllegalStateException("Display topology changed during all " + MAX_QUERY_ATTEMPTS
                + " QueryDisplayConfig attempts");
    }

    /// Decodes and reconciles every active target path matching one GDI source name.
    ///
    /// @param bindings the generated bindings
    /// @param arena the temporary request-packet arena
    /// @param paths the native active-path array
    /// @param pathCount the initialized element count
    /// @param dxgiDeviceName the target DXGI source name
    /// @return the common matching target state
    private static DisplayConfigAdvancedColor decodeMatchingPaths(
            Win32FfmBindings bindings,
            Arena arena,
            MemorySegment paths,
            int pathCount,
            String dxgiDeviceName
    ) {
        @Nullable DisplayConfigAdvancedColor commonState = null;
        int matchingPathCount = 0;
        for (int index = 0; index < pathCount; index++) {
            MemorySegment path = paths.asSlice(
                    (long) index * Win32Layouts.DISPLAYCONFIG_PATH_INFO.byteSize(),
                    Win32Layouts.DISPLAYCONFIG_PATH_INFO.byteSize()
            );
            MemorySegment source = path.asSlice(
                    Win32Layouts.DISPLAYCONFIG_PATH_INFO_SOURCE_INFO_OFFSET,
                    Win32Layouts.DISPLAYCONFIG_PATH_SOURCE_INFO.byteSize()
            );
            String sourceName = querySourceName(bindings, arena, source);
            if (!dxgiDeviceName.equalsIgnoreCase(sourceName)) {
                continue;
            }
            MemorySegment target = path.asSlice(
                    Win32Layouts.DISPLAYCONFIG_PATH_INFO_TARGET_INFO_OFFSET,
                    Win32Layouts.DISPLAYCONFIG_PATH_TARGET_INFO.byteSize()
            );
            DisplayConfigAdvancedColor candidate = queryAdvancedColor(bindings, arena, sourceName, target);
            if (commonState != null && !commonState.hasSameTargetState(candidate)) {
                throw new IllegalStateException("Active cloned targets for " + dxgiDeviceName
                        + " report inconsistent Advanced Color state");
            }
            commonState = candidate;
            matchingPathCount++;
        }
        if (commonState == null) {
            throw new IllegalStateException("No active DisplayConfig source matched DXGI output " + dxgiDeviceName);
        }
        return commonState.withMatchingActivePathCount(matchingPathCount);
    }

    /// Queries one source-path GDI device name.
    ///
    /// @param bindings the generated bindings
    /// @param arena the request-packet arena
    /// @param source the source path record
    /// @return the bounded UTF-16 source name
    private static String querySourceName(
            Win32FfmBindings bindings,
            Arena arena,
            MemorySegment source
    ) {
        MemorySegment packet = arena.allocate(Win32Layouts.DISPLAYCONFIG_SOURCE_DEVICE_NAME);
        packet.fill((byte) 0);
        writeRequestHeader(
                packet,
                DISPLAYCONFIG_DEVICE_INFO_GET_SOURCE_NAME,
                Win32Layouts.DISPLAYCONFIG_SOURCE_DEVICE_NAME.byteSize(),
                source,
                Win32Layouts.DISPLAYCONFIG_PATH_SOURCE_INFO_ADAPTER_ID_OFFSET,
                Win32Layouts.DISPLAYCONFIG_PATH_SOURCE_INFO_ID_OFFSET
        );
        int result = bindings.displayConfigGetDeviceInfo(packet);
        requireWin32Success(result, "DisplayConfigGetDeviceInfo(GET_SOURCE_NAME)");
        return readWideString(
                packet,
                Win32Layouts.DISPLAYCONFIG_SOURCE_DEVICE_NAME_VIEW_GDI_DEVICE_NAME_OFFSET,
                32
        );
    }

    /// Queries one target path's current Advanced Color state.
    ///
    /// @param bindings the generated bindings
    /// @param arena the request-packet arena
    /// @param sourceName the matched source name
    /// @param target the target path record
    /// @return the decoded target state with a provisional matching-path count
    private static DisplayConfigAdvancedColor queryAdvancedColor(
            Win32FfmBindings bindings,
            Arena arena,
            String sourceName,
            MemorySegment target
    ) {
        MemorySegment packet = arena.allocate(Win32Layouts.DISPLAYCONFIG_GET_ADVANCED_COLOR_INFO);
        packet.fill((byte) 0);
        writeRequestHeader(
                packet,
                DISPLAYCONFIG_DEVICE_INFO_GET_ADVANCED_COLOR_INFO,
                Win32Layouts.DISPLAYCONFIG_GET_ADVANCED_COLOR_INFO.byteSize(),
                target,
                Win32Layouts.DISPLAYCONFIG_PATH_TARGET_INFO_ADAPTER_ID_OFFSET,
                Win32Layouts.DISPLAYCONFIG_PATH_TARGET_INFO_ID_OFFSET
        );
        int result = bindings.displayConfigGetDeviceInfo(packet);
        requireWin32Success(result, "DisplayConfigGetDeviceInfo(GET_ADVANCED_COLOR_INFO)");
        int flags = packet.get(
                ValueLayout.JAVA_INT,
                Win32Layouts.DISPLAYCONFIG_GET_ADVANCED_COLOR_INFO_VALUE_OFFSET
        );
        int encoding = packet.get(
                ValueLayout.JAVA_INT,
                Win32Layouts.DISPLAYCONFIG_GET_ADVANCED_COLOR_INFO_COLOR_ENCODING_OFFSET
        );
        return new DisplayConfigAdvancedColor(
                sourceName,
                1,
                (flags & 0x1) != 0,
                (flags & 0x2) != 0,
                (flags & 0x4) != 0,
                (flags & 0x8) != 0,
                encoding,
                colorEncodingName(encoding),
                packet.get(
                        ValueLayout.JAVA_INT,
                        Win32Layouts.DISPLAYCONFIG_GET_ADVANCED_COLOR_INFO_BITS_PER_COLOR_CHANNEL_OFFSET
                )
        );
    }

    /// Writes a DisplayConfig request header from a source or target path record.
    ///
    /// @param packet the zero-filled request packet whose header begins at offset zero
    /// @param requestType the `DISPLAYCONFIG_DEVICE_INFO_TYPE` value
    /// @param packetSize the complete request-packet size
    /// @param pathInfo the source or target path record
    /// @param adapterOffset the nested adapter-LUID offset
    /// @param idOffset the nested source or target identifier offset
    private static void writeRequestHeader(
            MemorySegment packet,
            int requestType,
            long packetSize,
            MemorySegment pathInfo,
            long adapterOffset,
            long idOffset
    ) {
        packet.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.DISPLAYCONFIG_DEVICE_INFO_HEADER_TYPE_OFFSET,
                requestType
        );
        packet.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.DISPLAYCONFIG_DEVICE_INFO_HEADER_SIZE_OFFSET,
                Math.toIntExact(packetSize)
        );
        packet.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.DISPLAYCONFIG_DEVICE_INFO_HEADER_ADAPTER_ID_OFFSET
                        + Win32Layouts.LUID_LOW_PART_OFFSET,
                pathInfo.get(ValueLayout.JAVA_INT, adapterOffset + Win32Layouts.LUID_LOW_PART_OFFSET)
        );
        packet.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.DISPLAYCONFIG_DEVICE_INFO_HEADER_ADAPTER_ID_OFFSET
                        + Win32Layouts.LUID_HIGH_PART_OFFSET,
                pathInfo.get(ValueLayout.JAVA_INT, adapterOffset + Win32Layouts.LUID_HIGH_PART_OFFSET)
        );
        packet.set(
                ValueLayout.JAVA_INT,
                Win32Layouts.DISPLAYCONFIG_DEVICE_INFO_HEADER_ID_OFFSET,
                pathInfo.get(ValueLayout.JAVA_INT, idOffset)
        );
    }

    /// Reads an unsigned count and rejects values outside this profile's bounded allocation policy.
    ///
    /// @param pointer the initialized native `UINT32` storage
    /// @param description the diagnostic record description
    /// @return the bounded count
    private static int boundedCount(MemorySegment pointer, String description) {
        long value = Integer.toUnsignedLong(pointer.get(ValueLayout.JAVA_INT, 0L));
        if (value > MAX_RECORD_COUNT) {
            throw new IllegalStateException(description + " count exceeds " + MAX_RECORD_COUNT + ": " + value);
        }
        return (int) value;
    }

    /// Reads a bounded null-terminated UTF-16 array.
    ///
    /// @param segment the containing native structure
    /// @param byteOffset the first UTF-16 code unit
    /// @param capacity the maximum number of code units
    /// @return the decoded Java string
    private static String readWideString(MemorySegment segment, long byteOffset, int capacity) {
        StringBuilder value = new StringBuilder(capacity);
        for (int index = 0; index < capacity; index++) {
            char character = (char) Short.toUnsignedInt(segment.get(
                    ValueLayout.JAVA_SHORT,
                    byteOffset + (long) index * Short.BYTES
            ));
            if (character == 0) {
                break;
            }
            value.append(character);
        }
        return value.toString();
    }

    /// Returns a stable symbolic name for a `DISPLAYCONFIG_COLOR_ENCODING` value.
    ///
    /// @param encoding the raw enumeration value
    /// @return the SDK name or an explicit unknown spelling
    private static String colorEncodingName(int encoding) {
        return switch (encoding) {
            case 0 -> "RGB";
            case 1 -> "YCBCR444";
            case 2 -> "YCBCR422";
            case 3 -> "YCBCR420";
            case 4 -> "INTENSITY";
            case -1 -> "FORCE_UINT32";
            default -> "UNKNOWN_" + Integer.toUnsignedString(encoding);
        };
    }

    /// Requires a successful Win32 status returned directly by a DisplayConfig API.
    ///
    /// @param result the raw Win32 status
    /// @param operation the operation name
    private static void requireWin32Success(int result, String operation) {
        if (result != ERROR_SUCCESS) {
            throw new IllegalStateException(operation + " failed with Win32 status "
                    + Integer.toUnsignedString(result));
        }
    }
}
