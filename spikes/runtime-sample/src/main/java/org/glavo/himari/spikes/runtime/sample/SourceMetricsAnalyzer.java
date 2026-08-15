package org.glavo.himari.spikes.runtime.sample;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Measures a declared candidate source corpus with a deterministic physical-line algorithm.
///
/// Blank lines, comments, package/import declarations, string or character literal payload, and
/// brace-or-punctuation-only lines are excluded. Every ceremony count comes from an explicit source
/// marker whose line is verified to contain significant Java code; the marker list remains in the
/// report for review instead of relying on candidate-specific method-name heuristics.
@NotNullByDefault
public final class SourceMetricsAnalyzer {
    /// Prevents construction.
    private SourceMetricsAnalyzer() {
    }

    /// Analyzes every file and marker in a corpus.
    ///
    /// @param corpus the source corpus
    /// @return deterministic source metrics
    /// @throws IllegalArgumentException if a file is missing, unreadable, or a marker does not name
    /// a significant source line
    public static SourceMetrics analyze(SourceCorpus corpus) {
        ArrayList<SourceFileMetrics> files = new ArrayList<>();
        HashMap<String, boolean[]> significantLines = new HashMap<>();
        long totalLines = 0L;
        for (SourceUnit sourceUnit : corpus.sourceUnits()) {
            String relativeFile = sourceUnit.relativePath();
            Path file = corpus.repositoryRoot().resolve(relativeFile).normalize();
            List<String> lines;
            try {
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalArgumentException("Cannot read candidate source " + relativeFile, exception);
            }
            boolean[] significant = significantLines(lines);
            significantLines.put(relativeFile, significant);
            long count = 0L;
            for (boolean line : significant) {
                if (line) {
                    count = Math.incrementExact(count);
                }
            }
            totalLines = Math.addExact(totalLines, count);
            files.add(new SourceFileMetrics(relativeFile, sourceUnit.stage(), count));
        }

        EnumMap<SourceCeremonyKind, Long> counts = new EnumMap<>(SourceCeremonyKind.class);
        for (SourceCeremonyKind kind : SourceCeremonyKind.values()) {
            counts.put(kind, 0L);
        }
        for (SourceMarker marker : corpus.markers()) {
            boolean @Nullable [] significant = significantLines.get(marker.relativePath());
            if (significant == null || marker.line() > significant.length || !significant[marker.line() - 1]) {
                throw new IllegalArgumentException(
                        "Source marker does not identify significant code: "
                                + marker.relativePath() + ':' + marker.line()
                );
            }
            counts.compute(marker.kind(), (ignored, value) -> Math.addExact(value == null ? 0L : value, 1L));
        }

        HashMap<String, Long> reportCounts = new HashMap<>();
        for (Map.Entry<SourceCeremonyKind, Long> entry : counts.entrySet()) {
            reportCounts.put(canonical(entry.getKey()), entry.getValue());
        }
        return new SourceMetrics(totalLines, files, reportCounts, corpus.markers());
    }

    /// Computes whether each physical line contains significant Java code.
    ///
    /// @param lines the physical source lines
    /// @return one flag per input line
    private static boolean[] significantLines(List<String> lines) {
        JavaLineFilter filter = new JavaLineFilter();
        boolean[] result = new boolean[lines.size()];
        for (int index = 0; index < lines.size(); index++) {
            String code = filter.codePortion(lines.get(index)).trim();
            result[index] = !code.isEmpty()
                    && !code.startsWith("package ")
                    && !code.startsWith("import ")
                    && !code.matches("[{}();,]+")
                    && !code.equals("else")
                    && !code.equals("else {");
        }
        return result;
    }

    /// Returns the report spelling of a ceremony category.
    ///
    /// @param kind the category
    /// @return the lower-camel report key
    private static String canonical(SourceCeremonyKind kind) {
        return switch (kind) {
            case EXPLICIT_KEY -> "explicitKeys";
            case DEFERRED_GETTER -> "deferredGetters";
            case STRUCTURAL_CONTROL -> "structuralControls";
            case GROUP_BOUNDARY -> "groupBoundaries";
            case GENERIC_TYPE_NOISE -> "genericTypeNoise";
            case CALLBACK_WRAPPER -> "callbackWrappers";
        };
    }

    /// Removes comments and literal payload while preserving code outside them.
    @NotNullByDefault
    private static final class JavaLineFilter {
        /// Whether the next physical line begins inside a block comment.
        private boolean blockComment;

        /// Whether the next physical line begins inside a text block.
        private boolean textBlock;

        /// Returns the code portion of one physical line and advances lexical state.
        ///
        /// @param line the physical source line
        /// @return code with comments and literal payload removed
        private String codePortion(String line) {
            StringBuilder result = new StringBuilder(line.length());
            int index = 0;
            while (index < line.length()) {
                if (blockComment) {
                    int end = line.indexOf("*/", index);
                    if (end < 0) {
                        return result.toString();
                    }
                    blockComment = false;
                    index = end + 2;
                    continue;
                }
                if (textBlock) {
                    int end = line.indexOf("\"\"\"", index);
                    if (end < 0) {
                        return result.toString();
                    }
                    textBlock = false;
                    index = end + 3;
                    result.append(" literal ");
                    continue;
                }
                if (line.startsWith("//", index)) {
                    break;
                }
                if (line.startsWith("/*", index)) {
                    blockComment = true;
                    index += 2;
                    continue;
                }
                if (line.startsWith("\"\"\"", index)) {
                    textBlock = true;
                    result.append(" literal ");
                    index += 3;
                    continue;
                }
                char character = line.charAt(index);
                if (character == '\"' || character == '\'') {
                    index = skipQuotedLiteral(line, index, character);
                    result.append(" literal ");
                    continue;
                }
                result.append(character);
                index++;
            }
            return result.toString();
        }

        /// Skips one ordinary quoted literal, including escaped delimiters.
        ///
        /// @param line the physical line
        /// @param opening the opening delimiter index
        /// @param delimiter the quote character
        /// @return the index immediately after the closing delimiter, or the line end
        private static int skipQuotedLiteral(String line, int opening, char delimiter) {
            boolean escaped = false;
            for (int index = opening + 1; index < line.length(); index++) {
                char character = line.charAt(index);
                if (escaped) {
                    escaped = false;
                } else if (character == '\\') {
                    escaped = true;
                } else if (character == delimiter) {
                    return index + 1;
                }
            }
            return line.length();
        }
    }
}
