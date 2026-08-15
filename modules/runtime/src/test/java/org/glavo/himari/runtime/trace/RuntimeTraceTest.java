package org.glavo.himari.runtime.trace;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies canonical encoding and parse round-trip of the runtime trace.
@NotNullByDefault
final class RuntimeTraceTest {
    /// Verifies that two identical recordings encode to the same JSON and parse back.
    @Test
    void canonicalJsonRoundTrips() {
        RuntimeTrace first = new RuntimeTrace();
        first.record(10L, TraceEventKind.STATE_EPOCH, "root", "epoch=1");
        first.record(20L, TraceEventKind.STRUCTURE_ATTEMPT, "root", "COMMITTED:1");
        String json = first.toCanonicalJson();
        RuntimeTrace second = RuntimeTrace.parse(json);
        assertEquals(json, second.toCanonicalJson());
        assertEquals(first.events(), second.events());
        assertEquals(
                "{\"schema\":\"himari-runtime-trace-v1\",\"events\":["
                        + "{\"sequence\":0,\"timestampNanos\":10,\"kind\":\"STATE_EPOCH\","
                        + "\"ownerPath\":\"root\",\"detail\":\"epoch=1\"},"
                        + "{\"sequence\":1,\"timestampNanos\":20,\"kind\":\"STRUCTURE_ATTEMPT\","
                        + "\"ownerPath\":\"root\",\"detail\":\"COMMITTED:1\"}]}",
                json
        );
    }
}
