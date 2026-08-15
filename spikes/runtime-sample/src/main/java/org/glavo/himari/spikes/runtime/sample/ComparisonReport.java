package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

/// Captures one complete, canonicalizable candidate run under the frozen M1 protocol.
///
/// @param schemaVersion the report schema version, currently one
/// @param suiteVersion the fixture suite version
/// @param rubricVersion the decision rubric version
/// @param status the top-level outcome
/// @param candidate the candidate descriptor
/// @param evidence external evidence known for the candidate
/// @param environment the process environment
/// @param sourceMetrics ordinary-Java source metrics
/// @param fixtures immutable results in catalog order
/// @param disqualifications immutable non-compensatory failures
@NotNullByDefault
public record ComparisonReport(
        int schemaVersion,
        String suiteVersion,
        String rubricVersion,
        ComparisonStatus status,
        CandidateDescriptor candidate,
        CandidateEvidence evidence,
        ComparisonEnvironmentRecord environment,
        SourceMetrics sourceMetrics,
        @Unmodifiable List<FixtureResult> fixtures,
        @Unmodifiable List<Disqualification> disqualifications
) {
    /// Creates an immutable validated report.
    public ComparisonReport {
        if (schemaVersion != 1) {
            throw new IllegalArgumentException("comparison report schemaVersion must be 1");
        }
        suiteVersion = ComparisonContracts.requireIdentifier(suiteVersion, "suiteVersion");
        rubricVersion = ComparisonContracts.requireIdentifier(rubricVersion, "rubricVersion");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(sourceMetrics, "sourceMetrics");
        Objects.requireNonNull(fixtures, "fixtures");
        fixtures = List.copyOf(fixtures);
        for (FixtureResult fixture : fixtures) {
            Objects.requireNonNull(fixture, "fixture result");
        }
        Objects.requireNonNull(disqualifications, "disqualifications");
        disqualifications = List.copyOf(disqualifications);
        for (Disqualification disqualification : disqualifications) {
            Objects.requireNonNull(disqualification, "disqualification");
        }
    }
}
