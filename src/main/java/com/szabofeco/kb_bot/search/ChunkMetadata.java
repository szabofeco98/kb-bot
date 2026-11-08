package com.szabofeco.kb_bot.search;

import org.springframework.ai.document.Document;

/** Null-safe access to the metadata and score of a retrieved chunk (metadata keys are set by ingestion). */
public final class ChunkMetadata {

    private ChunkMetadata() {
    }

    /** @return the metadata value as a string, or {@code null} if absent */
    public static String meta(Document chunk, String key) {
        Object value = chunk.getMetadata().get(key);
        return value == null ? null : value.toString();
    }

    /** @return the similarity score, or 0 if the store did not set one */
    public static double score(Document chunk) {
        return chunk.getScore() == null ? 0.0 : chunk.getScore();
    }
}
