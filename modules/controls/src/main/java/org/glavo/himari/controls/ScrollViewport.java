package org.glavo.himari.controls;

import org.glavo.himari.layout.LayoutFactory;
import org.glavo.himari.layout.LayoutModifier;
import org.glavo.himari.layout.LayoutNode;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;

/// Creates an unstyled vertical scroll viewport.
@NotNullByDefault
public final class ScrollViewport {
    /// Pixel step applied by increment or decrement.
    private final float step;

    /// Logical scroll offset retained across tree rebuilds.
    private float offset;

    /// The viewport node after [#create], or unused before then.
    private LayoutNode viewport;

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
        return viewport;
    }

    /// Advances the viewport by one step.
    public void scrollForward() {
        scrollBy(step);
    }

    /// Applies a signed logical-pixel delta and clamps the offset at zero.
    ///
    /// @param delta the signed delta
    public void scrollBy(float delta) {
        if (!Float.isFinite(delta)) {
            throw new IllegalArgumentException("Scroll delta must be finite");
        }
        float current = viewport == null ? offset : viewport.scrollOffset();
        offset = Math.max(0.0f, current + delta);
        if (viewport != null) {
            viewport.setScrollOffset(offset);
        }
    }

    /// Rewinds the viewport by one step, stopping at zero.
    public void scrollBackward() {
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
