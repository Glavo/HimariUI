package org.glavo.himari.buildlogic;

import org.gradle.api.GradleException;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.glavo.himari.buildlogic.JsonParser.JsonArray;
import static org.glavo.himari.buildlogic.JsonParser.JsonBoolean;
import static org.glavo.himari.buildlogic.JsonParser.JsonNumber;
import static org.glavo.himari.buildlogic.JsonParser.JsonObject;
import static org.glavo.himari.buildlogic.JsonParser.JsonString;
import static org.glavo.himari.buildlogic.JsonParser.JsonValue;

/// Validates the governance artifacts that form the M0 repository baseline.
@NotNullByDefault
final class RepositoryGovernanceValidator {
    /// The accepted ADR identifiers established by the current plan.
    private static final @Unmodifiable Set<String> ACCEPTED_ADRS = range("001", "019", "022");

    /// The unresolved ADR identifiers that require M1 evidence.
    private static final @Unmodifiable Set<String> PROPOSED_ADRS = Set.of("020", "021");

    /// The complete canonical ADR identifier set.
    private static final @Unmodifiable Set<String> ALL_ADRS = union(ACCEPTED_ADRS, PROPOSED_ADRS);

    /// Metadata fields required in every canonical ADR.
    private static final @Unmodifiable Set<String> ADR_METADATA_FIELDS = Set.of(
            "Status",
            "Date",
            "Decision milestone",
            "Evidence",
            "Supersedes",
            "Superseded by"
    );

    /// Section headings required in every canonical ADR.
    private static final @Unmodifiable List<String> ADR_SECTIONS = List.of(
            "## Context",
            "## Decision",
            "## Consequences",
            "## Evidence",
            "## Replacement"
    );

    /// Work packages that must have initial M0 conformance profiles.
    private static final @Unmodifiable Set<String> REQUIRED_M0_PROFILES = Set.of(
            "FFI-SCHEMA-001",
            "FFI-FFM-001",
            "ABI-PROBE-001",
            "SPIKE-WAYLAND-001",
            "SPIKE-VK-001",
            "SPIKE-WIN-001",
            "SPIKE-D3D12-001",
            "SPIKE-MAC-001",
            "SPIKE-METAL-001",
            "SPIKE-OBJC-BLOCK-001",
            "NI-FFM-001"
    );

    /// Status values supported by the conformance policy.
    private static final @Unmodifiable Set<String> CONFORMANCE_STATUSES = Set.of(
            "planned",
            "active",
            "passed",
            "failed",
            "waived"
    );

    /// Operating-system selectors supported by conformance environments.
    private static final @Unmodifiable Set<String> CONFORMANCE_OPERATING_SYSTEMS = Set.of(
            "any",
            "linux",
            "windows",
            "macos"
    );

    /// Architecture selectors supported by conformance environments.
    private static final @Unmodifiable Set<String> CONFORMANCE_ARCHITECTURES = Set.of("any", "x86_64", "arm64");

    /// Runtime selectors supported by conformance environments.
    private static final @Unmodifiable Set<String> CONFORMANCE_RUNTIMES = Set.of("jvm", "native-image");

    /// Pin kinds supported by the reference lock.
    private static final @Unmodifiable Set<String> REFERENCE_PIN_KINDS = Set.of(
            "edition",
            "release",
            "commit",
            "retrieval-date"
    );

    /// Runtime resource suffixes that require a provenance record.
    private static final @Unmodifiable Set<String> PROVENANCE_SUFFIXES = Set.of(
            ".bin",
            ".dat",
            ".otf",
            ".spv",
            ".ttf",
            ".wasm"
    );

    /// Repository directories excluded from provenance payload discovery.
    private static final @Unmodifiable Set<String> PROVENANCE_EXCLUDED_DIRECTORIES = Set.of(
            ".git",
            ".gradle",
            ".gradle-user-home",
            ".idea",
            "build",
            "external"
    );

    /// Matches canonical ADR file names.
    private static final Pattern ADR_FILE_NAME = Pattern.compile("ADR-(\\d{3})\\.md");

    /// Matches canonical ADR metadata lines.
    private static final Pattern ADR_METADATA = Pattern.compile("^- \\*\\*([^*]+):\\*\\* (.+)$");

