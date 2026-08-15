package org.glavo.himari.spikes.runtime.grouped;

import org.glavo.himari.spikes.runtime.sample.ComparisonProbe;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Verifies grouped-runtime invariants below the neutral fixture adapter.
@NotNullByDefault
final class GroupedRuntimeTest {
    /// Verifies positional identity after keyed siblings and preservation across a rejected draft.
    @Test
    void preservesInterleavedPositionalMemoryAcrossFailedDraft() {
        ComparisonProbe probe = new ComparisonProbe();
        TestComposition composition = new TestComposition();
        try (GroupedRuntime runtime = new GroupedRuntime(composition::compose, probe)) {
            runtime.recompose();
            GroupedRuntime.LocalInt initial = composition.local();
            initial.increment();

            runtime.recompose();
            assertSame(initial, composition.local());
            assertEquals(1, composition.local().get());

            composition.fail = true;
            assertThrows(GroupedRuntime.GroupedCompositionException.class, runtime::recompose);
            assertEquals(List.of("root", "keyed", "positional"), runtime.mountedNodes());

            composition.fail = false;
            runtime.recompose();
            assertSame(initial, composition.local());
            assertEquals(1, composition.local().get());
        }
        assertEquals(0L, probe.metrics().activeDependencyEdges());
        assertEquals(0L, probe.metrics().retainedBytes());
    }

    /// Declares a keyed group before an unkeyed positional-memory group.
    @NotNullByDefault
    private static final class TestComposition {
        /// The last committed positional cell, or `null` before the first commit.
        private @Nullable GroupedRuntime.LocalInt committedLocal;

        /// Whether the current declaration must reject its draft.
        private boolean fail;

        /// Creates an initially successful declaration.
        private TestComposition() {
        }

        /// Declares interleaved keyed and positional groups.
        ///
        /// @param scope the grouped scope
        private void compose(GroupedRuntime.Scope scope) {
            scope.node("root");
            scope.keyedGroup("items", "stable", () -> scope.node("keyed"));
            scope.group("positional-owner", () -> {
                GroupedRuntime.LocalInt local = scope.rememberInt(0);
                scope.onCommit(() -> committedLocal = local);
                scope.node("positional");
                if (fail) {
                    scope.fail("test-failure");
                }
            });
        }

        /// Returns the last committed local cell.
        ///
        /// @return the cell
        private GroupedRuntime.LocalInt local() {
            if (committedLocal == null) {
                throw new IllegalStateException("No positional cell has committed");
            }
            return committedLocal;
        }
    }
}
