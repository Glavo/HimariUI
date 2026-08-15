package org.glavo.himari.spikes.runtime.grouped;

import org.glavo.himari.spikes.runtime.sample.CancellationSupport;
import org.glavo.himari.spikes.runtime.sample.CandidateCapabilities;
import org.glavo.himari.spikes.runtime.sample.CandidateDescriptor;
import org.glavo.himari.spikes.runtime.sample.CandidateEvidence;
import org.glavo.himari.spikes.runtime.sample.ComparisonEnvironment;
import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.glavo.himari.spikes.runtime.sample.EvidenceStatus;
import org.glavo.himari.spikes.runtime.sample.FixtureDefinition;
import org.glavo.himari.spikes.runtime.sample.FixtureStage;
import org.glavo.himari.spikes.runtime.sample.MeasureMaterializationMode;
import org.glavo.himari.spikes.runtime.sample.RuntimeCandidate;
import org.glavo.himari.spikes.runtime.sample.RuntimeFixtureSession;
import org.glavo.himari.spikes.runtime.sample.SourceCeremonyKind;
import org.glavo.himari.spikes.runtime.sample.SourceCorpus;
import org.glavo.himari.spikes.runtime.sample.SourceMarker;
import org.glavo.himari.spikes.runtime.sample.SourceUnit;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Adapts the independently implemented explicit grouped runtime to the frozen comparison suite.
@NotNullByDefault
public final class GroupedRuntimeCandidate implements RuntimeCandidate {
    /// The ordinary-Java micro application source.
    private static final String MICRO_SOURCE =
            "spikes/runtime-grouped/src/main/java/org/glavo/himari/spikes/runtime/grouped/GroupedMicroApplications.java";

    /// The ordinary-Java integration application source.
    private static final String INTEGRATION_SOURCE =
            "spikes/runtime-grouped/src/main/java/org/glavo/himari/spikes/runtime/grouped/GroupedIntegrationApplications.java";

    /// The ordinary-Java realistic application source.
    private static final String REALISTIC_SOURCE =
            "spikes/runtime-grouped/src/main/java/org/glavo/himari/spikes/runtime/grouped/GroupedSettingsChatApplication.java";

    /// The stable candidate descriptor.
    private static final CandidateDescriptor DESCRIPTOR = new CandidateDescriptor(
            "explicit-grouped-recomposition",
            "Explicit grouped recomposition",
            "Handwritten rerunnable groups with positional memory, semantic keyed reconciliation, and draft commit",
            new CandidateCapabilities(
                    MeasureMaterializationMode.SCOPED_MEASURE_TIME,
                    CancellationSupport.COOPERATIVE,
                    false
            ),
            false,
            true
    );

    /// External evidence intentionally left incomplete until its independent review steps run.
    private static final CandidateEvidence EVIDENCE = new CandidateEvidence(
            EvidenceStatus.NOT_RUN,
            EvidenceStatus.NOT_APPLICABLE,
            EvidenceStatus.NOT_RUN,
            Map.of()
    );

    /// The absolute repository root used for auditable source measurement.
    private final Path repositoryRoot;

    /// Creates the grouped candidate adapter.
    ///
    /// @param repositoryRoot the repository root
    public GroupedRuntimeCandidate(Path repositoryRoot) {
        this.repositoryRoot = Objects.requireNonNull(repositoryRoot, "repositoryRoot").toAbsolutePath().normalize();
    }

    /// Returns the stable explicit-grouped descriptor.
    ///
    /// @return the descriptor
    @Override
    public CandidateDescriptor descriptor() {
        return DESCRIPTOR;
    }

    /// Returns deliberately incomplete external evidence.
    ///
    /// @return the evidence snapshot
    @Override
    public CandidateEvidence evidence() {
        return EVIDENCE;
    }

    /// Returns ordinary-Java application files and reviewable ceremony locations.
    ///
    /// Runtime implementation and neutral adapter plumbing are intentionally excluded.
    ///
    /// @return the source corpus
    @Override
    public SourceCorpus sourceCorpus() {
        return new SourceCorpus(
                repositoryRoot,
                List.of(
                        new SourceUnit(MICRO_SOURCE, FixtureStage.MICRO),
                        new SourceUnit(INTEGRATION_SOURCE, FixtureStage.INTEGRATION),
                        new SourceUnit(REALISTIC_SOURCE, FixtureStage.REALISTIC)
                ),
                markers()
        );
    }

