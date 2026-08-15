package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Defines one timeboxed decision checkpoint and its continuation rule.
///
/// @param id the stable checkpoint identifier
/// @param completedStage the stage that must have completed
/// @param continuationRule the fixed condition for continuing to the next checkpoint
@NotNullByDefault
public record RubricCheckpoint(String id, FixtureStage completedStage, String continuationRule) {
    /// Creates a validated checkpoint.
    public RubricCheckpoint {
        id = ComparisonContracts.requireIdentifier(id, "rubric checkpoint id");
        Objects.requireNonNull(completedStage, "completedStage");
        continuationRule = ComparisonContracts.requireText(continuationRule, "continuation rule");
    }
}
