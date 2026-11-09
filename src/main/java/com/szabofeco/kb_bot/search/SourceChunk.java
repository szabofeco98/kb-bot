package com.szabofeco.kb_bot.search;

public record SourceChunk(String chunkId, String section, double score, String text) {

    static SourceChunk from(RetrievedChunk chunk) {
        return new SourceChunk(chunk.id(), chunk.section(), chunk.score(), chunk.text());
    }
}
