package org.glavo.himari.spikes.win32;

import org.glavo.himari.spikes.win32.generated.Win32FfmBindings;
import org.glavo.himari.spikes.win32.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Locale;

/// Queries the `IDXGIOutput6` that owns a window's current monitor.
@SuppressWarnings("restricted")
@NotNullByDefault
final class DxgiOutputQuery {
    /// `DXGI_ERROR_NOT_FOUND` as a signed `HRESULT`.
    private static final int DXGI_ERROR_NOT_FOUND = (int) 0x887A0002L;

    /// Maximum factory refresh attempts when DXGI reports that enumeration became stale.
    private static final int MAX_FACTORY_ATTEMPTS = 3;

    /// Prevents instantiation of this utility class.
    private DxgiOutputQuery() {
    }

    /// Queries a current output snapshot that exactly matches `targetMonitor`.
    ///
    /// A stale DXGI factory is discarded and recreated up to three times, as required by the dynamic `IsCurrent`
    /// contract.
    ///
    /// @param bindings the generated Win32 and DXGI bindings
    /// @param targetMonitor the non-null monitor handle associated with the window
    /// @return the current matching output description
    /// @throws IllegalStateException if DXGI fails, no output matches, or every factory becomes stale
    static DxgiOutputSnapshot query(Win32FfmBindings bindings, MemorySegment targetMonitor) {
        if (isNull(targetMonitor)) {
            throw new IllegalArgumentException("targetMonitor must not be NULL");
        }
        @Nullable DxgiOutputSnapshot stale = null;
        for (int attempt = 0; attempt < MAX_FACTORY_ATTEMPTS; attempt++) {
            DxgiOutputSnapshot snapshot = queryOnce(bindings, targetMonitor);
            if (snapshot.factoryCurrent()) {
                return snapshot;
            }
            stale = snapshot;
        }
        throw new IllegalStateException("DXGI factory remained stale after " + MAX_FACTORY_ATTEMPTS
                + " attempts; last output was " + stale.deviceName());
    }

