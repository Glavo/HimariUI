package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies `MVAR` `hasc` through [`SfntFont#ascender(float[])`].
@NotNullByDefault
final class MvarTableTest {
    /// Default-instance ascender keeps the stored `hhea` value.
    @Test
    void defaultInstanceKeepsHheaAscender() {
        SfntFont font = MvarSampleFont.create();
        assertEquals(MvarSampleFont.DEFAULT_ASCENDER, font.ascender());
        assertEquals(MvarSampleFont.DEFAULT_ASCENDER, font.ascender(new float[] {MvarSampleFont.DEFAULT_WEIGHT}));
    }

    /// Peak weight adds the stored `hasc` delta.
    @Test
    void peakInstanceAppliesHascDelta() {
        SfntFont font = MvarSampleFont.create();
        assertEquals(
                MvarSampleFont.DEFAULT_ASCENDER + MvarSampleFont.ASCENDER_DELTA,
                font.ascender(new float[] {MvarSampleFont.MAX_WEIGHT})
        );
    }
}
