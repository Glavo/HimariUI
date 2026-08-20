package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.input.gesture.ClampingScrollPhysics;
import org.glavo.himari.layout.input.gesture.ScrollPhysics;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;

/// Creates an unstyled vertical scroll viewport.
@NotNullByDefault
public final class ScrollViewport {
    /// Pixel step applied by increment or decrement.
    private final float step;

    /// Logical vertical scroll offset retained across tree rebuilds.
    private float offset;

    /// Logical horizontal scroll offset retained across tree rebuilds.
    private float horizontalOffset;

    /// The viewport node after [#create], or unused before then.
    private LayoutNode viewport;

    /// Whether the viewport ignores scroll deltas.
    private boolean disabled;

    /// Policy that clamps offsets and decays fling velocity.
    private ScrollPhysics physics = ClampingScrollPhysics.INSTANCE;

    /// Remaining fling velocity in logical pixels per second.
    private float flingVelocity;

    /// Creates a viewport.
    ///
    /// @param step the positive scroll step
    public ScrollViewport(float step) {
        if (!Float.isFinite(step) || step <= 0.0f) {
            throw new IllegalArgumentException("Scroll step must be finite and positive");
        }
        this.step = step;
    }

    /// Returns the current offset.
    ///
    /// @return the offset
    public float offset() {
        return offset;
    }

    /// Returns the current horizontal offset.
    ///
    /// @return the horizontal offset
    public float horizontalOffset() {
        return horizontalOffset;
    }

    /// Returns the increment/decrement step.
    ///
    /// @return the positive step
    public float step() {
        return step;
    }

    /// Returns the scroll-physics policy.
    ///
    /// @return the policy
    public ScrollPhysics physics() {
        return physics;
    }

    /// Replaces the scroll-physics policy.
    ///
    /// @param physics the policy
    public void setPhysics(ScrollPhysics physics) {
        this.physics = Objects.requireNonNull(physics, "physics");
    }

    /// Returns the remaining fling velocity.
    ///
    /// @return the velocity in logical pixels per second
    public float flingVelocity() {
        return flingVelocity;
    }

    /// Starts a fling with `velocity` logical pixels per second.
    ///
    /// @param velocity the finite velocity
    public void fling(float velocity) {
        if (disabled) {
            return;
        }
        if (!Float.isFinite(velocity)) {
            throw new IllegalArgumentException("Fling velocity must be finite");
        }
        flingVelocity = velocity;
    }

    /// Advances an active fling by `elapsedNanos` using [#physics()].
    ///
    /// @param elapsedNanos the nonnegative sample duration
    /// @return whether the fling is still moving
    public boolean advanceFling(long elapsedNanos) {
        if (disabled || flingVelocity == 0.0f) {
            return false;
        }
        float next = physics.decayVelocity(flingVelocity, elapsedNanos);
        float dt = elapsedNanos / 1_000_000_000.0f;
        float delta = (flingVelocity + next) * 0.5f * dt;
        flingVelocity = next;
        scrollBy(delta);
        return flingVelocity != 0.0f;
    }

    /// Builds the viewport around one content node.
    ///
    /// @param factory the node factory
    /// @param name the diagnostic name
    /// @param content the scroll content
    /// @return the viewport
    public LayoutNode create(LayoutFactory factory, String name, LayoutNode content) {
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(content, "content");
        viewport = factory.scroll(name, List.of(new LayoutModifier.ExactSize(200.0f, 80.0f)), content);
        viewport.setScrollOffset(offset);
        viewport.setDisabled(disabled);
        return viewport;
    }

    /// Returns whether the viewport is disabled.
    ///
    /// @return whether the viewport is disabled
    public boolean disabled() {
        return disabled;
    }

    /// Sets the disabled state and publishes it to the mounted viewport when present.
    ///
    /// @param disabled the state
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
        if (viewport != null) {
            viewport.setDisabled(disabled);
        }
    }

    /// Advances the viewport by one step.
    public void scrollForward() {
        scrollBy(step);
    }

    /// Applies a signed logical-pixel delta and clamps the offset at zero.
    ///
    /// @param delta the signed delta
    public void scrollBy(float delta) {
        if (disabled) {
            return;
        }
        if (!Float.isFinite(delta)) {
            throw new IllegalArgumentException("Scroll delta must be finite");
        }
        float current = viewport == null ? offset : viewport.scrollOffset();
        offset = physics.applyOffset(current, delta, 0.0f, Float.MAX_VALUE);
        if (viewport != null) {
            viewport.setScrollOffset(offset);
        }
    }

    /// Applies a signed logical-pixel horizontal delta and clamps the offset at zero.
    ///
    /// @param delta the signed delta
    public void scrollByHorizontal(float delta) {
        if (disabled) {
            return;
        }
        if (!Float.isFinite(delta)) {
            throw new IllegalArgumentException("Scroll delta must be finite");
        }
        horizontalOffset = physics.applyOffset(horizontalOffset, delta, 0.0f, Float.MAX_VALUE);
    }

    /// Rewinds the viewport by one step, stopping at zero.
    public void scrollBackward() {
        if (disabled) {
            return;
        }
        requireCreated();
        offset = Math.max(0.0f, viewport.scrollOffset() - step);
        viewport.setScrollOffset(offset);
    }

    /// Requires [#create] to have run.
    private void requireCreated() {
        if (viewport == null) {
            throw new IllegalStateException("Scroll viewport has not been created");
        }
    }
}
