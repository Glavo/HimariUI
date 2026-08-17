package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Encodes and decodes baseline sequential 8-bit JPEG (SOF0) as unassociated sRGB RGBA.
///
/// The encoder writes YCbCr 4:4:4 with quality-100 quantization (all ones) and the ITU T.81
/// Annex K Huffman tables. The decoder accepts baseline SOF0 with 1 or 3 components and rejects
/// progressive, arithmetic, and 12-bit streams.
@NotNullByDefault
public final class JpegImage {
    /// Start of image.
    private static final int SOI = 0xFFD8;

    /// End of image.
    private static final int EOI = 0xFFD9;

    /// Start of frame, baseline.
    private static final int SOF0 = 0xFFC0;

    /// Define Huffman table.
    private static final int DHT = 0xFFC4;

    /// Start of scan.
    private static final int SOS = 0xFFDA;

    /// Define quantization table.
    private static final int DQT = 0xFFDB;

    /// APP0.
    private static final int APP0 = 0xFFE0;

    /// Zigzag scan order.
    private static final int[] ZIGZAG = {
            0, 1, 5, 6, 14, 15, 27, 28,
            2, 4, 7, 13, 16, 26, 29, 42,
            3, 8, 12, 17, 25, 30, 41, 43,
            9, 11, 18, 24, 31, 40, 44, 53,
            10, 19, 23, 32, 39, 45, 52, 54,
            20, 22, 33, 38, 46, 51, 55, 60,
            21, 34, 37, 47, 50, 56, 59, 61,
            35, 36, 48, 49, 57, 58, 62, 63
    };

    /// Inverse zigzag.
    private static final int[] UNZIGZAG = unzigzag();

    /// Annex K luminance DC bits.
    private static final int[] LUMA_DC_BITS = {
            0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0
    };

    /// Annex K luminance DC values.
    private static final int[] LUMA_DC_VAL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    /// Annex K luminance AC bits.
    private static final int[] LUMA_AC_BITS = {
            0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 125
    };

    /// Annex K luminance AC values.
    private static final int[] LUMA_AC_VAL = {
            0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
            0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xA1, 0x08, 0x23, 0x42, 0xB1, 0xC1, 0x15, 0x52, 0xD1, 0xF0,
            0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0A, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x25, 0x26, 0x27, 0x28,
            0x29, 0x2A, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
            0x4A, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
            0x6A, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
            0x8A, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9A, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7,
            0xA8, 0xA9, 0xAA, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9, 0xBA, 0xC2, 0xC3, 0xC4, 0xC5,
            0xC6, 0xC7, 0xC8, 0xC9, 0xCA, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7, 0xD8, 0xD9, 0xDA, 0xE1, 0xE2,
            0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9, 0xEA, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8,
            0xF9, 0xFA
    };

    /// Annex K chrominance DC bits.
    private static final int[] CHROMA_DC_BITS = {
            0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0
    };

    /// Annex K chrominance DC values.
    private static final int[] CHROMA_DC_VAL = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    /// Annex K chrominance AC bits.
    private static final int[] CHROMA_AC_BITS = {
            0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 119
    };

    /// Annex K chrominance AC values.
    private static final int[] CHROMA_AC_VAL = {
            0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
            0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91, 0xA1, 0xB1, 0xC1, 0x09, 0x23, 0x33, 0x52, 0xF0,
            0x15, 0x62, 0x72, 0xD1, 0x0A, 0x16, 0x24, 0x34, 0xE1, 0x25, 0xF1, 0x17, 0x18, 0x19, 0x1A, 0x26,
            0x27, 0x28, 0x29, 0x2A, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x4A, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
            0x69, 0x6A, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
            0x88, 0x89, 0x8A, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9A, 0xA2, 0xA3, 0xA4, 0xA5,
            0xA6, 0xA7, 0xA8, 0xA9, 0xAA, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9, 0xBA, 0xC2, 0xC3,
            0xC4, 0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0xCA, 0xD2, 0xD3, 0xD4, 0xD5, 0xD6, 0xD7, 0xD8, 0xD9, 0xDA,
            0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9, 0xEA, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8,
            0xF9, 0xFA
    };

