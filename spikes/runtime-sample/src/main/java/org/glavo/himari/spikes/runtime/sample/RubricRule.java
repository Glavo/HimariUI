package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Defines one frozen correctness or evidence rule.
///
/// @param id the stable rule identifier
/// @param category the stable rule category
/// @param description the condition and required outcome
/// @param disqualifying whether violating the rule removes a candidate from scoring
@NotNullByDefault
public record RubricRule(String id, String category, String description, boolean disqualifying) {
    /// Creates a validated rule.
    public RubricRule {
        id = ComparisonContracts.requireIdentifier(id, "rubric rule id");
        category = ComparisonContracts.requireIdentifier(category, "rubric rule category");
        description = ComparisonContracts.requireText(description, "rubric rule description");
    }
}
