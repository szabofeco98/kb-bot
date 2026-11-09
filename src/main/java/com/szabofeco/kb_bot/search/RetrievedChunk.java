package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.ChunkMetadataKeys;
import com.szabofeco.kb_bot.common.DocumentRef;
import org.springframework.ai.document.Document;

import java.util.Comparator;

public record RetrievedChunk(String id, String text, double score, String section, DocumentRef document) {

    public static final Comparator<RetrievedChunk> BEST_FIRST =
            Comparator.comparingDouble(RetrievedChunk::score).reversed();

    static RetrievedChunk from(Document document) {
        return new RetrievedChunk(
                document.getId(),
                document.getText(),
                document.getScore() == null ? 0.0 : document.getScore(),
                DocumentRef.requiredMetadata(document.getId(), document.getMetadata(), ChunkMetadataKeys.SECTION),
                DocumentRef.fromMetadata(document.getId(), document.getMetadata()));
    }
}