    /// Cosine table for the 8-point DCT: `cos((2x+1) u π / 16)`.
    private static final double[][] COS = cosineTable();

    /// Prevents instantiation.
    private JpegImage() {
    }

    /// Returns whether `bytes` begin with a JPEG SOI marker.
    ///
    /// @param bytes the candidate stream
    /// @return whether the stream is JPEG
    public static boolean isJpeg(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        return bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8;
    }

    /// Encodes row-major unassociated RGBA8 pixels as baseline JPEG.
    ///
    /// Alpha is dropped. Width and height must lie in `(0, `[`PixelBuffer#MAX_EDGE`]`)`.
    ///
    /// @param width the positive pixel width
    /// @param height the positive pixel height
    /// @param rgba `width * height * 4` bytes
    /// @return the JPEG bytes
    public static byte @Unmodifiable [] encode(int width, int height, byte[] rgba) {
        Objects.requireNonNull(rgba, "rgba");
        int pixelCount = checkedPixelCount(width, height);
        if (rgba.length != pixelCount * 4) {
            throw new IllegalArgumentException("RGBA length must be width * height * 4");
        }
        Huffman lumaDc = Huffman.fromSpec(LUMA_DC_BITS, LUMA_DC_VAL);
        Huffman lumaAc = Huffman.fromSpec(LUMA_AC_BITS, LUMA_AC_VAL);
        Huffman chromaDc = Huffman.fromSpec(CHROMA_DC_BITS, CHROMA_DC_VAL);
        Huffman chromaAc = Huffman.fromSpec(CHROMA_AC_BITS, CHROMA_AC_VAL);
        BitSink sink = new BitSink();
        sink.marker(SOI);
        writeApp0(sink);
        writeDqt(sink);
        writeSof0(sink, width, height);
        writeDht(sink, 0, 0, LUMA_DC_BITS, LUMA_DC_VAL);
        writeDht(sink, 0, 1, LUMA_AC_BITS, LUMA_AC_VAL);
        writeDht(sink, 1, 0, CHROMA_DC_BITS, CHROMA_DC_VAL);
        writeDht(sink, 1, 1, CHROMA_AC_BITS, CHROMA_AC_VAL);
        writeSos(sink);
        int paddedW = (width + 7) & ~7;
        int paddedH = (height + 7) & ~7;
        float[] yPlane = new float[paddedW * paddedH];
        float[] cbPlane = new float[paddedW * paddedH];
        float[] crPlane = new float[paddedW * paddedH];
        sampleYcbcr(width, height, rgba, paddedW, yPlane, cbPlane, crPlane);
        int prevY = 0;
        int prevCb = 0;
        int prevCr = 0;
        for (int by = 0; by < paddedH; by += 8) {
            for (int bx = 0; bx < paddedW; bx += 8) {
                prevY = encodeBlock(sink, yPlane, paddedW, bx, by, prevY, lumaDc, lumaAc);
                prevCb = encodeBlock(sink, cbPlane, paddedW, bx, by, prevCb, chromaDc, chromaAc);
                prevCr = encodeBlock(sink, crPlane, paddedW, bx, by, prevCr, chromaDc, chromaAc);
            }
        }
        sink.flushBits();
        sink.marker(EOI);
        return sink.toArray();
    }

