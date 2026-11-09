package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.ChunkMetadataKeys;
import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.common.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChunkRetriever {

    private static final Logger log = LoggerFactory.getLogger(ChunkRetriever.class);

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    public ChunkRetriever(VectorStore vectorStore, RagProperties rag) {
        this.vectorStore = vectorStore;
        this.topK = rag.topK();
        this.similarityThreshold = rag.similarityThreshold();
    }

    /**
     * {@code product == null} means all products. Each product is searched separately (top-k each): a single
     * unfiltered top-k would let one product crowd the others out of a cross-product question.
     */
    public List<RetrievedChunk> retrieveRelevant(String question, Product product) {
        List<Product> products = product == null ? List.of(Product.values()) : List.of(product);
        List<RetrievedChunk> hits = new ArrayList<>();
        for (Product current : products) {
            vectorStore.similaritySearch(SearchRequest.builder()
                            .query(question)
                            .topK(topK)
                            .similarityThresholdAll()
                            .filterExpression(filterBuilder.eq(ChunkMetadataKeys.PRODUCT, current.name()).build())
                            .build())
                    .stream()
                    .map(RetrievedChunk::from)
                    .forEach(hits::add);
        }

        List<RetrievedChunk> relevant = hits.stream()
                .filter(chunk -> chunk.score() >= similarityThreshold)
                .sorted(RetrievedChunk.BEST_FIRST)
                .toList();


        log.debug("Retrieval: question=\"{}\", product={}, threshold={}, relevant={}/{}, hits=[{}]",
                question, product == null ? "ALL" : product, similarityThreshold,
                relevant.size(), hits.size(), describe(hits));
        return relevant;
    }

    private static String describe(List<RetrievedChunk> hits) {
        return hits.stream()
                .map(c -> String.format("%s/%s/%s/%s=%.3f",
                        c.document().product(), c.document().docId(), c.section(), c.id(), c.score()))
                .collect(Collectors.joining(", "));
    }
}
