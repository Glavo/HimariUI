package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Supplies one independently implemented structural-runtime candidate to the frozen suite.
///
/// Implementations may depend on the shared state graph and Headless host but must not reuse a
/// structural abstraction from another candidate. The source corpus must point at the ungenerated
/// ordinary-Java comparison applications compiled for this candidate.
@NotNullByDefault
public interface RuntimeCandidate {
    /// Returns the stable candidate descriptor.
    ///
    /// @return the descriptor
    CandidateDescriptor descriptor();

    /// Returns external evidence known at report generation time.
    ///
    /// @return the evidence snapshot
    CandidateEvidence evidence();

    /// Returns the application-source corpus used for ceremony measurement.
    ///
    /// @return the source corpus
    SourceCorpus sourceCorpus();

    /// Opens an unmounted session for one fixture.
    ///
    /// @param fixture the shared fixture definition
    /// @param environment the fresh state and Headless environment
    /// @param probe the shared instrumentation sink
    /// @return the candidate session
    RuntimeFixtureSession open(
            FixtureDefinition fixture,
            ComparisonEnvironment environment,
            ComparisonProbe probe
    );
}
