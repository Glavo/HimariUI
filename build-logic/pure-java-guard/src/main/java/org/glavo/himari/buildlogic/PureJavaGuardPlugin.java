package org.glavo.himari.buildlogic;

import org.gradle.api.GradleException;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.jetbrains.annotations.NotNullByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

/// Installs the staged pure-Java distribution gates on a Java module.
@NotNullByDefault
public final class PureJavaGuardPlugin implements Plugin<Project> {
    /// File suffixes forbidden in core and desktop runtime JARs.
    private static final Set<String> NATIVE_SUFFIXES = Set.of(
            ".a",
            ".dll",
            ".dylib",
            ".exe",
            ".jnilib",
            ".lib",
            ".o",
            ".obj",
            ".so",
            ".wasm"
    );

    /// Runtime resource suffixes that require provenance records.
    private static final Set<String> PROVENANCE_SUFFIXES = Set.of(
            "bin",
            "dat",
            "otf",
            "spv",
            "ttf",
            "wasm"
    );

    /// Dependency coordinate prefixes forbidden in production runtime graphs.
    private static final Set<String> BANNED_RUNTIME_COORDINATES = Set.of(
            "com.github.jnr:",
            "com.googlecode.juniversalchardet:",
            "com.sun.jna:",
            "org.bytedeco:",
            "org.graalvm.nativeimage:",
            "org.lwjgl:"
    );

    /// Patterns for native bridges that are never valid production dependencies.
    private static final List<Pattern> ALTERNATE_NATIVE_ACCESS_PATTERNS = List.of(
            Pattern.compile("\\bcom\\.sun\\.jna\\b"),
            Pattern.compile("\\borg\\.lwjgl\\b"),
            Pattern.compile("\\borg\\.graalvm\\.nativeimage\\b")
    );

    /// Patterns for FFM linkage operations restricted to designated boundary modules.
    private static final List<Pattern> FFM_LINKAGE_PATTERNS = List.of(
            Pattern.compile("\\bLinker\\s*\\.\\s*nativeLinker\\s*\\("),
            Pattern.compile("\\bdowncallHandle\\s*\\("),
            Pattern.compile("\\bupcallStub\\s*\\(")
    );

    /// Applies the guard after the Java plugin establishes source sets and archive tasks.
    ///
    /// @param project the project receiving the guard
    @Override
    public void apply(Project project) {
        PureJavaGuardExtension extension = project.getExtensions().create(
                "pureJavaGuardConfig",
                PureJavaGuardExtension.class
        );
        extension.getFfmBoundary().convention(false);
        extension.getNativeAccess().convention(false);

        project.getPlugins().withType(
                JavaPlugin.class,
                ignored -> configureJavaProject(project, extension)
        );
    }

