package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.ByteBuffer;
import java.util.Objects;

/// Decodes simple and composite `glyf` outlines into an [`OutlinePen`].
///
/// Implied on-curve midpoints between consecutive off-curve points use untruncated averages.
/// Composite glyphs apply XY offsets and optional F2DOT14 scales. Hint instructions are skipped.
/// Recursion deeper than [`#MAX_COMPONENT_DEPTH`] is rejected.
@NotNullByDefault
final class OutlineWalker {
    /// On-curve point flag.
    private static final int ON_CURVE = 0x01;

    /// One-byte X delta flag.
    private static final int X_SHORT = 0x02;

    /// One-byte Y delta flag.
    private static final int Y_SHORT = 0x04;

    /// Repeat-count flag.
    private static final int REPEAT = 0x08;

    /// Positive short X, or identical X when the vector is not short.
    private static final int X_SAME_OR_POSITIVE = 0x10;

    /// Positive short Y, or identical Y when the vector is not short.
    private static final int Y_SAME_OR_POSITIVE = 0x20;

    /// Component arguments are 16-bit.
    private static final int ARG_1_AND_2_ARE_WORDS = 0x0001;

    /// Component arguments are XY offsets rather than matching points.
    private static final int ARGS_ARE_XY_VALUES = 0x0002;

    /// Component has a uniform F2DOT14 scale.
    private static final int WE_HAVE_A_SCALE = 0x0008;

    /// Further components follow.
    private static final int MORE_COMPONENTS = 0x0020;

    /// Component has independent X and Y F2DOT14 scales.
    private static final int WE_HAVE_AN_X_AND_Y_SCALE = 0x0040;

    /// Component has a 2×2 F2DOT14 matrix.
    private static final int WE_HAVE_A_TWO_BY_TWO = 0x0080;

    /// Instructions follow the last component.
    private static final int WE_HAVE_INSTRUCTIONS = 0x0100;

    /// Offset is transformed by the component matrix.
    private static final int SCALED_COMPONENT_OFFSET = 0x0800;

    /// Offset is not transformed by the component matrix.
    private static final int UNSCALED_COMPONENT_OFFSET = 0x1000;

    /// Maximum composite nesting depth, inclusive of the root.
    static final int MAX_COMPONENT_DEPTH = 16;

    /// Prevents instantiation.
    private OutlineWalker() {
    }

    /// Walks `glyphId` into `pen`.
    ///
    /// Empty `glyf` ranges emit no commands. Composite components that do not set
    /// [`#ARGS_ARE_XY_VALUES`] are placed at the origin.
    ///
    /// @param font the font
    /// @param glyphId the glyph identity
    /// @param pen the destination
    /// @param depth the current composite depth, `0` at the root
    static void walk(SfntFont font, int glyphId, OutlinePen pen, int depth) {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(pen, "pen");
        if (depth > MAX_COMPONENT_DEPTH) {
            throw new IllegalArgumentException("glyf composite depth exceeds " + MAX_COMPONENT_DEPTH);
        }
        ByteBuffer glyf = font.glyf(glyphId);
        if (glyf.remaining() < 2) {
            return;
        }
        short contours = glyf.getShort();
        if (glyf.remaining() < 8) {
            throw new IllegalArgumentException("glyf header is truncated");
        }
        glyf.getShort();
        glyf.getShort();
        glyf.getShort();
        glyf.getShort();
        if (contours > 0) {
            walkSimple(glyf, contours, pen);
            return;
        }
        if (contours < 0) {
            walkComposite(font, glyf, pen, depth);
        }
    }

