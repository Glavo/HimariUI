package org.glavo.himari.inspector;

import org.glavo.himari.layout.LayoutNode;
import org.glavo.himari.layout.LayoutTree;
import org.glavo.himari.layout.semantics.SemanticsNode;
import org.glavo.himari.layout.semantics.SemanticsSnapshot;
import org.glavo.himari.layout.semantics.SemanticsTextRange;
import org.glavo.himari.runtime.trace.RuntimeTrace;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

/// Captures a pointer-free inspector snapshot from a placed layout tree.
@NotNullByDefault
public final class Inspector {
    /// Prevents instantiation.
    private Inspector() {
    }

    /// Captures layout, semantics, and an optional runtime trace.
    ///
    /// @param tree the placed tree
    /// @param trace the runtime trace, or `null`
    /// @return the snapshot
    public static InspectorSnapshot capture(LayoutTree tree, @Nullable RuntimeTrace trace) {
        Objects.requireNonNull(tree, "tree");
        LinkedHashMap<Long, SemanticsNode> byId = new LinkedHashMap<>();
        @Nullable Long focusedId = null;
        if (!tree.needsPlace() && !tree.needsMeasure()) {
            SemanticsSnapshot semantics = tree.semantics();
            focusedId = semantics.focusedId();
            for (SemanticsNode node : semantics.nodes()) {
                byId.put(node.id(), node);
            }
        }
        ArrayList<InspectorNode> nodes = new ArrayList<>();
        collect(tree.root(), byId, nodes);
        return new InspectorSnapshot(
                nodes,
                focusedId,
                trace == null ? null : trace.toCanonicalJson()
        );
    }

    /// Walks one layout node and its descendants.
    ///
    /// @param node the layout node
    /// @param semantics the semantics index
    /// @param nodes the accumulator
    private static void collect(
            LayoutNode node,
            LinkedHashMap<Long, SemanticsNode> semantics,
            ArrayList<InspectorNode> nodes
    ) {
        SemanticsNode semantic = semantics.get(node.id());
        @Nullable SemanticsTextRange range = node.textRange();
        nodes.add(new InspectorNode(
                node.id(),
                node.name(),
                node.kind().name(),
                node.invalidationPhase(),
                node.clipKind(),
                node.role().name(),
                node.label(),
                node.origin().x(),
                node.origin().y(),
                node.size().width(),
                node.size().height(),
                semantic != null && semantic.focused(),
                node.liveRegion().name(),
                range == null ? -1 : range.start(),
                range == null ? -1 : range.end(),
                range == null ? -1 : range.caret(),
                node.rotationDegrees(),
                node.translation().x(),
                node.translation().y(),
                node.shear().x(),
                node.shear().y(),
                node.rangeMinimum(),
                node.rangeMaximum()
        ));
        for (LayoutNode child : node.children()) {
            collect(child, semantics, nodes);
        }
    }
}
