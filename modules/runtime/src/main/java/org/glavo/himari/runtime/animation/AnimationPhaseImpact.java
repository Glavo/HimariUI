package org.glavo.himari.runtime.animation;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Stores an immutable set of UI phases affected by a presentation-value change.
///
/// The predefined earliest-phase constants include their required downstream phases. Exact masks
/// remain available for properties whose semantics or hit-test effects differ from those defaults.
///
/// @param mask the validated phase bit mask
@NotNullByDefault
public record AnimationPhaseImpact(int mask) {
    /// The mask containing every supported phase.
    private static final int ALL_MASK = (1 << AnimationPhase.values().length) - 1;

    /// Canonical instances for every valid phase combination.
    private static final AnimationPhaseImpact @Unmodifiable [] CANONICAL_IMPACTS =
            createCanonicalImpacts();

    /// No UI phase is affected.
    public static final AnimationPhaseImpact NONE = canonical(0);

    /// Structure and every downstream UI phase are affected.
    public static final AnimationPhaseImpact STRUCTURE = canonical(ALL_MASK);

    /// Measure, place, paint, composite, semantics, and hit testing are affected.
    public static final AnimationPhaseImpact MEASURE = exact(
            AnimationPhase.MEASURE,
            AnimationPhase.PLACE,
            AnimationPhase.PAINT,
            AnimationPhase.COMPOSITE,
            AnimationPhase.SEMANTICS,
            AnimationPhase.HIT_TEST
    );

    /// Placement, paint, composite, semantics, and hit testing are affected.
    public static final AnimationPhaseImpact PLACE = exact(
            AnimationPhase.PLACE,
            AnimationPhase.PAINT,
            AnimationPhase.COMPOSITE,
            AnimationPhase.SEMANTICS,
            AnimationPhase.HIT_TEST
    );

    /// Paint and composite work are affected.
    public static final AnimationPhaseImpact PAINT = exact(
            AnimationPhase.PAINT,
            AnimationPhase.COMPOSITE
    );

    /// Only retained-layer composition is affected.
    public static final AnimationPhaseImpact COMPOSITE = of(AnimationPhase.COMPOSITE);

    /// Only semantics are affected.
    public static final AnimationPhaseImpact SEMANTICS = of(AnimationPhase.SEMANTICS);

    /// Only authoritative hit testing is affected.
    public static final AnimationPhaseImpact HIT_TEST = of(AnimationPhase.HIT_TEST);

    /// Validates the mask.
    ///
    /// @throws IllegalArgumentException if `mask` contains an unknown phase bit
    public AnimationPhaseImpact {
        if ((mask & ~ALL_MASK) != 0) {
            throw new IllegalArgumentException("Animation phase mask contains unknown bits");
        }
    }

    /// Creates an impact containing one phase.
    ///
    /// @param phase the included phase
    /// @return the one-phase impact
    public static AnimationPhaseImpact of(AnimationPhase phase) {
        Objects.requireNonNull(phase, "phase");
        return canonical(bit(phase));
    }

    /// Creates an exact impact from two or more explicitly listed phases.
    ///
    /// @param first the first phase
    /// @param second the second phase
    /// @param remaining any additional phases
    /// @return the exact phase union
    public static AnimationPhaseImpact exact(
            AnimationPhase first,
            AnimationPhase second,
            AnimationPhase... remaining
    ) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(remaining, "remaining");
        int result = bit(first) | bit(second);
        for (AnimationPhase phase : remaining) {
            result |= bit(Objects.requireNonNull(phase, "remaining phase"));
        }
        return canonical(result);
    }

    /// Returns whether this impact contains a phase.
    ///
    /// @param phase the candidate phase
    /// @return whether the phase is included
    public boolean includes(AnimationPhase phase) {
        Objects.requireNonNull(phase, "phase");
        return (mask & bit(phase)) != 0;
    }

    /// Returns the union of this impact and another impact.
    ///
    /// @param other the other impact
    /// @return the phase union
    public AnimationPhaseImpact union(AnimationPhaseImpact other) {
        Objects.requireNonNull(other, "other");
        int unionMask = mask | other.mask;
        if (unionMask == mask) {
            return this;
        }
        if (unionMask == other.mask) {
            return other;
        }
        return canonical(unionMask);
    }

    /// Returns whether this impact contains no phase.
    ///
    /// @return whether the mask is empty
    public boolean isEmpty() {
        return mask == 0;
    }

    /// Returns the included phases in declaration order.
    ///
    /// @return an immutable phase list
    public @Unmodifiable List<AnimationPhase> phases() {
        ArrayList<AnimationPhase> result = new ArrayList<>();
        for (AnimationPhase phase : AnimationPhase.values()) {
            if (includes(phase)) {
                result.add(phase);
            }
        }
        return List.copyOf(result);
    }

    /// Returns the bit representing one phase.
    ///
    /// @param phase the phase
    /// @return the phase bit
    private static int bit(AnimationPhase phase) {
        return 1 << phase.ordinal();
    }

    /// Returns the canonical instance for a validated phase mask.
    ///
    /// @param mask the valid phase mask
    /// @return the canonical impact
    static AnimationPhaseImpact canonical(int mask) {
        if ((mask & ~ALL_MASK) != 0) {
            throw new IllegalArgumentException("Animation phase mask contains unknown bits");
        }
        return CANONICAL_IMPACTS[mask];
    }

    /// Creates the complete bounded canonical phase-impact table.
    ///
    /// @return every valid phase combination indexed by mask
    private static AnimationPhaseImpact @Unmodifiable [] createCanonicalImpacts() {
        AnimationPhaseImpact[] impacts = new AnimationPhaseImpact[ALL_MASK + 1];
        for (int mask = 0; mask <= ALL_MASK; mask++) {
            impacts[mask] = new AnimationPhaseImpact(mask);
        }
        return impacts;
    }
}
