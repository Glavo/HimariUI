package org.glavo.himari.spikes.runtime.hybrid;

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

/// Adapts the independently implemented hybrid owner runtime to the frozen comparison suite.
@NotNullByDefault
public final class HybridRuntimeCandidate implements RuntimeCandidate {
    /// The ordinary-Java micro application source.
    private static final String MICRO_SOURCE =
            "spikes/runtime-hybrid/src/main/java/org/glavo/himari/spikes/runtime/hybrid/HybridMicroApplications.java";

    /// The ordinary-Java integration application source.
    private static final String INTEGRATION_SOURCE =
            "spikes/runtime-hybrid/src/main/java/org/glavo/himari/spikes/runtime/hybrid/HybridIntegrationApplications.java";

    /// The ordinary-Java realistic application source.
    private static final String REALISTIC_SOURCE =
            "spikes/runtime-hybrid/src/main/java/org/glavo/himari/spikes/runtime/hybrid/HybridSettingsChatApplication.java";

    /// The stable candidate descriptor.
    private static final CandidateDescriptor DESCRIPTOR = new CandidateDescriptor(
            "fine-grained-structural-scopes",
            "Fine-grained structural scopes",
            "Initialize-once owners with direct property bindings and explicit rerunnable scopes for topology only",
            new CandidateCapabilities(
                    MeasureMaterializationMode.SCOPED_MEASURE_TIME,
                    CancellationSupport.NONE,
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

    /// Creates the hybrid candidate adapter.
    ///
    /// @param repositoryRoot the repository root
    public HybridRuntimeCandidate(Path repositoryRoot) {
        this.repositoryRoot = Objects.requireNonNull(repositoryRoot, "repositoryRoot").toAbsolutePath().normalize();
    }

    /// Returns the stable hybrid owner descriptor.
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
    /// @return the hybrid application session
    @Override
    public RuntimeFixtureSession open(
            FixtureDefinition fixture,
            ComparisonEnvironment environment,
            ComparisonProbe probe
    ) {
        Objects.requireNonNull(fixture, "fixture");
        @Nullable HybridFixtureSession micro = HybridMicroApplications.open(fixture.id(), environment, probe);
        if (micro != null) {
            return micro;
        }
        @Nullable HybridFixtureSession integration =
                HybridIntegrationApplications.open(fixture.id(), environment, probe);
        if (integration != null) {
            return integration;
        }
        if (fixture.id().equals("settings-chat-application")) {
            return new HybridSettingsChatApplication(environment, probe);
        }
        throw new IllegalArgumentException("Unsupported hybrid fixture: " + fixture.id());
    }

    /// Builds the auditable ceremony-marker list from application-source fragments.
    ///
    /// @return the immutable markers
    private List<SourceMarker> markers() {
        ArrayList<SourceMarker> markers = new ArrayList<>();
        for (String source : List.of(MICRO_SOURCE, INTEGRATION_SOURCE, REALISTIC_SOURCE)) {
            addEvery(markers, source, "HybridRuntime.StructuralScope<",
                    SourceCeremonyKind.GENERIC_TYPE_NOISE,
                    "The application must spell the framework structural-scope key and value types.");
            addEvery(markers, source, ".component(", SourceCeremonyKind.GROUP_BOUNDARY,
                    "A runtime-owned component boundary wraps an initialize-once callback.");
            addEvery(markers, source, ".component(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "The component initializer must be supplied as a runtime callback.");
            addEvery(markers, source, ".bind(", SourceCeremonyKind.DEFERRED_GETTER,
                    "The reactive read must remain inside a deferred property callback.");
            addEvery(markers, source, ".bind(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "A property update is wrapped in a runtime binding callback.");
            addEvery(markers, source, ".structure(", SourceCeremonyKind.STRUCTURAL_CONTROL,
                    "The application isolates topology reads in a small rerunnable scope.");
            addEvery(markers, source, ".structure(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "A structural declaration must be supplied as a runtime callback.");
            addEvery(markers, source, ".fragment(", SourceCeremonyKind.EXPLICIT_KEY,
                    "Each conditional or collection fragment exposes a semantic key.");
            addEvery(markers, source, ".fragment(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "A new fragment identity is initialized through a runtime callback.");
            addEvery(markers, source, ".effect(", SourceCeremonyKind.CALLBACK_WRAPPER,
                    "Effect activation and cleanup require runtime callback wrappers.");
        }
        addEvery(markers, INTEGRATION_SOURCE, ".onAbort(", SourceCeremonyKind.CALLBACK_WRAPPER,
                "Staged resources require explicit abort-cleanup callbacks.");
        addEvery(markers, REALISTIC_SOURCE, ".onCommit(", SourceCeremonyKind.CALLBACK_WRAPPER,
                "A newly mounted message schedules one post-commit application callback.");
        addEvery(markers, REALISTIC_SOURCE, "for (Message message : messages)",
                SourceCeremonyKind.STRUCTURAL_CONTROL,
                "The application filter chooses the semantic message-key collection.");
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
            throw new IllegalArgumentException("Cannot inspect hybrid source " + relativePath, exception);
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
