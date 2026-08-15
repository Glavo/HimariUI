package org.glavo.himari.spikes.runtime.decision;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Verifies the reproducible blinded ceremony packet and checked-review chain.
@NotNullByDefault
final class CeremonyEvidenceTest {
    /// Generates fresh packets, verifies redaction, and validates the checked review hashes.
    ///
    /// @param temporaryDirectory the isolated packet directory
    /// @throws IOException if source or evidence files cannot be read or written
    @Test
    void regeneratesReviewedBlindedPackets(@TempDir Path temporaryDirectory) throws IOException {
        Path repositoryRoot = Path.of(System.getProperty("himari.repository.root")).toAbsolutePath().normalize();
        CeremonyPacketGenerator.generate(repositoryRoot, temporaryDirectory);
        Map<String, String> manifest = DecisionProperties.read(temporaryDirectory.resolve("manifest.properties"));
        assertEquals("23", manifest.get("candidate.A.accidentalMarkers"));
        assertEquals("34", manifest.get("candidate.B.accidentalMarkers"));
        assertEquals("37", manifest.get("candidate.C.accidentalMarkers"));
        for (RuntimeDecisionCandidate candidate : RuntimeDecisionCandidate.values()) {
            String label = candidate.reviewLabel().toLowerCase(java.util.Locale.ROOT);
            String packet = Files.readString(
                    temporaryDirectory.resolve("candidate-" + label + ".txt"),
                    StandardCharsets.UTF_8
            );
            assertFalse(packet.toLowerCase(java.util.Locale.ROOT)
                    .contains(candidate.candidateId().toLowerCase(java.util.Locale.ROOT)));
            assertEquals("false", manifest.get(
                    "candidate." + candidate.reviewLabel() + ".mandatoryThreePersonReview"
            ));
        }
        CeremonyReviewEvidence review = new CeremonyReviewEvidence(
                repositoryRoot.resolve("evidence/m1-runtime-decision/ceremony-review.properties"),
                temporaryDirectory.resolve("manifest.properties")
        );
        assertEquals(1, review.reviewerCount());
    }
}
