package com.szabofeco.kb_bot.search;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.szabofeco.kb_bot.common.DocumentRef;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record SourceDocument(@JsonUnwrapped DocumentRef document, double bestScore, List<SourceChunk> chunks) {

    /** Groups by document version; documents are ordered by their best chunk. */
    public static List<SourceDocument> fromChunks(List<RetrievedChunk> chunks) {
        Map<DocumentRef, List<RetrievedChunk>> byDocument = chunks.stream()
                .sorted(RetrievedChunk.BEST_FIRST)
                .collect(Collectors.groupingBy(RetrievedChunk::document, LinkedHashMap::new, Collectors.toList()));
        return byDocument.entrySet().stream()
                .map(entry -> new SourceDocument(entry.getKey(), entry.getValue().getFirst().score(),
                        entry.getValue().stream().map(SourceChunk::from).toList()))
                .toList();
    }
}
