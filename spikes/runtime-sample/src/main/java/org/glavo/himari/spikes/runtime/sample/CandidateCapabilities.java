package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/// Declares behavior that selects capability-dependent fixture paths without selecting a runtime model.
///
/// @param measureMaterializationMode the ADR-020 strategy implemented by the candidate
/// @param cancellationSupport the staged-work cancellation claim
/// @param reloadIdentityClaimed whether the candidate claims to preserve identity across compatible code reload
@NotNullByDefault
public record CandidateCapabilities(
        MeasureMaterializationMode measureMaterializationMode,
        CancellationSupport cancellationSupport,
        boolean reloadIdentityClaimed
) {
    /// The capability key used by viewport-dependent fixture steps.
    public static final String MEASURE_MATERIALIZATION = "measure-materialization";

    /// The capability key used by cancellation fixture steps.
    public static final String CANCELLATION = "cancellation";

    /// The capability key used by development-time reload evidence.
    public static final String RELOAD_IDENTITY = "reload-identity";

    /// Creates a capability declaration.
    public CandidateCapabilities {
        Objects.requireNonNull(measureMaterializationMode, "measureMaterializationMode");
        Objects.requireNonNull(cancellationSupport, "cancellationSupport");
    }

    /// Returns the canonical value for a known capability key.
    ///
    /// @param key the canonical capability key
    /// @return the canonical value, or `null` when the key is unknown
    public @Nullable String value(String key) {
        Objects.requireNonNull(key, "key");
        return switch (key) {
            case MEASURE_MATERIALIZATION -> canonical(measureMaterializationMode);
            case CANCELLATION -> canonical(cancellationSupport);
            case RELOAD_IDENTITY -> reloadIdentityClaimed ? "claimed" : "not-claimed";
            default -> null;
        };
    }

    /// Returns all declared capabilities in canonical key order.
    ///
    /// @return the immutable capability map
    public @Unmodifiable Map<String, String> asMap() {
        return ComparisonContracts.immutableSortedMap(Map.of(
                MEASURE_MATERIALIZATION, canonical(measureMaterializationMode),
                CANCELLATION, canonical(cancellationSupport),
                RELOAD_IDENTITY, reloadIdentityClaimed ? "claimed" : "not-claimed"
        ), "capabilities");
    }

    /// Converts an enum constant to its report spelling.
    ///
    /// @param value the enum value
    /// @return the lower-kebab-case spelling
    private static String canonical(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
