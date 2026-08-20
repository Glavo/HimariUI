package org.glavo.himari.layout;

import org.glavo.himari.layout.focus.FocusTree;
import org.glavo.himari.layout.hit.HitTester;
import org.glavo.himari.layout.hit.SpatialIndex;
import org.glavo.himari.layout.input.KeyEvent;
import org.glavo.himari.layout.input.KeyEventType;
import org.glavo.himari.layout.input.LogicalKey;
import org.glavo.himari.layout.input.PointerEvent;
import org.glavo.himari.layout.input.PointerEventType;
import org.glavo.himari.layout.semantics.SemanticsAction;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsSnapshot;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// Owns one layout tree, focus traversal, hit testing, and semantics snapshot.
@NotNullByDefault
public final class LayoutTree {
    /// The next positive node identity.
    private long nextId = 1L;

    /// The root node, or `null` before the first composition.
    private @Nullable LayoutNode root;

    /// The focus model.
    private final FocusTree focus = new FocusTree();

    /// Whether measure must run before place.
    private boolean needsMeasure = true;

    /// Whether place must run after measure.
    private boolean needsPlace = true;

    /// Reverse-z spatial index rebuilt after placement, or `null` before the first place.
    private @Nullable SpatialIndex spatialIndex;

    /// Pointer-capture target from the last `DOWN`, or `null` when none.
    private @Nullable LayoutNode pointerCapture;

    /// Creates an empty tree.
    public LayoutTree() {
    }

    /// Returns the next unused node identity.
    ///
    /// @return the identity
    public long allocateId() {
        long id = nextId;
        nextId = Math.incrementExact(nextId);
        return id;
    }

    /// Installs the root node and invalidates layout.
    ///
    /// @param root the new root
    public void setRoot(LayoutNode root) {
        this.root = Objects.requireNonNull(root, "root");
        needsMeasure = true;
        needsPlace = true;
        spatialIndex = null;
        pointerCapture = null;
    }

    /// Returns the root node.
    ///
    /// @return the root
    /// @throws IllegalStateException if no root is installed
    public LayoutNode root() {
        return requireRoot();
    }

    /// Measures the tree under the supplied constraints.
    ///
    /// Each node is measured at most once. A second call with identical constraints is a no-op.
    ///
    /// @param constraints the root constraints
    /// @return the root size
    public Size measure(Constraints constraints) {
        Objects.requireNonNull(constraints, "constraints");
        LayoutNode current = requireRoot();
        current.beginPass();
        Size size = current.measure(constraints);
        needsMeasure = false;
        needsPlace = true;
        return size;
    }

    /// Places the tree at the origin.
    ///
    /// @throws IllegalStateException if the tree has not been measured
    public void place() {
        if (needsMeasure) {
            throw new IllegalStateException("Layout tree must be measured before placement");
        }
        LayoutNode current = requireRoot();
        current.place(Offset.ZERO, Offset.ZERO);
        needsPlace = false;
        spatialIndex = SpatialIndex.build(current);
        focus.rebuild(current);
    }

    /// Returns the spatial index rebuilt by the last [#place()].
    ///
    /// @return the index, or `null` before the first successful place
    public @Nullable SpatialIndex spatialIndex() {
        return spatialIndex;
    }

    /// Returns whether placement is required.
    ///
    /// @return whether [#place()] must run
    public boolean needsPlace() {
        return needsPlace;
    }

    /// Returns whether measure is required.
    ///
    /// @return whether [#measure(Constraints)] must run
    public boolean needsMeasure() {
        return needsMeasure;
    }

    /// Returns the focus model.
    ///
    /// @return the focus tree
    public FocusTree focus() {
        return focus;
    }

    /// Dispatches one pointer event through hit testing and target/bubble routing.
    ///
    /// A `DOWN` on a focusable node requests focus. A `UP` on a node that exposes
    /// [SemanticsAction#ACTIVATE] activates it.
    ///
    /// @param event the pointer event
    /// @return whether a node handled the event
    public boolean dispatch(PointerEvent event) {
        Objects.requireNonNull(event, "event");
        if (needsPlace) {
            throw new IllegalStateException("Pointer dispatch requires a placed layout tree");
        }
        SpatialIndex hits = spatialIndex;
        if (hits != null) {
            LayoutNode indexed = hits.hit(event.x(), event.y());
            LayoutNode walked = HitTester.hit(requireRoot(), event.x(), event.y());
            if (indexed != walked) {
                throw new IllegalStateException("Spatial index disagreed with tree hit testing");
            }
        }
        List<LayoutNode> path;
        if (pointerCapture != null
                && event.type() != PointerEventType.DOWN
                && event.type() != PointerEventType.ENTER
                && event.type() != PointerEventType.CAPTURE_CHANGED) {
            path = pathTo(pointerCapture);
        } else {
            path = HitTester.path(requireRoot(), event.x(), event.y());
        }
        if (path.isEmpty()) {
            if (event.type() == PointerEventType.UP
                    || event.type() == PointerEventType.LEAVE
                    || event.type() == PointerEventType.CAPTURE_CHANGED) {
                pointerCapture = null;
            }
            return false;
        }
        LayoutNode target = path.getLast();
        if (event.type() == PointerEventType.DOWN) {
            pointerCapture = target;
            if (target.focusable() && !target.disabled()) {
                focus.request(target);
                focus.setFocusVisible(false);
            }
        }
        if (event.type() == PointerEventType.UP
                || event.type() == PointerEventType.LEAVE
                || event.type() == PointerEventType.CAPTURE_CHANGED) {
            pointerCapture = null;
        }
        for (int index = path.size() - 1; index >= 0; index--) {
            if (path.get(index).dispatchPointer(event)) {
                return true;
            }
        }
        if (event.type() == PointerEventType.UP) {
            for (int index = path.size() - 1; index >= 0; index--) {
                LayoutNode candidate = path.get(index);
                if (candidate.activate()) {
                    return true;
                }
                LayoutRect bounds = candidate.bounds();
                int delta = event.x() >= bounds.x() + bounds.width() * 0.5f ? 1 : -1;
                if (candidate.adjust(delta)) {
                    return true;
                }
            }
        }
        return true;
    }

