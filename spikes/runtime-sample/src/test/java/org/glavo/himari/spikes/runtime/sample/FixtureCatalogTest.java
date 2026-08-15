package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the frozen fixture inventory, capability paths, and canonical suite encoding.
@NotNullByDefault
final class FixtureCatalogTest {
    /// Verifies the exact six-micro, six-integration, and one-realistic inventory.
    @Test
    void exposesUniqueFrozenInventory() {
        List<FixtureDefinition> fixtures = FixtureCatalog.fixtures();

        assertEquals(13, fixtures.size());
        assertEquals(13, new HashSet<>(fixtures.stream().map(FixtureDefinition::id).toList()).size());
        assertEquals(6, fixtures.stream().filter(fixture -> fixture.stage() == FixtureStage.MICRO).count());
        assertEquals(6, fixtures.stream().filter(fixture -> fixture.stage() == FixtureStage.INTEGRATION).count());
        assertEquals(1, fixtures.stream().filter(fixture -> fixture.stage() == FixtureStage.REALISTIC).count());
        assertTrue(fixtures.stream().allMatch(fixture -> fixture.steps().getFirst().command().operation().equals("mount")));
        assertTrue(fixtures.stream().allMatch(fixture -> !fixture.correctnessTags().isEmpty()));
    }

    /// Verifies that each ADR-020 strategy receives one command path and converges to the same keys.
    @Test
    void selectsViewportStrategyWithoutSharingStructure() {
        FixtureDefinition fixture = FixtureCatalog.fixture("viewport-materialization");
        CandidateCapabilities measureTime = new CandidateCapabilities(
                MeasureMaterializationMode.SCOPED_MEASURE_TIME,
                CancellationSupport.NONE,
                false
        );
        CandidateCapabilities previous = new CandidateCapabilities(
                MeasureMaterializationMode.PREVIOUS_VIEWPORT,
                CancellationSupport.NONE,
                false
        );

        List<FixtureStep> measureSteps = fixture.steps().stream()
                .filter(step -> step.appliesTo(measureTime))
                .toList();
        List<FixtureStep> previousSteps = fixture.steps().stream()
                .filter(step -> step.appliesTo(previous))
                .toList();
        assertFalse(measureSteps.stream().anyMatch(step -> step.id().startsWith("settle-")));
        assertTrue(previousSteps.stream().anyMatch(step -> step.id().startsWith("settle-scroll")));
        assertTrue(previousSteps.stream().anyMatch(step -> step.id().startsWith("settle-shrink")));
        assertEquals(
                measureSteps.getLast().expected().values().get("visible.keys"),
                previousSteps.getLast().expected().values().get("visible.keys")
        );
    }

    /// Verifies stable key ordering and inclusion of every required integration scenario.
    @Test
    void encodesCanonicalSuiteContract() {
        String first = ComparisonJson.suite();
        String second = ComparisonJson.suite();

        assertEquals(first, second);
        assertTrue(first.endsWith("\n"));
        assertTrue(first.contains("\"controlled-text-editing\""));
        assertTrue(first.contains("\"phase-failure-containment\""));
        assertTrue(first.contains("\"settings-chat-application\""));
        assertTrue(first.indexOf("\"fixtures\"") < first.indexOf("\"schemaVersion\""));
    }
}
