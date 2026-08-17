package org.glavo.himari.text;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Stores one wrapped line of already-shaped glyphs.
///
/// @param glyphs the glyphs on this line, in visual-left-to-right order
/// @param width the sum of glyph X-advances in font units
/// @param startCluster the first source cluster on the line
/// @param endClusterExclusive the first cluster after the line
/// @param bidiLevels resolved embedding levels in visual order, empty when the line is all LTR
/// @param indent leftover-width indent from [`LineAlignment#CENTER`] or [`LineAlignment#END`]
@NotNullByDefault
public record LaidLine(
        @Unmodifiable List<ShapedGlyph> glyphs,
        int width,
        int startCluster,
        int endClusterExclusive,
        int @Unmodifiable [] bidiLevels,
        int indent
) {
    /// Shared empty level array for LTR lines.
    private static final int[] EMPTY_LEVELS = new int[0];

    /// Validates the line.
    public LaidLine {
        Objects.requireNonNull(glyphs, "glyphs");
        Objects.requireNonNull(bidiLevels, "bidiLevels");
        if (width < 0 || startCluster < 0 || endClusterExclusive < startCluster || indent < 0) {
            throw new IllegalArgumentException("Laid line extents must be nonnegative and ordered");
        }
        if (bidiLevels.length != 0 && bidiLevels.length != glyphs.size()) {
            throw new IllegalArgumentException("Bidi levels must match the glyph count");
        }
        glyphs = List.copyOf(glyphs);
        bidiLevels = bidiLevels.length == 0 ? EMPTY_LEVELS : bidiLevels.clone();
    }

    /// Creates an all-LTR line.
    ///
    /// @param glyphs visual glyphs
    /// @param width the advance sum
    /// @param startCluster the first cluster
    /// @param endClusterExclusive the first cluster after the line
    public LaidLine(
            @Unmodifiable List<ShapedGlyph> glyphs,
            int width,
            int startCluster,
            int endClusterExclusive
    ) {
        this(glyphs, width, startCluster, endClusterExclusive, EMPTY_LEVELS, 0);
    }

    /// Creates a line with resolved bidi levels and no indent.
    ///
    /// @param glyphs visual glyphs
    /// @param width the advance sum
    /// @param startCluster the first cluster
    /// @param endClusterExclusive the first cluster after the line
    /// @param bidiLevels resolved embedding levels
    public LaidLine(
            @Unmodifiable List<ShapedGlyph> glyphs,
            int width,
            int startCluster,
            int endClusterExclusive,
            int @Unmodifiable [] bidiLevels
    ) {
        this(glyphs, width, startCluster, endClusterExclusive, bidiLevels, 0);
    }

    /// Returns this line with `indent` applied.
    ///
    /// @param indent the leftover-width indent
    /// @return the indented line
    public LaidLine withIndent(int indent) {
        return new LaidLine(glyphs, width, startCluster, endClusterExclusive, bidiLevels, indent);
    }

    /// Returns the X origin of the caret before `cluster`, in font units from the line start.
    ///
    /// An LTR glyph uses its visual left edge. An RTL glyph uses its visual right edge, so the
    /// insertion point sits on the logical-leading side of that glyph. Clusters before the line
    /// start use the first logical cluster. Clusters at or past [`#endClusterExclusive()`] use
    /// the trailing edge of the last logical cluster.
    ///
    /// @param cluster the source cluster
    /// @return the nonnegative caret X
    public int caretX(int cluster) {
        if (glyphs.isEmpty()) {
            return indent;
        }
        if (!hasRtl()) {
            if (cluster <= startCluster) {
                return indent;
            }
            int x = indent;
            for (ShapedGlyph glyph : glyphs) {
                if (glyph.cluster() >= cluster) {
                    return x;
                }
                x += glyph.xAdvance();
            }
            return indent + width;
        }
        int[] origins = visualOrigins();
        if (cluster <= startCluster) {
            return indent + edgeBefore(startCluster, origins);
        }
        if (cluster >= endClusterExclusive) {
            return indent + edgeAfter(endClusterExclusive - 1, origins);
        }
        return indent + edgeBefore(cluster, origins);
    }

    /// Returns the visual left edge of the selection covering `[fromCluster, toCluster)`.
    ///
    /// An empty range returns [`#caretX(int)`] of `fromCluster`.
    ///
    /// @param fromCluster the inclusive start cluster
    /// @param toCluster the exclusive end cluster
    /// @return the leftmost selected X
    public int selectionLeft(int fromCluster, int toCluster) {
        if (fromCluster >= toCluster || glyphs.isEmpty()) {
            return caretX(fromCluster);
        }
        int left = Integer.MAX_VALUE;
        int x = 0;
        for (int index = 0; index < glyphs.size(); index++) {
            ShapedGlyph glyph = glyphs.get(index);
            int cluster = glyph.cluster();
            if (cluster >= fromCluster && cluster < toCluster) {
                if (x < left) {
                    left = x;
                }
            }
            x += glyph.xAdvance();
        }
        return left == Integer.MAX_VALUE ? caretX(fromCluster) : indent + left;
    }

    /// Returns the visual width of the selection covering `[fromCluster, toCluster)`.
    ///
    /// @param fromCluster the inclusive start cluster
    /// @param toCluster the exclusive end cluster
    /// @return the nonnegative width
    public int selectionWidth(int fromCluster, int toCluster) {
        if (fromCluster >= toCluster || glyphs.isEmpty()) {
            return 0;
        }
        int left = Integer.MAX_VALUE;
        int right = 0;
        int x = 0;
        for (int index = 0; index < glyphs.size(); index++) {
            ShapedGlyph glyph = glyphs.get(index);
            int cluster = glyph.cluster();
            int next = x + glyph.xAdvance();
            if (cluster >= fromCluster && cluster < toCluster) {
                if (x < left) {
                    left = x;
                }
                if (next > right) {
                    right = next;
                }
            }
            x = next;
        }
        return left == Integer.MAX_VALUE ? 0 : right - left;
    }

    /// Returns the source cluster whose visual caret is nearest `x`.
    ///
    /// This is the inverse of [`#caretX(int)`] used for click-to-caret. An empty line
    /// returns [`#startCluster()`]. Distances that tie prefer the cluster whose caret
    /// sits at or left of `x`.
    ///
    /// @param x the font-unit X from the line start
    /// @return a cluster in `[startCluster, endClusterExclusive]`
    public int clusterAt(int x) {
        if (glyphs.isEmpty()) {
            return startCluster;
        }
        int bestCluster = startCluster;
        int bestDistance = Math.abs(caretX(startCluster) - x);
        for (int cluster = startCluster + 1; cluster <= endClusterExclusive; cluster++) {
            int distance = Math.abs(caretX(cluster) - x);
            if (distance < bestDistance || (distance == bestDistance && caretX(cluster) <= x)) {
                bestCluster = cluster;
                bestDistance = distance;
            }
        }
        return bestCluster;
    }

    /// Returns whether any stored level is RTL.
    private boolean hasRtl() {
        for (int index = 0; index < bidiLevels.length; index++) {
            if (bidiLevels[index] == BidiOrder.RTL) {
                return true;
            }
        }
        return false;
    }

    /// Returns visual X origins for each glyph.
    private int[] visualOrigins() {
        int[] origins = new int[glyphs.size()];
        int x = 0;
        for (int index = 0; index < glyphs.size(); index++) {
            origins[index] = x;
            x += glyphs.get(index).xAdvance();
        }
        return origins;
    }

    /// Returns the visual edge used as the caret before `cluster`.
    private int edgeBefore(int cluster, int[] origins) {
        int index = indexOfCluster(cluster);
        if (index < 0) {
            return width;
        }
        if (levelAt(index) == BidiOrder.RTL) {
            return origins[index] + glyphs.get(index).xAdvance();
        }
        return origins[index];
    }

    /// Returns the visual edge used as the caret after `cluster`.
    private int edgeAfter(int cluster, int[] origins) {
        int index = indexOfCluster(cluster);
        if (index < 0) {
            return width;
        }
        if (levelAt(index) == BidiOrder.RTL) {
            return origins[index];
        }
        return origins[index] + glyphs.get(index).xAdvance();
    }

    /// Returns the visual index of `cluster`, or `-1`.
    private int indexOfCluster(int cluster) {
        for (int index = 0; index < glyphs.size(); index++) {
            if (glyphs.get(index).cluster() == cluster) {
                return index;
            }
        }
        return -1;
    }

    /// Returns the resolved level of the visual glyph at `index`.
    private int levelAt(int index) {
        if (index < 0 || index >= bidiLevels.length) {
            return BidiOrder.LTR;
        }
        return bidiLevels[index];
    }
}