    /// Opens the independently implemented application for one frozen fixture.
    ///
    /// @param fixture the fixture definition
    /// @param environment the fresh environment
    /// @param probe the shared probe
    /// @return the grouped application session
    @Override
    public RuntimeFixtureSession open(
            FixtureDefinition fixture,
            ComparisonEnvironment environment,
            ComparisonProbe probe
    ) {
        Objects.requireNonNull(fixture, "fixture");
        @Nullable GroupedFixtureSession micro = GroupedMicroApplications.open(fixture.id(), environment, probe);
        if (micro != null) {
            return micro;
        }
        @Nullable GroupedFixtureSession integration =
                GroupedIntegrationApplications.open(fixture.id(), environment, probe);
        if (integration != null) {
            return integration;
        }
        if (fixture.id().equals("settings-chat-application")) {
            return new GroupedSettingsChatApplication(environment, probe);
        }
        throw new IllegalArgumentException("Unsupported grouped fixture: " + fixture.id());
    }

    /// Builds the auditable ceremony-marker list from unique application-source fragments.
    ///
    /// @return the immutable markers
    private List<SourceMarker> markers() {
        ArrayList<SourceMarker> markers = new ArrayList<>();
        for (String source : List.of(MICRO_SOURCE, INTEGRATION_SOURCE, REALISTIC_SOURCE)) {
            addEvery(markers, source, "Map<String, GroupedRuntime.LocalInt>",
                    SourceCeremonyKind.GENERIC_TYPE_NOISE,
                    "The application must spell a framework-local value type inside its reconciliation map.");
            addEvery(markers, source, "scope.group(", SourceCeremonyKind.GROUP_BOUNDARY,
                    "A handwritten restart boundary is required by the grouped runtime API.");
            addEvery(markers, source, "scope.group(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "The group body must be wrapped in a runtime callback.");
            addEvery(markers, source, "scope.keyedGroup(", SourceCeremonyKind.EXPLICIT_KEY,
                    "A collection supplies an application semantic identity key.");
            addEvery(markers, source, "scope.keyedGroup(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "The keyed item body must be wrapped in a runtime callback.");
            addEvery(markers, source, "scope.branch(", SourceCeremonyKind.STRUCTURAL_CONTROL,
                    "The application explicitly declares conditional topology and its memory policy.");
            addEvery(markers, source, "scope.branch(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "The conditional body must be wrapped in a runtime callback.");
            addEvery(markers, source, "scope.effect(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "Effect mount and cleanup require runtime callback wrappers.");
            addEvery(markers, source, "scope.onCommit(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "Draft-local references require a post-commit callback wrapper.");
        }
        addEvery(markers, INTEGRATION_SOURCE, "scope.onAbort(", SourceCeremonyKind.CALLBACK_WRAPPER,
                "A staged resource requires an abort-cleanup callback wrapper.");
        addEvery(markers, MICRO_SOURCE, "for (String key : keys)", SourceCeremonyKind.STRUCTURAL_CONTROL,
                "The loop controls the keyed list topology.");
        addEvery(markers, INTEGRATION_SOURCE, "for (String key : nextKeys)", SourceCeremonyKind.STRUCTURAL_CONTROL,
                "The loop controls measure-time viewport topology.");
        addEvery(markers, INTEGRATION_SOURCE, "if (currentFallback == null)", SourceCeremonyKind.STRUCTURAL_CONTROL,
                "The condition selects content or fallback topology.");
        addEvery(markers, REALISTIC_SOURCE, "for (Message message : visible)",
                SourceCeremonyKind.STRUCTURAL_CONTROL,
                "The loop controls visible keyed message topology.");
        addEvery(markers, REALISTIC_SOURCE, "if (filter.get().equals(\"all\"))",
                SourceCeremonyKind.STRUCTURAL_CONTROL,
                "The filter selects the message collection topology.");
        return List.copyOf(markers);
    }

    /// Adds a marker for every physical source line containing one reviewed fragment.
    ///
    /// @param markers the mutable marker list
    /// @param relativePath the source path
    /// @param fragment the reviewed significant-line fragment
    /// @param kind the ceremony category
    /// @param rationale the review rationale
    private void addEvery(
            List<SourceMarker> markers,
            String relativePath,
            String fragment,
            SourceCeremonyKind kind,
            String rationale
    ) {
        List<String> lines;
        try {
            lines = Files.readAllLines(repositoryRoot.resolve(relativePath), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot inspect grouped source " + relativePath, exception);
        }
        int matches = 0;
        for (int index = 0; index < lines.size(); index++) {
            if (lines.get(index).contains(fragment)) {
                markers.add(new SourceMarker(relativePath, index + 1, kind, rationale));
                matches++;
            }
        }
        if (matches == 0) {
            throw new IllegalArgumentException("Ceremony source fragment is absent: " + fragment);
        }
    }
}