    /// Returns the node that currently holds pointer capture.
    ///
    /// @return the capture target, or `null`
    public @Nullable LayoutNode pointerCapture() {
        return pointerCapture;
    }

    /// Builds the root-to-`target` path.
    ///
    /// @param target the captured node
    /// @return the path, or empty when `target` is not in the tree
    private List<LayoutNode> pathTo(LayoutNode target) {
        ArrayList<LayoutNode> path = new ArrayList<>();
        if (collectPath(requireRoot(), target, path)) {
            return path;
        }
        return List.of();
    }

    /// Walks `node` until `target` is found.
    ///
    /// @param node the current node
    /// @param target the captured node
    /// @param path the accumulator
    /// @return whether `target` was found
    private static boolean collectPath(LayoutNode node, LayoutNode target, List<LayoutNode> path) {
        path.add(node);
        if (node == target) {
            return true;
        }
        for (LayoutNode child : node.children()) {
            if (collectPath(child, target, path)) {
                return true;
            }
        }
        path.removeLast();
        return false;
    }

    /// Dispatches one keyboard event to the focused node.
    ///
    /// Tab moves focus. Enter and Space activate the focused node.
    ///
    /// @param event the key event
    /// @return whether the event was handled
    public boolean dispatch(KeyEvent event) {
        Objects.requireNonNull(event, "event");
        if (needsPlace) {
            throw new IllegalStateException("Key dispatch requires a placed layout tree");
        }
        if (event.type() != KeyEventType.DOWN) {
            return false;
        }
        if (event.key() == LogicalKey.TAB) {
            return (event.shift() ? focus.previous() : focus.next()) != null;
        }
        if (event.key() == LogicalKey.ENTER || event.key() == LogicalKey.SPACE) {
            @Nullable LayoutNode focused = focus.focusedNode();
            return focused != null && focused.activate();
        }
        if (event.key() == LogicalKey.ARROW_RIGHT || event.key() == LogicalKey.ARROW_DOWN) {
            @Nullable LayoutNode focused = focus.focusedNode();
            return focused != null && focused.adjust(1);
        }
        if (event.key() == LogicalKey.ARROW_LEFT || event.key() == LogicalKey.ARROW_UP) {
            @Nullable LayoutNode focused = focus.focusedNode();
            return focused != null && focused.adjust(-1);
        }
        return false;
    }

    /// Returns a semantics snapshot whose bounds match the last placement.
    ///
    /// @return the snapshot
    public SemanticsSnapshot semantics() {
        if (needsPlace) {
            throw new IllegalStateException("Semantics snapshot requires a placed layout tree");
        }
        ArrayList<SemanticsNode> nodes = new ArrayList<>();
        collectSemantics(requireRoot(), nodes);
        return new SemanticsSnapshot(List.copyOf(nodes), focus.focusedId());
    }

    /// Collects semantics nodes in document order.
    ///
    /// @param node the current node
    /// @param nodes the accumulator
    private void collectSemantics(LayoutNode node, List<SemanticsNode> nodes) {
        nodes.add(new SemanticsNode(
                node.id(),
                node.role(),
                node.label(),
                node.actions(),
                node.bounds(),
                focus.focusedId() != null && focus.focusedId() == node.id(),
                node.selected(),
                node.rangeValue(),
                node.rangeMinimum(),
                node.rangeMaximum(),
                node.liveRegion(),
                node.textRange(),
                node.grid(),
                node.scroll(),
                node.gridItem(),
                node.disabled(),
                node.readOnly(),
                node.hint(),
                node.focusable(),
                node.password(),
                node.accessKey(),
                node.acceleratorKey(),
                node.required(),
                node.itemStatus(),
                node.itemType(),
                node.locale(),
                node.level(),
                node.positionInSet(),
                node.sizeOfSet(),
                node.description(),
                node.error(),
                node.landmarkType(),
                node.localizedLandmarkType(),
                node.ariaRole(),
                node.ariaProperties(),
                node.controllerFor(),
                node.describedBy(),
                node.flowsTo(),
                node.labeledBy(),
                node.flowsFrom(),
                node.optimizeForVisualContent(),
                node.fillColor(),
                node.outlineColor(),
                node.fillType(),
                node.visualEffects(),
                node.outlineThickness(),
                node.rotation(),
                node.peripheral(),
                node.annotationType(),
                node.annotationObjects()
        ));
        for (LayoutNode child : node.children()) {
            collectSemantics(child, nodes);
        }
    }

    /// Returns the installed root.
    ///
    /// @return the root
    private LayoutNode requireRoot() {
        if (root == null) {
            throw new IllegalStateException("Layout tree has no root");
        }
        return root;
    }
}