    /// Decodes a baseline JPEG stream into row-major unassociated RGBA8.
    ///
    /// @param bytes the JPEG stream
    /// @return the decoded image
    public static Decoded decode(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        if (!isJpeg(bytes)) {
            throw new IllegalArgumentException("JPEG SOI marker is missing");
        }
        BitSource source = new BitSource(bytes);
        source.position = 2;
        Huffman[] dc = new Huffman[4];
        Huffman[] ac = new Huffman[4];
        int[][] quant = new int[4][];
        int width = 0;
        int height = 0;
        int components = 0;
        int[] quantSelector = new int[3];
        boolean sawSof = false;
        while (source.position + 1 < bytes.length) {
            int marker = source.marker();
            if (marker == EOI) {
                break;
            }
            if (marker == SOS) {
                if (!sawSof) {
                    throw new IllegalArgumentException("JPEG SOS before SOF0");
                }
                int ls = source.u16();
                int ns = source.u8();
                if (ns != components) {
                    throw new IllegalArgumentException("JPEG scan component count must match SOF0");
                }
                int[] dcSel = new int[ns];
                int[] acSel = new int[ns];
                for (int index = 0; index < ns; index++) {
                    source.u8();
                    int sel = source.u8();
                    dcSel[index] = sel >>> 4;
                    acSel[index] = sel & 0x0F;
                }
                source.u8();
                source.u8();
                source.u8();
                if (ls != 6 + 2 * ns) {
                    throw new IllegalArgumentException("JPEG SOS length is invalid");
                }
                byte[] rgba = decodeScan(
                        source,
                        width,
                        height,
                        components,
                        quant,
                        quantSelector,
                        dc,
                        ac,
                        dcSel,
                        acSel
                );
                return new Decoded(width, height, rgba);
            }
            int length = source.u16();
            if (length < 2 || source.position + length - 2 > bytes.length) {
                throw new IllegalArgumentException("JPEG marker is truncated");
            }
            int payloadStart = source.position;
            int payloadEnd = payloadStart + length - 2;
            if (marker == SOF0) {
                int precision = source.u8();
                height = source.u16();
                width = source.u16();
                components = source.u8();
                if (precision != 8 || (components != 1 && components != 3)) {
                    throw new IllegalArgumentException("JPEG SOF0 must be 8-bit with 1 or 3 components");
                }
                checkedPixelCount(width, height);
                for (int index = 0; index < components; index++) {
                    source.u8();
                    int samp = source.u8();
                    if (samp != 0x11) {
                        throw new IllegalArgumentException("JPEG first-stable decode requires 4:4:4 sampling");
                    }
                    quantSelector[index] = source.u8();
                }
                sawSof = true;
            } else if (marker == DQT) {
                while (source.position < payloadEnd) {
                    int info = source.u8();
                    int id = info & 0x0F;
                    if ((info >>> 4) != 0 || id > 3) {
                        throw new IllegalArgumentException("JPEG DQT must be 8-bit");
                    }
                    int[] table = new int[64];
                    for (int index = 0; index < 64; index++) {
                        table[index] = source.u8();
                    }
                    quant[id] = table;
                }
            } else if (marker == DHT) {
                while (source.position < payloadEnd) {
                    int info = source.u8();
                    int cls = info >>> 4;
                    int id = info & 0x0F;
                    int[] bits = new int[16];
                    int count = 0;
                    for (int index = 0; index < 16; index++) {
                        bits[index] = source.u8();
                        count += bits[index];
                    }
                    int[] values = new int[count];
                    for (int index = 0; index < count; index++) {
                        values[index] = source.u8();
                    }
                    Huffman table = Huffman.fromSpec(bits, values);
                    if (cls == 0) {
                        dc[id] = table;
                    } else {
                        ac[id] = table;
                    }
                }
            } else {
                source.position = payloadEnd;
            }
        }
        throw new IllegalArgumentException("JPEG scan is missing");
    }

