package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Map;
import java.util.Objects;

/// Records external compatibility evidence that the in-process fixture runner cannot establish.
///
/// @param nativeImage the Native Image build-and-run result
/// @param reloadIdentity the development-time structural-identity reload result
/// @param ceremonyReview the blind ordinary-Java ceremony review result
/// @param artifacts immutable evidence names mapped to repository-relative or CI artifact paths
@NotNullByDefault
public record CandidateEvidence(
        EvidenceStatus nativeImage,
        EvidenceStatus reloadIdentity,
        EvidenceStatus ceremonyReview,
        @Unmodifiable Map<String, String> artifacts
) {
    /// Creates a validated immutable evidence snapshot.
    public CandidateEvidence {
        Objects.requireNonNull(nativeImage, "nativeImage");
        Objects.requireNonNull(reloadIdentity, "reloadIdentity");
        Objects.requireNonNull(ceremonyReview, "ceremonyReview");
        artifacts = ComparisonContracts.immutableSortedMap(artifacts, "evidence artifacts");
    }

    /// Returns an evidence snapshot in which no external run has occurred.
    ///
    /// @return the empty evidence snapshot
    public static CandidateEvidence untested() {
        return new CandidateEvidence(
                EvidenceStatus.NOT_RUN,
                EvidenceStatus.NOT_RUN,
                EvidenceStatus.NOT_RUN,
                Map.of()
        );
    }
}
