package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.search.RetrievedChunk;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The retrieved chunks labelled {@code S1..Sn} for the model; the only owner of the label scheme. Short labels
 * instead of chunk UUIDs, because small models copy long IDs unreliably.
 */
record LabelledContext(List<Excerpt> excerpts) {

    private static final String LABEL_PREFIX = "S";

    record Excerpt(String label, RetrievedChunk chunk) {

        String render() {
            DocumentRef document = chunk.document();
            StringBuilder sb = new StringBuilder()
                    .append('[').append(label).append("] (product: ").append(document.product().displayName())
                    .append(", document: ").append(document.title())
                    .append(", version: ").append(document.version());
            if (document.effectiveFrom() != null) {
                sb.append(", effective from: ").append(document.effectiveFrom());
            }
            return sb.append(")\n").append(chunk.text().strip()).toString();
        }
    }

    LabelledContext {
        excerpts = List.copyOf(excerpts);
    }

    static LabelledContext of(List<RetrievedChunk> chunks) {
        return new LabelledContext(IntStream.range(0, chunks.size())
                .mapToObj(i -> new Excerpt(LABEL_PREFIX + (i + 1), chunks.get(i)))
                .toList());
    }

    List<RetrievedChunk> chunks() {
        return excerpts.stream().map(Excerpt::chunk).toList();
    }

    String render() {
        return excerpts.stream().map(Excerpt::render).collect(Collectors.joining("\n\n"));
    }

    /** The chunks behind the model's labels, in retrieval order. Unknown labels are ignored: the model cannot cite
     * anything we did not retrieve. */
    List<RetrievedChunk> resolve(List<String> usedSources) {
        if (usedSources == null) {
            return List.of();
        }
        Set<String> requested = usedSources.stream().map(LabelledContext::normalise).collect(Collectors.toSet());
        return excerpts.stream().filter(e -> requested.contains(e.label())).map(Excerpt::chunk).toList();
    }

    String describe() {
        return excerpts.stream()
                .map(e -> e.label() + "=" + e.chunk().id())
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String normalise(String raw) {
        return raw == null ? "" : raw.replaceAll("[\\[\\]\\s]", "").toUpperCase(Locale.ROOT);
    }
}