    /// Performs one factory lifetime and returns the matching output even if the factory just became stale.
    ///
    /// @param bindings the generated bindings
    /// @param targetMonitor the target monitor handle
    /// @return the matching output snapshot
    private static DxgiOutputSnapshot queryOnce(Win32FfmBindings bindings, MemorySegment targetMonitor) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment factoryId = arena.allocate(Win32Layouts.GUID);
            writeFactory1Guid(factoryId);
            MemorySegment outputId = arena.allocate(Win32Layouts.GUID);
            writeOutput6Guid(outputId);
            MemorySegment resultPointer = arena.allocate(ValueLayout.ADDRESS);
            int result = bindings.createDxgiFactory1(factoryId, resultPointer);
            requireHresult(result, "CreateDXGIFactory1");
            MemorySegment factory = resultPointer.get(ValueLayout.ADDRESS, 0L);
            requireNonNull(factory, "CreateDXGIFactory1 returned NULL");
            try {
                return enumerateFactory(bindings, arena, factory, outputId, targetMonitor, resultPointer);
            } finally {
                release(bindings, factory);
            }
        }
    }

    /// Enumerates every adapter and output until the target monitor is found.
    ///
    /// @param bindings the generated bindings
    /// @param arena the temporary allocation arena
    /// @param factory the owned factory interface
    /// @param outputId the `IDXGIOutput6` interface identifier
    /// @param targetMonitor the target window monitor
    /// @param resultPointer reusable pointer-result storage
    /// @return the matching snapshot
    private static DxgiOutputSnapshot enumerateFactory(
            Win32FfmBindings bindings,
            Arena arena,
            MemorySegment factory,
            MemorySegment outputId,
            MemorySegment targetMonitor,
            MemorySegment resultPointer
    ) {
        MemorySegment enumAdapters = functionAt(
                factory,
                Win32Layouts.IDXGI_FACTORY1_VTABLE.byteSize(),
                Win32Layouts.IDXGI_FACTORY1_VTABLE_ENUM_ADAPTERS1_OFFSET
        );
        int output6Unavailable = 0;
        int outputsVisited = 0;
        for (int adapterIndex = 0; ; adapterIndex++) {
            clearPointer(resultPointer);
            int result = Win32FfmBindings.invokeIdxgiFactory1EnumAdapters1Pointer(
                    enumAdapters,
                    factory,
                    adapterIndex,
                    resultPointer
            );
            if (result == DXGI_ERROR_NOT_FOUND) {
                break;
            }
            requireHresult(result, "IDXGIFactory1::EnumAdapters1(" + adapterIndex + ")");
            MemorySegment adapter = resultPointer.get(ValueLayout.ADDRESS, 0L);
            requireNonNull(adapter, "EnumAdapters1 returned NULL at index " + adapterIndex);
            try {
                MemorySegment enumOutputs = functionAt(
                        adapter,
                        Win32Layouts.IDXGI_ADAPTER1_VTABLE.byteSize(),
                        Win32Layouts.IDXGI_ADAPTER1_VTABLE_ENUM_OUTPUTS_OFFSET
                );
                for (int outputIndex = 0; ; outputIndex++) {
                    clearPointer(resultPointer);
                    result = Win32FfmBindings.invokeIdxgiAdapterEnumOutputsPointer(
                            enumOutputs,
                            adapter,
                            outputIndex,
                            resultPointer
                    );
                    if (result == DXGI_ERROR_NOT_FOUND) {
                        break;
                    }
                    requireHresult(result, "IDXGIAdapter::EnumOutputs(" + outputIndex + ")");
                    MemorySegment output = resultPointer.get(ValueLayout.ADDRESS, 0L);
                    requireNonNull(output, "EnumOutputs returned NULL at index " + outputIndex);
                    outputsVisited++;
                    try {
                        clearPointer(resultPointer);
                        MemorySegment queryInterface = functionAt(output, 24L, 0L);
                        result = Win32FfmBindings.invokeIunknownQueryInterfacePointer(
                                queryInterface,
                                output,
                                outputId,
                                resultPointer
                        );
                        if (result < 0) {
                            output6Unavailable++;
                            continue;
                        }
                        MemorySegment output6 = resultPointer.get(ValueLayout.ADDRESS, 0L);
                        requireNonNull(output6, "QueryInterface(IDXGIOutput6) returned NULL");
                        try {
                            MemorySegment description = arena.allocate(Win32Layouts.DXGI_OUTPUT_DESC1);
                            MemorySegment getDescription = functionAt(
                                    output6,
                                    Win32Layouts.IDXGI_OUTPUT6_VTABLE.byteSize(),
                                    Win32Layouts.IDXGI_OUTPUT6_VTABLE_GET_DESC1_OFFSET
                            );
                            result = Win32FfmBindings.invokeIdxgiOutput6GetDesc1Pointer(
                                    getDescription,
                                    output6,
                                    description
                            );
                            requireHresult(result, "IDXGIOutput6::GetDesc1");
                            MemorySegment monitor = description.get(
                                    ValueLayout.ADDRESS,
                                    Win32Layouts.DXGI_OUTPUT_DESC1_MONITOR_OFFSET
                            );
                            if (monitor.address() == targetMonitor.address()) {
                                boolean factoryCurrent = isFactoryCurrent(bindings, factory);
                                return decode(
                                        bindings,
                                        description,
                                        adapterIndex,
                                        outputIndex,
                                        factoryCurrent,
                                        monitor
                                );
                            }
                        } finally {
                            release(bindings, output6);
                        }
                    } finally {
                        release(bindings, output);
                    }
                }
            } finally {
                release(bindings, adapter);
            }
        }
        throw new IllegalStateException("No IDXGIOutput6 matched HMONITOR " + hexadecimal(targetMonitor.address())
                + "; visited " + outputsVisited + " outputs and " + output6Unavailable
                + " lacked IDXGIOutput6");
    }

    /// Returns whether an enumerated factory still represents current adapter state.
    ///
    /// @param bindings the generated bindings
    /// @param factory the factory interface
    /// @return whether the factory is current
    private static boolean isFactoryCurrent(Win32FfmBindings bindings, MemorySegment factory) {
        MemorySegment function = functionAt(
                factory,
                Win32Layouts.IDXGI_FACTORY1_VTABLE.byteSize(),
                Win32Layouts.IDXGI_FACTORY1_VTABLE_IS_CURRENT_OFFSET
        );
        return Win32FfmBindings.invokeIdxgiFactory1IsCurrentPointer(function, factory) != 0;
    }

    /// Decodes one target-matched `DXGI_OUTPUT_DESC1`.
    ///
    /// @param bindings the generated DXGI and DisplayConfig bindings
    /// @param description the native structure
    /// @param adapterIndex the adapter index
    /// @param outputIndex the output index
    /// @param factoryCurrent whether the factory remained current
    /// @param monitor the exact monitor handle
    /// @return the immutable capability snapshot
    private static DxgiOutputSnapshot decode(
            Win32FfmBindings bindings,
            MemorySegment description,
            int adapterIndex,
            int outputIndex,
            boolean factoryCurrent,
            MemorySegment monitor
    ) {
        int colorSpace = description.get(ValueLayout.JAVA_INT, Win32Layouts.DXGI_OUTPUT_DESC1_COLOR_SPACE_OFFSET);
        String deviceName = readWideString(
                description,
                Win32Layouts.DXGI_OUTPUT_DESC1_DEVICE_NAME_OFFSET,
                32
        );
        DisplayConfigAdvancedColor advancedColor = DisplayConfigQuery.query(bindings, deviceName);
        return new DxgiOutputSnapshot(
                adapterIndex,
                outputIndex,
                factoryCurrent,
                deviceName,
                hexadecimal(monitor.address()),
                description.get(ValueLayout.JAVA_INT, Win32Layouts.DXGI_OUTPUT_DESC1_DESKTOP_COORDINATES_OFFSET),
                description.get(ValueLayout.JAVA_INT, Win32Layouts.DXGI_OUTPUT_DESC1_DESKTOP_COORDINATES_OFFSET + 4L),
                description.get(ValueLayout.JAVA_INT, Win32Layouts.DXGI_OUTPUT_DESC1_DESKTOP_COORDINATES_OFFSET + 8L),
                description.get(ValueLayout.JAVA_INT, Win32Layouts.DXGI_OUTPUT_DESC1_DESKTOP_COORDINATES_OFFSET + 12L),
                description.get(ValueLayout.JAVA_INT, Win32Layouts.DXGI_OUTPUT_DESC1_ATTACHED_TO_DESKTOP_OFFSET) != 0,
                description.get(ValueLayout.JAVA_INT, Win32Layouts.DXGI_OUTPUT_DESC1_ROTATION_OFFSET),
                description.get(ValueLayout.JAVA_INT, Win32Layouts.DXGI_OUTPUT_DESC1_BITS_PER_COLOR_OFFSET),
                colorSpace,
                colorSpaceName(colorSpace),
                effectivePresentationMode(colorSpace),
                advancedColor,
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_RED_PRIMARY_OFFSET),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_RED_PRIMARY_OFFSET + 4L),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_GREEN_PRIMARY_OFFSET),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_GREEN_PRIMARY_OFFSET + 4L),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_BLUE_PRIMARY_OFFSET),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_BLUE_PRIMARY_OFFSET + 4L),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_WHITE_POINT_OFFSET),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_WHITE_POINT_OFFSET + 4L),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_MINIMUM_LUMINANCE_OFFSET),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_MAXIMUM_LUMINANCE_OFFSET),
                description.get(ValueLayout.JAVA_FLOAT, Win32Layouts.DXGI_OUTPUT_DESC1_MAXIMUM_FULL_FRAME_LUMINANCE_OFFSET)
        );
    }

    /// Returns a COM vtable function address using schema-generated byte offsets.
    ///
    /// @param object the non-null COM interface pointer
    /// @param tableSize the verified size needed for the selected slot
    /// @param byteOffset the generated vtable member offset
    /// @return the non-null native function address
    private static MemorySegment functionAt(MemorySegment object, long tableSize, long byteOffset) {
        requireNonNull(object, "COM interface pointer is NULL");
        MemorySegment objectView = object.reinterpret(Win32Layouts.COM_INTERFACE_OBJECT.byteSize());
        MemorySegment table = objectView.get(
                ValueLayout.ADDRESS,
                Win32Layouts.COM_INTERFACE_OBJECT_VTABLE_OFFSET
        );
        requireNonNull(table, "COM vtable pointer is NULL");
        MemorySegment function = table.reinterpret(tableSize).get(ValueLayout.ADDRESS, byteOffset);
        requireNonNull(function, "COM vtable slot at byte offset " + byteOffset + " is NULL");
        return function;
    }

    /// Releases one owned COM interface exactly once.
    ///
    /// @param bindings the generated function-pointer bindings
    /// @param object the owned interface pointer
    private static void release(Win32FfmBindings bindings, MemorySegment object) {
        if (isNull(object)) {
            return;
        }
        MemorySegment function = functionAt(object, 24L, 16L);
        Win32FfmBindings.invokeIunknownReleasePointer(function, object);
    }

    /// Writes `IID_IDXGIFactory1` in native GUID layout.
    ///
    /// @param guid the sixteen-byte destination
    private static void writeFactory1Guid(MemorySegment guid) {
        writeGuidHead(guid, 0x770aae78, (short) 0xf26f, (short) 0x4dba);
        guid.set(ValueLayout.JAVA_BYTE, 8L, (byte) 0xa8);
        guid.set(ValueLayout.JAVA_BYTE, 9L, (byte) 0x29);
        guid.set(ValueLayout.JAVA_BYTE, 10L, (byte) 0x25);
        guid.set(ValueLayout.JAVA_BYTE, 11L, (byte) 0x3c);
        guid.set(ValueLayout.JAVA_BYTE, 12L, (byte) 0x83);
        guid.set(ValueLayout.JAVA_BYTE, 13L, (byte) 0xd1);
        guid.set(ValueLayout.JAVA_BYTE, 14L, (byte) 0xb3);
        guid.set(ValueLayout.JAVA_BYTE, 15L, (byte) 0x87);
    }

    /// Writes `IID_IDXGIOutput6` in native GUID layout.
    ///
    /// @param guid the sixteen-byte destination
    private static void writeOutput6Guid(MemorySegment guid) {
        writeGuidHead(guid, 0x068346e8, (short) 0xaaec, (short) 0x4b84);
        guid.set(ValueLayout.JAVA_BYTE, 8L, (byte) 0xad);
        guid.set(ValueLayout.JAVA_BYTE, 9L, (byte) 0xd7);
        guid.set(ValueLayout.JAVA_BYTE, 10L, (byte) 0x13);
        guid.set(ValueLayout.JAVA_BYTE, 11L, (byte) 0x7f);
        guid.set(ValueLayout.JAVA_BYTE, 12L, (byte) 0x51);
        guid.set(ValueLayout.JAVA_BYTE, 13L, (byte) 0x3f);
        guid.set(ValueLayout.JAVA_BYTE, 14L, (byte) 0x77);
        guid.set(ValueLayout.JAVA_BYTE, 15L, (byte) 0xa1);
    }

    /// Writes the integer fields shared by every native GUID.
    ///
    /// @param guid the sixteen-byte destination
    /// @param data1 the first GUID field
    /// @param data2 the second GUID field
    /// @param data3 the third GUID field
    private static void writeGuidHead(MemorySegment guid, int data1, short data2, short data3) {
        guid.fill((byte) 0);
        guid.set(ValueLayout.JAVA_INT, Win32Layouts.GUID_DATA1_OFFSET, data1);
        guid.set(ValueLayout.JAVA_SHORT, Win32Layouts.GUID_DATA2_OFFSET, data2);
        guid.set(ValueLayout.JAVA_SHORT, Win32Layouts.GUID_DATA3_OFFSET, data3);
    }

    /// Reads a bounded UTF-16 native array without scanning beyond its declared capacity.
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

    /// Returns the symbolic SDK name for one `DXGI_COLOR_SPACE_TYPE` value.
    ///
    /// @param value the raw enumeration value
    /// @return the stable symbolic name or an explicit unknown spelling
    private static String colorSpaceName(int value) {
        return switch (value) {
            case 0 -> "RGB_FULL_G22_NONE_P709";
            case 1 -> "RGB_FULL_G10_NONE_P709";
            case 2 -> "RGB_STUDIO_G22_NONE_P709";
            case 3 -> "RGB_STUDIO_G22_NONE_P2020";
            case 4 -> "RESERVED";
            case 5 -> "YCBCR_FULL_G22_NONE_P709_X601";
            case 6 -> "YCBCR_STUDIO_G22_LEFT_P601";
            case 7 -> "YCBCR_FULL_G22_LEFT_P601";
            case 8 -> "YCBCR_STUDIO_G22_LEFT_P709";
            case 9 -> "YCBCR_FULL_G22_LEFT_P709";
            case 10 -> "YCBCR_STUDIO_G22_LEFT_P2020";
            case 11 -> "YCBCR_FULL_G22_LEFT_P2020";
            case 12 -> "RGB_FULL_G2084_NONE_P2020";
            case 13 -> "YCBCR_STUDIO_G2084_LEFT_P2020";
            case 14 -> "RGB_STUDIO_G2084_NONE_P2020";
            case 15 -> "YCBCR_STUDIO_G22_TOPLEFT_P2020";
            case 16 -> "YCBCR_STUDIO_G2084_TOPLEFT_P2020";
            case 17 -> "RGB_FULL_G22_NONE_P2020";
            case 18 -> "YCBCR_STUDIO_GHLG_TOPLEFT_P2020";
            case 19 -> "YCBCR_FULL_GHLG_TOPLEFT_P2020";
            case 20 -> "RGB_STUDIO_G24_NONE_P709";
            case 21 -> "RGB_STUDIO_G24_NONE_P2020";
            case 22 -> "YCBCR_STUDIO_G24_LEFT_P709";
            case 23 -> "YCBCR_STUDIO_G24_LEFT_P2020";
            case 24 -> "YCBCR_STUDIO_G24_TOPLEFT_P2020";
            case -1 -> "CUSTOM";
            default -> "UNKNOWN_" + Integer.toUnsignedString(value);
        };
    }

    /// Classifies the current output encoding without treating it as a permanent capability promise.
    ///
    /// @param colorSpace the current `DXGI_COLOR_SPACE_TYPE`
    /// @return the current presentation-mode label
    private static String effectivePresentationMode(int colorSpace) {
        return switch (colorSpace) {
            case 1 -> "scRGB-linear";
            case 12, 13, 14, 16 -> "HDR10-PQ";
            case 18, 19 -> "HLG";
            default -> "SDR-or-unspecified";
        };
    }

    /// Clears one pointer-sized out parameter before reuse.
    ///
    /// @param pointer the pointer-sized storage
    private static void clearPointer(MemorySegment pointer) {
        pointer.set(ValueLayout.ADDRESS, 0L, MemorySegment.NULL);
    }

    /// Requires a successful COM `HRESULT`.
    ///
    /// @param result the raw result
    /// @param operation the operation name
    private static void requireHresult(int result, String operation) {
        if (result < 0) {
            throw new IllegalStateException(operation + " failed with HRESULT "
                    + String.format(Locale.ROOT, "0x%08x", result));
        }
    }

    /// Requires a non-null native address.
    ///
    /// @param segment the native address
    /// @param message the failure message
    private static void requireNonNull(MemorySegment segment, String message) {
        if (isNull(segment)) {
            throw new IllegalStateException(message);
        }
    }

    /// Returns whether a native address is the null pointer.
    ///
    /// @param segment the address segment
    /// @return whether its address is zero
    private static boolean isNull(MemorySegment segment) {
        return segment.address() == 0L;
    }

    /// Formats one native address as fixed-width unsigned hexadecimal.
    ///
    /// @param address the raw address
    /// @return the hexadecimal spelling
    private static String hexadecimal(long address) {
        return String.format(Locale.ROOT, "0x%016x", address);
    }
}
