package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;

/// Identifies the top-level outcome of one candidate report.
@NotNullByDefault
public enum ComparisonStatus {
    /// Every required fixture and external evidence gate passed.
    PASSED,

    /// At least one non-compensatory correctness or ceremony rule failed.
    DISQUALIFIED,

    /// In-process fixtures passed but required external or review evidence is incomplete.
    INCOMPLETE,

    /// A deliberately ineligible harness adapter validated the suite and report path.
    SELF_TEST_PASSED
}