    /// Encodes one 8x8 block and returns the new DC predictor.
    private static int encodeBlock(
            BitSink sink,
            float[] plane,
            int stride,
            int bx,
            int by,
            int prevDc,
            Huffman dcTable,
            Huffman acTable
    ) {
        double[] spatial = new double[64];
        int index = 0;
        for (int y = 0; y < 8; y++) {
            int row = (by + y) * stride + bx;
            for (int x = 0; x < 8; x++) {
                spatial[index++] = plane[row + x] - 128.0;
            }
        }
        int[] coeff = new int[64];
        forwardDct(spatial, coeff);
        int dc = coeff[0];
        writeCoefficient(sink, dcTable, dc - prevDc);
        int run = 0;
        for (int zz = 1; zz < 64; zz++) {
            int value = coeff[UNZIGZAG[zz]];
            if (value == 0) {
                run++;
                continue;
            }
            while (run >= 16) {
                acTable.write(sink, 0xF0);
                run -= 16;
            }
            writeCoefficient(sink, acTable, value, run);
            run = 0;
        }
        if (run > 0) {
            acTable.write(sink, 0x00);
        }
        return dc;
    }

    /// Writes one Huffman-coded coefficient with optional AC run.
    private static void writeCoefficient(BitSink sink, Huffman table, int value) {
        writeCoefficient(sink, table, value, 0);
    }

    /// Writes one Huffman-coded coefficient.
    private static void writeCoefficient(BitSink sink, Huffman table, int value, int run) {
        int category = bitCategory(value);
        table.write(sink, (run << 4) | category);
        if (category > 0) {
            int bits = value >= 0 ? value : value + ((1 << category) - 1);
            sink.bits(bits, category);
        }
    }

