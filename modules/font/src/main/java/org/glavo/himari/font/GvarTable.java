package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Parses a first-stable `gvar` subset and applies tuple deltas to simple-glyph points.
///
/// Supported glyph records use one tuple with an embedded peak, no private point numbers, and
/// packed 8/16-bit or zero-run deltas. Composite glyphs and shared point numbers are ignored.
/// A missing table applies no deltas.
@NotNullByDefault
final class GvarTable {
    /// Shared empty table.
    static final GvarTable EMPTY = new GvarTable(0, new GlyphVar[0]);

    /// `EMBEDDED_PEAK_TUPLE`.
    private static final int EMBEDDED_PEAK = 0x8000;

    /// Axis count declared by the table.
    private final int axisCount;

    /// Per-glyph variation, or `null` when the glyph has no data.
    private final @Nullable GlyphVar[] glyphs;

    /// Creates a parsed table.
    private GvarTable(int axisCount, @Nullable GlyphVar[] glyphs) {
        this.axisCount = axisCount;
        this.glyphs = glyphs;
    }

    /// Parses an optional `gvar` table.
    ///
    /// @param table the table, or `null`
    /// @param glyphCount the face glyph count
    /// @return the parsed table
    static GvarTable parse(@Nullable ByteBuffer table, int glyphCount) {
        if (table == null || glyphCount < 0) {
            return EMPTY;
        }
        ByteBuffer buffer = table.duplicate().order(ByteOrder.BIG_ENDIAN);
        buffer.clear();
        if (buffer.remaining() < 20) {
            return EMPTY;
        }
        int major = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        int axisCount = Short.toUnsignedInt(buffer.getShort());
        buffer.getShort();
        buffer.getInt();
        int declared = Short.toUnsignedInt(buffer.getShort());
        int flags = Short.toUnsignedInt(buffer.getShort());
        if (buffer.remaining() < 4) {
            return EMPTY;
        }
        int dataArrayOffset = buffer.getInt();
        if (major != 1 || axisCount < 1 || declared != glyphCount || dataArrayOffset < 0) {
            return EMPTY;
        }
        boolean longOffsets = (flags & 1) != 0;
        int offsetBytes = longOffsets ? 4 : 2;
        int offsetCount = glyphCount + 1;
        if (buffer.remaining() < offsetCount * offsetBytes) {
            throw new IllegalArgumentException("gvar offset array is truncated");
        }
        int[] offsets = new int[offsetCount];
        for (int index = 0; index < offsetCount; index++) {
            offsets[index] = longOffsets ? buffer.getInt() : Short.toUnsignedInt(buffer.getShort()) * 2;
        }
        @Nullable GlyphVar[] glyphs = new GlyphVar[glyphCount];
        for (int glyph = 0; glyph < glyphCount; glyph++) {
            int start = dataArrayOffset + offsets[glyph];
            int end = dataArrayOffset + offsets[glyph + 1];
            if (end <= start) {
                continue;
            }
            if (start < 0 || end > buffer.capacity()) {
                throw new IllegalArgumentException("gvar glyph range is out of bounds");
            }
            ByteBuffer slice = buffer.duplicate().order(ByteOrder.BIG_ENDIAN);
            slice.clear();
            slice.position(start);
            slice.limit(end);
            glyphs[glyph] = readGlyph(slice, axisCount);
        }
        return new GvarTable(axisCount, glyphs);
    }

    /// Adds scaled contour deltas to `xs`/`ys` for `glyphId`.
    ///
    /// The packed stream is `xs.length + 4` points: the contour, then the four phantom points
    /// (left side bearing, advance width, top, bottom). Phantom slots are consumed so the Y
    /// stream stays aligned; they are not written into `xs`/`ys`. `normalized` is one coordinate
    /// per axis in `[-1, 1]` relative to the default instance.
    ///
    /// @param glyphId the glyph
    /// @param xs contour x coordinates
    /// @param ys contour y coordinates
    /// @param normalized normalized axis coordinates
    void apply(int glyphId, float[] xs, float[] ys, float[] normalized) {
        @Nullable Unpacked unpacked = unpackDeltas(glyphId, xs.length, normalized);
        if (unpacked == null) {
            return;
        }
        int[] dx = unpacked.dx;
        int[] dy = unpacked.dy;
        float scalar = unpacked.scalar;
        int limit = xs.length;
        for (int index = 0; index < limit; index++) {
            xs[index] += scalar * dx[index];
            ys[index] += scalar * dy[index];
        }
    }

    /// Returns the scaled advance-width change from the first two phantom X deltas.
    ///
    /// The value is `round(scalar * (phantom1.x - phantom0.x))`. Missing data returns `0`.
    ///
    /// @param glyphId the glyph
    /// @param pointCount the simple-glyph contour count
    /// @param normalized normalized axis coordinates
    /// @return the signed advance delta in font units
    int advanceDelta(int glyphId, int pointCount, float[] normalized) {
        @Nullable Unpacked unpacked = unpackDeltas(glyphId, pointCount, normalized);
        if (unpacked == null) {
            return 0;
        }
        int left = pointCount;
        int right = pointCount + 1;
        return Math.round(unpacked.scalar * (unpacked.dx[right] - unpacked.dx[left]));
    }

