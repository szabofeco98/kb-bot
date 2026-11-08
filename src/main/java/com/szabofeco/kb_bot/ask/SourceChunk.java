package com.szabofeco.kb_bot.ask;

/** A retrieved chunk of a {@link SourceDocument}. */
public record SourceChunk(String chunkId, String section, double score, String text) {
}
