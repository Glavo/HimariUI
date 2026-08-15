package org.glavo.himari.spikes.runtime.decision;

import org.glavo.himari.spikes.runtime.sample.DecisionRubric;
import org.glavo.himari.spikes.runtime.sample.FixtureStage;
import org.glavo.himari.spikes.runtime.sample.RuntimeCandidate;
import org.glavo.himari.spikes.runtime.sample.SourceCeremonyKind;
import org.glavo.himari.spikes.runtime.sample.SourceFileMetrics;
import org.glavo.himari.spikes.runtime.sample.SourceMarker;
import org.glavo.himari.spikes.runtime.sample.SourceMetrics;
import org.glavo.himari.spikes.runtime.sample.SourceMetricsAnalyzer;
import org.glavo.himari.spikes.runtime.sample.SourceUnit;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/// Generates name-free review packets from each complete ordinary-Java micro source corpus.
@NotNullByDefault
public final class CeremonyPacketGenerator {
    /// The packet format version recorded by the checked review.
    static final String PACKET_VERSION = "runtime-ceremony-packet-v1";

    /// Ceremony categories that contribute to the frozen mandatory-review threshold.
    private static final @Unmodifiable Set<SourceCeremonyKind> ACCIDENTAL_KINDS = Set.copyOf(EnumSet.of(
            SourceCeremonyKind.DEFERRED_GETTER,
            SourceCeremonyKind.GROUP_BOUNDARY,
            SourceCeremonyKind.GENERIC_TYPE_NOISE,
            SourceCeremonyKind.CALLBACK_WRAPPER
    ));

    /// Prevents construction.
    private CeremonyPacketGenerator() {
    }

    /// Generates all packets and their name-free manifest.
    ///
    /// @param repositoryRoot the repository root
    /// @param outputDirectory the packet output directory
    /// @throws IOException if source or evidence files cannot be read or written
    public static void generate(Path repositoryRoot, Path outputDirectory) throws IOException {
        Path normalizedRoot = repositoryRoot.toAbsolutePath().normalize();
        Path normalizedOutput = outputDirectory.toAbsolutePath().normalize();
        ArrayList<PacketData> packets = new ArrayList<>();
        long minimumMicroLines = Long.MAX_VALUE;
        for (RuntimeDecisionCandidate candidate : RuntimeDecisionCandidate.values()) {
            RuntimeCandidate adapter = candidate.create(normalizedRoot);
            SourceMetrics metrics = SourceMetricsAnalyzer.analyze(adapter.sourceCorpus());
            long microLines = metrics.files().stream()
                    .filter(file -> file.stage() == FixtureStage.MICRO)
                    .mapToLong(SourceFileMetrics::sourceLines)
                    .sum();
            long accidentalMarkers = metrics.markers().stream()
                    .filter(marker -> isMicro(metrics, marker))
                    .filter(marker -> ACCIDENTAL_KINDS.contains(marker.kind()))
                    .count();
            long accidentalLines = metrics.markers().stream()
                    .filter(marker -> isMicro(metrics, marker))
                    .filter(marker -> ACCIDENTAL_KINDS.contains(marker.kind()))
                    .map(marker -> marker.relativePath() + ':' + marker.line())
                    .distinct()
                    .count();
            if (microLines <= 0L) {
                throw new IllegalStateException("Candidate has no significant micro source lines: " + candidate.key());
            }
            minimumMicroLines = Math.min(minimumMicroLines, microLines);
            packets.add(new PacketData(candidate, adapter, metrics, microLines, accidentalMarkers, accidentalLines));
        }

        Files.createDirectories(normalizedOutput);
        TreeMap<String, String> manifest = new TreeMap<>();
        manifest.put("schemaVersion", "1");
        manifest.put("packetVersion", PACKET_VERSION);
        manifest.put("rubricVersion", DecisionRubric.VERSION);
        manifest.put("minimumMicroSourceLines", Long.toString(minimumMicroLines));
        for (PacketData packet : packets) {
            boolean markerTrigger = Math.multiplyExact(packet.accidentalMarkers(), 5L) >= packet.microLines();
            boolean sizeTrigger = Math.multiplyExact(packet.microLines(), 4L)
                    > Math.multiplyExact(minimumMicroLines, 7L);
            Path packetPath = normalizedOutput.resolve("candidate-"
                    + packet.candidate().reviewLabel().toLowerCase(Locale.ROOT) + ".txt");
            Files.writeString(
                    packetPath,
                    render(packet, markerTrigger, sizeTrigger),
                    StandardCharsets.UTF_8
            );
            String prefix = "candidate." + packet.candidate().reviewLabel() + '.';
            manifest.put(prefix + "packetFile", packetPath.getFileName().toString());
            manifest.put(prefix + "packetSha256", DecisionProperties.sha256(packetPath));
            manifest.put(prefix + "microSourceLines", Long.toString(packet.microLines()));
            manifest.put(prefix + "accidentalMarkers", Long.toString(packet.accidentalMarkers()));
            manifest.put(prefix + "accidentalLines", Long.toString(packet.accidentalLines()));
            manifest.put(prefix + "markerThresholdTriggered", Boolean.toString(markerTrigger));
            manifest.put(prefix + "sizeThresholdTriggered", Boolean.toString(sizeTrigger));
            manifest.put(prefix + "mandatoryThreePersonReview", Boolean.toString(markerTrigger || sizeTrigger));
        }
        DecisionProperties.write(normalizedOutput.resolve("manifest.properties"), manifest);
    }

