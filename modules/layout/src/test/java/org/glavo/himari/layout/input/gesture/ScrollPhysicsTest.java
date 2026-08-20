package org.glavo.himari.layout.input.gesture;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies clamping and bouncing scroll-origin policies.
@NotNullByDefault
final class ScrollPhysicsTest {
    /// Clamps an origin into `[min, max]` with no extra range.
    @Test
    void clampingRejectsOriginsPastTheEdges() {
        ScrollPhysics physics = ClampingScrollPhysics.INSTANCE;
        assertEquals(0, physics.clampIndex(-4, 0, 10));
        assertEquals(10, physics.clampIndex(40, 0, 10));
        assertEquals(4, physics.applyIndex(3, 1, 0, 10));
        assertEquals(10, physics.applyIndex(10, 1, 0, 10));
    }

    /// Permits one extra item past each edge.
    @Test
    void bouncingAllowsBoundedOverscroll() {
        BouncingScrollPhysics physics = new BouncingScrollPhysics(1);
        assertEquals(1, physics.overscroll());
        assertEquals(-1, physics.clampIndex(-4, 0, 10));
        assertEquals(11, physics.clampIndex(40, 0, 10));
        assertEquals(0, physics.clampIndex(0, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new BouncingScrollPhysics(-1));
    }

    /// Decays a fling exponentially and settles below the rest threshold.
    @Test
    void clampingDecaysFlingVelocityTowardRest() {
        ClampingScrollPhysics physics = ClampingScrollPhysics.INSTANCE;
        assertEquals(20.0f, physics.decayVelocity(20.0f, 0L));
        float later = physics.decayVelocity(20.0f, 100_000_000L);
        assertTrue(later > 0.0f);
        assertTrue(later < 20.0f);
        assertEquals(0.0f, physics.decayVelocity(20.0f, 2_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> physics.decayVelocity(Float.NaN, 1L));
        assertThrows(IllegalArgumentException.class, () -> physics.decayVelocity(1.0f, -1L));
    }
}