    /// Registers active and future guard tasks for a Java project.
    ///
    /// @param project the project being configured
    /// @param extension the module-specific guard configuration
    private static void configureJavaProject(Project project, PureJavaGuardExtension extension) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        NamedDomainObjectProvider<SourceSet> mainSourceSet = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME);
        TaskProvider<Jar> jarTask = project.getTasks().named(JavaPlugin.JAR_TASK_NAME, Jar.class);
        Configuration runtimeClasspath = project.getConfigurations().getByName(
                JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
        );

        List<GateDefinition> gateDefinitions = List.of(
                new GateDefinition("verifyNoNativeEntries", "M0", true),
                new GateDefinition("verifyDependencyAllowlist", "M0", true),
                new GateDefinition("verifyNoDesktopModule", "M0", true),
                new GateDefinition("verifyNoUnsupportedJdkApi", "M0", true),
                new GateDefinition("verifyNoNativeKeyword", "M0", true),
                new GateDefinition("verifyNoExtractionPattern", "M0", true),
                new GateDefinition("verifyNativeLoadTrace", "M0", true),
                new GateDefinition("verifyReproducibleArtifacts", "M0", true),
                new GateDefinition("verifyLicenseManifest", "M0", true),
                new GateDefinition("verifyTestRuntimeIsolation", "M0", true),
                new GateDefinition("verifySingleFfmPath", "M0", true),
                new GateDefinition("verifySceneCodec", "M3", false),
                new GateDefinition("verifyWebHostIsolation", "W0/W1", false),
                new GateDefinition("verifyMobileAotIsolation", "A0", false)
        );

        List<TaskProvider<Task>> activeTasks = new ArrayList<>();
        activeTasks.add(guardTask(
                project,
                "verifyNoNativeEntries",
                "Rejects native payloads in the module and its runtime JARs.",
                List.of(jarTask, runtimeClasspath),
                () -> {
                    Set<File> archives = new LinkedHashSet<>();
                    archives.add(jarTask.get().getArchiveFile().get().getAsFile());
                    runtimeClasspath.getFiles().stream()
                            .filter(File::isFile)
                            .filter(file -> extension(file).equals("jar"))
                            .forEach(archives::add);

                    List<String> violations = archives.stream()
                            .flatMap(archive -> nativeArchiveEntries(archive).stream())
                            .toList();
                    failIfNotEmpty("native archive entries", violations);
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifyDependencyAllowlist",
                "Compares runtime dependencies with the approved pure-Java allowlist.",
                List.of(runtimeClasspath),
                () -> {
                    Set<String> allowlist = readAllowlist(
                            project.getRootProject().file("gradle/pure-java-runtime-allowlist.txt")
                    );
                    List<String> violations = runtimeCoordinates(runtimeClasspath).stream()
                            .filter(Predicate.not(allowlist::contains))
                            .sorted()
                            .toList();
                    failIfNotEmpty("unapproved runtime dependencies", violations);
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifyNoDesktopModule",
                "Uses jdeps to reject java.desktop from the module graph.",
                List.of(jarTask, runtimeClasspath),
                () -> {
                    ToolResult result = runJdeps(
                            project,
                            jarTask.get().getArchiveFile().get().getAsFile(),
                            runtimeClasspath.getFiles(),
                            "--recursive",
                            "--print-module-deps"
                    );
                    if (result.exitCode() != 0) {
                        throw new GradleException("jdeps failed for " + project.getPath() + ":\n" + result.output());
                    }

                    boolean usesDesktop = Pattern.compile("[,\\r\\n]+")
                            .splitAsStream(result.output())
                            .map(String::trim)
                            .anyMatch("java.desktop"::equals);
                    if (usesDesktop) {
                        throw new GradleException(project.getPath() + " depends on forbidden module java.desktop");
                    }
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifyNoUnsupportedJdkApi",
                "Uses jdeps to reject internal JDK APIs.",
                List.of(jarTask, runtimeClasspath),
                () -> {
                    ToolResult result = runJdeps(
                            project,
                            jarTask.get().getArchiveFile().get().getAsFile(),
                            runtimeClasspath.getFiles(),
                            "--jdk-internals"
                    );
                    if (result.exitCode() != 0) {
                        throw new GradleException(
                                "jdeps --jdk-internals failed for " + project.getPath() + ":\n" + result.output()
                        );
                    }
                    if (result.output().contains("JDK internal API")) {
                        throw new GradleException(
                                project.getPath() + " uses an internal JDK API:\n" + result.output()
                        );
                    }
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifyNoNativeKeyword",
                "Rejects Java native method declarations in production sources.",
                List.of(),
                () -> failIfNotEmpty(
                        "native method declarations",
                        findSourceMatches(
                                mainSourceSet.get().getAllSource().getFiles(),
                                List.of(Pattern.compile("\\bnative\\b"))
                        )
                )
        ));

        activeTasks.add(guardTask(
                project,
                "verifyNoExtractionPattern",
                "Rejects framework-owned native loading and extraction patterns.",
                List.of(),
                () -> {
                    Set<File> sourceFiles = mainSourceSet.get().getAllSource().getFiles();
                    List<String> violations = new ArrayList<>(findSourceMatches(
                            sourceFiles,
                            List.of(Pattern.compile("\\bSystem\\s*\\.\\s*load(?:Library)?\\s*\\("))
                    ));

                    sourceFiles.stream()
                            .filter(File::isFile)
                            .filter(file -> Set.of("java", "kt").contains(extension(file)))
                            .forEach(file -> {
                                String code = codeOnly(readUtf8(file));
                                String lower = code.toLowerCase(Locale.ROOT);
                                boolean createsTemporaryFile = lower.contains("createtempfile")
                                        || lower.contains("createtempdirectory");
                                if (createsTemporaryFile && NATIVE_SUFFIXES.stream().anyMatch(lower::contains)) {
                                    violations.add(invariantPath(file) + ": temporary native-file pattern");
                                }
                            });

                    failIfNotEmpty("native loading or extraction patterns", violations);
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifyNativeLoadTrace",
                "Validates declared native-library load traces against the system allowlist.",
                List.of(),
                () -> {
                    if (!extension.getNativeAccess().get()) {
                        project.getLogger().info(
                                project.getPath() + " declares no native access; the load trace is empty by contract."
                        );
                        return;
                    }

                    if (!extension.getNativeLoadTrace().isPresent()) {
                        throw new GradleException(project.getPath() + " declares native access but has no nativeLoadTrace");
                    }
                    File trace = extension.getNativeLoadTrace().get().getAsFile();
                    if (!trace.isFile()) {
                        throw new GradleException("Native load trace does not exist: " + trace);
                    }

                    Set<String> allowlist = readAllowlist(
                            project.getRootProject().file("gradle/native-load-system-allowlist.txt")
                    );
                    List<String> violations = loadedLibraryBasenames(readUtf8Lines(trace)).stream()
                            .filter(Predicate.not(allowlist::contains))
                            .toList();
                    failIfNotEmpty("unapproved native-library trace records", violations);
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifyReproducibleArtifacts",
                "Requires deterministic archive ordering and timestamps.",
                List.of(jarTask),
                () -> {
                    List<String> violations = project.getTasks().withType(AbstractArchiveTask.class).stream()
                            .filter(task -> task.isPreserveFileTimestamps() || !task.isReproducibleFileOrder())
                            .map(Task::getPath)
                            .toList();
                    failIfNotEmpty("non-reproducible archive tasks", violations);
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifyLicenseManifest",
                "Requires provenance for generated and externally sourced runtime data.",
                List.of(),
                () -> {
                    File manifest = project.getRootProject().file("PROVENANCE.json");
                    if (!manifest.isFile()) {
                        throw new GradleException("Missing provenance manifest: " + manifest);
                    }

                    String manifestText = readUtf8(manifest);
                    boolean validSchema = Pattern.compile("\\\"schemaVersion\\\"\\s*:\\s*1\\b")
                            .matcher(manifestText)
                            .find();
                    boolean hasEntries = Pattern.compile("\\\"entries\\\"\\s*:\\s*\\[")
                            .matcher(manifestText)
                            .find();
                    if (!validSchema || !hasEntries) {
                        throw new GradleException("PROVENANCE.json does not match schema version 1");
                    }

                    List<String> violations = mainSourceSet.get().getResources().getFiles().stream()
                            .filter(File::isFile)
                            .filter(file -> PROVENANCE_SUFFIXES.contains(extension(file).toLowerCase(Locale.ROOT)))
                            .map(file -> relativeInvariantPath(project.getRootDir(), file))
                            .filter(Predicate.not(manifestText::contains))
                            .toList();
                    failIfNotEmpty("runtime resources missing from PROVENANCE.json", violations);
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifyTestRuntimeIsolation",
                "Rejects Oracle and native-wrapper dependencies from production runtime graphs.",
                List.of(runtimeClasspath),
                () -> {
                    List<String> violations = runtimeClasspath.getIncoming()
                            .getResolutionResult()
                            .getAllComponents()
                            .stream()
                            .map(component -> component.getId())
                            .map(PureJavaGuardPlugin::forbiddenRuntimeComponent)
                            .filter(Predicate.not(String::isEmpty))
                            .toList();
                    failIfNotEmpty("test or Oracle dependencies in the production runtime", violations);
                }
        ));

        activeTasks.add(guardTask(
                project,
                "verifySingleFfmPath",
                "Confines FFM linkage to generated or explicitly allowlisted boundary modules.",
                List.of(),
                () -> {
                    Set<File> sourceFiles = mainSourceSet.get().getAllSource().getFiles();
                    List<String> violations = new ArrayList<>(findSourceMatches(
                            sourceFiles,
                            ALTERNATE_NATIVE_ACCESS_PATTERNS
                    ));
                    if (!extension.getFfmBoundary().get()) {
                        violations.addAll(findSourceMatches(sourceFiles, FFM_LINKAGE_PATTERNS));
                    }
                    failIfNotEmpty("alternate or out-of-boundary native access", violations);
                }
        ));

        gateDefinitions.stream().filter(Predicate.not(GateDefinition::active)).forEach(gate ->
                project.getTasks().register(gate.taskName(), task -> {
                    task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
                    task.setDescription(
                            "Reports the " + gate.taskName() + " gate as not applicable before " + gate.milestone() + "."
                    );
                    task.doLast(ignored -> project.getLogger().lifecycle(
                            gate.taskName() + ": NOT_APPLICABLE (activates at " + gate.milestone() + ")"
                    ));
                })
        );

        var reportFile = project.getLayout().getBuildDirectory().file("reports/pure-java-guard/registry.txt");
        TaskProvider<Task> reportTask = project.getTasks().register("pureJavaGuardReport", task -> {
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            task.setDescription("Writes the staged pure-Java gate registry.");
            task.getOutputs().file(reportFile);
            task.doLast(ignored -> {
                File output = reportFile.get().getAsFile();
                StringBuilder report = new StringBuilder("project=").append(project.getPath()).append('\n');
                gateDefinitions.forEach(gate -> report
                        .append(gate.taskName())
                        .append('=')
                        .append(gate.active() ? "ACTIVE" : "NOT_APPLICABLE")
                        .append(" milestone=")
                        .append(gate.milestone())
                        .append('\n'));
                writeUtf8(output, report.toString());
            });
        });

        TaskProvider<Task> aggregate = project.getTasks().register("pureJavaGuard", task -> {
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            task.setDescription("Runs every pure-Java gate active for this module.");
            task.dependsOn(activeTasks);
            task.dependsOn(reportTask);
        });
        project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure(task -> task.dependsOn(aggregate));
    }

    /// Registers a guard task whose action is evaluated only during task execution.
    ///
    /// @param project the task-owning project
    /// @param name the task name
    /// @param description the task description
    /// @param dependencies build dependencies required before evaluation
    /// @param action the guard assertion
    /// @return the registered task provider
    private static TaskProvider<Task> guardTask(
            Project project,
            String name,
            String description,
            Collection<?> dependencies,
            Runnable action
    ) {
        return project.getTasks().register(name, task -> {
            task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
            task.setDescription(description);
            task.dependsOn(dependencies);
            task.notCompatibleWithConfigurationCache(
                    "The guard inspects resolved graphs, source text, and JDK tool output."
            );
            task.doLast(ignored -> action.run());
        });
    }

    /// Returns external module coordinates present in a runtime configuration.
    ///
    /// @param configuration the resolved runtime configuration
    /// @return sorted `group:module` coordinates
    private static Set<String> runtimeCoordinates(Configuration configuration) {
        return configuration.getIncoming().getResolutionResult().getAllComponents().stream()
                .map(component -> component.getId())
                .filter(ModuleComponentIdentifier.class::isInstance)
                .map(ModuleComponentIdentifier.class::cast)
                .map(identifier -> identifier.getGroup() + ":" + identifier.getModule())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /// Lists forbidden native entries in a JAR.
    ///
    /// @param archive the candidate JAR
    /// @return invariant archive-entry descriptions
    private static List<String> nativeArchiveEntries(File archive) {
        if (!archive.isFile() || !extension(archive).equalsIgnoreCase("jar")) {
            return List.of();
        }

        try (ZipFile zip = new ZipFile(archive)) {
            return zip.stream()
                    .filter(Predicate.not(entry -> entry.isDirectory()))
                    .map(entry -> entry.getName())
                    .filter(name -> {
                        String lower = name.toLowerCase(Locale.ROOT);
                        return NATIVE_SUFFIXES.stream().anyMatch(lower::endsWith);
                    })
                    .map(name -> invariantPath(archive) + "!/" + name)
                    .toList();
        } catch (IOException exception) {
            throw new GradleException("Cannot inspect archive " + archive, exception);
        }
    }

    /// Executes the JDK dependency analyzer against a module archive.
    ///
    /// @param project the project used for logging
    /// @param archive the module archive to inspect
    /// @param runtimeFiles files placed on the module path
    /// @param arguments additional `jdeps` arguments
    /// @return the process exit code and merged output
    private static ToolResult runJdeps(
            Project project,
            File archive,
            Set<File> runtimeFiles,
            String... arguments
    ) {
        String executableName = System.getProperty("os.name").startsWith("Windows") ? "jdeps.exe" : "jdeps";
        File executable = Path.of(System.getProperty("java.home"), "bin", executableName).toFile();
        if (!executable.isFile()) {
            throw new GradleException("jdeps was not found in the Gradle JVM: " + executable);
        }

        List<String> command = new ArrayList<>();
        command.add(executable.getAbsolutePath());
        command.add("--ignore-missing-deps");
        String modulePath = runtimeFiles.stream()
                .filter(File::exists)
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));
        if (!modulePath.isEmpty()) {
            command.add("--module-path");
            command.add(modulePath);
        }
        command.addAll(Arrays.asList(arguments));
        command.add(archive.getAbsolutePath());

        project.getLogger().info("Running " + String.join(" ", command));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return new ToolResult(process.waitFor(), output);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GradleException("Interrupted while running jdeps for " + archive, exception);
        } catch (IOException exception) {
            throw new GradleException("Cannot run jdeps for " + archive, exception);
        }
    }

    /// Reads a line-oriented allowlist, ignoring comments and empty lines.
    ///
    /// @param file the allowlist file
    /// @return its entries
    private static Set<String> readAllowlist(File file) {
        if (!file.isFile()) {
            throw new GradleException("Missing allowlist: " + file);
        }
        return readUtf8Lines(file).stream()
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .filter(line -> !line.startsWith("#"))
                .collect(Collectors.toUnmodifiableSet());
    }

    /// Extracts loaded-library basenames from JDK unified `library` log records.
    ///
    /// Symbol lookup, failed lookup, and unload records are deliberately excluded; only lines containing a successful
    /// `Loaded library` event contribute to the distribution allowlist check.
    ///
    /// @param lines the complete unified-log lines
    /// @return the loaded basenames in log order
    private static List<String> loadedLibraryBasenames(List<String> lines) {
        String marker = "Loaded library ";
        String handleMarker = ", handle ";
        List<String> libraries = new ArrayList<>();
        for (String line : lines) {
            int start = line.indexOf(marker);
            if (start < 0) {
                continue;
            }
            start += marker.length();
            int end = line.indexOf(handleMarker, start);
            if (end < 0) {
                end = line.length();
            }
            String path = line.substring(start, end).trim();
            if (!path.isEmpty()) {
                libraries.add(new File(path).getName());
            }
        }
        return List.copyOf(libraries);
    }

    /// Finds source-code occurrences after comments and literals are masked.
    ///
    /// @param files candidate source files
    /// @param patterns forbidden code patterns
    /// @return invariant path, line, and match descriptions
    private static List<String> findSourceMatches(Set<File> files, List<Pattern> patterns) {
        List<String> matches = new ArrayList<>();
        files.stream()
                .filter(File::isFile)
                .filter(file -> Set.of("java", "kt").contains(extension(file)))
                .forEach(file -> {
                    String code = codeOnly(readUtf8(file));
                    patterns.forEach(pattern -> pattern.matcher(code).results().forEach(result -> {
                        long line = code.substring(0, result.start()).chars().filter(character -> character == '\n').count() + 1;
                        matches.add(invariantPath(file) + ":" + line + ": " + result.group());
                    }));
                });
        return List.copyOf(matches);
    }

    /// Replaces comments and character, string, and text-block contents with spaces while preserving newlines.
    ///
    /// @param source Java or Kotlin source text
    /// @return source-shaped text containing only executable tokens
    private static String codeOnly(String source) {
        char[] output = new char[source.length()];
        Arrays.fill(output, ' ');
        LexicalState state = LexicalState.CODE;
        int index = 0;

        while (index < source.length()) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            char third = index + 2 < source.length() ? source.charAt(index + 2) : '\0';

            switch (state) {
                case CODE -> {
                    if (current == '/' && next == '/') {
                        state = LexicalState.LINE_COMMENT;
                        index += 2;
                        continue;
                    }
                    if (current == '/' && next == '*') {
                        state = LexicalState.BLOCK_COMMENT;
                        index += 2;
                        continue;
                    }
                    if (current == '"' && next == '"' && third == '"') {
                        state = LexicalState.TEXT_BLOCK;
                        index += 3;
                        continue;
                    }
                    if (current == '"') {
                        state = LexicalState.STRING;
                    } else if (current == '\'') {
                        state = LexicalState.CHARACTER;
                    } else {
                        output[index] = current;
                    }
                }
                case LINE_COMMENT -> {
                    if (current == '\n') {
                        output[index] = current;
                        state = LexicalState.CODE;
                    }
                }
                case BLOCK_COMMENT -> {
                    if (current == '*' && next == '/') {
                        state = LexicalState.CODE;
                        index += 2;
                        continue;
                    }
                    if (current == '\n') {
                        output[index] = current;
                    }
                }
                case STRING, CHARACTER -> {
                    if (current == '\\') {
                        index += 2;
                        continue;
                    }
                    if ((state == LexicalState.STRING && current == '"')
                            || (state == LexicalState.CHARACTER && current == '\'')) {
                        state = LexicalState.CODE;
                    } else if (current == '\n') {
                        output[index] = current;
                    }
                }
                case TEXT_BLOCK -> {
                    if (current == '"' && next == '"' && third == '"') {
                        state = LexicalState.CODE;
                        index += 3;
                        continue;
                    }
                    if (current == '\n') {
                        output[index] = current;
                    }
                }
            }
            index++;
        }

        return new String(output);
    }

    /// Fails the current task when a collection contains violations.
    ///
    /// @param label the violation category
    /// @param violations violation descriptions
    private static void failIfNotEmpty(String label, Collection<String> violations) {
        if (violations.isEmpty()) {
            return;
        }
        String details = violations.stream()
                .sorted()
                .map(violation -> "  - " + violation)
                .collect(Collectors.joining("\n"));
        throw new GradleException("Found " + label + ":\n" + details);
    }

    /// Returns a forbidden production-runtime coordinate or an empty string for an allowed component.
    ///
    /// @param identifier the resolved component identifier
    /// @return the forbidden coordinate or path, or an empty string
    private static String forbiddenRuntimeComponent(ComponentIdentifier identifier) {
        if (identifier instanceof ModuleComponentIdentifier module) {
            String coordinate = module.getGroup() + ":" + module.getModule();
            return BANNED_RUNTIME_COORDINATES.stream().anyMatch(coordinate::startsWith) ? coordinate : "";
        }
        if (identifier instanceof ProjectComponentIdentifier project) {
            String path = project.getProjectPath();
            return path.startsWith(":oracles:") || path.startsWith(":external:") ? path : "";
        }
        return "";
    }

    /// Returns a file extension without its leading period.
    ///
    /// @param file the file to inspect
    /// @return the extension, or an empty string when absent
    private static String extension(File file) {
        String name = file.getName();
        int separator = name.lastIndexOf('.');
        return separator < 0 ? "" : name.substring(separator + 1);
    }

    /// Returns a path with forward-slash separators.
    ///
    /// @param file the file to describe
    /// @return the invariant path
    private static String invariantPath(File file) {
        return file.toPath().toString().replace(File.separatorChar, '/');
    }

    /// Returns an invariant path relative to a root directory.
    ///
    /// @param root the root directory
    /// @param file the descendant file
    /// @return the relative invariant path
    private static String relativeInvariantPath(File root, File file) {
        return root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
    }

    /// Reads an entire UTF-8 file.
    ///
    /// @param file the file to read
    /// @return its contents
    private static String readUtf8(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot read " + file, exception);
        }
    }

    /// Reads all lines from a UTF-8 file.
    ///
    /// @param file the file to read
    /// @return its lines
    private static List<String> readUtf8Lines(File file) {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot read " + file, exception);
        }
    }

    /// Writes a complete UTF-8 file and creates its parent directory when needed.
    ///
    /// @param file the destination file
    /// @param content the complete file contents
    private static void writeUtf8(File file, String content) {
        try {
            Files.createDirectories(file.toPath().getParent());
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot write " + file, exception);
        }
    }

    /// Configures module-specific exceptions for the pure-Java guard.
    @NotNullByDefault
    public abstract static class PureJavaGuardExtension {
        /// Creates an extension instance managed by Gradle.
        @Inject
        public PureJavaGuardExtension() {
        }

        /// Returns whether the module is an allowlisted FFM boundary.
        ///
        /// @return the boundary convention property
        public abstract Property<Boolean> getFfmBoundary();

        /// Returns whether the module intentionally loads operating-system libraries.
        ///
        /// @return the native-access convention property
        public abstract Property<Boolean> getNativeAccess();

        /// Returns the captured `-Xlog:library+load` output for a native-access module.
        ///
        /// @return the trace file property
        public abstract RegularFileProperty getNativeLoadTrace();
    }

    /// Describes one staged guard and its activation milestone.
    ///
    /// @param taskName the Gradle task name
    /// @param milestone the activation milestone
    /// @param active whether the gate is currently enforced
    @NotNullByDefault
    private record GateDefinition(String taskName, String milestone, boolean active) {
    }

    /// Captures the result of an external JDK tool invocation.
    ///
    /// @param exitCode the process exit code
    /// @param output merged standard output and error text
    @NotNullByDefault
    private record ToolResult(int exitCode, String output) {
    }

    /// Tracks lexical regions while source comments and literals are masked.
    @NotNullByDefault
    private enum LexicalState {
        /// Executable source code.
        CODE,

        /// A line comment.
        LINE_COMMENT,

        /// A block comment.
        BLOCK_COMMENT,

        /// A string literal.
        STRING,

        /// A character literal.
        CHARACTER,

        /// A text block.
        TEXT_BLOCK
    }
}
