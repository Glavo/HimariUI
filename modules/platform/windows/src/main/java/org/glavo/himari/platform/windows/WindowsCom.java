package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.UUID;

/// Encodes and compares Windows GUID values for production COM objects.
@SuppressWarnings("restricted")
@NotNullByDefault
final class WindowsCom {
    /// Prevents instantiation.
    private WindowsCom() {
    }

    /// Writes one UUID into a native GUID record.
    ///
    /// @param arena the destination arena
    /// @param spelling the canonical UUID spelling
    /// @return the 16-byte GUID
    static MemorySegment guid(Arena arena, String spelling) {
        UUID value = UUID.fromString(spelling);
        long most = value.getMostSignificantBits();
        long least = value.getLeastSignificantBits();
        MemorySegment result = arena.allocate(Win32Layouts.GUID);
        result.set(ValueLayout.JAVA_INT, Win32Layouts.GUID_DATA1_OFFSET, (int) (most >>> 32));
        result.set(ValueLayout.JAVA_SHORT, Win32Layouts.GUID_DATA2_OFFSET, (short) (most >>> 16));
        result.set(ValueLayout.JAVA_SHORT, Win32Layouts.GUID_DATA3_OFFSET, (short) most);
        for (int index = 0; index < 8; index++) {
            result.set(
                    ValueLayout.JAVA_BYTE,
                    Win32Layouts.GUID_DATA4_0_OFFSET + index,
                    (byte) (least >>> (56 - index * 8))
            );
        }
        return result;
    }

    /// Returns whether a native GUID matches a Java UUID.
    ///
    /// @param interfaceId the native GUID pointer
    /// @param expected the expected identity
    /// @return whether the 16 bytes match
    static boolean matches(MemorySegment interfaceId, UUID expected) {
        if (interfaceId.address() == 0L) {
            return false;
        }
        MemorySegment guid = interfaceId.reinterpret(Win32Layouts.GUID.byteSize());
        int data1 = guid.get(ValueLayout.JAVA_INT, Win32Layouts.GUID_DATA1_OFFSET);
        short data2 = guid.get(ValueLayout.JAVA_SHORT, Win32Layouts.GUID_DATA2_OFFSET);
        short data3 = guid.get(ValueLayout.JAVA_SHORT, Win32Layouts.GUID_DATA3_OFFSET);
        long most = expected.getMostSignificantBits();
        if (data1 != (int) (most >>> 32) || data2 != (short) (most >>> 16) || data3 != (short) most) {
            return false;
        }
        long least = expected.getLeastSignificantBits();
        for (int index = 0; index < 8; index++) {
            byte actual = guid.get(ValueLayout.JAVA_BYTE, Win32Layouts.GUID_DATA4_0_OFFSET + index);
            byte wanted = (byte) (least >>> (56 - index * 8));
            if (actual != wanted) {
                return false;
            }
        }
        return true;
    }
}
