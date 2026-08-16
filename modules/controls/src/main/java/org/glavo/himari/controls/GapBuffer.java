package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores committed single-line text in a gap buffer.
///
/// Characters before [`#gapStart()`] and after the gap are the logical document. Inserts and
/// deletes move the gap to the edit point and grow it instead of shifting the whole array.
@NotNullByDefault
public final class GapBuffer {
    /// Backing storage. The unused gap occupies `[gapStart, gapEnd)`.
    private char[] data;

    /// First unused index.
    private int gapStart;

    /// First index after the gap.
    private int gapEnd;

    /// Creates an empty buffer with a 16-unit gap.
    public GapBuffer() {
        this.data = new char[16];
        this.gapStart = 0;
        this.gapEnd = 16;
    }

    /// Returns the logical character count.
    ///
    /// @return the length
    public int length() {
        return data.length - (gapEnd - gapStart);
    }

    /// Returns the gap origin, used by tests to prove the gap moved.
    ///
    /// @return the first unused index
    public int gapStart() {
        return gapStart;
    }

    /// Returns the logical text.
    ///
    /// @return a new string
    @Override
    public String toString() {
        int length = length();
        char[] out = new char[length];
        System.arraycopy(data, 0, out, 0, gapStart);
        System.arraycopy(data, gapEnd, out, gapStart, data.length - gapEnd);
        return new String(out);
    }

    /// Replaces the entire logical text.
    ///
    /// @param value the next text
    public void assign(String value) {
        replace(0, length(), value);
    }

    /// Replaces `[start, end)` with `replacement`.
    ///
    /// @param start the inclusive start
    /// @param end the exclusive end
    /// @param replacement the inserted text
    public void replace(int start, int end, String replacement) {
        Objects.requireNonNull(replacement, "replacement");
        if (start < 0 || end < start || end > length()) {
            throw new IllegalArgumentException("Range must lie within the buffer");
        }
        moveGap(start);
        gapEnd += end - start;
        ensureGap(replacement.length());
        replacement.getChars(0, replacement.length(), data, gapStart);
        gapStart += replacement.length();
    }

    /// Moves the gap so logical index `dest` sits at [`#gapStart()`].
    private void moveGap(int dest) {
        if (dest == gapStart) {
            return;
        }
        if (dest < gapStart) {
            int count = gapStart - dest;
            System.arraycopy(data, dest, data, gapEnd - count, count);
            gapStart = dest;
            gapEnd -= count;
            return;
        }
        int count = dest - gapStart;
        System.arraycopy(data, gapEnd, data, gapStart, count);
        gapStart += count;
        gapEnd += count;
    }

    /// Grows the unused gap to at least `needed` characters.
    private void ensureGap(int needed) {
        int gap = gapEnd - gapStart;
        if (gap >= needed) {
            return;
        }
        int extra = Math.max(needed - gap, data.length);
        char[] next = new char[data.length + extra];
        System.arraycopy(data, 0, next, 0, gapStart);
        int tail = data.length - gapEnd;
        System.arraycopy(data, gapEnd, next, next.length - tail, tail);
        gapEnd = next.length - tail;
        data = next;
    }
}
