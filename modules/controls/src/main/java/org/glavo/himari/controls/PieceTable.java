package org.glavo.himari.controls;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.Objects;

/// Stores committed multiline text as an add-buffer piece table.
///
/// The original span is empty. Inserts append to the add buffer and record a piece. Deletes split
/// and drop pieces without rewriting surviving spans.
@NotNullByDefault
public final class PieceTable {
    /// One contiguous span in the add buffer.
    ///
    /// @param start the add-buffer origin
    /// @param length the span length
    public record Piece(int start, int length) {
        /// Creates a span.
        public Piece {
            if (start < 0 || length < 0) {
                throw new IllegalArgumentException("Piece span must be nonnegative");
            }
        }
    }

    /// Append-only character storage.
    private final StringBuilder add = new StringBuilder();

    /// Ordered pieces that reconstruct the document.
    private Piece[] pieces = new Piece[0];

    /// Creates an empty table.
    public PieceTable() {
    }

    /// Returns the logical character count.
    ///
    /// @return the length
    public int length() {
        int length = 0;
        for (Piece piece : pieces) {
            length += piece.length();
        }
        return length;
    }

    /// Returns the current piece count.
    ///
    /// @return the number of spans
    public int pieceCount() {
        return pieces.length;
    }

    /// Returns an immutable view of the current pieces.
    ///
    /// @return the pieces
    public @Unmodifiable Piece[] pieces() {
        return Arrays.copyOf(pieces, pieces.length);
    }

    /// Returns the logical text.
    ///
    /// @return a new string
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder(length());
        for (Piece piece : pieces) {
            out.append(add, piece.start(), piece.start() + piece.length());
        }
        return out.toString();
    }

    /// Replaces the entire logical text with one piece.
    ///
    /// @param value the next text
    public void assign(String value) {
        replace(0, length(), value);
    }

    /// Appends `value` as one piece.
    ///
    /// @param value the inserted text
    public void append(String value) {
        replace(length(), length(), value);
    }

    /// Replaces `[start, end)` with `replacement`.
    ///
    /// @param start the inclusive start
    /// @param end the exclusive end
    /// @param replacement the inserted text
    public void replace(int start, int end, String replacement) {
        Objects.requireNonNull(replacement, "replacement");
        int length = length();
        if (start < 0 || end < start || end > length) {
            throw new IllegalArgumentException("Range must lie within the table");
        }
        Piece[] prefix = splitPrefix(start);
        Piece[] suffix = splitSuffix(end);
        if (replacement.isEmpty()) {
            pieces = concat(prefix, suffix);
            return;
        }
        int origin = add.length();
        add.append(replacement);
        Piece inserted = new Piece(origin, replacement.length());
        pieces = concat(concat(prefix, new Piece[] {inserted}), suffix);
    }

    /// Returns pieces covering `[0, offset)`.
    private Piece[] splitPrefix(int offset) {
        if (offset == 0) {
            return new Piece[0];
        }
        Piece[] prefix = new Piece[pieces.length];
        int written = 0;
        int cursor = 0;
        for (Piece piece : pieces) {
            int next = cursor + piece.length();
            if (next <= offset) {
                prefix[written++] = piece;
                cursor = next;
                continue;
            }
            int keep = offset - cursor;
            if (keep > 0) {
                prefix[written++] = new Piece(piece.start(), keep);
            }
            break;
        }
        return written == prefix.length ? prefix : Arrays.copyOf(prefix, written);
    }

    /// Returns pieces covering `[offset, length)`.
    private Piece[] splitSuffix(int offset) {
        int length = length();
        if (offset == length) {
            return new Piece[0];
        }
        Piece[] suffix = new Piece[pieces.length];
        int written = 0;
        int cursor = 0;
        for (Piece piece : pieces) {
            int next = cursor + piece.length();
            if (next <= offset) {
                cursor = next;
                continue;
            }
            int skip = Math.max(0, offset - cursor);
            suffix[written++] = new Piece(piece.start() + skip, piece.length() - skip);
            cursor = next;
        }
        return written == suffix.length ? suffix : Arrays.copyOf(suffix, written);
    }

    /// Concatenates two piece arrays.
    private static Piece[] concat(Piece[] left, Piece[] right) {
        if (left.length == 0) {
            return right;
        }
        if (right.length == 0) {
            return left;
        }
        Piece[] out = Arrays.copyOf(left, left.length + right.length);
        System.arraycopy(right, 0, out, left.length, right.length);
        return out;
    }
}
