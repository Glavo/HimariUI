package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies production reload isolation through [`ReloadIsolation`].
@NotNullByDefault
final class ReloadIsolationTest {
    /// Rejects agent and JBR tokens and accepts the Headless coordinator source.
    @Test
    void rejectsAgentTokensAndAllowsCoordinator() {
        assertFalse(ReloadIsolation.scan("Premain-Class: org.example.Agent").isEmpty());
        assertFalse(ReloadIsolation.scan("import java.lang.instrument.Instrumentation;").isEmpty());
        assertTrue(ReloadIsolation.scan("ReloadCoordinator coordinator = new ReloadCoordinator();").isEmpty());
        Path runtimeMain = Path.of("src/main/java/org/glavo/himari/runtime");
        List<String> violations = ReloadIsolation.scanTree(runtimeMain);
        assertTrue(violations.isEmpty(), String.valueOf(violations));
        Path desktopMain = Path.of("../desktop/src/main/java");
        if (java.nio.file.Files.isDirectory(desktopMain)) {
            List<String> desktop = ReloadIsolation.scanTree(desktopMain);
            assertTrue(desktop.isEmpty(), String.valueOf(desktop));
        }
    }
}
