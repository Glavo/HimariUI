package org.glavo.himari.packaging;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the Counter Native Image recipe through [`NativeImageProbe`].
@NotNullByDefault
final class NativeImageProbeTest {
    /// Counter jar used to prove the command line names the sample.
    private static final String COUNTER_JAR = "himari-samples-counter-0.1.0-SNAPSHOT.jar";

    /// The shipped command includes `-cp`, the Counter jar, and the sample main class.
    @Test
    void commandLineReferencesCounterArtifacts() {
        Path nativeImage = Path.of("native-image.cmd");
        Path output = Path.of("image-out");
        List<String> classPath = List.of(COUNTER_JAR, "himari-runtime-0.1.0-SNAPSHOT.jar");
        List<String> command = NativeImageProbe.commandLine(nativeImage, output, classPath);
        assertFalse(command.contains("--dry-run"));
        assertTrue(command.contains("-cp"));
        String joined = command.get(command.indexOf("-cp") + 1);
        assertTrue(joined.contains("himari-samples-counter"));
        assertTrue(command.contains(NativeImageProbe.COUNTER_MAIN));
        String imageOut = command.get(command.indexOf("-o") + 1);
        assertTrue(imageOut.contains(NativeImageProbe.IMAGE_NAME));
        assertFalse(imageOut.endsWith(".exe.exe"));
        assertTrue(NativeImageProbe.referencesCounter(classPath));
    }

    /// An empty classpath never claims a built image.
    @Test
    void emptyClasspathDoesNotClaimImage(@TempDir Path output) {
        NativeImageProbe.Result result = NativeImageProbe.probe(output, List.of());
        assertFalse(result.builtImage());
        assertEquals(NativeImageProbe.COUNTER_MAIN, result.mainClass());
        if (NativeImageProbe.locate() == null) {
            assertTrue(result.environmentBlocked());
        } else {
            assertFalse(result.environmentBlocked());
            assertTrue(result.detail().contains("classpath was not supplied"));
        }
    }

    /// A missing Counter jar is invoked only when the toolchain exists, and still does not invent an image.
    @Test
    void missingCounterJarDoesNotClaimImage(@TempDir Path output) {
        List<String> classPath = List.of(output.resolve(COUNTER_JAR).toString());
        NativeImageProbe.Result result = NativeImageProbe.probe(output, classPath);
        assertTrue(result.toJson().contains("himari-samples-counter"));
        assertTrue(result.toJson().contains("\"referencedCounter\": true"));
        assertFalse(result.builtImage());
        assertFalse(Files.isRegularFile(NativeImageProbe.imageFile(output)));
        if (NativeImageProbe.locate() == null) {
            assertTrue(result.environmentBlocked());
            assertEquals(-1, result.exitCode());
        } else {
            assertFalse(result.environmentBlocked());
            List<String> command = NativeImageProbe.commandLine(
                    Path.of(result.nativeImagePath()),
                    output,
                    classPath
            );
            assertFalse(command.contains("--dry-run"));
            assertTrue(command.contains("-cp"));
            assertTrue(command.contains(NativeImageProbe.COUNTER_MAIN));
            assertFalse(result.builtImage());
        }
    }

    /// When the toolchain is present the M11 image must exist and name Counter; otherwise this is a captured block.
    @Test
    void toolchainRequiresRealCounterImageOrCapturedBlock() throws Exception {
        @Nullable Path nativeImage = NativeImageProbe.locate();
        Path packaging = packagingDirectory();
        if (nativeImage == null) {
            assertTrue(packaging == null || !Files.isRegularFile(
                    NativeImageProbe.imageFile(packaging.resolve("native-image-counter"))
            ) || Files.readString(packaging.resolve("native-image-probe.json")).contains("environment-blocked"));
            return;
        }
        if (packaging == null) {
            throw new AssertionError("native-image is on PATH but M11 packaging output is missing");
        }
        Path imageDirectory = packaging.resolve("native-image-counter");
        Path probeFile = packaging.resolve("native-image-probe.json");
        Path resultsFile = packaging.resolve("results.json");
        if (!Files.isRegularFile(probeFile) || !Files.isRegularFile(resultsFile)) {
            throw new AssertionError("native-image is present but native-image-probe.json was not written");
        }
        String probe = Files.readString(probeFile);
        String results = Files.readString(resultsFile);
        assertTrue(probe.contains("\"mainClass\": \"org.glavo.himari.samples.counter.V0CounterApp\""));
        assertTrue(probe.contains("/samples/counter/") || probe.contains("himari-samples-counter")
                || probe.contains("himari-counter-"));
        assertFalse(probe.contains("--dry-run"));
        Path exe = NativeImageProbe.imageFile(imageDirectory);
        if (!Files.isRegularFile(exe)) {
            throw new AssertionError("native-image is present but " + exe + " is missing; probe=" + probe);
        }
        assertTrue(probe.contains("\"builtImage\": true"));
        assertTrue(probe.contains("\"exitCode\": 0"));
        assertTrue(results.contains("\"nativeImageBuiltImage\": true"));
        assertTrue(results.contains("\"nativeImageReferencedCounter\": true"));
        assertTrue(Files.size(exe) > 1_000_000L);
    }

    /// Resolves the M11 packaging directory from the module or repository root.
    private static @Nullable Path packagingDirectory() {
        Path cwd = Path.of("").toAbsolutePath();
        Path[] candidates = {
                cwd.resolve("build/conformance/m11-packaging"),
                cwd.resolve("../../build/conformance/m11-packaging")
        };
        for (int index = 0; index < candidates.length; index++) {
            Path candidate = candidates[index].normalize();
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
