package org.glavo.himari.platform.windows;

import org.glavo.himari.platform.windows.generated.Win32FfmBindings;
import org.glavo.himari.platform.windows.generated.Win32Layouts;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/// Blits unassociated 8-bit sRGB RGBA pixels through GDI as a top-down 32bpp BGRA DIB.
@SuppressWarnings("restricted")
@NotNullByDefault
final class WindowsSoftwarePresent {
    /// `BI_RGB` uncompressed DIB compression.
    static final int BI_RGB = 0;

    /// `DIB_RGB_COLORS` color-table interpretation.
    static final int DIB_RGB_COLORS = 0;

    /// `BITMAPINFOHEADER.biSize` in bytes.
    static final int BITMAPINFOHEADER_SIZE = 40;

    /// Prevents instantiation.
    private WindowsSoftwarePresent() {
    }

    /// Converts row-major RGBA into top-down BGRA and presents through `SetDIBitsToDevice`.
    ///
    /// @param bindings the generated Win32 bindings
    /// @param deviceContext the destination `HDC`
    /// @param rgba unassociated 8-bit sRGB pixels in row-major RGBA order
    /// @param width the pixel width
    /// @param height the pixel height
    /// @return the scanline count reported by `SetDIBitsToDevice`
    static int presentSdrRgba(
            Win32FfmBindings bindings,
            MemorySegment deviceContext,
            MemorySegment rgba,
            int width,
            int height
    ) {
        Objects.requireNonNull(bindings, "bindings");
        Objects.requireNonNull(deviceContext, "deviceContext");
        Objects.requireNonNull(rgba, "rgba");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Present dimensions must be positive");
        }
        if (deviceContext.address() == 0L) {
            throw new IllegalArgumentException("Device context must not be NULL");
        }
        int expected = Math.multiplyExact(Math.multiplyExact(width, height), 4);
        if (rgba.byteSize() != expected) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        MemorySegment bgra = toTopDownBgra(rgba);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment header = arena.allocate(Win32Layouts.BITMAPINFOHEADER);
            header.fill((byte) 0);
            header.set(ValueLayout.JAVA_INT, Win32Layouts.BITMAPINFOHEADER_BI_SIZE_OFFSET, BITMAPINFOHEADER_SIZE);
            header.set(ValueLayout.JAVA_INT, Win32Layouts.BITMAPINFOHEADER_BI_WIDTH_OFFSET, width);
            header.set(ValueLayout.JAVA_INT, Win32Layouts.BITMAPINFOHEADER_BI_HEIGHT_OFFSET, -height);
            header.set(ValueLayout.JAVA_SHORT, Win32Layouts.BITMAPINFOHEADER_BI_PLANES_OFFSET, (short) 1);
            header.set(ValueLayout.JAVA_SHORT, Win32Layouts.BITMAPINFOHEADER_BI_BIT_COUNT_OFFSET, (short) 32);
            header.set(ValueLayout.JAVA_INT, Win32Layouts.BITMAPINFOHEADER_BI_COMPRESSION_OFFSET, BI_RGB);
            header.set(ValueLayout.JAVA_INT, Win32Layouts.BITMAPINFOHEADER_BI_SIZE_IMAGE_OFFSET, Math.toIntExact(bgra.byteSize()));
            MemorySegment bits = arena.allocate(bgra.byteSize());
            bits.copyFrom(bgra);
            Win32FfmBindings.SetDiBitsToDeviceResult result = bindings.setDiBitsToDevice(
                    deviceContext,
                    0,
                    0,
                    width,
                    height,
                    0,
                    0,
                    0,
                    height,
                    bits,
                    header,
                    DIB_RGB_COLORS
            );
            if (result.value() == 0) {
                throw new IllegalStateException("SetDIBitsToDevice failed: " + result.errorCode());
            }
            return result.value();
        }
    }

    /// Converts row-major RGBA into row-major BGRA without changing image orientation.
    ///
    /// @param rgba the source pixels
    /// @return a new BGRA buffer of the same length
    static MemorySegment toTopDownBgra(MemorySegment rgba) {
        byte[] source = rgba.toArray(ValueLayout.JAVA_BYTE);
        byte[] bgra = new byte[source.length];
        for (int pixel = 0; pixel < source.length; pixel += 4) {
            bgra[pixel] = source[pixel + 2];
            bgra[pixel + 1] = source[pixel + 1];
            bgra[pixel + 2] = source[pixel];
            bgra[pixel + 3] = source[pixel + 3];
        }
        return MemorySegment.ofArray(bgra);
    }
}