    /// Returns the scaled left-side-bearing change from phantom 0.
    ///
    /// @param glyphId the glyph
    /// @param pointCount the simple-glyph contour count
    /// @param normalized normalized axis coordinates
    /// @return the signed LSB delta in font units
    int leftSideBearingDelta(int glyphId, int pointCount, float[] normalized) {
        @Nullable Unpacked unpacked = unpackDeltas(glyphId, pointCount, normalized);
        if (unpacked == null) {
            return 0;
        }
        return Math.round(unpacked.scalar * unpacked.dx[pointCount]);
    }

    /// Unpacks packed X then Y deltas for `pointCount + 4` slots, or `null` when unused.
    private @Nullable Unpacked unpackDeltas(int glyphId, int pointCount, float[] normalized) {
        if (glyphId < 0 || glyphId >= glyphs.length || pointCount < 0) {
            return null;
        }
        @Nullable GlyphVar var = glyphs[glyphId];
        if (var == null) {
            return null;
        }
        float factor = scalar(normalized, var.peak);
        if (factor == 0.0f) {
            return null;
        }
        int count = pointCount + 4;
        int[] dx = new int[count];
        int[] dy = new int[count];
        ByteBuffer packed = ByteBuffer.wrap(var.packed).order(ByteOrder.BIG_ENDIAN);
        unpack(packed, dx);
        unpack(packed, dy);
        return new Unpacked(factor, dx, dy);
    }

    /// Reads one embedded-peak tuple with shared (all-points) packed deltas.
    private static @Nullable GlyphVar readGlyph(ByteBuffer buffer, int axisCount) {
        if (buffer.remaining() < 8) {
            return null;
        }
        int glyphStart = buffer.position();
        int tupleCount = Short.toUnsignedInt(buffer.getShort()) & 0x0FFF;
        int dataOffset = Short.toUnsignedInt(buffer.getShort());
        if (tupleCount < 1) {
            return null;
        }
        int variationDataSize = Short.toUnsignedInt(buffer.getShort());
        int tupleIndex = Short.toUnsignedInt(buffer.getShort());
        if ((tupleIndex & EMBEDDED_PEAK) == 0
                || (tupleIndex & 0x4000) != 0
                || (tupleIndex & 0x2000) != 0
                || buffer.remaining() < axisCount * 2) {
            return null;
        }
        float[] peak = new float[axisCount];
        for (int axis = 0; axis < axisCount; axis++) {
            peak[axis] = f2dot14(buffer.getShort());
        }
        int packedStart = glyphStart + dataOffset;
        if (packedStart < 0 || packedStart > buffer.limit() || variationDataSize < 0) {
            return null;
        }
        int packedEnd = Math.min(buffer.limit(), packedStart + variationDataSize);
        byte[] packed = new byte[packedEnd - packedStart];
        buffer.position(packedStart);
        buffer.get(packed);
        return new GlyphVar(peak, packed);
    }

    /// Computes the tuple scalar.
    static float scalar(float[] normalized, float[] peak) {
        if (normalized.length == 0 || peak.length == 0) {
            return 0.0f;
        }
        int axes = Math.min(normalized.length, peak.length);
        float factor = 1.0f;
        for (int index = 0; index < axes; index++) {
            float peakValue = peak[index];
            if (peakValue == 0.0f) {
                continue;
            }
            float coord = normalized[index];
            if (coord == 0.0f || coord * peakValue < 0.0f) {
                return 0.0f;
            }
            float ratio = coord / peakValue;
            if (ratio > 1.0f) {
                ratio = 1.0f;
            }
            factor *= ratio;
        }
        return factor;
    }

    /// Unpacks one packed-delta stream into `dest`.
    static void unpack(ByteBuffer buffer, int[] dest) {
        int written = 0;
        while (written < dest.length && buffer.hasRemaining()) {
            int control = buffer.get() & 0xFF;
            int run = (control & 0x3F) + 1;
            if ((control & 0x80) != 0) {
                written += run;
                continue;
            }
            boolean wide = (control & 0x40) != 0;
            for (int index = 0; index < run && written < dest.length; index++) {
                if (wide) {
                    if (buffer.remaining() < 2) {
                        throw new IllegalArgumentException("gvar 16-bit delta is truncated");
                    }
                    dest[written++] = buffer.getShort();
                } else {
                    if (!buffer.hasRemaining()) {
                        throw new IllegalArgumentException("gvar 8-bit delta is truncated");
                    }
                    dest[written++] = buffer.get();
                }
            }
        }
    }

    /// Converts an F2DOT14 value to float.
    private static float f2dot14(short value) {
        return value / 16384.0f;
    }

    /// One glyph's peak tuple and packed deltas.
    ///
    /// @param peak embedded peak coordinates
    /// @param packed concatenated packed X then Y streams
    private record GlyphVar(float[] peak, byte[] packed) {
    }

    /// Unpacked contour-plus-phantom deltas and the active scalar.
    ///
    /// @param scalar the tuple scalar
    /// @param dx X deltas including four phantoms
    /// @param dy Y deltas including four phantoms
    private record Unpacked(float scalar, int[] dx, int[] dy) {
    }
}
