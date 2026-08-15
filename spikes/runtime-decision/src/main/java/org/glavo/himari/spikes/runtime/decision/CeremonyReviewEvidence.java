package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.DecisionRubric;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Objects;

/// Validates the checked blinded ceremony review against freshly generated packet hashes.
@NotNullByDefault
final class CeremonyReviewEvidence {
    /// The checked review file.
    private final Path reviewPath;

    /// The generated packet manifest.
    private final Path manifestPath;

    /// The number of named reviewers.
    private final int reviewerCount;

    /// Creates validated ceremony evidence.
    ///
    /// @param reviewPath the checked review file
    /// @param manifestPath the regenerated packet manifest
    /// @throws IOException if either evidence file cannot be read
    CeremonyReviewEvidence(Path reviewPath, Path manifestPath) throws IOException {
        this.reviewPath = Objects.requireNonNull(reviewPath, "reviewPath").toAbsolutePath().normalize();
        this.manifestPath = Objects.requireNonNull(manifestPath, "manifestPath").toAbsolutePath().normalize();
        Map<String, String> review = DecisionProperties.read(this.reviewPath);
        Map<String, String> manifest = DecisionProperties.read(this.manifestPath);
        requireEquals(review, "schemaVersion", "1");
        requireEquals(review, "rubricVersion", DecisionRubric.VERSION);
        requireEquals(review, "packetVersion", CeremonyPacketGenerator.PACKET_VERSION);
        requireEquals(review, "reviewMode", "blinded-complete-micro-source");
        requireEquals(manifest, "schemaVersion", "1");
        requireEquals(manifest, "rubricVersion", DecisionRubric.VERSION);
        requireEquals(manifest, "packetVersion", CeremonyPacketGenerator.PACKET_VERSION);
        validateDate(DecisionProperties.require(review, "reviewedOn"));

        long count = DecisionProperties.requireNonNegativeLong(review, "reviewerCount");
        if (count < 1L || count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Ceremony review must name at least one reviewer");
        }
        reviewerCount = Math.toIntExact(count);
        for (int index = 1; index <= reviewerCount; index++) {
            DecisionProperties.require(review, "reviewer." + index);
        }

        boolean mandatoryThreePersonReview = false;
        for (RuntimeDecisionCandidate candidate : RuntimeDecisionCandidate.values()) {
            String prefix = "candidate." + candidate.reviewLabel() + '.';
            boolean mandatory = DecisionProperties.requireBoolean(manifest, prefix + "mandatoryThreePersonReview");
            mandatoryThreePersonReview |= mandatory;
            requireEquals(
                    review,
                    prefix + "packetSha256",
                    DecisionProperties.require(manifest, prefix + "packetSha256")
            );
            requireEquals(
                    review,
                    prefix + "microSourceLines",
                    DecisionProperties.require(manifest, prefix + "microSourceLines")
            );
            requireEquals(
                    review,
                    prefix + "accidentalMarkers",
                    DecisionProperties.require(manifest, prefix + "accidentalMarkers")
            );
            requireEquals(review, prefix + "status", "passed");
            if (DecisionProperties.requireBoolean(review, prefix + "pervasiveAccidentalCeremony")) {
                throw new IllegalArgumentException("Ceremony review classified candidate "
                        + candidate.reviewLabel() + " as pervasive");
            }
            DecisionProperties.require(review, prefix + "rationale");
        }
        if (mandatoryThreePersonReview && reviewerCount < 3) {
            throw new IllegalArgumentException("The frozen threshold requires three ceremony reviewers");
        }
    }

    /// Returns the checked review path.
    ///
    /// @return the normalized absolute review path
    Path reviewPath() {
        return reviewPath;
    }

    /// Returns the generated packet manifest path.
    ///
    /// @return the normalized absolute manifest path
    Path manifestPath() {
        return manifestPath;
    }

    /// Returns the number of reviewers whose identities were recorded.
    ///
    /// @return the positive reviewer count
    int reviewerCount() {
        return reviewerCount;
    }

    /// Requires one property to equal an expected value.
    ///
    /// @param values the property map
    /// @param key the property key
    /// @param expected the required value
    private static void requireEquals(Map<String, String> values, String key, String expected) {
        String actual = DecisionProperties.require(values, key);
        if (!actual.equals(expected)) {
            throw new IllegalArgumentException(
                    "Unexpected evidence property " + key + ": expected " + expected + ", found " + actual
            );
        }
    }

    /// Validates an ISO local date without imposing a wall-clock dependency on reproduction.
    ///
    /// @param value the date text
    private static void validateDate(String value) {
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Ceremony review date is not an ISO local date", exception);
        }
    }
}
