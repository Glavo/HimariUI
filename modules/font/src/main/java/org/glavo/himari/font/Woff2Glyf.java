package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// Reconstructs and emits the WOFF2 version-0 `glyf`/`loca` transform and the version-1 `hmtx`
/// transform.
///
/// The streams follow W3C WOFF2 §5.1–5.4. Reconstructed glyph records are semantically equivalent
/// to the input outlines; they are not required to be a bitwise match.
@NotNullByDefault
final class Woff2Glyf {
    /// Size of the transformed `glyf` header.
    private static final int HEADER_SIZE = 36;

    /// Composite flag: 16-bit arguments.
    private static final int ARG_WORDS = 0x0001;

    /// Composite flag: additional components follow.
    private static final int MORE_COMPONENTS = 0x0020;

    /// Composite flag: we have a uniform scale.
    private static final int HAVE_SCALE = 0x0008;

    /// Composite flag: we have separate x/y scales.
    private static final int HAVE_XY_SCALE = 0x0040;

    /// Composite flag: we have a 2x2 transform.
    private static final int HAVE_2X2 = 0x0080;

    /// Composite flag: instructions follow the last component.
    private static final int HAVE_INSTRUCTIONS = 0x0100;

    /// Simple-glyf flag: on-curve point.
    private static final int ON_CURVE = 0x01;

    /// Simple-glyf flag: x is a uint8.
    private static final int X_SHORT = 0x02;

    /// Simple-glyf flag: y is a uint8.
    private static final int Y_SHORT = 0x04;

    /// Simple-glyf flag: the next byte is a repeat count.
    private static final int REPEAT = 0x08;

    /// Simple-glyf flag: x-same or positive-x-short.
    private static final int X_SAME = 0x10;

    /// Simple-glyf flag: y-same or positive-y-short.
    private static final int Y_SAME = 0x20;

    /// Prevents instantiation.
    private Woff2Glyf() {
    }

    /// Reconstructed `glyf` and `loca` tables.
    ///
    /// @param glyf the reconstructed `glyf` bytes
    /// @param loca the reconstructed `loca` bytes
    record Tables(byte[] glyf, byte[] loca) {
    }

    /// Reconstructs `glyf` and `loca` from a version-0 transformed `glyf` payload.
    ///
    /// Transformed `loca` consumes no compressed bytes; `origLocaLength` is the reconstructed
    /// `loca` size and must match `indexFormat`.
    ///
    /// @param transformed the transformed `glyf` bytes
    /// @param origLocaLength the `loca` `origLength` from the WOFF2 directory
    /// @return the reconstructed tables
    static Tables reconstruct(byte[] transformed, int origLocaLength) {
        if (transformed.length < HEADER_SIZE) {
            throw new IllegalArgumentException("WOFF2 transformed glyf header is truncated");
        }
        ByteBuffer header = ByteBuffer.wrap(transformed).order(ByteOrder.BIG_ENDIAN);
        header.getShort();
        int optionFlags = Short.toUnsignedInt(header.getShort());
        int numGlyphs = Short.toUnsignedInt(header.getShort());
        int indexFormat = Short.toUnsignedInt(header.getShort());
        int nContourSize = header.getInt();
        int nPointsSize = header.getInt();
        int flagSize = header.getInt();
        int glyphSize = header.getInt();
        int compositeSize = header.getInt();
        int bboxSize = header.getInt();
        int instructionSize = header.getInt();
        int offset = HEADER_SIZE;
        byte[] nContour = slice(transformed, offset, nContourSize);
        offset += nContourSize;
        byte[] nPoints = slice(transformed, offset, nPointsSize);
        offset += nPointsSize;
        byte[] flags = slice(transformed, offset, flagSize);
        offset += flagSize;
        byte[] glyphStream = slice(transformed, offset, glyphSize);
        offset += glyphSize;
        byte[] composite = slice(transformed, offset, compositeSize);
        offset += compositeSize;
        byte[] bbox = slice(transformed, offset, bboxSize);
        offset += bboxSize;
        byte[] instructions = slice(transformed, offset, instructionSize);
        offset += instructionSize;
        int overlapSize = 0;
        if ((optionFlags & 1) != 0) {
            overlapSize = (numGlyphs + 7) >> 3;
            if (offset + overlapSize > transformed.length) {
                throw new IllegalArgumentException("WOFF2 overlapSimpleBitmap is truncated");
            }
            offset += overlapSize;
        }
        if (offset != transformed.length) {
            throw new IllegalArgumentException("WOFF2 transformed glyf size does not match the streams");
        }
        int bboxBitmapSize = ((numGlyphs + 31) >> 5) << 2;
        if (bbox.length < bboxBitmapSize) {
            throw new IllegalArgumentException("WOFF2 bboxStream is shorter than bboxBitmap");
        }
        byte[] bboxBitmap = new byte[bboxBitmapSize];
        System.arraycopy(bbox, 0, bboxBitmap, 0, bboxBitmapSize);
        ByteBuffer nContourBuf = ByteBuffer.wrap(nContour).order(ByteOrder.BIG_ENDIAN);
        ByteBuffer nPointsBuf = ByteBuffer.wrap(nPoints).order(ByteOrder.BIG_ENDIAN);
        ByteBuffer flagBuf = ByteBuffer.wrap(flags);
        ByteBuffer glyphBuf = ByteBuffer.wrap(glyphStream).order(ByteOrder.BIG_ENDIAN);
        ByteBuffer compositeBuf = ByteBuffer.wrap(composite).order(ByteOrder.BIG_ENDIAN);
        ByteBuffer bboxBuf = ByteBuffer.wrap(bbox, bboxBitmapSize, bbox.length - bboxBitmapSize)
                .order(ByteOrder.BIG_ENDIAN);
        ByteBuffer instructionBuf = ByteBuffer.wrap(instructions);
        byte @Nullable [] overlap = overlapSize == 0
                ? null
                : java.util.Arrays.copyOfRange(transformed, HEADER_SIZE
                        + nContourSize + nPointsSize + flagSize + glyphSize + compositeSize + bboxSize
                        + instructionSize,
                        transformed.length);
        if (nContourBuf.remaining() != numGlyphs * 2) {
            throw new IllegalArgumentException("WOFF2 nContourStream size does not match numGlyphs");
        }
        int expectedLoca = (numGlyphs + 1) * (indexFormat == 0 ? 2 : 4);
        if (origLocaLength != expectedLoca) {
            throw new IllegalArgumentException("WOFF2 loca origLength does not match indexFormat");
        }
        ByteArrayOutputStream glyfOut = new ByteArrayOutputStream();
        int[] loca = new int[numGlyphs + 1];
        for (int glyphId = 0; glyphId < numGlyphs; glyphId++) {
            loca[glyphId] = glyfOut.size();
            short contours = nContourBuf.getShort();
            if (contours == 0) {
                if (bboxBit(bboxBitmap, glyphId)) {
                    throw new IllegalArgumentException("WOFF2 empty glyph must not carry a bbox");
                }
                continue;
            }
            if (contours == -1) {
                writeComposite(glyfOut, compositeBuf, glyphBuf, instructionBuf, bboxBuf, bboxBitmap, glyphId);
            } else if (contours > 0) {
                writeSimple(
                        glyfOut,
                        contours,
                        nPointsBuf,
                        flagBuf,
                        glyphBuf,
                        instructionBuf,
                        bboxBuf,
                        bboxBitmap,
                        overlap,
                        glyphId
                );
            } else {
                throw new IllegalArgumentException("WOFF2 nContour value is invalid");
            }
            padEven(glyfOut);
        }
        loca[numGlyphs] = glyfOut.size();
        return new Tables(glyfOut.toByteArray(), writeLoca(loca, indexFormat));
    }

