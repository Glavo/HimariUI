package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

/// Records [`OutlinePen`] commands in emission order.
@NotNullByDefault
public final class CollectingPen implements OutlinePen {
    /// Recorded commands.
    private final ArrayList<OutlineOp> commands = new ArrayList<>();

    /// Creates an empty recording.
    public CollectingPen() {
    }

    @Override
    public void moveTo(float x, float y) {
        commands.add(OutlineOp.move(x, y));
    }

    @Override
    public void lineTo(float x, float y) {
        commands.add(OutlineOp.line(x, y));
    }

    @Override
    public void quadTo(float cx, float cy, float x, float y) {
        commands.add(OutlineOp.quad(cx, cy, x, y));
    }

    @Override
    public void close() {
        commands.add(OutlineOp.close());
    }

    /// Returns a snapshot of the recorded commands.
    ///
    /// @return an unmodifiable copy
    public @Unmodifiable List<OutlineOp> commands() {
        return List.copyOf(commands);
    }
}