    /// Emits one simple glyph.
    private static void walkSimple(ByteBuffer glyf, int contours, OutlinePen pen) {
        if (glyf.remaining() < contours * 2 + 2) {
            throw new IllegalArgumentException("glyf end points are truncated");
        }
        int[] endPts = new int[contours];
        int lastPoint = -1;
        for (int index = 0; index < contours; index++) {
            int end = Short.toUnsignedInt(glyf.getShort());
            if (end < lastPoint) {
                throw new IllegalArgumentException("glyf end points are not increasing");
            }
            endPts[index] = end;
            lastPoint = end;
        }
        int instructionLength = Short.toUnsignedInt(glyf.getShort());
        if (glyf.remaining() < instructionLength) {
            throw new IllegalArgumentException("glyf instructions are truncated");
        }
        glyf.position(glyf.position() + instructionLength);
        int pointCount = lastPoint + 1;
        byte[] flags = readFlags(glyf, pointCount);
        float[] xs = new float[pointCount];
        float[] ys = new float[pointCount];
        readCoordinates(glyf, flags, xs, true);
        readCoordinates(glyf, flags, ys, false);
        int start = 0;
        for (int contour = 0; contour < contours; contour++) {
            emitContour(xs, ys, flags, start, endPts[contour], pen);
            start = endPts[contour] + 1;
        }
    }

    /// Reads packed point flags, expanding repeats.
    private static byte[] readFlags(ByteBuffer glyf, int pointCount) {
        byte[] flags = new byte[pointCount];
        int written = 0;
        while (written < pointCount) {
            if (glyf.remaining() < 1) {
                throw new IllegalArgumentException("glyf flags are truncated");
            }
            int flag = glyf.get() & 0xFF;
            flags[written++] = (byte) flag;
            if ((flag & REPEAT) != 0) {
                if (glyf.remaining() < 1) {
                    throw new IllegalArgumentException("glyf flag repeat is truncated");
                }
                int repeat = glyf.get() & 0xFF;
                if (written + repeat > pointCount) {
                    throw new IllegalArgumentException("glyf flag repeat exceeds the point count");
                }
                for (int index = 0; index < repeat; index++) {
                    flags[written++] = (byte) flag;
                }
            }
        }
        return flags;
    }

    /// Reads accumulated X or Y coordinates.
    private static void readCoordinates(ByteBuffer glyf, byte[] flags, float[] values, boolean xAxis) {
        int shortBit = xAxis ? X_SHORT : Y_SHORT;
        int sameBit = xAxis ? X_SAME_OR_POSITIVE : Y_SAME_OR_POSITIVE;
        int value = 0;
        for (int index = 0; index < flags.length; index++) {
            int flag = flags[index] & 0xFF;
            if ((flag & shortBit) != 0) {
                if (glyf.remaining() < 1) {
                    throw new IllegalArgumentException("glyf coordinate is truncated");
                }
                int delta = glyf.get() & 0xFF;
                value += (flag & sameBit) != 0 ? delta : -delta;
            } else if ((flag & sameBit) == 0) {
                if (glyf.remaining() < 2) {
                    throw new IllegalArgumentException("glyf coordinate is truncated");
                }
                value += glyf.getShort();
            }
            values[index] = value;
        }
    }

    /// Emits one contour using FreeType-style start-point selection.
    private static void emitContour(
            float[] xs,
            float[] ys,
            byte[] flags,
            int first,
            int last,
            OutlinePen pen
    ) {
        if (first > last) {
            return;
        }
        boolean firstOn = (flags[first] & ON_CURVE) != 0;
        boolean lastOn = (flags[last] & ON_CURVE) != 0;
        float startX;
        float startY;
        int begin;
        int limit = last;
        if (!firstOn) {
            if (lastOn) {
                startX = xs[last];
                startY = ys[last];
                limit = last - 1;
                begin = first;
            } else {
                startX = (xs[last] + xs[first]) * 0.5f;
                startY = (ys[last] + ys[first]) * 0.5f;
                begin = first;
            }
        } else {
            startX = xs[first];
            startY = ys[first];
            begin = first + 1;
        }
        pen.moveTo(startX, startY);
        boolean pending = false;
        float controlX = 0.0f;
        float controlY = 0.0f;
        for (int index = begin; index <= limit; index++) {
            float x = xs[index];
            float y = ys[index];
            if ((flags[index] & ON_CURVE) != 0) {
                if (pending) {
                    pen.quadTo(controlX, controlY, x, y);
                    pending = false;
                } else {
                    pen.lineTo(x, y);
                }
            } else if (pending) {
                float midX = (controlX + x) * 0.5f;
                float midY = (controlY + y) * 0.5f;
                pen.quadTo(controlX, controlY, midX, midY);
                controlX = x;
                controlY = y;
            } else {
                pending = true;
                controlX = x;
                controlY = y;
            }
        }
        if (pending) {
            pen.quadTo(controlX, controlY, startX, startY);
        }
        pen.close();
    }

