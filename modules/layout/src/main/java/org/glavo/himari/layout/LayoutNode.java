package org.glavo.himari.layout;

import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsRole;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntConsumer;

/// Stores one layout node, its children, and the last measure/place result.
@NotNullByDefault
public final class LayoutNode {
    /// The stable node identity.
    private final long id;

    /// The diagnostic name.
    private final String name;

    /// The measure policy.
    private final LayoutKind kind;

    /// Cross-axis alignment for row and column.
    private final Alignment alignment;

    /// Applied modifiers in declaration order.
    private final @Unmodifiable List<LayoutModifier> modifiers;

    /// Direct children in z-order / document order.
    private final ArrayList<LayoutNode> children = new ArrayList<>();

    /// The leaf intrinsic size, or `null` for a container.
    private final @Nullable Size intrinsicSize;

    /// Whether this node can receive focus.
    private final boolean focusable;

    /// The semantics role.
    private final SemanticsRole role;

    /// The semantics label.
    private final String label;

    /// Declared semantics actions.
    private final @Unmodifiable Set<SemanticsAction> actions;

    /// The activation callback, or `null`.
    private final @Nullable Runnable onActivate;

    /// The increment/decrement callback, or `null`.
    private final @Nullable IntConsumer onAdjust;

    /// Toggle state published to semantics, or `null`.
    private @Nullable Boolean selected;

    /// Range value published to semantics, or `null`.
    private @Nullable Double rangeValue;

    /// Scroll offset applied when this node uses [LayoutKind#SCROLL].
    private float scrollOffset;

    /// Constraints used by the current measure pass, or `null` before measure.
    private @Nullable Constraints measuredConstraints;

    /// The size published by the current measure pass.
    private Size size = Size.ZERO;

    /// The origin relative to the parent.
    private Offset offset = Offset.ZERO;

    /// The origin relative to the layout root.
    private Offset origin = Offset.ZERO;

    /// Whether this node was measured in the current pass.
    private boolean measured;

    /// Whether this node was placed in the current pass.
    private boolean placed;

    /// Creates one node.
    ///
    /// @param id the identity
    /// @param name the diagnostic name
    /// @param kind the policy
    /// @param alignment the cross-axis alignment
    /// @param modifiers the modifiers
    /// @param intrinsicSize the leaf size, or `null`
    /// @param focusable whether the node is focusable
    /// @param role the semantics role
    /// @param label the semantics label
    /// @param actions the semantics actions
    /// @param onActivate the activation callback, or `null`
    /// @param onAdjust the increment/decrement callback, or `null`
    LayoutNode(
            long id,
            String name,
            LayoutKind kind,
            Alignment alignment,
            List<LayoutModifier> modifiers,
            @Nullable Size intrinsicSize,
            boolean focusable,
            SemanticsRole role,
            String label,
            Set<SemanticsAction> actions,
            @Nullable Runnable onActivate,
            @Nullable IntConsumer onAdjust
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.alignment = Objects.requireNonNull(alignment, "alignment");
        this.modifiers = List.copyOf(modifiers);
        this.intrinsicSize = intrinsicSize;
        this.focusable = focusable;
        this.role = Objects.requireNonNull(role, "role");
        this.label = Objects.requireNonNull(label, "label");
        this.actions = Set.copyOf(actions);
        this.onActivate = onActivate;
        this.onAdjust = onAdjust;
    }

    /// Returns the identity.
    ///
    /// @return the identity
    public long id() {
        return id;
    }

    /// Returns the diagnostic name.
    ///
    /// @return the name
    public String name() {
        return name;
    }

    /// Returns the layout policy.
    ///
    /// @return the kind
    public LayoutKind kind() {
        return kind;
    }

    /// Returns the children in document order.
    ///
    /// @return the children
    public @Unmodifiable List<LayoutNode> children() {
        return List.copyOf(children);
    }

    /// Returns the last measured size.
    ///
    /// @return the size
    public Size size() {
        return size;
    }

    /// Returns the origin relative to the parent.
    ///
    /// @return the offset
    public Offset offset() {
        return offset;
    }

    /// Returns the origin relative to the layout root.
    ///
    /// @return the origin
    public Offset origin() {
        return origin;
    }

    /// Returns the axis-aligned bounds in root coordinates.
    ///
    /// @return the bounds
    public LayoutRect bounds() {
        return new LayoutRect(origin.x(), origin.y(), size.width(), size.height());
    }

    /// Returns whether the node is focusable.
    ///
    /// @return whether the node is focusable
    public boolean focusable() {
        return focusable;
    }