    /// Encodes raw `glyf`/`loca` as a version-0 transformed `glyf` payload.
    ///
    /// @param glyf the raw `glyf` table
    /// @param loca the raw `loca` table
    /// @param numGlyphs the `maxp` glyph count
    /// @param indexFormat `0` for short `loca`, `1` for long `loca`
    /// @return the transformed payload
    static byte[] transform(byte[] glyf, byte[] loca, int numGlyphs, int indexFormat) {
        int[] offsets = readLoca(loca, numGlyphs, indexFormat);
        ByteArrayOutputStream nContour = new ByteArrayOutputStream();
        ByteArrayOutputStream nPoints = new ByteArrayOutputStream();
        ByteArrayOutputStream flags = new ByteArrayOutputStream();
        ByteArrayOutputStream glyphStream = new ByteArrayOutputStream();
        ByteArrayOutputStream composite = new ByteArrayOutputStream();
        ByteArrayOutputStream bboxValues = new ByteArrayOutputStream();
        ByteArrayOutputStream instructions = new ByteArrayOutputStream();
        int bboxBitmapSize = ((numGlyphs + 31) >> 5) << 2;
        byte[] bboxBitmap = new byte[bboxBitmapSize];
        byte[] overlapBitmap = new byte[(numGlyphs + 7) >> 3];
        boolean anyOverlap = false;
        for (int glyphId = 0; glyphId < numGlyphs; glyphId++) {
            int start = offsets[glyphId];
            int end = offsets[glyphId + 1];
            if (start == end || end - start < 2) {
                writeI16(nContour, 0);
                continue;
            }
            ByteBuffer glyph = ByteBuffer.wrap(glyf, start, end - start).order(ByteOrder.BIG_ENDIAN);
            short contours = glyph.getShort();
            writeI16(nContour, contours);
            if (contours == 0) {
                continue;
            }
            int xMin = glyph.getShort();
            int yMin = glyph.getShort();
            int xMax = glyph.getShort();
            int yMax = glyph.getShort();
            if (contours == -1) {
                setBit(bboxBitmap, glyphId);
                writeI16(bboxValues, xMin);
                writeI16(bboxValues, yMin);
                writeI16(bboxValues, xMax);
                writeI16(bboxValues, yMax);
                encodeComposite(glyph, composite, glyphStream, instructions);
            } else if (contours > 0) {
                SimpleOutline outline = parseSimple(glyph, contours);
                for (int count : outline.pointsPerContour) {
                    write255(nPoints, count);
                }
                encodeTriplets(outline, flags, glyphStream);
                write255(glyphStream, outline.instructions.length);
                instructions.writeBytes(outline.instructions);
                if (outline.overlap) {
                    setBit(overlapBitmap, glyphId);
                    anyOverlap = true;
                }
                if (xMin != outline.computedMinX
                        || yMin != outline.computedMinY
                        || xMax != outline.computedMaxX
                        || yMax != outline.computedMaxY) {
                    setBit(bboxBitmap, glyphId);
                    writeI16(bboxValues, xMin);
                    writeI16(bboxValues, yMin);
                    writeI16(bboxValues, xMax);
                    writeI16(bboxValues, yMax);
                }
            } else {
                throw new IllegalArgumentException("glyf contour count is invalid");
            }
        }
        ByteArrayOutputStream bboxStream = new ByteArrayOutputStream();
        bboxStream.writeBytes(bboxBitmap);
        bboxStream.writeBytes(bboxValues.toByteArray());
        byte[] nContourBytes = nContour.toByteArray();
        byte[] nPointsBytes = nPoints.toByteArray();
        byte[] flagBytes = flags.toByteArray();
        byte[] glyphBytes = glyphStream.toByteArray();
        byte[] compositeBytes = composite.toByteArray();
        byte[] bboxBytes = bboxStream.toByteArray();
        byte[] instructionBytes = instructions.toByteArray();
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.BIG_ENDIAN);
        header.putShort((short) 0);
        header.putShort((short) (anyOverlap ? 1 : 0));
        header.putShort((short) numGlyphs);
        header.putShort((short) indexFormat);
        header.putInt(nContourBytes.length);
        header.putInt(nPointsBytes.length);
        header.putInt(flagBytes.length);
        header.putInt(glyphBytes.length);
        header.putInt(compositeBytes.length);
        header.putInt(bboxBytes.length);
        header.putInt(instructionBytes.length);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(header.array());
        output.writeBytes(nContourBytes);
        output.writeBytes(nPointsBytes);
        output.writeBytes(flagBytes);
        output.writeBytes(glyphBytes);
        output.writeBytes(compositeBytes);
        output.writeBytes(bboxBytes);
        output.writeBytes(instructionBytes);
        if (anyOverlap) {
            output.writeBytes(overlapBitmap);
        }
        return output.toByteArray();
    }

    /// Reconstructs a version-1 transformed `hmtx` table.
    ///
    /// @param transformed the transformed `hmtx` bytes
    /// @param glyf the reconstructed `glyf` table
    /// @param loca the reconstructed `loca` table
    /// @param numGlyphs the glyph count
    /// @param numberOfHMetrics `hhea.numberOfHMetrics`
    /// @return the reconstructed `hmtx` bytes
    static byte[] reconstructHmtx(
            byte[] transformed,
            byte[] glyf,
            byte[] loca,
            int numGlyphs,
            int numberOfHMetrics
    ) {
        if (transformed.length < 1 + 2 * numberOfHMetrics) {
            throw new IllegalArgumentException("WOFF2 transformed hmtx is truncated");
        }
        int flags = Byte.toUnsignedInt(transformed[0]);
        if ((flags & ~3) != 0) {
            throw new IllegalArgumentException("WOFF2 transformed hmtx reserved flags must be clear");
        }
        boolean hasLsb = (flags & 1) == 0;
        boolean hasMono = (flags & 2) == 0;
        if (hasLsb && hasMono) {
            throw new IllegalArgumentException("WOFF2 transformed hmtx must omit at least one LSB array");
        }
        ByteBuffer source = ByteBuffer.wrap(transformed).order(ByteOrder.BIG_ENDIAN);
        source.get();
        int[] advances = new int[numberOfHMetrics];
        for (int index = 0; index < numberOfHMetrics; index++) {
            advances[index] = Short.toUnsignedInt(source.getShort());
        }
        int[] lsb = new int[numGlyphs];
        if (hasLsb) {
            for (int index = 0; index < numberOfHMetrics; index++) {
                lsb[index] = source.getShort();
            }
        } else {
            for (int index = 0; index < numberOfHMetrics; index++) {
                lsb[index] = xMin(glyf, loca, index, numGlyphs);
            }
        }
        if (hasMono) {
            for (int index = numberOfHMetrics; index < numGlyphs; index++) {
                lsb[index] = source.getShort();
            }
        } else {
            for (int index = numberOfHMetrics; index < numGlyphs; index++) {
                lsb[index] = xMin(glyf, loca, index, numGlyphs);
            }
        }
        if (source.hasRemaining()) {
            throw new IllegalArgumentException("WOFF2 transformed hmtx has trailing bytes");
        }
        ByteBuffer output = ByteBuffer.allocate(numberOfHMetrics * 4 + (numGlyphs - numberOfHMetrics) * 2)
                .order(ByteOrder.BIG_ENDIAN);
        for (int index = 0; index < numberOfHMetrics; index++) {
            output.putShort((short) advances[index]);
            output.putShort((short) lsb[index]);
        }
        for (int index = numberOfHMetrics; index < numGlyphs; index++) {
            output.putShort((short) lsb[index]);
        }
        return output.array();
    }

    /// Encodes `hmtx` as a version-1 transform, or returns `null` when both LSB arrays must stay.
    ///
    /// @param hmtx the raw `hmtx` table
    /// @param glyf the raw `glyf` table
    /// @param loca the raw `loca` table
    /// @param numGlyphs the glyph count
    /// @param numberOfHMetrics `hhea.numberOfHMetrics`
    /// @return the transformed payload, or `null` when the transform does not apply
    static byte @Nullable [] transformHmtx(
            byte[] hmtx,
            byte[] glyf,
            byte[] loca,
            int numGlyphs,
            int numberOfHMetrics
    ) {
        if (numberOfHMetrics < 1 || numberOfHMetrics > numGlyphs) {
            return null;
        }
        int expected = numberOfHMetrics * 4 + (numGlyphs - numberOfHMetrics) * 2;
        if (hmtx.length != expected) {
            return null;
        }
        ByteBuffer source = ByteBuffer.wrap(hmtx).order(ByteOrder.BIG_ENDIAN);
        int[] advances = new int[numberOfHMetrics];
        int[] lsb = new int[numGlyphs];
        for (int index = 0; index < numberOfHMetrics; index++) {
            advances[index] = Short.toUnsignedInt(source.getShort());
            lsb[index] = source.getShort();
        }
        for (int index = numberOfHMetrics; index < numGlyphs; index++) {
            lsb[index] = source.getShort();
        }
        boolean hasLsb = false;
        for (int index = 0; index < numberOfHMetrics; index++) {
            if (lsb[index] != xMin(glyf, loca, index, numGlyphs)) {
                hasLsb = true;
                break;
            }
        }
        boolean hasMono = false;
        for (int index = numberOfHMetrics; index < numGlyphs; index++) {
            if (lsb[index] != xMin(glyf, loca, index, numGlyphs)) {
                hasMono = true;
                break;
            }
        }
        if (hasLsb && hasMono) {
            return null;
        }
        int flags = 0;
        if (!hasLsb) {
            flags |= 1;
        }
        if (!hasMono) {
            flags |= 2;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(flags);
        for (int advance : advances) {
            writeI16(output, advance);
        }
        if (hasLsb) {
            for (int index = 0; index < numberOfHMetrics; index++) {
                writeI16(output, lsb[index]);
            }
        }
        if (hasMono) {
            for (int index = numberOfHMetrics; index < numGlyphs; index++) {
                writeI16(output, lsb[index]);
            }
        }
        return output.toByteArray();
    }

    /// Returns `xMin` of `glyphId`, or `0` when the glyph is empty.
    static int xMin(byte[] glyf, byte[] loca, int glyphId, int numGlyphs) {
        int indexFormat = loca.length == (numGlyphs + 1) * 2 ? 0 : 1;
        int[] offsets = readLoca(loca, numGlyphs, indexFormat);
        int start = offsets[glyphId];
        int end = offsets[glyphId + 1];
        if (end - start < 10) {
            return 0;
        }
        ByteBuffer glyph = ByteBuffer.wrap(glyf, start, end - start).order(ByteOrder.BIG_ENDIAN);
        short contours = glyph.getShort();
        if (contours == 0) {
            return 0;
        }
        return glyph.getShort();
    }

    /// Writes one reconstructed simple glyph.
    private static void writeSimple(
            ByteArrayOutputStream glyf,
            short contours,
            ByteBuffer nPoints,
            ByteBuffer flags,
            ByteBuffer glyphStream,
            ByteBuffer instructions,
            ByteBuffer bbox,
            byte[] bboxBitmap,
            byte @Nullable [] overlap,
            int glyphId
    ) {
        int[] counts = new int[contours];
        int nPointsTotal = 0;
        for (int index = 0; index < contours; index++) {
            counts[index] = read255(nPoints);
            if (counts[index] < 1) {
                throw new IllegalArgumentException("WOFF2 contour must contain at least one point");
            }
            nPointsTotal += counts[index];
        }
        int[] xs = new int[nPointsTotal];
        int[] ys = new int[nPointsTotal];
        boolean[] on = new boolean[nPointsTotal];
        int x = 0;
        int y = 0;
        for (int index = 0; index < nPointsTotal; index++) {
            if (!flags.hasRemaining()) {
                throw new IllegalArgumentException("WOFF2 flagStream is truncated");
            }
            int flag = Byte.toUnsignedInt(flags.get());
            on[index] = (flag & 0x80) == 0;
            int[] delta = decodeTriplet(flag & 0x7F, glyphStream);
            x += delta[0];
            y += delta[1];
            xs[index] = x;
            ys[index] = y;
        }
        int xMin;
        int yMin;
        int xMax;
        int yMax;
        if (bboxBit(bboxBitmap, glyphId)) {
            xMin = bbox.getShort();
            yMin = bbox.getShort();
            xMax = bbox.getShort();
            yMax = bbox.getShort();
        } else {
            xMin = xs[0];
            yMin = ys[0];
            xMax = xs[0];
            yMax = ys[0];
            for (int index = 1; index < nPointsTotal; index++) {
                xMin = Math.min(xMin, xs[index]);
                yMin = Math.min(yMin, ys[index]);
                xMax = Math.max(xMax, xs[index]);
                yMax = Math.max(yMax, ys[index]);
            }
        }
        int instructionLength = read255(glyphStream);
        if (instructions.remaining() < instructionLength) {
            throw new IllegalArgumentException("WOFF2 instructionStream is truncated");
        }
        byte[] program = new byte[instructionLength];
        instructions.get(program);
        writeI16(glyf, contours);
        writeI16(glyf, xMin);
        writeI16(glyf, yMin);
        writeI16(glyf, xMax);
        writeI16(glyf, yMax);
        int end = -1;
        for (int count : counts) {
            end += count;
            writeI16(glyf, end);
        }
        writeI16(glyf, instructionLength);
        glyf.writeBytes(program);
        boolean overlapSimple = overlap != null && bboxBit(overlap, glyphId);
        writeSimpleCoordinates(glyf, xs, ys, on, overlapSimple);
    }

    /// Writes glyf flags plus signed 16-bit x/y deltas.
    private static void writeSimpleCoordinates(
            ByteArrayOutputStream glyf,
            int[] xs,
            int[] ys,
            boolean[] on,
            boolean overlapSimple
    ) {
        for (int index = 0; index < xs.length; index++) {
            int flag = on[index] ? ON_CURVE : 0;
            if (index == 0 && overlapSimple) {
                flag |= 0x40;
            }
            glyf.write(flag);
        }
        int prevX = 0;
        int prevY = 0;
        for (int x : xs) {
            writeI16(glyf, x - prevX);
            prevX = x;
        }
        prevY = 0;
        for (int y : ys) {
            writeI16(glyf, y - prevY);
            prevY = y;
        }
    }

    /// Parsed simple outline used while encoding.
    private static final class SimpleOutline {
        /// Points in each contour.
        final int[] pointsPerContour;
        /// Absolute X.
        final int[] xs;
        /// Absolute Y.
        final int[] ys;
        /// On-curve flags.
        final boolean[] on;
        /// Whether `OVERLAP_SIMPLE` is set on the first flag.
        final boolean overlap;
        /// Hinting program.
        final byte[] instructions;
        /// Computed xMin.
        final int computedMinX;
        /// Computed yMin.
        final int computedMinY;
        /// Computed xMax.
        final int computedMaxX;
        /// Computed yMax.
        final int computedMaxY;

        /// Creates an outline.
        SimpleOutline(
                int[] pointsPerContour,
                int[] xs,
                int[] ys,
                boolean[] on,
                boolean overlap,
                byte[] instructions,
                int computedMinX,
                int computedMinY,
                int computedMaxX,
                int computedMaxY
        ) {
            this.pointsPerContour = pointsPerContour;
            this.xs = xs;
            this.ys = ys;
            this.on = on;
            this.overlap = overlap;
            this.instructions = instructions;
            this.computedMinX = computedMinX;
            this.computedMinY = computedMinY;
            this.computedMaxX = computedMaxX;
            this.computedMaxY = computedMaxY;
        }
    }

    /// Writes one reconstructed composite glyph.
    private static void writeComposite(
            ByteArrayOutputStream glyf,
            ByteBuffer composite,
            ByteBuffer glyphStream,
            ByteBuffer instructions,
            ByteBuffer bbox,
            byte[] bboxBitmap,
            int glyphId
    ) {
        if (!bboxBit(bboxBitmap, glyphId)) {
            throw new IllegalArgumentException("WOFF2 composite glyph must carry a bbox");
        }
        int xMin = bbox.getShort();
        int yMin = bbox.getShort();
        int xMax = bbox.getShort();
        int yMax = bbox.getShort();
        writeI16(glyf, -1);
        writeI16(glyf, xMin);
        writeI16(glyf, yMin);
        writeI16(glyf, xMax);
        writeI16(glyf, yMax);
        boolean haveInstructions = false;
        int flags;
        do {
            if (composite.remaining() < 4) {
                throw new IllegalArgumentException("WOFF2 compositeStream is truncated");
            }
            flags = Short.toUnsignedInt(composite.getShort());
            int glyphIndex = Short.toUnsignedInt(composite.getShort());
            int extra = (flags & ARG_WORDS) != 0 ? 4 : 2;
            if ((flags & HAVE_SCALE) != 0) {
                extra += 2;
            }
            if ((flags & HAVE_XY_SCALE) != 0) {
                extra += 4;
            }
            if ((flags & HAVE_2X2) != 0) {
                extra += 8;
            }
            if (composite.remaining() < extra) {
                throw new IllegalArgumentException("WOFF2 composite arguments are truncated");
            }
            writeI16(glyf, flags);
            writeI16(glyf, glyphIndex);
            for (int index = 0; index < extra; index++) {
                glyf.write(composite.get());
            }
            haveInstructions |= (flags & HAVE_INSTRUCTIONS) != 0;
        } while ((flags & MORE_COMPONENTS) != 0);
        if (haveInstructions) {
            int instructionLength = read255(glyphStream);
            if (instructions.remaining() < instructionLength) {
                throw new IllegalArgumentException("WOFF2 composite instructions are truncated");
            }
            byte[] program = new byte[instructionLength];
            instructions.get(program);
            writeI16(glyf, instructionLength);
            glyf.writeBytes(program);
        }
    }

    /// Copies one composite glyph from raw `glyf` into the WOFF2 streams.
    private static void encodeComposite(
            ByteBuffer glyph,
            ByteArrayOutputStream composite,
            ByteArrayOutputStream glyphStream,
            ByteArrayOutputStream instructions
    ) {
        boolean haveInstructions = false;
        int flags;
        do {
            if (glyph.remaining() < 4) {
                throw new IllegalArgumentException("glyf composite is truncated");
            }
            flags = Short.toUnsignedInt(glyph.getShort());
            int glyphIndex = Short.toUnsignedInt(glyph.getShort());
            int extra = (flags & ARG_WORDS) != 0 ? 4 : 2;
            if ((flags & HAVE_SCALE) != 0) {
                extra += 2;
            }
            if ((flags & HAVE_XY_SCALE) != 0) {
                extra += 4;
            }
            if ((flags & HAVE_2X2) != 0) {
                extra += 8;
            }
            if (glyph.remaining() < extra) {
                throw new IllegalArgumentException("glyf composite arguments are truncated");
            }
            writeI16(composite, flags);
            writeI16(composite, glyphIndex);
            for (int index = 0; index < extra; index++) {
                composite.write(glyph.get());
            }
            haveInstructions |= (flags & HAVE_INSTRUCTIONS) != 0;
        } while ((flags & MORE_COMPONENTS) != 0);
        if (haveInstructions) {
            if (glyph.remaining() < 2) {
                throw new IllegalArgumentException("glyf composite instructions are truncated");
            }
            int instructionLength = Short.toUnsignedInt(glyph.getShort());
            if (glyph.remaining() < instructionLength) {
                throw new IllegalArgumentException("glyf composite instruction bytes are truncated");
            }
            byte[] program = new byte[instructionLength];
            glyph.get(program);
            write255(glyphStream, instructionLength);
            instructions.writeBytes(program);
        }
    }

    /// Parses a simple glyph after the bbox fields have been consumed.
    private static SimpleOutline parseSimple(ByteBuffer glyph, int contours) {
        if (glyph.remaining() < contours * 2 + 2) {
            throw new IllegalArgumentException("glyf simple header is truncated");
        }
        int[] ends = new int[contours];
        for (int index = 0; index < contours; index++) {
            ends[index] = Short.toUnsignedInt(glyph.getShort());
        }
        int nPoints = ends[contours - 1] + 1;
        int[] pointsPerContour = new int[contours];
        int previous = -1;
        for (int index = 0; index < contours; index++) {
            pointsPerContour[index] = ends[index] - previous;
            previous = ends[index];
        }
        int instructionLength = Short.toUnsignedInt(glyph.getShort());
        if (glyph.remaining() < instructionLength) {
            throw new IllegalArgumentException("glyf instructions are truncated");
        }
        byte[] program = new byte[instructionLength];
        glyph.get(program);
        int[] rawFlags = new int[nPoints];
        int point = 0;
        while (point < nPoints) {
            if (!glyph.hasRemaining()) {
                throw new IllegalArgumentException("glyf flags are truncated");
            }
            int flag = Byte.toUnsignedInt(glyph.get());
            int repeat = 1;
            if ((flag & REPEAT) != 0) {
                if (!glyph.hasRemaining()) {
                    throw new IllegalArgumentException("glyf flag repeat is truncated");
                }
                repeat += Byte.toUnsignedInt(glyph.get());
            }
            for (int index = 0; index < repeat; index++) {
                if (point >= nPoints) {
                    throw new IllegalArgumentException("glyf flag repeat exceeds the point count");
                }
                rawFlags[point++] = flag;
            }
        }
        int[] xs = new int[nPoints];
        int[] ys = new int[nPoints];
        boolean[] on = new boolean[nPoints];
        int x = 0;
        for (int index = 0; index < nPoints; index++) {
            int flag = rawFlags[index];
            on[index] = (flag & ON_CURVE) != 0;
            if ((flag & X_SHORT) != 0) {
                if (!glyph.hasRemaining()) {
                    throw new IllegalArgumentException("glyf x-short is truncated");
                }
                int value = Byte.toUnsignedInt(glyph.get());
                x += (flag & X_SAME) != 0 ? value : -value;
            } else if ((flag & X_SAME) == 0) {
                if (glyph.remaining() < 2) {
                    throw new IllegalArgumentException("glyf x-delta is truncated");
                }
                x += glyph.getShort();
            }
            xs[index] = x;
        }
        int y = 0;
        for (int index = 0; index < nPoints; index++) {
            int flag = rawFlags[index];
            if ((flag & Y_SHORT) != 0) {
                if (!glyph.hasRemaining()) {
                    throw new IllegalArgumentException("glyf y-short is truncated");
                }
                int value = Byte.toUnsignedInt(glyph.get());
                y += (flag & Y_SAME) != 0 ? value : -value;
            } else if ((flag & Y_SAME) == 0) {
                if (glyph.remaining() < 2) {
                    throw new IllegalArgumentException("glyf y-delta is truncated");
                }
                y += glyph.getShort();
            }
            ys[index] = y;
        }
        int minX = xs[0];
        int minY = ys[0];
        int maxX = xs[0];
        int maxY = ys[0];
        for (int index = 1; index < nPoints; index++) {
            minX = Math.min(minX, xs[index]);
            minY = Math.min(minY, ys[index]);
            maxX = Math.max(maxX, xs[index]);
            maxY = Math.max(maxY, ys[index]);
        }
        return new SimpleOutline(
                pointsPerContour,
                xs,
                ys,
                on,
                (rawFlags[0] & 0x40) != 0,
                program,
                minX,
                minY,
                maxX,
                maxY
        );
    }

    /// Encodes relative point coordinates with the WOFF2 triplet table.
    private static void encodeTriplets(
            SimpleOutline outline,
            ByteArrayOutputStream flags,
            ByteArrayOutputStream glyphStream
    ) {
        int prevX = 0;
        int prevY = 0;
        for (int index = 0; index < outline.xs.length; index++) {
            int dx = outline.xs[index] - prevX;
            int dy = outline.ys[index] - prevY;
            prevX = outline.xs[index];
            prevY = outline.ys[index];
            int absX = Math.abs(dx);
            int absY = Math.abs(dy);
            int onCurveBit = outline.on[index] ? 0 : 128;
            int xSign = dx < 0 ? 0 : 1;
            int ySign = dy < 0 ? 0 : 1;
            int xySign = xSign + 2 * ySign;
            if (dx == 0 && absY < 1280) {
                flags.write(onCurveBit + ((absY & 0xF00) >> 7) + ySign);
                glyphStream.write(absY & 0xFF);
            } else if (dy == 0 && absX < 1280) {
                flags.write(onCurveBit + 10 + ((absX & 0xF00) >> 7) + xSign);
                glyphStream.write(absX & 0xFF);
            } else if (absX < 65 && absY < 65 && absX > 0 && absY > 0) {
                flags.write(onCurveBit + 20 + ((absX - 1) & 0x30) + (((absY - 1) & 0x30) >> 2) + xySign);
                glyphStream.write((((absX - 1) & 0xF) << 4) | ((absY - 1) & 0xF));
            } else if (absX < 769 && absY < 769 && absX > 0 && absY > 0) {
                flags.write(onCurveBit + 84 + 12 * (((absX - 1) & 0x300) >> 8) + (((absY - 1) & 0x300) >> 6) + xySign);
                glyphStream.write((absX - 1) & 0xFF);
                glyphStream.write((absY - 1) & 0xFF);
            } else if (absX < 4096 && absY < 4096) {
                flags.write(onCurveBit + 120 + xySign);
                glyphStream.write(absX >> 4);
                glyphStream.write(((absX & 0xF) << 4) | (absY >> 8));
                glyphStream.write(absY & 0xFF);
            } else {
                flags.write(onCurveBit + 124 + xySign);
                glyphStream.write(absX >> 8);
                glyphStream.write(absX & 0xFF);
                glyphStream.write(absY >> 8);
                glyphStream.write(absY & 0xFF);
            }
        }
    }

    /// Decodes one WOFF2 coordinate triplet and returns `{dx, dy}`.
    private static int[] decodeTriplet(int flag, ByteBuffer glyphStream) {
        int nBytes;
        if (flag < 84) {
            nBytes = 1;
        } else if (flag < 120) {
            nBytes = 2;
        } else if (flag < 124) {
            nBytes = 3;
        } else {
            nBytes = 4;
        }
        if (glyphStream.remaining() < nBytes) {
            throw new IllegalArgumentException("WOFF2 glyphStream triplet is truncated");
        }
        int dx;
        int dy;
        if (flag < 10) {
            dx = 0;
            dy = withSign(flag, ((flag & 14) << 7) + Byte.toUnsignedInt(glyphStream.get()));
        } else if (flag < 20) {
            dx = withSign(flag, (((flag - 10) & 14) << 7) + Byte.toUnsignedInt(glyphStream.get()));
            dy = 0;
        } else if (flag < 84) {
            int b0 = flag - 20;
            int b1 = Byte.toUnsignedInt(glyphStream.get());
            dx = withSign(flag, 1 + (b0 & 0x30) + (b1 >> 4));
            dy = withSign(flag >> 1, 1 + ((b0 & 0x0C) << 2) + (b1 & 0x0F));
        } else if (flag < 120) {
            int b0 = flag - 84;
            int b1 = Byte.toUnsignedInt(glyphStream.get());
            int b2 = Byte.toUnsignedInt(glyphStream.get());
            dx = withSign(flag, 1 + ((b0 / 12) << 8) + b1);
            dy = withSign(flag >> 1, 1 + (((b0 % 12) >> 2) << 8) + b2);
        } else if (flag < 124) {
            int b0 = Byte.toUnsignedInt(glyphStream.get());
            int b1 = Byte.toUnsignedInt(glyphStream.get());
            int b2 = Byte.toUnsignedInt(glyphStream.get());
            dx = withSign(flag, (b0 << 4) + (b1 >> 4));
            dy = withSign(flag >> 1, ((b1 & 0x0F) << 8) + b2);
        } else {
            int b0 = Byte.toUnsignedInt(glyphStream.get());
            int b1 = Byte.toUnsignedInt(glyphStream.get());
            int b2 = Byte.toUnsignedInt(glyphStream.get());
            int b3 = Byte.toUnsignedInt(glyphStream.get());
            dx = withSign(flag, (b0 << 8) + b1);
            dy = withSign(flag >> 1, (b2 << 8) + b3);
        }
        return new int[] {dx, dy};
    }

    /// Applies the WOFF2 sign bit: odd `flag` is positive.
    private static int withSign(int flag, int magnitude) {
        return (flag & 1) != 0 ? magnitude : -magnitude;
    }

    /// Reads `loca` offsets.
    private static int[] readLoca(byte[] loca, int numGlyphs, int indexFormat) {
        int expected = (numGlyphs + 1) * (indexFormat == 0 ? 2 : 4);
        if (loca.length != expected) {
            throw new IllegalArgumentException("loca length does not match indexFormat");
        }
        ByteBuffer buffer = ByteBuffer.wrap(loca).order(ByteOrder.BIG_ENDIAN);
        int[] offsets = new int[numGlyphs + 1];
        for (int index = 0; index <= numGlyphs; index++) {
            offsets[index] = indexFormat == 0
                    ? Short.toUnsignedInt(buffer.getShort()) * 2
                    : buffer.getInt();
        }
        return offsets;
    }

    /// Writes `loca` offsets.
    private static byte[] writeLoca(int[] offsets, int indexFormat) {
        ByteBuffer buffer = ByteBuffer.allocate(offsets.length * (indexFormat == 0 ? 2 : 4))
                .order(ByteOrder.BIG_ENDIAN);
        for (int offset : offsets) {
            if (indexFormat == 0) {
                if ((offset & 1) != 0 || offset > 0x1FFFE) {
                    throw new IllegalArgumentException("short loca offset is not representable");
                }
                buffer.putShort((short) (offset / 2));
            } else {
                buffer.putInt(offset);
            }
        }
        return buffer.array();
    }

    /// Reads a spec 255UInt16.
    private static int read255(ByteBuffer buffer) {
        if (!buffer.hasRemaining()) {
            throw new IllegalArgumentException("WOFF2 255UInt16 is truncated");
        }
        int code = Byte.toUnsignedInt(buffer.get());
        if (code == 253) {
            if (buffer.remaining() < 2) {
                throw new IllegalArgumentException("WOFF2 255UInt16 word is truncated");
            }
            return Short.toUnsignedInt(buffer.getShort());
        }
        if (code == 254) {
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("WOFF2 255UInt16 plus-506 is truncated");
            }
            return Byte.toUnsignedInt(buffer.get()) + 506;
        }
        if (code == 255) {
            if (!buffer.hasRemaining()) {
                throw new IllegalArgumentException("WOFF2 255UInt16 plus-253 is truncated");
            }
            return Byte.toUnsignedInt(buffer.get()) + 253;
        }
        return code;
    }

    /// Writes a spec 255UInt16.
    private static void write255(ByteArrayOutputStream output, int value) {
        if (value < 0 || value > 0xFFFF) {
            throw new IllegalArgumentException("WOFF2 255UInt16 is out of range");
        }
        if (value < 253) {
            output.write(value);
        } else if (value < 506) {
            output.write(255);
            output.write(value - 253);
        } else if (value < 762) {
            output.write(254);
            output.write(value - 506);
        } else {
            output.write(253);
            output.write((value >>> 8) & 0xFF);
            output.write(value & 0xFF);
        }
    }

    /// Writes a big-endian int16.
    private static void writeI16(ByteArrayOutputStream output, int value) {
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    /// Copies `length` bytes from `source` at `offset`.
    private static byte[] slice(byte[] source, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > source.length) {
            throw new IllegalArgumentException("WOFF2 transformed glyf stream is truncated");
        }
        byte[] slice = new byte[length];
        System.arraycopy(source, offset, slice, 0, length);
        return slice;
    }

    /// Returns whether bit `index` is set in a WOFF2 MSB-first bitmap.
    private static boolean bboxBit(byte[] bitmap, int index) {
        int byteIndex = index >> 3;
        if (byteIndex >= bitmap.length) {
            return false;
        }
        return (bitmap[byteIndex] & (0x80 >> (index & 7))) != 0;
    }

    /// Sets bit `index` in a WOFF2 MSB-first bitmap.
    private static void setBit(byte[] bitmap, int index) {
        bitmap[index >> 3] |= (byte) (0x80 >> (index & 7));
    }

    /// Pads `glyf` to an even length.
    private static void padEven(ByteArrayOutputStream glyf) {
        if ((glyf.size() & 1) != 0) {
            glyf.write(0);
        }
    }
}