    /// Expands one composite glyph.
    private static void walkComposite(SfntFont font, ByteBuffer glyf, OutlinePen pen, int depth) {
        boolean more = true;
        int flags = 0;
        while (more) {
            if (glyf.remaining() < 4) {
                throw new IllegalArgumentException("glyf composite is truncated");
            }
            flags = Short.toUnsignedInt(glyf.getShort());
            int child = Short.toUnsignedInt(glyf.getShort());
            float arg1;
            float arg2;
            if ((flags & ARG_1_AND_2_ARE_WORDS) != 0) {
                if (glyf.remaining() < 4) {
                    throw new IllegalArgumentException("glyf composite arguments are truncated");
                }
                arg1 = glyf.getShort();
                arg2 = glyf.getShort();
            } else {
                if (glyf.remaining() < 2) {
                    throw new IllegalArgumentException("glyf composite arguments are truncated");
                }
                arg1 = glyf.get();
                arg2 = glyf.get();
            }
            float xx = 1.0f;
            float xy = 0.0f;
            float yx = 0.0f;
            float yy = 1.0f;
            if ((flags & WE_HAVE_A_SCALE) != 0) {
                xx = f2dot14(glyf);
                yy = xx;
            } else if ((flags & WE_HAVE_AN_X_AND_Y_SCALE) != 0) {
                xx = f2dot14(glyf);
                yy = f2dot14(glyf);
            } else if ((flags & WE_HAVE_A_TWO_BY_TWO) != 0) {
                xx = f2dot14(glyf);
                yx = f2dot14(glyf);
                xy = f2dot14(glyf);
                yy = f2dot14(glyf);
            }
            float dx = 0.0f;
            float dy = 0.0f;
            if ((flags & ARGS_ARE_XY_VALUES) != 0) {
                dx = arg1;
                dy = arg2;
                if ((flags & SCALED_COMPONENT_OFFSET) != 0 && (flags & UNSCALED_COMPONENT_OFFSET) == 0) {
                    float scaledX = xx * dx + xy * dy;
                    float scaledY = yx * dx + yy * dy;
                    dx = scaledX;
                    dy = scaledY;
                }
            }
            OutlinePen childPen = identity(xx, xy, yx, yy, dx, dy)
                    ? pen
                    : new TransformPen(pen, xx, xy, yx, yy, dx, dy);
            walk(font, child, childPen, depth + 1);
            more = (flags & MORE_COMPONENTS) != 0;
        }
        if ((flags & WE_HAVE_INSTRUCTIONS) != 0) {
            if (glyf.remaining() < 2) {
                throw new IllegalArgumentException("glyf composite instructions are truncated");
            }
            int instructionLength = Short.toUnsignedInt(glyf.getShort());
            if (glyf.remaining() < instructionLength) {
                throw new IllegalArgumentException("glyf composite instructions are truncated");
            }
            glyf.position(glyf.position() + instructionLength);
        }
    }

    /// Reads one F2DOT14 value.
    private static float f2dot14(ByteBuffer glyf) {
        if (glyf.remaining() < 2) {
            throw new IllegalArgumentException("glyf transform is truncated");
        }
        return glyf.getShort() / 16384.0f;
    }

    /// Returns whether the matrix is a pure identity including translation.
    private static boolean identity(float xx, float xy, float yx, float yy, float dx, float dy) {
        return xx == 1.0f && xy == 0.0f && yx == 0.0f && yy == 1.0f && dx == 0.0f && dy == 0.0f;
    }
}