    /// Returns the semantics role.
    ///
    /// @return the role
    public SemanticsRole role() {
        return role;
    }

    /// Returns the semantics label.
    ///
    /// @return the label
    public String label() {
        return label;
    }

    /// Returns the declared semantics actions.
    ///
    /// @return the actions
    public @Unmodifiable Set<SemanticsAction> actions() {
        return actions;
    }

    /// Returns the published toggle state, or `null`.
    ///
    /// @return the state
    public @Nullable Boolean selected() {
        return selected;
    }

    /// Returns the published range value, or `null`.
    ///
    /// @return the value
    public @Nullable Double rangeValue() {
        return rangeValue;
    }

    /// Publishes a toggle state for the next semantics snapshot.
    ///
    /// @param selected the state
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    /// Publishes a finite range value for the next semantics snapshot.
    ///
    /// @param value the value
    public void setRangeValue(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Range value must be finite");
        }
        this.rangeValue = value;
    }

    /// Adds a child in document order.
    ///
    /// @param child the child
    void add(LayoutNode child) {
        children.add(Objects.requireNonNull(child, "child"));
    }

    /// Clears per-pass measure and place flags.
    void beginPass() {
        measured = false;
        placed = false;
        measuredConstraints = null;
        for (LayoutNode child : children) {
            child.beginPass();
        }
    }

    /// Measures this node once under the incoming constraints.
    ///
    /// @param incoming the parent constraints
    /// @return the published size
    Size measure(Constraints incoming) {
        if (measured) {
            throw new IllegalStateException("Layout node " + name + " was measured more than once");
        }
        Constraints current = incoming;
        for (LayoutModifier modifier : modifiers) {
            current = modifier.apply(current);
        }
        measuredConstraints = current;
        Size inner = switch (kind) {
            case LEAF -> Objects.requireNonNull(intrinsicSize, "intrinsicSize");
            case BOX -> measureBox(current);
            case ROW -> measureRow(current);
            case COLUMN -> measureColumn(current);
            case SCROLL -> measureScroll(current);
        };
        Size wrapped = inner;
        for (int index = modifiers.size() - 1; index >= 0; index--) {
            wrapped = modifiers.get(index).wrap(wrapped);
        }
        size = incoming.constrain(wrapped.width(), wrapped.height());
        measured = true;
        return size;
    }

    /// Places this node at a parent-relative offset and a root origin.
    ///
    /// @param parentOffset the parent-relative origin
    /// @param rootOrigin the root-relative origin
    void place(Offset parentOffset, Offset rootOrigin) {
        if (!measured) {
            throw new IllegalStateException("Layout node " + name + " was placed before measure");
        }
        if (placed) {
            throw new IllegalStateException("Layout node " + name + " was placed more than once");
        }
        offset = parentOffset;
        origin = rootOrigin;
        Offset inner = Offset.ZERO;
        for (LayoutModifier modifier : modifiers) {
            inner = inner.plus(modifier.childOrigin());
        }
        Constraints childConstraints = Objects.requireNonNull(measuredConstraints, "measuredConstraints");
        switch (kind) {
            case LEAF -> {
            }
            case BOX -> placeBox(inner, rootOrigin.plus(inner), childConstraints);
            case ROW -> placeRow(inner, rootOrigin.plus(inner), childConstraints);
            case COLUMN -> placeColumn(inner, rootOrigin.plus(inner), childConstraints);
            case SCROLL -> placeScroll(inner, rootOrigin.plus(inner));
        }
        placed = true;
    }

    /// Activates this node when it declares [SemanticsAction#ACTIVATE].
    ///
    /// @return whether an activation callback ran
    boolean activate() {
        if (onActivate == null || !actions.contains(SemanticsAction.ACTIVATE)) {
            return false;
        }
        onActivate.run();
        return true;
    }

    /// Applies one signed adjustment when this node exposes increment or decrement.
    ///
    /// @param delta `1` to increment or `-1` to decrement
    /// @return whether an adjustment callback ran
    boolean adjust(int delta) {
        if (onAdjust == null || delta == 0) {
            return false;
        }
        SemanticsAction required = delta > 0 ? SemanticsAction.INCREMENT : SemanticsAction.DECREMENT;
        if (!actions.contains(required)) {
            return false;
        }
        onAdjust.accept(delta > 0 ? 1 : -1);
        return true;
    }

    /// Returns the current scroll offset in logical pixels.
    ///
    /// @return the nonnegative offset
    public float scrollOffset() {
        return scrollOffset;
    }

    /// Replaces the scroll offset used by a [LayoutKind#SCROLL] node.
    ///
    /// The next [#place(Offset, Offset)] call applies the new offset. Callers must measure
    /// again so placement flags reset.
    ///
    /// @param offset the nonnegative offset
    public void setScrollOffset(float offset) {
        if (!Float.isFinite(offset) || offset < 0.0f) {
            throw new IllegalArgumentException("Scroll offset must be finite and nonnegative");
        }
        this.scrollOffset = offset;
    }

    /// Measures stacked children.
    ///
    /// @param constraints the inner constraints
    /// @return the stack size
    private Size measureBox(Constraints constraints) {
        float width = constraints.minWidth();
        float height = constraints.minHeight();
        for (LayoutNode child : children) {
            Size childSize = child.measure(constraints);
            width = Math.max(width, childSize.width());
            height = Math.max(height, childSize.height());
        }
        return constraints.constrain(width, height);
    }

    /// Measures children on a horizontal axis.
    ///
    /// @param constraints the inner constraints
    /// @return the row size
    private Size measureRow(Constraints constraints) {
        float width = 0.0f;
        float height = constraints.minHeight();
        float remaining = constraints.maxWidth();
        for (LayoutNode child : children) {
            Size childSize = child.measure(Constraints.loose(remaining, constraints.maxHeight()));
            width += childSize.width();
            remaining = Math.max(0.0f, remaining - childSize.width());
            height = Math.max(height, childSize.height());
        }
        return constraints.constrain(width, height);
    }

    /// Measures scroll content against an unbounded block axis and reports the viewport size.
    ///
    /// @param constraints the inner constraints
    /// @return the viewport size
    private Size measureScroll(Constraints constraints) {
        float width = constraints.minWidth();
        float contentHeight = 0.0f;
        Constraints childConstraints = Constraints.loose(constraints.maxWidth(), Float.MAX_VALUE);
        for (LayoutNode child : children) {
            Size childSize = child.measure(childConstraints);
            width = Math.max(width, childSize.width());
            contentHeight += childSize.height();
        }
        float viewportHeight = constraints.maxHeight() == Float.MAX_VALUE
                ? Math.max(constraints.minHeight(), contentHeight)
                : constraints.maxHeight();
        return constraints.constrain(width, viewportHeight);
    }

    /// Measures children on a vertical axis.
    ///
    /// @param constraints the inner constraints
    /// @return the column size
    private Size measureColumn(Constraints constraints) {
        float width = constraints.minWidth();
        float height = 0.0f;
        float remaining = constraints.maxHeight();
        for (LayoutNode child : children) {
            Size childSize = child.measure(Constraints.loose(constraints.maxWidth(), remaining));
            height += childSize.height();
            remaining = Math.max(0.0f, remaining - childSize.height());
            width = Math.max(width, childSize.width());
        }
        return constraints.constrain(width, height);
    }

    /// Places stacked children.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    /// @param constraints the inner constraints
    private void placeBox(Offset inner, Offset root, Constraints constraints) {
        for (LayoutNode child : children) {
            float x = alignment.place(constraints.maxWidth(), child.size.width());
            float y = Alignment.START.place(constraints.maxHeight(), child.size.height());
            Offset childOffset = inner.plus(new Offset(x, y));
            child.place(childOffset, root.plus(new Offset(x, y)));
        }
    }

    /// Places row children.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    /// @param constraints the inner constraints
    private void placeRow(Offset inner, Offset root, Constraints constraints) {
        float x = 0.0f;
        for (LayoutNode child : children) {
            float y = alignment.place(size.height(), child.size.height());
            Offset childOffset = inner.plus(new Offset(x, y));
            child.place(childOffset, root.plus(new Offset(x, y)));
            x += child.size.width();
        }
    }

    /// Places scroll children using the stored offset.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    private void placeScroll(Offset inner, Offset root) {
        float y = -scrollOffset;
        for (LayoutNode child : children) {
            Offset childOffset = inner.plus(new Offset(0.0f, y));
            child.place(childOffset, root.plus(new Offset(0.0f, y)));
            y += child.size.height();
        }
    }

    /// Places column children.
    ///
    /// @param inner the inner origin relative to this node
    /// @param root the inner origin in root coordinates
    /// @param constraints the inner constraints
    private void placeColumn(Offset inner, Offset root, Constraints constraints) {
        float y = 0.0f;
        for (LayoutNode child : children) {
            float x = alignment.place(size.width(), child.size.width());
            Offset childOffset = inner.plus(new Offset(x, y));
            child.place(childOffset, root.plus(new Offset(x, y)));
            y += child.size.height();
        }
    }
}
