package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class SearchService {

    private final ChunkRetriever retriever;

    SearchService(ChunkRetriever retriever) {
        this.retriever = retriever;
    }

    SearchResponse search(String question, Product product) {
        List<SourceDocument> documents = SourceDocument.fromChunks(retriever.retrieveRelevant(question, product));
        SearchStatus status = documents.isEmpty() ? SearchStatus.NO_RELEVANT_SOURCE : SearchStatus.SOURCES_FOUND;
        return new SearchResponse(status, product, documents);
    }
}
