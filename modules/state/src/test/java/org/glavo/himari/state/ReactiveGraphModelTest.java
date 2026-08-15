package org.glavo.himari.state;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Differentially checks a dynamic derived graph against a naive evaluator over randomized epochs.
@NotNullByDefault
final class ReactiveGraphModelTest {
    /// Compares values and semantic versions after deterministic randomized transactions.
    @Test
    void matchesNaiveDynamicReferenceModel() {
        StateDomain domain = new StateDomain();
        IntState first = domain.intState(0);
        IntState second = domain.intState(0);
        BooleanState chooseFirst = domain.booleanState(true);
        ReactiveOwner owner = domain.reactiveGraph().createOwner();
        DerivedState<Integer> selected = owner.derivedState(
                () -> chooseFirst.get() ? first.get() : second.get()
        );
        DerivedState<Integer> parity = owner.derivedState(() -> selected.get() & 1);
        DerivedState<Integer> result = owner.derivedState(
                () -> selected.get() * 31 + parity.get() + (chooseFirst.get() ? 7 : -7)
        );

        int expectedSelected = 0;
        int expectedParity = 0;
        int expectedResult = 7;
        long expectedSelectedVersion = 0L;
        long expectedParityVersion = 0L;
        long expectedResultVersion = 0L;
        assertEquals(expectedResult, result.get());

        Random random = new Random(0x48494d415249L);
        for (int operation = 0; operation < 2_000; operation++) {
            int nextFirst = random.nextInt(-50, 51);
            int nextSecond = random.nextInt(-50, 51);
            boolean nextChoice = random.nextBoolean();
            int writeMask = random.nextInt(1, 8);
            StateTransaction.run(domain, () -> {
                if ((writeMask & 1) != 0) {
                    first.set(nextFirst);
                }
                if ((writeMask & 2) != 0) {
                    second.set(nextSecond);
                }
                if ((writeMask & 4) != 0) {
                    chooseFirst.set(nextChoice);
                }
            });

            int nextExpectedSelected = chooseFirst.get() ? first.get() : second.get();
            int nextExpectedParity = nextExpectedSelected & 1;
            int nextExpectedResult = nextExpectedSelected * 31
                    + nextExpectedParity
                    + (chooseFirst.get() ? 7 : -7);
            if (nextExpectedSelected != expectedSelected) {
                expectedSelectedVersion++;
            }
            if (nextExpectedParity != expectedParity) {
                expectedParityVersion++;
            }
            if (nextExpectedResult != expectedResult) {
                expectedResultVersion++;
            }
            expectedSelected = nextExpectedSelected;
            expectedParity = nextExpectedParity;
            expectedResult = nextExpectedResult;

            assertEquals(expectedResult, result.get(), "value at operation " + operation);
            assertEquals(
                    expectedSelectedVersion,
                    selected.version(),
                    "selected version at operation " + operation
            );
            assertEquals(
                    expectedParityVersion,
                    parity.version(),
                    "parity version at operation " + operation
            );
            assertEquals(
                    expectedResultVersion,
                    result.version(),
                    "result version at operation " + operation
            );
        }
    }
}
