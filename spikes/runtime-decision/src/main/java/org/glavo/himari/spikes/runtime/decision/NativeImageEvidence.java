package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.DecisionRubric;
import org.glavo.himari.spikes.runtime.sample.FixtureCatalog;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/// Validates that the real candidate suite executed inside a GraalVM Native Image process.
@NotNullByDefault
final class NativeImageEvidence {
    /// The generated native execution result.
    private final Path resultsPath;

    /// The decoded immutable properties.
    private final @Unmodifiable Map<String, String> values;

    /// Creates and validates Native Image evidence for all three candidates.
    ///
    /// @param resultsPath the generated Native Image result file
    /// @throws IOException if the result cannot be read
    NativeImageEvidence(Path resultsPath) throws IOException {
        this.resultsPath = Objects.requireNonNull(resultsPath, "resultsPath").toAbsolutePath().normalize();
        values = DecisionProperties.read(this.resultsPath);
        requireEquals("schemaVersion", "1");
        requireEquals("suiteVersion", FixtureCatalog.VERSION);
        requireEquals("rubricVersion", DecisionRubric.VERSION);
        requireEquals("imageCode", "runtime");
        requireEquals("status", "passed");
        for (RuntimeDecisionCandidate candidate : RuntimeDecisionCandidate.values()) {
            String prefix = "candidate." + candidate.key() + '.';
            requireEquals(prefix + "id", candidate.candidateId());
            requireEquals(prefix + "status", "incomplete");
            requireEquals(prefix + "disqualifications", "0");
            requireEquals(prefix + "passedFixtures", Integer.toString(FixtureCatalog.fixtures().size()));
            requireEquals(prefix + "totalFixtures", Integer.toString(FixtureCatalog.fixtures().size()));
            String digest = DecisionProperties.require(values, prefix + "reportSha256");
            if (!digest.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("Invalid native candidate report digest: " + candidate.key());
            }
            Path reportPath = this.resultsPath.getParent().resolve(candidate.key() + "-report.json");
            if (!DecisionProperties.sha256(reportPath).equals(digest)) {
                throw new IllegalArgumentException("Native candidate report digest changed: " + candidate.key());
            }
        }
    }

    /// Returns the generated result path.
    ///
    /// @return the normalized absolute path
    Path resultsPath() {
        return resultsPath;
    }

    /// Returns the Native Image VM name recorded by the executable.
    ///
    /// @return the VM name
    String virtualMachineName() {
        return DecisionProperties.require(values, "vmName");
    }

    /// Requires one decoded property to match an expected value.
    ///
    /// @param key the property key
    /// @param expected the expected value
    private void requireEquals(String key, String expected) {
        String actual = DecisionProperties.require(values, key);
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException(
                    "Unexpected Native Image evidence property " + key
                            + ": expected " + expected + ", found " + actual
            );
        }
    }
}
