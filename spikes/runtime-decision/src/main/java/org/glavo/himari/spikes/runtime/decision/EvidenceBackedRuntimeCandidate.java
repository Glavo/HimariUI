package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.CandidateDescriptor;
import org.glavo.himari.spikes.runtime.sample.CandidateEvidence;
import org.glavo.himari.spikes.runtime.sample.ComparisonEnvironment;
import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.glavo.himari.spikes.runtime.sample.FixtureDefinition;
import org.glavo.himari.spikes.runtime.sample.RuntimeCandidate;
import org.glavo.himari.spikes.runtime.sample.RuntimeFixtureSession;
import org.glavo.himari.spikes.runtime.sample.SourceCorpus;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Objects;

/// Replaces only a candidate's externally established evidence while preserving its implementation.
@NotNullByDefault
final class EvidenceBackedRuntimeCandidate implements RuntimeCandidate {
    /// The real independently implemented candidate.
    private final RuntimeCandidate delegate;

    /// The validated external evidence supplied to the frozen runner.
    private final CandidateEvidence evidence;

    /// Creates an evidence-backed adapter.
    ///
    /// @param delegate the real candidate
    /// @param evidence the externally validated evidence
    EvidenceBackedRuntimeCandidate(RuntimeCandidate delegate, CandidateEvidence evidence) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.evidence = Objects.requireNonNull(evidence, "evidence");
    }

    /// Returns the real candidate descriptor unchanged.
    ///
    /// @return the descriptor
    @Override
    public CandidateDescriptor descriptor() {
        return delegate.descriptor();
    }

    /// Returns the externally validated evidence snapshot.
    ///
    /// @return the evidence
    @Override
    public CandidateEvidence evidence() {
        return evidence;
    }

    /// Returns the real candidate source corpus unchanged.
    ///
    /// @return the source corpus
    @Override
    public SourceCorpus sourceCorpus() {
        return delegate.sourceCorpus();
    }

    /// Opens a real candidate fixture session.
    ///
    /// @param fixture the fixture definition
    /// @param environment the fresh comparison environment
    /// @param probe the shared instrumentation sink
    /// @return the real fixture session
    @Override
    public RuntimeFixtureSession open(
            FixtureDefinition fixture,
            ComparisonEnvironment environment,
            ComparisonProbe probe
    ) {
        return delegate.open(fixture, environment, probe);
    }
}
