package com.szabofeco.kb_bot.search;

/** A retrieved chunk of a {@link SourceDocument}. */
public record SourceChunk(String chunkId, String section, double score, String text) {
}
