package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.szabofeco.kb_bot.search.ChunkMetadata.meta;
import static com.szabofeco.kb_bot.search.ChunkMetadata.score;

/**
 * Retrieval: embeds the question (inside the {@link VectorStore}, with the same embedding model as ingestion),
 * runs a product-filtered similarity search (each product separately if none is given), and applies a relevance
 * threshold.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    public SearchService(VectorStore vectorStore,
                         @Value("${rag.top-k:5}") int topK,
                         @Value("${rag.similarity-threshold:0.6}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    /** Retrieval with the relevant chunks grouped by document. */
    public SearchResponse search(String question, Product product) {
        List<SourceDocument> documents = SourceDocument.fromChunks(retrieveRelevant(question, product));
        SearchStatus status = documents.isEmpty() ? SearchStatus.NO_RELEVANT_SOURCE : SearchStatus.SOURCES_FOUND;
        return new SearchResponse(status, product, documents);
    }

    /**
     * Similarity search + relevance gate. Returns the chunks above the threshold, best first (empty if none).
     * <p>
     * Without a product, every product is searched <b>separately</b> (top-k each) and the results are merged. A single
     * unfiltered top-k would let one product with several similar chunks crowd the others out, and a cross-product
     * question ("milyen kamatperiódusok vannak?") would silently get a one-product answer.
     * <p>
     * Input is validated at the API boundary (controllers); here it is only normalised.
     */
    public List<Document> retrieveRelevant(String question, Product product) {
        String trimmed = question.strip();
        List<Product> products = product == null ? List.of(Product.values()) : List.of(product);

        List<Document> hits = new ArrayList<>();
        for (Product p : products) {
            hits.addAll(vectorStore.similaritySearch(SearchRequest.builder()
                    .query(trimmed)
                    .topK(topK)
                    .similarityThresholdAll()
                    .filterExpression(filterBuilder.eq("product", p.name()).build())
                    .build()));
        }

        List<Document> relevant = hits.stream()
                .filter(d -> score(d) >= similarityThreshold)
                .sorted(Comparator.comparingDouble(ChunkMetadata::score).reversed())
                .toList();

        log.info("Retrieval: question=\"{}\", product={}, threshold={}, relevant={}/{}, hits=[{}]",
                trimmed, product == null ? "ALL" : product, similarityThreshold, relevant.size(), hits.size(),
                hits.stream()
                        .map(d -> String.format("%s/%s/%s/%s=%.3f", meta(d, "product"), meta(d, "docId"),
                                meta(d, "section"), d.getId(), score(d)))
                        .collect(Collectors.joining(", ")));

        return relevant;
    }
}
