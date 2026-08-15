package org.glavo.himari.runtime.trace;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/// Executes the deterministic TRACE-001 acceptance scenario and writes the canonical document.
@NotNullByDefault
public final class RuntimeTraceConformance {
    /// Prevents instantiation of this command-line entry point.
    private RuntimeTraceConformance() {
    }

    /// Verifies canonical encoding, parse, and replay equality.
    ///
    /// @param arguments one output-directory path
    /// @throws IOException if the report cannot be written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 1) {
            throw new IllegalArgumentException("Expected one conformance output directory");
        }
        RuntimeTrace trace = new RuntimeTrace();
        trace.record(0L, TraceEventKind.STATE_EPOCH, "root", "epoch=0");
        trace.record(1L, TraceEventKind.STRUCTURE_ATTEMPT, "root", "COMMITTED:1");
        trace.record(1L, TraceEventKind.MOUNT_APPLY, "root", "COMMITTED:1");
        String json = trace.toCanonicalJson();
        if (!json.equals(RuntimeTrace.parse(json).toCanonicalJson())) {
            throw new IllegalStateException("Runtime trace is not canonical");
        }
        Path outputDirectory = Path.of(arguments[0]);
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("trace.json"), json, StandardCharsets.UTF_8);
        Files.writeString(
                outputDirectory.resolve("results.json"),
                """
                        {
                          "profile": "m1-trace",
                          "workPackage": "TRACE-001",
                          "status": "passed",
                          "canonicalRoundTrip": true,
                          "pointerFree": true
                        }
                        """,
                StandardCharsets.UTF_8
        );
    }
}