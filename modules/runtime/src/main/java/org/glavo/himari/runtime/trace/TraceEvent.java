package org.glavo.himari.runtime.trace;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Stores one pointer-free deterministic runtime-trace record.
///
/// @param sequence the nonnegative monotonically increasing trace-local identity
/// @param timestampNanos the nonnegative sampled clock value
/// @param kind the record kind
/// @param ownerPath the deterministic owner path
/// @param detail a stable, pointer-free diagnostic payload
@NotNullByDefault
public record TraceEvent(
        long sequence,
        long timestampNanos,
        TraceEventKind kind,
        String ownerPath,
        String detail
) {
    /// Validates one trace record.
    public TraceEvent {
        if (sequence < 0L || timestampNanos < 0L) {
            throw new IllegalArgumentException("Trace sequence and timestamp must be nonnegative");
        }
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(ownerPath, "ownerPath");
        if (ownerPath.isBlank()) {
            throw new IllegalArgumentException("ownerPath must not be blank");
        }
        Objects.requireNonNull(detail, "detail");
    }
}
