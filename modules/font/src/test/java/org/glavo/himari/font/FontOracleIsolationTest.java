package org.glavo.himari.font;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checks isolated oracle sources exist and compare stays honest without native binaries.
@NotNullByDefault
final class FontOracleIsolationTest {
    /// Confirms the checked-in C runners and policy documents exist.
    @Test
    void oracleSourcesAreIsolatedFromProduction() throws Exception {
        Path root = repositoryRoot();
        assertTrue(Files.isRegularFile(root.resolve("oracles/freetype/outline_oracle.c")));
        assertTrue(Files.isRegularFile(root.resolve("oracles/harfbuzz/shape_oracle.c")));
        assertTrue(Files.isRegularFile(root.resolve("oracles/ORACLE_FORMAT.md")));
        assertTrue(Files.isRegularFile(root.resolve("DIFFERENCE_POLICY.md")));
        String fontSource = Files.readString(root.resolve("modules/font/src/main/java/org/glavo/himari/font/SfntFont.java"));
        assertTrue(!fontSource.contains("freetype") && !fontSource.contains("harfbuzz"));
    }

    /// Regularizes closes the way fauntlet drops implicit FreeType closes.
    @Test
    void regularizeDropsCloseWhenAlreadyAtStart() {
        List<OutlineOp> commands = List.of(
                OutlineOp.move(1.0f, 2.0f),
                OutlineOp.line(3.0f, 4.0f),
                OutlineOp.line(1.0f, 2.0f),
                OutlineOp.close()
        );
        List<OutlineOp> regularized = OutlineCompare.regularize(commands, true);
        assertEquals(3, regularized.size());
        assertEquals(OutlineVerb.MOVE, regularized.get(0).verb());
        assertEquals(OutlineVerb.LINE, regularized.get(1).verb());
        assertEquals(OutlineVerb.LINE, regularized.get(2).verb());
    }

    /// Compares the shipped bump outline to its own JSON encoding.
    @Test
    void javaOutlineMatchesItsSerializedForm() {
        SfntFont font = OutlineSampleFont.create();
        CollectingPen pen = new CollectingPen();
        font.outline(OutlineSampleFont.GLYPH_BUMP, pen);
        String json = OutlineCompare.toJson(pen.commands());
        List<OutlineOp> parsed = OutlineCompare.parseCommands(json);
        @Nullable String difference = OutlineCompare.difference(
                pen.commands(),
                parsed,
                true,
                OutlineCompare.DEFAULT_TOLERANCE
        );
        assertNull(difference, difference);
    }

    /// Records an environment block when no native oracle is present.
    @Test
    void missingNativeOracleIsEnvironmentBlocked() {
        FontOracleRunner.OracleProbe freeType = FontOracleRunner.probeFreeType(repositoryRoot());
        FontOracleRunner.OracleProbe harfBuzz = FontOracleRunner.probeHarfBuzz(repositoryRoot());
        assertNotNull(freeType.status());
        assertNotNull(harfBuzz.status());
        if (!"resolved".equals(freeType.status())) {
            assertEquals("environment-blocked", freeType.status());
            assertTrue(freeType.detail().contains("outline-oracle") || freeType.detail().contains("HIMARI_FREETYPE"));
        }
        if (!"resolved".equals(harfBuzz.status())) {
            assertEquals("environment-blocked", harfBuzz.status());
        }
    }

    /// Walks up from the working directory to the repository root.
    private static Path repositoryRoot() {
        Path dir = Path.of("").toAbsolutePath();
        for (int index = 0; index < 6 && dir != null; index++) {
            if (Files.isRegularFile(dir.resolve("settings.gradle.kts")) && Files.isDirectory(dir.resolve("modules"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("Could not locate the HimariUI repository root");
    }
}