    /// Matches Markdown inline link destinations.
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[[^]\\r\\n]*]\\(([^)\\s]+)");

    /// Matches HTTPS Markdown links in the plan bibliography.
    private static final Pattern HTTPS_MARKDOWN_LINK = Pattern.compile("\\[[^]\\r\\n]*]\\((https://[^)\\s]+)\\)");

    /// Matches lower-case SHA-256 digests.
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    /// Matches stable lower-case conformance profile identifiers.
    private static final Pattern PROFILE_ID = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /// Matches stable upper-case work-package identifiers.
    private static final Pattern WORK_PACKAGE_ID = Pattern.compile("[A-Z0-9]+(?:-[A-Z0-9]+)+");

    /// Prevents instantiation of this utility class.
    private RepositoryGovernanceValidator() {
    }

    /// Validates the repository license, contribution policy, and canonical ADR catalog.
    ///
    /// @param root the repository root
    static void verifyAdrCatalog(Path root) {
        List<Path> policyFiles = new ArrayList<>(List.of(
                requireFile(root, "LICENSE"),
                requireFile(root, "CONTRIBUTING.md"),
                requireFile(root, "adr/README.md"),
                requireFile(root, "adr/TEMPLATE.md")
        ));
        String licenseHeader = readUtf8(root.resolve("LICENSE")).lines()
                .limit(2)
                .map(String::trim)
                .collect(Collectors.joining("\n"));
        require(licenseHeader.equals("Apache License\nVersion 2.0, January 2004"),
                "LICENSE must contain the canonical Apache License 2.0 text");

        Path adrDirectory = root.resolve("adr");
        Map<String, Path> records = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.list(adrDirectory)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                Matcher matcher = ADR_FILE_NAME.matcher(path.getFileName().toString());
                if (matcher.matches()) {
                    @Nullable Path previous = records.put(matcher.group(1), path);
                    require(previous == null, "Duplicate ADR identifier " + matcher.group(1));
                }
            });
        } catch (IOException exception) {
            throw new GradleException("Cannot enumerate " + adrDirectory, exception);
        }
        require(records.keySet().containsAll(ALL_ADRS), setDifferenceMessage(
                "baseline ADR catalog",
                ALL_ADRS,
                records.keySet()
        ));

        List<Path> markdownFiles = new ArrayList<>(policyFiles);
        for (String identifier : new TreeSet<>(records.keySet())) {
            Path path = requireFile(root, "adr/ADR-" + identifier + ".md");
            markdownFiles.add(path);
            String content = readUtf8(path);
            require(content.startsWith("# ADR-" + identifier + ": "),
                    path + " must begin with its canonical identifier and a title");
            Map<String, String> metadata = adrMetadata(path, content);
            require(metadata.keySet().equals(ADR_METADATA_FIELDS),
                    setDifferenceMessage(path + " metadata", ADR_METADATA_FIELDS, metadata.keySet()));

            String status = metadata.getOrDefault("Status", "");
            String date = metadata.getOrDefault("Date", "");
            String decisionMilestone = metadata.getOrDefault("Decision milestone", "");
            String evidence = metadata.getOrDefault("Evidence", "");
            String supersedes = metadata.getOrDefault("Supersedes", "");
            String supersededBy = metadata.getOrDefault("Superseded by", "");
            require(Set.of("Proposed", "Accepted", "Superseded", "Rejected").contains(status),
                    path + " has unsupported status " + status);
            if (ACCEPTED_ADRS.contains(identifier)) {
                require(status.equals("Accepted"), path + " must have status Accepted");
            } else if (PROPOSED_ADRS.contains(identifier)) {
                require(status.equals("Proposed"), path + " must have status Proposed");
            }
            requireIsoDate(date, path + " Date");
            requireNonBlank(decisionMilestone, path + " Decision milestone");
            requireNonBlank(evidence, path + " Evidence metadata");
            requireNonBlank(supersedes, path + " Supersedes metadata");
            requireNonBlank(supersededBy, path + " Superseded by metadata");

            int previousSection = -1;
            for (String section : ADR_SECTIONS) {
                int sectionOffset = content.indexOf("\n" + section + "\n");
                require(sectionOffset > previousSection, path + " is missing or misorders section " + section);
                previousSection = sectionOffset;
            }
            if (!status.equals("Proposed")) {
                require(!content.toUpperCase(Locale.ROOT).contains("TBD"),
                        path + " contains an unresolved placeholder despite its terminal status");
            }
            if (PROPOSED_ADRS.contains(identifier)) {
                require(decisionMilestone.startsWith("M1"),
                        path + " must name its M1 decision milestone");
                require(content.contains("Required:"), path + " must state the evidence required before acceptance");
            }
        }
        verifyMarkdownLinks(root, markdownFiles);
    }

    /// Validates the locked references and exact coverage of the plan bibliography.
    ///
    /// @param root the repository root
    static void verifyReferencesLock(Path root) {
        JsonParser.parseObject(requireFile(root, "schema/references-lock.schema.json"));
        JsonObject document = JsonParser.parseObject(requireFile(root, "REFERENCES.lock"));
        requireExactKeys(document, "REFERENCES.lock", Set.of(
                "$schema",
                "schemaVersion",
                "reviewed",
                "policy",
                "sources"
        ));
        require(stringMember(document, "$schema", "REFERENCES.lock")
                        .equals("schema/references-lock.schema.json"),
                "REFERENCES.lock must identify schema/references-lock.schema.json");
        require(integerMember(document, "schemaVersion", "REFERENCES.lock") == 1,
                "REFERENCES.lock schemaVersion must be 1");
        requireIsoDate(stringMember(document, "reviewed", "REFERENCES.lock"), "REFERENCES.lock reviewed");
        requireNonBlank(stringMember(document, "policy", "REFERENCES.lock"), "REFERENCES.lock policy");

        List<JsonValue> sources = arrayMember(document, "sources", "REFERENCES.lock");
        require(!sources.isEmpty(), "REFERENCES.lock must contain at least one source");
        Set<String> identifiers = new LinkedHashSet<>();
        Set<String> urls = new LinkedHashSet<>();
        for (int index = 0; index < sources.size(); index++) {
            String context = "REFERENCES.lock sources[" + index + "]";
            JsonObject source = asObject(sources.get(index), context);
            requireExactKeys(source, context, Set.of(
                    "id",
                    "category",
                    "title",
                    "url",
                    "pinKind",
                    "pin",
                    "retrieved"
            ));
            String identifier = stringMember(source, "id", context);
            require(PROFILE_ID.matcher(identifier).matches(), context + " has an invalid id");
            require(identifiers.add(identifier), "Duplicate reference id " + identifier);
            requireNonBlank(stringMember(source, "category", context), context + " category");
            requireNonBlank(stringMember(source, "title", context), context + " title");

            String url = stringMember(source, "url", context);
            requireHttpsUrl(url, context + " url");
            require(urls.add(url), "Duplicate reference URL " + url);
            String pinKind = stringMember(source, "pinKind", context);
            require(REFERENCE_PIN_KINDS.contains(pinKind), context + " has unsupported pinKind " + pinKind);
            String pin = stringMember(source, "pin", context);
            requireNonBlank(pin, context + " pin");
            String retrieved = stringMember(source, "retrieved", context);
            requireIsoDate(retrieved, context + " retrieved");
            if (pinKind.equals("retrieval-date")) {
                require(pin.equals(retrieved), context + " retrieval-date pin must equal retrieved");
            }
        }

        String plan = readUtf8(requireFile(root, "PLANS.md"));
        int sectionStart = plan.indexOf("## 28. Primary References");
        int sectionEnd = plan.indexOf("## 29.", sectionStart + 1);
        require(sectionStart >= 0 && sectionEnd > sectionStart,
                "PLANS.md must contain bounded Sections 28 and 29");
        String bibliography = plan.substring(sectionStart, sectionEnd);
        List<String> planUrlList = HTTPS_MARKDOWN_LINK.matcher(bibliography).results()
                .map(result -> result.group(1))
                .collect(Collectors.toCollection(ArrayList::new));
        Set<String> planUrls = new LinkedHashSet<>(planUrlList);
        require(planUrls.size() == planUrlList.size(), "PLANS.md Section 28 contains a duplicate reference URL");
        require(sources.size() == planUrlList.size(),
                "REFERENCES.lock source count differs from PLANS.md Section 28");
        require(planUrls.equals(urls), setDifferenceMessage("reference URL coverage", planUrls, urls));
    }

    /// Validates provenance records, SHA-256 values, and runtime payload coverage.
    ///
    /// @param root the repository root
    static void verifyProvenanceManifest(Path root) {
        JsonParser.parseObject(requireFile(root, "schema/provenance.schema.json"));
        JsonObject document = JsonParser.parseObject(requireFile(root, "PROVENANCE.json"));
        requireExactKeys(document, "PROVENANCE.json", Set.of("$schema", "schemaVersion", "entries"));
        require(stringMember(document, "$schema", "PROVENANCE.json").equals("schema/provenance.schema.json"),
                "PROVENANCE.json must identify schema/provenance.schema.json");
        require(integerMember(document, "schemaVersion", "PROVENANCE.json") == 1,
                "PROVENANCE.json schemaVersion must be 1");

        List<JsonValue> entries = arrayMember(document, "entries", "PROVENANCE.json");
        Set<String> recordedPaths = new LinkedHashSet<>();
        for (int index = 0; index < entries.size(); index++) {
            String context = "PROVENANCE.json entries[" + index + "]";
            JsonObject entry = asObject(entries.get(index), context);
            requireExactKeys(entry, context, Set.of("path", "kind", "source", "version", "license", "sha256"));
            String relativePath = stringMember(entry, "path", context);
            Path target = resolveRepositoryPath(root, relativePath, context + " path");
            require(Files.isRegularFile(target), context + " path does not name a regular file: " + relativePath);
            require(recordedPaths.add(relativePath), "Duplicate provenance path " + relativePath);
            requireNonBlank(stringMember(entry, "kind", context), context + " kind");
            requireHttpsUrl(stringMember(entry, "source", context), context + " source");
            requireNonBlank(stringMember(entry, "version", context), context + " version");
            requireNonBlank(stringMember(entry, "license", context), context + " license");
            String expectedHash = stringMember(entry, "sha256", context);
            require(SHA_256.matcher(expectedHash).matches(), context + " has an invalid SHA-256 digest");
            String actualHash = sha256(target);
            require(expectedHash.equals(actualHash),
                    context + " SHA-256 mismatch for " + relativePath + ": expected " + expectedHash
                            + ", actual " + actualHash);
        }
        require(recordedPaths.contains("LICENSE"), "PROVENANCE.json must record the repository license text");

        Set<String> payloadPaths = discoverProvenancePayloads(root);
        require(recordedPaths.containsAll(payloadPaths), setDifferenceMessage(
                "provenance payload coverage",
                payloadPaths,
                recordedPaths
        ));
    }

    /// Validates platform profile structure, evidence state, limits, and waivers.
    ///
    /// @param root the repository root
    static void verifyPlatformConformance(Path root) {
        requireFile(root, "CONFORMANCE.md");
        JsonParser.parseObject(requireFile(root, "schema/platform-conformance.schema.json"));
        JsonObject document = JsonParser.parseObject(requireFile(root, "PLATFORM_CONFORMANCE.yaml"));
        requireExactKeys(document, "PLATFORM_CONFORMANCE.yaml", Set.of(
                "$schema",
                "schemaVersion",
                "policyVersion",
                "reviewed",
                "defaults",
                "profiles"
        ));
        require(stringMember(document, "$schema", "PLATFORM_CONFORMANCE.yaml")
                        .equals("schema/platform-conformance.schema.json"),
                "PLATFORM_CONFORMANCE.yaml must identify schema/platform-conformance.schema.json");
        require(integerMember(document, "schemaVersion", "PLATFORM_CONFORMANCE.yaml") == 1,
                "PLATFORM_CONFORMANCE.yaml schemaVersion must be 1");
        require(Pattern.matches("[0-9]+\\.[0-9]+",
                        stringMember(document, "policyVersion", "PLATFORM_CONFORMANCE.yaml")),
                "PLATFORM_CONFORMANCE.yaml policyVersion must have major.minor form");
        requireIsoDate(stringMember(document, "reviewed", "PLATFORM_CONFORMANCE.yaml"),
                "PLATFORM_CONFORMANCE.yaml reviewed");
        verifyLimits(objectMember(document, "defaults", "PLATFORM_CONFORMANCE.yaml"),
                "PLATFORM_CONFORMANCE.yaml defaults");

        List<JsonValue> profiles = arrayMember(document, "profiles", "PLATFORM_CONFORMANCE.yaml");
        require(!profiles.isEmpty(), "PLATFORM_CONFORMANCE.yaml must contain at least one profile");
        Set<String> profileIdentifiers = new LinkedHashSet<>();
        Set<String> workPackages = new LinkedHashSet<>();
        for (int index = 0; index < profiles.size(); index++) {
            String context = "PLATFORM_CONFORMANCE.yaml profiles[" + index + "]";
            JsonObject profile = asObject(profiles.get(index), context);
            requireExactKeys(profile, context, Set.of(
                    "id",
                    "profileVersion",
                    "workPackage",
                    "milestone",
                    "required",
                    "status",
                    "owner",
                    "environments",
                    "commands",
                    "fixtures",
                    "assertions",
                    "capabilityModes",
                    "limits",
                    "evidence",
                    "waivers"
            ));

            String identifier = stringMember(profile, "id", context);
            require(PROFILE_ID.matcher(identifier).matches(), context + " has an invalid id " + identifier);
            require(profileIdentifiers.add(identifier), "Duplicate conformance profile id " + identifier);
            require(integerMember(profile, "profileVersion", context) >= 1,
                    context + " profileVersion must be positive");

            String workPackage = stringMember(profile, "workPackage", context);
            require(WORK_PACKAGE_ID.matcher(workPackage).matches(),
                    context + " has an invalid workPackage " + workPackage);
            workPackages.add(workPackage);
            requireNonBlank(stringMember(profile, "milestone", context), context + " milestone");
            boolean required = booleanMember(profile, "required", context);
            String status = stringMember(profile, "status", context);
            require(CONFORMANCE_STATUSES.contains(status), context + " has unsupported status " + status);
            requireNonBlank(stringMember(profile, "owner", context), context + " owner");

            Set<String> environmentIdentifiers = verifyEnvironments(
                    arrayMember(profile, "environments", context),
                    context + " environments"
            );
            List<String> commands = nonEmptyStringArray(profile, "commands", context);
            commands.forEach(command -> require(command.startsWith("./gradlew -g .gradle-user-home "),
                    context + " command must use the workspace-local Gradle user home: " + command));
            nonEmptyStringArray(profile, "fixtures", context);
            nonEmptyStringArray(profile, "assertions", context);
            nonEmptyStringArray(profile, "capabilityModes", context);
            verifyLimits(objectMember(profile, "limits", context), context + " limits");

            JsonObject evidence = objectMember(profile, "evidence", context);
            requireExactKeys(evidence, context + " evidence", Set.of("requiredArtifacts", "recordedArtifacts"));
            List<String> requiredArtifacts = nonEmptyStringArray(evidence, "requiredArtifacts", context + " evidence");
            List<String> recordedArtifacts = stringArray(evidence, "recordedArtifacts", context + " evidence");
            String evidencePrefix = "build/conformance/" + identifier + "/";
            requiredArtifacts.forEach(path -> {
                require(path.startsWith(evidencePrefix) && path.length() > evidencePrefix.length(),
                        context + " required artifact must be under " + evidencePrefix + ": " + path);
                resolveRepositoryPath(root, path, context + " required artifact");
            });
            recordedArtifacts.forEach(path -> {
                require(path.startsWith(evidencePrefix) && path.length() > evidencePrefix.length(),
                        context + " recorded artifact must be under " + evidencePrefix + ": " + path);
                resolveRepositoryPath(root, path, context + " recorded artifact");
            });
            require(new LinkedHashSet<>(requiredArtifacts).size() == requiredArtifacts.size(),
                    context + " contains duplicate required artifacts");
            require(new LinkedHashSet<>(recordedArtifacts).size() == recordedArtifacts.size(),
                    context + " contains duplicate recorded artifacts");

            List<JsonValue> waivers = arrayMember(profile, "waivers", context);
            verifyWaivers(waivers, environmentIdentifiers, context + " waivers");
            switch (status) {
                case "planned" -> {
                    require(recordedArtifacts.isEmpty(), context + " planned profile cannot claim recorded evidence");
                    require(waivers.isEmpty(), context + " planned profile cannot carry a waiver");
                }
                case "passed" -> {
                    require(recordedArtifacts.containsAll(requiredArtifacts),
                            context + " passed profile does not record every required artifact");
                    require(waivers.isEmpty(), context + " passed profile cannot carry a waiver");
                }
                case "failed" -> {
                    // A failed profile is a valid honest registry state; milestone closure evaluates its required flag.
                }
                case "waived" -> require(!waivers.isEmpty(), context + " waived profile must carry a waiver");
                case "active" -> require(waivers.isEmpty(), context + " active profile cannot carry a waiver");
                default -> throw new GradleException(context + " has an unreachable status " + status);
            }
        }
        require(workPackages.containsAll(REQUIRED_M0_PROFILES), setDifferenceMessage(
                "initial M0 conformance profile coverage",
                REQUIRED_M0_PROFILES,
                workPackages
        ));
    }

    /// Parses the metadata block of one canonical ADR.
    ///
    /// @param path the ADR path used in diagnostics
    /// @param content the complete ADR content
    /// @return the parsed metadata fields
    private static Map<String, String> adrMetadata(Path path, String content) {
        Map<String, String> metadata = new LinkedHashMap<>();
        for (String line : content.lines().toList()) {
            Matcher matcher = ADR_METADATA.matcher(line);
            if (matcher.matches()) {
                @Nullable String previous = metadata.put(matcher.group(1), matcher.group(2).trim());
                require(previous == null, path + " repeats metadata field " + matcher.group(1));
            }
        }
        return metadata;
    }

    /// Verifies relative Markdown links in governance documents.
    ///
    /// @param root the repository root
    /// @param documents the Markdown files to inspect
    private static void verifyMarkdownLinks(Path root, List<Path> documents) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        for (Path document : documents) {
            String content = readUtf8(document);
            MARKDOWN_LINK.matcher(content).results().forEach(result -> {
                String destination = result.group(1);
                if (destination.startsWith("http://")
                        || destination.startsWith("https://")
                        || destination.startsWith("mailto:")
                        || destination.startsWith("#")) {
                    return;
                }
                String filePart = destination.split("[#?]", 2)[0];
                if (filePart.isEmpty()) {
                    return;
                }
                Path target = document.getParent().resolve(filePart).normalize().toAbsolutePath();
                require(target.startsWith(normalizedRoot), document + " links outside the repository: " + destination);
                require(Files.exists(target), document + " has a broken relative link: " + destination);
            });
        }
    }

    /// Validates one conformance profile environment list.
    ///
    /// @param values the environment values
    /// @param context the diagnostic location
    /// @return the unique environment identifiers
    private static Set<String> verifyEnvironments(List<JsonValue> values, String context) {
        require(!values.isEmpty(), context + " must not be empty");
        Set<String> identifiers = new LinkedHashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemContext = context + "[" + index + "]";
            JsonObject environment = asObject(values.get(index), itemContext);
            requireExactKeys(environment, itemContext, Set.of("id", "os", "architecture", "runtime"));
            String identifier = stringMember(environment, "id", itemContext);
            requireNonBlank(identifier, itemContext + " id");
            require(identifiers.add(identifier), context + " contains duplicate id " + identifier);
            String operatingSystem = stringMember(environment, "os", itemContext);
            require(CONFORMANCE_OPERATING_SYSTEMS.contains(operatingSystem),
                    itemContext + " has unsupported os " + operatingSystem);
            String architecture = stringMember(environment, "architecture", itemContext);
            require(CONFORMANCE_ARCHITECTURES.contains(architecture),
                    itemContext + " has unsupported architecture " + architecture);
            String runtime = stringMember(environment, "runtime", itemContext);
            require(CONFORMANCE_RUNTIMES.contains(runtime),
                    itemContext + " has unsupported runtime " + runtime);
        }
        return identifiers;
    }

    /// Validates limits shared by conformance defaults and profiles.
    ///
    /// @param limits the limits object
    /// @param context the diagnostic location
    private static void verifyLimits(JsonObject limits, String context) {
        requireExactKeys(limits, context, Set.of("tolerances", "durations", "budgets"));
        JsonObject tolerances = objectMember(limits, "tolerances", context);
        requireExactKeys(tolerances, context + " tolerances", Set.of(
                "numericAbsolute",
                "numericRelative",
                "imageMaxChannelDelta"
        ));
        require(numberMember(tolerances, "numericAbsolute", context) >= 0.0,
                context + " numericAbsolute must be non-negative");
        require(numberMember(tolerances, "numericRelative", context) >= 0.0,
                context + " numericRelative must be non-negative");
        double imageDelta = numberMember(tolerances, "imageMaxChannelDelta", context);
        require(imageDelta >= 0.0 && imageDelta <= 1.0,
                context + " imageMaxChannelDelta must be between 0 and 1");

        JsonObject durations = objectMember(limits, "durations", context);
        requireExactKeys(durations, context + " durations", Set.of(
                "timeoutSeconds",
                "soakSeconds",
                "repetitions"
        ));
        require(integerMember(durations, "timeoutSeconds", context) >= 1,
                context + " timeoutSeconds must be positive");
        require(integerMember(durations, "soakSeconds", context) >= 0,
                context + " soakSeconds must be non-negative");
        require(integerMember(durations, "repetitions", context) >= 1,
                context + " repetitions must be positive");

        JsonObject budgets = objectMember(limits, "budgets", context);
        requireExactKeys(budgets, context + " budgets", Set.of(
                "maxHeapBytes",
                "maxNativeBytes",
                "maxHandles",
                "maxThreads"
        ));
        require(integerMember(budgets, "maxHeapBytes", context) >= 0,
                context + " maxHeapBytes must be non-negative");
        require(integerMember(budgets, "maxNativeBytes", context) >= 0,
                context + " maxNativeBytes must be non-negative");
        require(integerMember(budgets, "maxHandles", context) >= 0,
                context + " maxHandles must be non-negative");
        require(integerMember(budgets, "maxThreads", context) >= 1,
                context + " maxThreads must be positive");
    }

    /// Validates profile waivers and their environment scopes.
    ///
    /// @param values the waiver values
    /// @param environmentIdentifiers valid environment identifiers for the profile
    /// @param context the diagnostic location
    private static void verifyWaivers(
            List<JsonValue> values,
            Set<String> environmentIdentifiers,
            String context
    ) {
        Set<String> identifiers = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            String itemContext = context + "[" + index + "]";
            JsonObject waiver = asObject(values.get(index), itemContext);
            requireExactKeys(waiver, itemContext, Set.of(
                    "id",
                    "assertion",
                    "issue",
                    "reviewer",
                    "approved",
                    "expires",
                    "environments",
                    "rationale"
            ));
            String identifier = stringMember(waiver, "id", itemContext);
            requireNonBlank(identifier, itemContext + " id");
            require(identifiers.add(identifier), context + " contains duplicate id " + identifier);
            requireNonBlank(stringMember(waiver, "assertion", itemContext), itemContext + " assertion");
            requireNonBlank(stringMember(waiver, "issue", itemContext), itemContext + " issue");
            requireNonBlank(stringMember(waiver, "reviewer", itemContext), itemContext + " reviewer");
            LocalDate approved = requireIsoDate(stringMember(waiver, "approved", itemContext),
                    itemContext + " approved");
            LocalDate expires = requireIsoDate(stringMember(waiver, "expires", itemContext),
                    itemContext + " expires");
            require(!expires.isBefore(approved), itemContext + " expires before approval");
            require(!expires.isBefore(LocalDate.now()), itemContext + " expired on " + expires);
            List<String> environments = nonEmptyStringArray(waiver, "environments", itemContext);
            require(environmentIdentifiers.containsAll(environments),
                    itemContext + " refers to an environment outside its profile");
            requireNonBlank(stringMember(waiver, "rationale", itemContext), itemContext + " rationale");
        }
    }

    /// Returns a required JSON object member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return the member object
    private static JsonObject objectMember(JsonObject object, String name, String context) {
        return asObject(member(object, name, context), context + " " + name);
    }

    /// Returns a required JSON array member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return the immutable array elements
    private static @Unmodifiable List<JsonValue> arrayMember(JsonObject object, String name, String context) {
        JsonValue value = member(object, name, context);
        if (value instanceof JsonArray array) {
            return array.elements();
        }
        throw new GradleException(context + " " + name + " must be an array");
    }

    /// Returns a required JSON string member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return the decoded member value
    private static String stringMember(JsonObject object, String name, String context) {
        JsonValue value = member(object, name, context);
        if (value instanceof JsonString string) {
            return string.value();
        }
        throw new GradleException(context + " " + name + " must be a string");
    }

    /// Returns a required integral JSON member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return the integral value
    private static long integerMember(JsonObject object, String name, String context) {
        JsonValue value = member(object, name, context);
        if (value instanceof JsonNumber number && number.integral()) {
            return number.value().longValue();
        }
        throw new GradleException(context + " " + name + " must be an integer");
    }

    /// Returns a required numeric JSON member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return the numeric value
    private static double numberMember(JsonObject object, String name, String context) {
        JsonValue value = member(object, name, context);
        if (value instanceof JsonNumber number) {
            return number.value().doubleValue();
        }
        throw new GradleException(context + " " + name + " must be a number");
    }

    /// Returns a required Boolean JSON member.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return the Boolean value
    private static boolean booleanMember(JsonObject object, String name, String context) {
        JsonValue value = member(object, name, context);
        if (value instanceof JsonBoolean bool) {
            return bool.value();
        }
        throw new GradleException(context + " " + name + " must be a Boolean");
    }

    /// Returns a JSON member after verifying its presence.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return the present member value
    private static JsonValue member(JsonObject object, String name, String context) {
        require(object.members().containsKey(name), context + " is missing member " + name);
        return object.members().getOrDefault(name, JsonParser.JsonNull.INSTANCE);
    }

    /// Requires a value to be a JSON object.
    ///
    /// @param value the candidate value
    /// @param context the diagnostic location
    /// @return the object value
    private static JsonObject asObject(JsonValue value, String context) {
        if (value instanceof JsonObject object) {
            return object;
        }
        throw new GradleException(context + " must be an object");
    }

    /// Reads an array containing only strings.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return a mutable list of decoded values
    private static List<String> stringArray(JsonObject object, String name, String context) {
        List<JsonValue> values = arrayMember(object, name, context);
        List<String> strings = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            JsonValue value = values.get(index);
            if (!(value instanceof JsonString string)) {
                throw new GradleException(context + " " + name + "[" + index + "] must be a string");
            }
            requireNonBlank(string.value(), context + " " + name + "[" + index + "]");
            strings.add(string.value());
        }
        return strings;
    }

    /// Reads a non-empty array containing only non-empty strings.
    ///
    /// @param object the containing object
    /// @param name the member name
    /// @param context the diagnostic location
    /// @return a mutable list of decoded values
    private static List<String> nonEmptyStringArray(JsonObject object, String name, String context) {
        List<String> values = stringArray(object, name, context);
        require(!values.isEmpty(), context + " " + name + " must not be empty");
        return values;
    }

    /// Requires an object to contain exactly the expected member names.
    ///
    /// @param object the object to inspect
    /// @param context the diagnostic location
    /// @param expectedKeys the exact member-name set
    private static void requireExactKeys(JsonObject object, String context, Set<String> expectedKeys) {
        require(object.members().keySet().equals(expectedKeys),
                setDifferenceMessage(context + " members", expectedKeys, object.members().keySet()));
    }

    /// Finds runtime resources whose checked-in bytes require provenance.
    ///
    /// @param root the repository root
    /// @return mutable invariant relative paths
    private static Set<String> discoverProvenancePayloads(Path root) {
        Set<String> payloads = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(Predicate.not(path -> isProvenanceExcluded(root, path)))
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return PROVENANCE_SUFFIXES.stream().anyMatch(name::endsWith);
                    })
                    .map(path -> invariantRelativePath(root, path))
                    .forEach(payloads::add);
        } catch (IOException exception) {
            throw new GradleException("Cannot scan repository payloads under " + root, exception);
        }
        return payloads;
    }

    /// Returns whether a repository path belongs to an excluded generated or vendored directory.
    ///
    /// @param root the repository root
    /// @param path the descendant path
    /// @return whether payload discovery must ignore the path
    private static boolean isProvenanceExcluded(Path root, Path path) {
        Path relative = root.relativize(path);
        for (Path component : relative) {
            if (PROVENANCE_EXCLUDED_DIRECTORIES.contains(component.toString())) {
                return true;
            }
        }
        return false;
    }

    /// Resolves and validates a canonical repository-relative path.
    ///
    /// @param root the repository root
    /// @param value the invariant relative path text
    /// @param context the diagnostic location
    /// @return the normalized path under the repository root
    private static Path resolveRepositoryPath(Path root, String value, String context) {
        requireNonBlank(value, context);
        require(!value.contains("\\"), context + " must use forward slashes");
        Path relative;
        try {
            relative = Path.of(value);
        } catch (RuntimeException exception) {
            throw new GradleException(context + " is not a valid path: " + value, exception);
        }
        require(!relative.isAbsolute(), context + " must be relative: " + value);
        require(relative.normalize().equals(relative), context + " must be normalized: " + value);
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path target = normalizedRoot.resolve(relative).normalize();
        require(target.startsWith(normalizedRoot), context + " escapes the repository: " + value);
        return target;
    }

    /// Computes the SHA-256 digest of one file.
    ///
    /// @param path the file to hash
    /// @return a lower-case hexadecimal digest
    private static String sha256(Path path) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new GradleException("The Gradle JVM does not provide SHA-256", exception);
        }
        try {
            digest.update(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new GradleException("Cannot hash " + path, exception);
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    /// Requires and returns a repository file.
    ///
    /// @param root the repository root
    /// @param relativePath the invariant repository-relative path
    /// @return the existing regular file
    private static Path requireFile(Path root, String relativePath) {
        Path path = root.resolve(relativePath);
        require(Files.isRegularFile(path), "Missing required repository file " + relativePath);
        return path;
    }

    /// Reads a complete UTF-8 file.
    ///
    /// @param path the file to read
    /// @return the decoded contents
    private static String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new GradleException("Cannot read " + path, exception);
        }
    }

    /// Returns an invariant path relative to a repository root.
    ///
    /// @param root the repository root
    /// @param path the descendant path
    /// @return the forward-slash relative path
    private static String invariantRelativePath(Path root, Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    /// Requires a valid HTTPS URL with an authority.
    ///
    /// @param value the candidate URL
    /// @param context the diagnostic location
    private static void requireHttpsUrl(String value, String context) {
        try {
            URI uri = new URI(value);
            require("https".equals(uri.getScheme())
                            && uri.getRawAuthority() != null
                            && !uri.getRawAuthority().isBlank(),
                    context + " must be an absolute HTTPS URL: " + value);
        } catch (URISyntaxException exception) {
            throw new GradleException(context + " is not a valid URL: " + value, exception);
        }
    }

    /// Requires and parses an ISO-8601 calendar date.
    ///
    /// @param value the date text
    /// @param context the diagnostic location
    /// @return the parsed date
    private static LocalDate requireIsoDate(String value, String context) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new GradleException(context + " must be an ISO-8601 date: " + value, exception);
        }
    }

    /// Requires a string to contain a non-whitespace character.
    ///
    /// @param value the candidate value
    /// @param context the diagnostic location
    private static void requireNonBlank(String value, String context) {
        require(!value.isBlank(), context + " must not be blank");
    }

    /// Builds the accepted ADR identifier range plus one additional identifier.
    ///
    /// @param first the inclusive three-digit lower bound
    /// @param last the inclusive three-digit upper bound
    /// @param additional an additional three-digit identifier
    /// @return the immutable identifier set
    private static @Unmodifiable Set<String> range(String first, String last, String additional) {
        Set<String> result = new LinkedHashSet<>();
        for (int value = Integer.parseInt(first); value <= Integer.parseInt(last); value++) {
            result.add(String.format(Locale.ROOT, "%03d", value));
        }
        result.add(additional);
        return Set.copyOf(result);
    }

    /// Returns an immutable union of two sets.
    ///
    /// @param first the first set
    /// @param second the second set
    /// @return the immutable union
    private static @Unmodifiable Set<String> union(Set<String> first, Set<String> second) {
        Set<String> result = new LinkedHashSet<>(first);
        result.addAll(second);
        return Set.copyOf(result);
    }

    /// Describes missing and unexpected values between two sets.
    ///
    /// @param label the compared contract
    /// @param expected the required values
    /// @param actual the observed values
    /// @return a stable diagnostic message
    private static String setDifferenceMessage(String label, Set<String> expected, Set<String> actual) {
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        Set<String> unexpected = new TreeSet<>(actual);
        unexpected.removeAll(expected);
        return label + " differs: missing=" + missing + ", unexpected=" + unexpected;
    }

    /// Throws a Gradle validation failure when a condition is false.
    ///
    /// @param condition the required condition
    /// @param message the failure detail
    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new GradleException(message);
        }
    }
}
