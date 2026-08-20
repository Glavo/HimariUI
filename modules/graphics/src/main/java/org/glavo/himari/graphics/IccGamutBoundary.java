package org.glavo.himari.graphics;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores one bounded ICC `gbd ` gamut-boundary vertex list.
///
/// The accepted subset is a vertex count followed by packed s15.16 XYZ triples. Triangle
/// connectivity and iccMAX spectral shells are rejected. Coordinates are not clamped.
///
/// @param vertices packed XYZ triples, length a multiple of three
@NotNullByDefault
public record IccGamutBoundary(float @Unmodifiable [] vertices) {
    /// Type and tag `'gbd '`.
    public static final int SIGNATURE = 0x6762_6420;

    /// Maximum accepted vertices.
    public static final int MAX_VERTICES = 32;

    /// Header size before the first vertex.
    private static final int HEADER = 12;

    /// Validates the packed vertex list.
    public IccGamutBoundary {
        Objects.requireNonNull(vertices, "vertices");
        if (vertices.length % 3 != 0) {
            throw new IllegalArgumentException("ICC gbd vertices must be packed XYZ triples");
        }
        int count = vertices.length / 3;
        if (count <= 0 || count > MAX_VERTICES) {
            throw new IllegalArgumentException("ICC gbd vertex count is outside the accepted bounds");
        }
        for (float value : vertices) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("ICC gbd coordinates must be finite");
            }
        }
        vertices = Arrays.copyOf(vertices, vertices.length);
    }

    /// Parses one `gbd ` tag.
    ///
    /// @param bytes the profile bytes
    /// @param offset the tag start
    /// @param size the tag size
    /// @return the vertices
    public static IccGamutBoundary parse(byte[] bytes, int offset, int size) {
        Objects.requireNonNull(bytes, "bytes");
        if (offset < 0 || size < HEADER || offset > bytes.length - size) {
            throw new IllegalArgumentException("ICC gbd tag is outside the profile");
        }
        if (u32(bytes, offset) != SIGNATURE) {
            throw new IllegalArgumentException("ICC gbd tag is not gbd");
        }
        int count = u32(bytes, offset + 8);
        if (count <= 0 || count > MAX_VERTICES) {
            throw new IllegalArgumentException("ICC gbd vertex count is outside the accepted bounds");
        }
        int needed = HEADER + count * 12;
        if (size < needed) {
            throw new IllegalArgumentException("ICC gbd vertices exceed the tag");
        }
        float[] vertices = new float[count * 3];
        int cursor = offset + HEADER;
        for (int index = 0; index < vertices.length; index++) {
            vertices[index] = s15(bytes, cursor);
            if (!Float.isFinite(vertices[index])) {
                throw new IllegalArgumentException("ICC gbd coordinates must be finite");
            }
            cursor += 4;
        }
        return new IccGamutBoundary(vertices);
    }

    /// Returns the number of XYZ vertices.
    ///
    /// @return the count
    public int vertexCount() {
        return vertices.length / 3;
    }

    /// Returns the X coordinate of vertex `index`.
    ///
    /// @param index the vertex index
    /// @return the X coordinate
    public float x(int index) {
        return vertices[triple(index)];
    }

    /// Returns the Y coordinate of vertex `index`.
    ///
    /// @param index the vertex index
    /// @return the Y coordinate
    public float y(int index) {
        return vertices[triple(index) + 1];
    }

    /// Returns the Z coordinate of vertex `index`.
    ///
    /// @param index the vertex index
    /// @return the Z coordinate
    public float z(int index) {
        return vertices[triple(index) + 2];
    }

    /// Returns the packed offset of vertex `index`.
    private int triple(int index) {
        if (index < 0 || index >= vertexCount()) {
            throw new IllegalArgumentException("ICC gbd vertex index is out of range");
        }
        return index * 3;
    }

    /// Reads a big-endian unsigned 32-bit integer.
    private static int u32(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /// Reads a big-endian `s15Fixed16` number.
    private static float s15(byte[] bytes, int offset) {
        return u32(bytes, offset) / 65536.0f;
    }
}
