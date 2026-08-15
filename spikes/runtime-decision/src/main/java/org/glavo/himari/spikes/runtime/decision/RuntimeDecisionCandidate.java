package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.grouped.GroupedRuntimeCandidate;
import org.glavo.himari.spikes.runtime.hybrid.HybridRuntimeCandidate;
import org.glavo.himari.spikes.runtime.oneshot.OneShotRuntimeCandidate;
import org.glavo.himari.spikes.runtime.sample.RuntimeCandidate;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/// Identifies the three independently implemented candidates in the frozen decision order.
@NotNullByDefault
enum RuntimeDecisionCandidate {
    /// The explicit grouped-recomposition candidate, blinded as candidate A.
    GROUPED(
            "grouped",
            "A",
            "explicit-grouped-recomposition",
            List.of("Grouped", "grouped", "GROUPED", "Explicit grouped recomposition")
    ),

    /// The initialize-once signal-ownership candidate, blinded as candidate B.
    ONE_SHOT(
            "oneshot",
            "B",
            "one-shot-signal-ownership",
            List.of("OneShot", "oneShot", "oneshot", "ONE_SHOT", "one-shot", "One-shot")
    ),

    /// The fine-grained binding and structural-scope candidate, blinded as candidate C.
    HYBRID(
            "hybrid",
            "C",
            "fine-grained-structural-scopes",
            List.of("Hybrid", "hybrid", "HYBRID", "Fine-grained structural scopes")
    );

    /// The stable command-line key.
    private final String key;

    /// The name-free review label.
    private final String reviewLabel;

    /// The descriptor identifier expected from the implementation.
    private final String candidateId;

    /// Candidate-specific words mechanically removed from review packets.
    private final @Unmodifiable List<String> redactions;

    /// Creates a decision-candidate entry.
    ///
    /// @param key the stable command-line key
    /// @param reviewLabel the name-free review label
    /// @param candidateId the expected descriptor identifier
    /// @param redactions candidate-identifying source fragments
    RuntimeDecisionCandidate(
            String key,
            String reviewLabel,
            String candidateId,
            @Unmodifiable List<String> redactions
    ) {
        this.key = key;
        this.reviewLabel = reviewLabel;
        this.candidateId = candidateId;
        this.redactions = List.copyOf(redactions);
    }

    /// Returns the stable command-line key.
    ///
    /// @return the lowercase key
    String key() {
        return key;
    }

    /// Returns the name-free label used in blinded evidence.
    ///
    /// @return `A`, `B`, or `C`
    String reviewLabel() {
        return reviewLabel;
    }

    /// Returns the expected candidate descriptor identifier.
    ///
    /// @return the candidate identifier
    String candidateId() {
        return candidateId;
    }

    /// Returns candidate-specific text replaced in the blinded source packet.
    ///
    /// @return immutable redaction fragments
    @Unmodifiable List<String> redactions() {
        return redactions;
    }

    /// Creates the real candidate adapter for one repository checkout.
    ///
    /// @param repositoryRoot the repository root
    /// @return a new candidate adapter
    RuntimeCandidate create(Path repositoryRoot) {
        RuntimeCandidate candidate = switch (this) {
            case GROUPED -> new GroupedRuntimeCandidate(repositoryRoot);
            case ONE_SHOT -> new OneShotRuntimeCandidate(repositoryRoot);
            case HYBRID -> new HybridRuntimeCandidate(repositoryRoot);
        };
        if (!candidate.descriptor().id().equals(candidateId)) {
            throw new IllegalStateException("Candidate descriptor changed for " + key);
        }
        return candidate;
    }

    /// Resolves one command-line key without accepting display names or aliases.
    ///
    /// @param key the lowercase stable key
    /// @return the matching candidate
    /// @throws IllegalArgumentException if the key is unknown
    static RuntimeDecisionCandidate fromKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (RuntimeDecisionCandidate candidate : values()) {
            if (candidate.key.equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unknown runtime decision candidate: " + key);
    }
}
