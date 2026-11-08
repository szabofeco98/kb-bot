package com.szabofeco.kb_bot.search;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.szabofeco.kb_bot.search.ChunkMetadata.meta;
import static com.szabofeco.kb_bot.search.ChunkMetadata.score;

/**
 * A document the answer is based on, with its relevant chunks (best first). This is what the expert sees as the
 * source: which file, which version, effective from when.
 *
 * @param bestScore score of the best-matching chunk; documents are ordered by it
 */
public record SourceDocument(String docId, String title, String version, String effectiveFrom, String product,
                             String sourceFile, double bestScore, List<SourceChunk> chunks) {

    /**
     * Groups chunks (already sorted best first) by docId. Insertion order is kept, so documents end up ordered by
     * their best chunk, and chunks within a document stay best first.
     */
    public static List<SourceDocument> fromChunks(List<Document> sortedChunks) {
        Map<String, List<Document>> byDocId = new LinkedHashMap<>();
        for (Document chunk : sortedChunks) {
            byDocId.computeIfAbsent(meta(chunk, "docId"), k -> new ArrayList<>()).add(chunk);
        }
        return byDocId.values().stream()
                .map(chunks -> {
                    Document best = chunks.get(0);
                    return new SourceDocument(meta(best, "docId"), meta(best, "title"), meta(best, "version"),
                            meta(best, "effectiveFrom"), meta(best, "product"), meta(best, "sourceFile"),
                            score(best), chunks.stream()
                                    .map(c -> new SourceChunk(c.getId(), meta(c, "section"), score(c), c.getText()))
                                    .toList());
                })
                .toList();
    }
}
