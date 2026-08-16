package org.glavo.himari.runtime.reload;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/// Scans production sources for development-only reload machinery.
///
/// [`ReloadCoordinator`] is the VM-independent contract and is allowed. Java agents, enhanced
/// redefinition vendor APIs, and the instrumentation package must not appear in production modules.
@NotNullByDefault
public final class ReloadIsolation {
    /// Forbidden production tokens, assembled so this file does not contain the literals.
    private static final Pattern FORBIDDEN = Pattern.compile(
            String.join(
                    "|",
                    "java\\." + "lang\\." + "instrument",
                    "Pre" + "main-Class",
                    "Agent" + "-Class",
                    "hotswap" + "agent",
                    "dce" + "vm",
                    "com\\.jetbrains\\." + "internal"
            ),
            Pattern.CASE_INSENSITIVE
    );

    /// Prevents instantiation.
    private ReloadIsolation() {
    }

    /// Returns one diagnostic per forbidden token in `source`.
    ///
    /// @param source Java or resource text
    /// @return the diagnostics, empty when the source is clean
    public static @Unmodifiable List<String> scan(String source) {
        Objects.requireNonNull(source, "source");
        ArrayList<String> violations = new ArrayList<>();
        Matcher matcher = FORBIDDEN.matcher(source);
        while (matcher.find()) {
            violations.add(matcher.group());
        }
        return List.copyOf(violations);
    }

    /// Walks `root` and scans regular files whose names end in `.java` or `.MF`.
    ///
    /// @param root a source or resource tree
    /// @return path-prefixed diagnostics
    public static @Unmodifiable List<String> scanTree(Path root) {
        Objects.requireNonNull(root, "root");
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Reload isolation root is not a directory: " + root);
        }
        ArrayList<String> violations = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!name.endsWith(".java") && !name.endsWith(".mf")) {
                    return;
                }
                String text;
                try {
                    text = Files.readString(file, StandardCharsets.UTF_8);
                } catch (IOException exception) {
                    violations.add(file + ": " + exception.getMessage());
                    return;
                }
                List<String> found = scan(text);
                for (int index = 0; index < found.size(); index++) {
                    violations.add(file + ": " + found.get(index));
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot walk " + root, exception);
        }
        return List.copyOf(violations);
    }
}