    /// Runs packet generation from the command line.
    ///
    /// @param arguments repository root and output directory
    /// @throws IOException if source or evidence files cannot be read or written
    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected repository root and ceremony-packet output directory");
        }
        generate(Path.of(arguments[0]), Path.of(arguments[1]));
    }

    /// Returns whether a marker belongs to a micro-stage source file.
    ///
    /// @param metrics the measured corpus
    /// @param marker the marker
    /// @return whether the marker is in the micro corpus
    private static boolean isMicro(SourceMetrics metrics, SourceMarker marker) {
        for (SourceFileMetrics file : metrics.files()) {
            if (file.relativePath().equals(marker.relativePath())) {
                return file.stage() == FixtureStage.MICRO;
            }
        }
        throw new IllegalArgumentException("Marker is outside its measured corpus: " + marker.relativePath());
    }

    /// Renders one complete, line-numbered, mechanically redacted source packet.
    ///
    /// @param packet the candidate source and measurements
    /// @param markerTrigger whether the 20-percent marker threshold fired
    /// @param sizeTrigger whether the 1.75-times size threshold fired
    /// @return the packet text
    /// @throws IOException if a source file cannot be read
    private static String render(PacketData packet, boolean markerTrigger, boolean sizeTrigger) throws IOException {
        StringBuilder output = new StringBuilder();
        output.append("M1 BLINDED ORDINARY-JAVA CEREMONY PACKET\n")
                .append("Packet version: ").append(PACKET_VERSION).append('\n')
                .append("Rubric version: ").append(DecisionRubric.VERSION).append('\n')
                .append("Candidate label: ").append(packet.candidate().reviewLabel()).append('\n')
                .append("Candidate names and package-specific identifiers are mechanically redacted.\n")
                .append("Micro significant source lines: ").append(packet.microLines()).append('\n')
                .append("Accidental ceremony markers: ").append(packet.accidentalMarkers()).append('\n')
                .append("Unique accidental ceremony lines: ").append(packet.accidentalLines()).append('\n')
                .append("20 percent marker threshold triggered: ").append(markerTrigger).append('\n')
                .append("1.75 times source-size threshold triggered: ").append(sizeTrigger).append('\n')
                .append("Mandatory three-person review: ").append(markerTrigger || sizeTrigger).append("\n\n");

        for (SourceUnit source : packet.adapter().sourceCorpus().sourceUnits()) {
            if (source.stage() != FixtureStage.MICRO) {
                continue;
            }
            Path file = packet.adapter().sourceCorpus().repositoryRoot().resolve(source.relativePath());
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            output.append("--- COMPLETE MICRO SOURCE ---\n");
            for (int index = 0; index < lines.size(); index++) {
                output.append(String.format(Locale.ROOT, "%04d | %s%n", index + 1, redact(packet.candidate(), lines.get(index))));
            }
            output.append("--- END MICRO SOURCE ---\n");
        }
        String text = output.toString();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains(packet.candidate().candidateId().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Blinded packet retained a candidate identifier");
        }
        return text;
    }

    /// Replaces candidate-identifying text while preserving the API's ceremony and control flow.
    ///
    /// @param candidate the candidate whose terms are hidden
    /// @param line the original physical source line
    /// @return the redacted line
    private static String redact(RuntimeDecisionCandidate candidate, String line) {
        String result = line;
        for (String fragment : candidate.redactions()) {
            result = result.replace(fragment, replacement(fragment));
        }
        return result.replace(candidate.candidateId(), "candidate-model");
    }

    /// Selects a neutral replacement with approximately matching identifier case.
    ///
    /// @param fragment the candidate-specific fragment
    /// @return the neutral replacement
    private static String replacement(String fragment) {
        if (fragment.equals(fragment.toUpperCase(Locale.ROOT))) {
            return "CANDIDATE";
        }
        if (!fragment.isEmpty() && Character.isUpperCase(fragment.charAt(0))) {
            return "Candidate";
        }
        return "candidate";
    }

    /// Holds one measured source corpus until cross-candidate thresholds are known.
    ///
    /// @param candidate the stable candidate entry
    /// @param adapter the real candidate adapter
    /// @param metrics the complete source measurements
    /// @param microLines significant micro source lines
    /// @param accidentalMarkers accidental-ceremony markers in micro source
    /// @param accidentalLines unique micro lines containing those markers
    @NotNullByDefault
    private record PacketData(
            RuntimeDecisionCandidate candidate,
            RuntimeCandidate adapter,
            SourceMetrics metrics,
            long microLines,
            long accidentalMarkers,
            long accidentalLines
    ) {
        /// Creates a validated packet-data snapshot.
        private PacketData {
            if (microLines <= 0L || accidentalMarkers < 0L || accidentalLines < 0L) {
                throw new IllegalArgumentException("Invalid ceremony packet measurements");
            }
        }
    }
}
