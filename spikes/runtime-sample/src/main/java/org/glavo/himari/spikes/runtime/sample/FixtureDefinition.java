package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Defines one frozen comparison fixture independently of any structural runtime representation.
///
/// @param id the stable fixture identifier
/// @param stage the decision checkpoint containing this fixture
/// @param description the observable behavior exercised by the fixture
/// @param correctnessTags immutable stable requirement identifiers
/// @param steps the ordered command and observation contract, beginning with `mount`
/// @param benchmark the steady-state cycle, or `null` for correctness-only fixtures
@NotNullByDefault
public record FixtureDefinition(
        String id,
        FixtureStage stage,
        String description,
        @Unmodifiable Set<String> correctnessTags,
        @Unmodifiable List<FixtureStep> steps,
        @Nullable BenchmarkPlan benchmark
) {
    /// Creates a validated immutable fixture definition.
    public FixtureDefinition {
        id = ComparisonContracts.requireIdentifier(id, "fixture id");
        Objects.requireNonNull(stage, "stage");
        description = ComparisonContracts.requireText(description, "fixture description");
        Objects.requireNonNull(correctnessTags, "correctnessTags");
        HashSet<String> tagCopy = new HashSet<>();
        for (String tag : correctnessTags) {
            tagCopy.add(ComparisonContracts.requireIdentifier(tag, "correctness tag"));
        }
        correctnessTags = Set.copyOf(tagCopy);
        Objects.requireNonNull(steps, "steps");
        steps = List.copyOf(steps);
        if (steps.isEmpty() || !steps.getFirst().command().operation().equals("mount")) {
            throw new IllegalArgumentException("fixture steps must begin with a mount command");
        }
        HashSet<String> stepIds = new HashSet<>();
        for (FixtureStep step : steps) {
            Objects.requireNonNull(step, "fixture step");
            if (!stepIds.add(step.id())) {
                throw new IllegalArgumentException("fixture repeats step id " + step.id());
            }
        }
    }
}