    /// Decodes the entropy-coded scan into RGBA.
    private static byte[] decodeScan(
            BitSource source,
            int width,
            int height,
            int components,
            int[][] quant,
            int[] quantSelector,
            Huffman[] dcTables,
            Huffman[] acTables,
            int[] dcSel,
            int[] acSel
    ) {
        int paddedW = (width + 7) & ~7;
        int paddedH = (height + 7) & ~7;
        float[][] planes = new float[3][paddedW * paddedH];
        int[] prevDc = new int[3];
        source.beginEntropy();
        for (int by = 0; by < paddedH; by += 8) {
            for (int bx = 0; bx < paddedW; bx += 8) {
                for (int component = 0; component < components; component++) {
                    Huffman dc = dcTables[dcSel[component]];
                    Huffman ac = acTables[acSel[component]];
                    int[] q = quant[quantSelector[component]];
                    if (dc == null || ac == null || q == null) {
                        throw new IllegalArgumentException("JPEG Huffman or quantization table is missing");
                    }
                    prevDc[component] = decodeBlock(
                            source,
                            planes[component],
                            paddedW,
                            bx,
                            by,
                            prevDc[component],
                            dc,
                            ac,
                            q
                    );
                }
                if (components == 1) {
                    System.arraycopy(planes[0], by * paddedW + bx, planes[1], by * paddedW + bx, 8);
                    // chroma stays 128 after IDCT of zeros; fill later per pixel
                }
            }
        }
        byte[] rgba = new byte[width * height * 4];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int src = y * paddedW + x;
                int dest = (y * width + x) * 4;
                if (components == 1) {
                    int gray = clamp(Math.round(planes[0][src]));
                    rgba[dest] = (byte) gray;
                    rgba[dest + 1] = (byte) gray;
                    rgba[dest + 2] = (byte) gray;
                } else {
                    int[] rgb = ycbcrToRgb(planes[0][src], planes[1][src], planes[2][src]);
                    rgba[dest] = (byte) rgb[0];
                    rgba[dest + 1] = (byte) rgb[1];
                    rgba[dest + 2] = (byte) rgb[2];
                }
                rgba[dest + 3] = (byte) 255;
            }
        }
        return rgba;
    }

    /// Decodes one 8x8 block into `plane` and returns the new DC predictor.
    private static int decodeBlock(
            BitSource source,
            float[] plane,
            int stride,
            int bx,
            int by,
            int prevDc,
            Huffman dcTable,
            Huffman acTable,
            int[] quant
    ) {
        int[] coeff = new int[64];
        int dcCategory = dcTable.read(source);
        coeff[0] = prevDc + receiveExtend(source, dcCategory);
        int zz = 1;
        while (zz < 64) {
            int symbol = acTable.read(source);
            if (symbol == 0x00) {
                break;
            }
            if (symbol == 0xF0) {
                zz += 16;
                continue;
            }
            zz += symbol >>> 4;
            if (zz >= 64) {
                throw new IllegalArgumentException("JPEG AC run exceeds the block");
            }
            coeff[UNZIGZAG[zz]] = receiveExtend(source, symbol & 0x0F);
            zz++;
        }
        for (int index = 0; index < 64; index++) {
            coeff[index] *= Math.max(1, quant[ZIGZAG[index]]);
        }
        double[] spatial = new double[64];
        inverseDct(coeff, spatial);
        int pos = 0;
        for (int y = 0; y < 8; y++) {
            int row = (by + y) * stride + bx;
            for (int x = 0; x < 8; x++) {
                plane[row + x] = (float) (spatial[pos++] + 128.0);
            }
        }
        return coeff[0] / Math.max(1, quant[0]);
    }

    /// Samples RGBA into padded YCbCr planes, level-shifted around 128.
    private static void sampleYcbcr(
            int width,
            int height,
            byte[] rgba,
            int paddedW,
            float[] yPlane,
            float[] cbPlane,
            float[] crPlane
    ) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = (y * width + x) * 4;
                int dest = y * paddedW + x;
                float[] ycbcr = rgbToYcbcr(rgba[offset] & 0xFF, rgba[offset + 1] & 0xFF, rgba[offset + 2] & 0xFF);
                yPlane[dest] = ycbcr[0];
                cbPlane[dest] = ycbcr[1];
                crPlane[dest] = ycbcr[2];
            }
            for (int x = width; x < paddedW; x++) {
                int dest = y * paddedW + x;
                int last = y * paddedW + width - 1;
                yPlane[dest] = yPlane[last];
                cbPlane[dest] = cbPlane[last];
                crPlane[dest] = crPlane[last];
            }
        }
        int paddedH = yPlane.length / paddedW;
        for (int y = height; y < paddedH; y++) {
            System.arraycopy(yPlane, (height - 1) * paddedW, yPlane, y * paddedW, paddedW);
            System.arraycopy(cbPlane, (height - 1) * paddedW, cbPlane, y * paddedW, paddedW);
            System.arraycopy(crPlane, (height - 1) * paddedW, crPlane, y * paddedW, paddedW);
        }
    }

    /// Applies the separable 8x8 forward DCT and stores integer coefficients.
    private static void forwardDct(double[] spatial, int[] coeff) {
        double[] temp = new double[64];
        for (int y = 0; y < 8; y++) {
            for (int u = 0; u < 8; u++) {
                double sum = 0.0;
                for (int x = 0; x < 8; x++) {
                    sum += spatial[y * 8 + x] * COS[x][u];
                }
                temp[y * 8 + u] = sum * (u == 0 ? 1.0 / Math.sqrt(2.0) : 1.0);
            }
        }
        for (int u = 0; u < 8; u++) {
            for (int v = 0; v < 8; v++) {
                double sum = 0.0;
                for (int y = 0; y < 8; y++) {
                    sum += temp[y * 8 + u] * COS[y][v];
                }
                double scaled = sum * 0.25 * (v == 0 ? 1.0 / Math.sqrt(2.0) : 1.0);
                coeff[v * 8 + u] = (int) Math.round(scaled);
            }
        }
    }

    /// Applies the separable 8x8 inverse DCT.
    private static void inverseDct(int[] coeff, double[] spatial) {
        double[] temp = new double[64];
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                double sum = 0.0;
                for (int v = 0; v < 8; v++) {
                    double cv = v == 0 ? 1.0 / Math.sqrt(2.0) : 1.0;
                    sum += cv * coeff[v * 8 + x] * COS[y][v];
                }
                temp[y * 8 + x] = sum;
            }
        }
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                double sum = 0.0;
                for (int u = 0; u < 8; u++) {
                    double cu = u == 0 ? 1.0 / Math.sqrt(2.0) : 1.0;
                    sum += cu * temp[y * 8 + u] * COS[x][u];
                }
                spatial[y * 8 + x] = sum * 0.25;
            }
        }
    }

    /// Converts sRGB 8-bit to JPEG YCbCr.
    private static float[] rgbToYcbcr(int red, int green, int blue) {
        float y = 0.299f * red + 0.587f * green + 0.114f * blue;
        float cb = -0.168736f * red - 0.331264f * green + 0.5f * blue + 128.0f;
        float cr = 0.5f * red - 0.418688f * green - 0.081312f * blue + 128.0f;
        return new float[] {y, cb, cr};
    }

    /// Converts JPEG YCbCr to sRGB 8-bit.
    private static int[] ycbcrToRgb(float y, float cb, float cr) {
        float cbShift = cb - 128.0f;
        float crShift = cr - 128.0f;
        int red = clamp(Math.round(y + 1.402f * crShift));
        int green = clamp(Math.round(y - 0.344136f * cbShift - 0.714136f * crShift));
        int blue = clamp(Math.round(y + 1.772f * cbShift));
        return new int[] {red, green, blue};
    }

    /// Returns the JPEG magnitude category of `value`.
    private static int bitCategory(int value) {
        int abs = Math.abs(value);
        int category = 0;
        while (abs > 0) {
            abs >>= 1;
            category++;
        }
        return category;
    }

    /// Reads a JPEG receive-and-extend integer of `category` bits.
    private static int receiveExtend(BitSource source, int category) {
        if (category == 0) {
            return 0;
        }
        int bits = source.bits(category);
        int vt = 1 << (category - 1);
        if (bits < vt) {
            return bits + ((-1) << category) + 1;
        }
        return bits;
    }

    /// Writes a JFIF APP0 marker.
    private static void writeApp0(BitSink sink) {
        sink.marker(APP0);
        sink.u16(16);
        sink.u8('J');
        sink.u8('F');
        sink.u8('I');
        sink.u8('F');
        sink.u8(0);
        sink.u8(1);
        sink.u8(1);
        sink.u8(0);
        sink.u16(1);
        sink.u16(1);
        sink.u8(0);
        sink.u8(0);
    }

    /// Writes two quality-100 quantization tables.
    private static void writeDqt(BitSink sink) {
        sink.marker(DQT);
        sink.u16(2 + 2 * 65);
        for (int id = 0; id < 2; id++) {
            sink.u8(id);
            for (int index = 0; index < 64; index++) {
                sink.u8(1);
            }
        }
    }

    /// Writes a 3-component 4:4:4 SOF0 marker.
    private static void writeSof0(BitSink sink, int width, int height) {
        sink.marker(SOF0);
        sink.u16(17);
        sink.u8(8);
        sink.u16(height);
        sink.u16(width);
        sink.u8(3);
        sink.u8(1);
        sink.u8(0x11);
        sink.u8(0);
        sink.u8(2);
        sink.u8(0x11);
        sink.u8(1);
        sink.u8(3);
        sink.u8(0x11);
        sink.u8(1);
    }

    /// Writes one Huffman table marker.
    private static void writeDht(BitSink sink, int destination, int cls, int[] bits, int[] values) {
        sink.marker(DHT);
        sink.u16(2 + 1 + 16 + values.length);
        sink.u8((cls << 4) | destination);
        for (int bit : bits) {
            sink.u8(bit);
        }
        for (int value : values) {
            sink.u8(value);
        }
    }

    /// Writes a 3-component SOS marker.
    private static void writeSos(BitSink sink) {
        sink.marker(SOS);
        sink.u16(12);
        sink.u8(3);
        sink.u8(1);
        sink.u8(0x00);
        sink.u8(2);
        sink.u8(0x11);
        sink.u8(3);
        sink.u8(0x11);
        sink.u8(0);
        sink.u8(63);
        sink.u8(0);
    }

    /// Returns `width * height` after rejecting non-positive or oversized images.
    private static int checkedPixelCount(int width, int height) {
        if (width <= 0 || height <= 0 || width > PixelBuffer.MAX_EDGE || height > PixelBuffer.MAX_EDGE) {
            throw new IllegalArgumentException("JPEG dimensions must be in (0, " + PixelBuffer.MAX_EDGE + "]");
        }
        return Math.multiplyExact(width, height);
    }

    /// Builds the inverse zigzag table.
    private static int[] unzigzag() {
        int[] table = new int[64];
        for (int index = 0; index < 64; index++) {
            table[ZIGZAG[index]] = index;
        }
        return table;
    }

    /// Builds the 8-point DCT cosine table.
    private static double[][] cosineTable() {
        double[][] table = new double[8][8];
        for (int sample = 0; sample < 8; sample++) {
            for (int freq = 0; freq < 8; freq++) {
                table[sample][freq] = Math.cos((2 * sample + 1) * freq * Math.PI / 16.0);
            }
        }
        return table;
    }

    /// Clamps a component into `[0, 255]`.
    private static int clamp(int value) {
        return Math.clamp(value, 0, 255);
    }

    /// Stores one decoded JPEG image.
    ///
    /// @param width the pixel width
    /// @param height the pixel height
    /// @param rgba opaque RGBA8 pixels
    public record Decoded(int width, int height, byte @Unmodifiable [] rgba) {
        /// Validates the decoded image.
        public Decoded {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Decoded size must be positive");
            }
            Objects.requireNonNull(rgba, "rgba");
            if (rgba.length != Math.multiplyExact(width, height) * 4) {
                throw new IllegalArgumentException("RGBA length must match width * height * 4");
            }
            rgba = Arrays.copyOf(rgba, rgba.length);
        }
    }

    /// Canonical Huffman encoder and decoder.
    private static final class Huffman {
        /// Code for each symbol, or `-1`.
        private final int[] codes = new int[256];

        /// Bit length for each symbol, or `0`.
        private final int[] lengths = new int[256];

        /// Decode table mapping `(length, code)` lookups via a 16-bit max trie of symbols.
        private final int[] decodeSymbol = new int[1 << 16];

        /// Decode table of code lengths.
        private final int[] decodeLength = new int[1 << 16];

        /// Creates empty tables.
        private Huffman() {
            Arrays.fill(codes, -1);
            Arrays.fill(decodeSymbol, -1);
        }

        /// Builds a table from JPEG BITS/HUFFVAL.
        private static Huffman fromSpec(int[] bits, int[] values) {
            Huffman table = new Huffman();
            int code = 0;
            int valueIndex = 0;
            for (int length = 1; length <= 16; length++) {
                code <<= 1;
                int count = bits[length - 1];
                for (int index = 0; index < count; index++) {
                    int symbol = values[valueIndex++];
                    table.codes[symbol] = code;
                    table.lengths[symbol] = length;
                    int shift = 16 - length;
                    int base = code << shift;
                    int span = 1 << shift;
                    for (int fill = 0; fill < span; fill++) {
                        table.decodeSymbol[base + fill] = symbol;
                        table.decodeLength[base + fill] = length;
                    }
                    code++;
                }
            }
            return table;
        }

        /// Writes `symbol`.
        private void write(BitSink sink, int symbol) {
            int length = lengths[symbol];
            if (length == 0) {
                throw new IllegalStateException("JPEG Huffman symbol is missing: " + symbol);
            }
            sink.bits(codes[symbol], length);
        }

        /// Reads one symbol.
        private int read(BitSource source) {
            int acc = source.peek(16);
            int length = decodeLength[acc];
            if (length == 0) {
                throw new IllegalArgumentException("JPEG Huffman code is invalid");
            }
            source.consume(length);
            return decodeSymbol[acc];
        }
    }

    /// Writes JPEG markers and stuffed entropy bits.
    private static final class BitSink {
        /// Accumulated bytes.
        private byte[] data = new byte[256];

        /// Number of valid bytes in [`data`].
        private int size;

        /// Bit buffer.
        private int bitBuffer;

        /// Number of bits in [`bitBuffer`].
        private int bitCount;

        /// Writes a 16-bit marker.
        private void marker(int value) {
            flushBits();
            u8(value >>> 8);
            u8(value & 0xFF);
        }

        /// Writes one byte.
        private void u8(int value) {
            if (size == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = (byte) value;
        }

        /// Writes a 16-bit big-endian integer.
        private void u16(int value) {
            u8(value >>> 8);
            u8(value & 0xFF);
        }

        /// Writes `count` low bits of `value`.
        private void bits(int value, int count) {
            bitBuffer = (bitBuffer << count) | (value & ((1 << count) - 1));
            bitCount += count;
            while (bitCount >= 8) {
                bitCount -= 8;
                int octet = (bitBuffer >>> bitCount) & 0xFF;
                u8(octet);
                if (octet == 0xFF) {
                    u8(0);
                }
            }
        }

        /// Pads remaining bits with ones and emits the last byte.
        private void flushBits() {
            if (bitCount > 0) {
                bits((1 << (8 - bitCount)) - 1, 8 - bitCount);
            }
            bitBuffer = 0;
            bitCount = 0;
        }

        /// Returns the written bytes.
        private byte[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }

    /// Reads JPEG markers and stuffed entropy bits.
    private static final class BitSource {
        /// Input stream.
        private final byte[] data;

        /// Next unread byte.
        private int position;

        /// Bit buffer.
        private int bitBuffer;

        /// Number of bits in [`bitBuffer`].
        private int bitCount;

        /// Creates a reader.
        private BitSource(byte[] data) {
            this.data = data;
        }

        /// Reads one marker, skipping fill `0xFF` bytes.
        private int marker() {
            if (position >= data.length) {
                throw new IllegalArgumentException("JPEG stream is truncated");
            }
            if ((data[position] & 0xFF) != 0xFF) {
                throw new IllegalArgumentException("JPEG marker prefix is missing");
            }
            while (position < data.length && (data[position] & 0xFF) == 0xFF) {
                position++;
            }
            if (position >= data.length) {
                throw new IllegalArgumentException("JPEG marker is truncated");
            }
            return 0xFF00 | (data[position++] & 0xFF);
        }

        /// Reads one byte.
        private int u8() {
            if (position >= data.length) {
                throw new IllegalArgumentException("JPEG stream is truncated");
            }
            return data[position++] & 0xFF;
        }

        /// Reads a 16-bit big-endian integer.
        private int u16() {
            return (u8() << 8) | u8();
        }

        /// Clears the entropy bit buffer at SOS.
        private void beginEntropy() {
            bitBuffer = 0;
            bitCount = 0;
        }

        /// Returns `count` bits without consuming them.
        private int peek(int count) {
            while (bitCount < count) {
                int octet = nextEntropyByte();
                bitBuffer = (bitBuffer << 8) | octet;
                bitCount += 8;
            }
            return (bitBuffer >>> (bitCount - count)) & ((1 << count) - 1);
        }

        /// Consumes `count` previously peeked bits.
        private void consume(int count) {
            bitCount -= count;
            bitBuffer &= (1 << bitCount) - 1;
        }

        /// Reads `count` bits.
        private int bits(int count) {
            int value = peek(count);
            consume(count);
            return value;
        }

        /// Reads one stuffed entropy byte.
        private int nextEntropyByte() {
            if (position >= data.length) {
                return 0xFF;
            }
            int octet = data[position++] & 0xFF;
            if (octet == 0xFF) {
                if (position >= data.length) {
                    throw new IllegalArgumentException("JPEG stuffed byte is truncated");
                }
                int next = data[position++] & 0xFF;
                if (next != 0) {
                    position -= 2;
                    return 0xFF;
                }
            }
            return octet;
        }
    }
}
