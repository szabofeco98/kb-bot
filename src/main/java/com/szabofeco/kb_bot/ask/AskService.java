package com.szabofeco.kb_bot.ask;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Retrieval: embeds the question (inside the {@link VectorStore}, with the same embedding model as ingestion),
 * runs a similarity search optionally filtered by product, and applies a relevance threshold.
 */
@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    public AskService(VectorStore vectorStore,
                      @Value("${rag.top-k:5}") int topK,
                      @Value("${rag.similarity-threshold:0.6}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    /** Input is validated at the API boundary ({@link AskController}); here it is only normalised. */
    public AskResponse ask(String question, Product product) {
        String trimmed = question.strip();

        SearchRequest.Builder request = SearchRequest.builder()
                .query(trimmed)
                .topK(topK)
                .similarityThresholdAll();
        if (product != null) {
            request.filterExpression(filterBuilder.eq("product", product.name()).build());
        }
        List<Document> hits = vectorStore.similaritySearch(request.build());

        List<Document> relevant = hits.stream()
                .filter(d -> score(d) >= similarityThreshold)
                .sorted(Comparator.comparingDouble(AskService::score).reversed())
                .toList();
        List<SourceDocument> documents = groupByDocument(relevant);
        AskStatus status = documents.isEmpty() ? AskStatus.NO_RELEVANT_SOURCE : AskStatus.SOURCES_FOUND;

        log.info("Search: question=\"{}\", product={}, threshold={}, status={}, hits=[{}]",
                trimmed, product == null ? "ALL" : product, similarityThreshold, status,
                hits.stream()
                        .map(d -> String.format("%s/%s/%s=%.3f", meta(d, "docId"), meta(d, "section"),
                                d.getId(), score(d)))
                        .collect(Collectors.joining(", ")));

        return new AskResponse(status, product, documents);
    }

    private static double score(Document document) {
        return document.getScore() == null ? 0.0 : document.getScore();
    }

    private static String meta(Document document, String key) {
        Object value = document.getMetadata().get(key);
        return value == null ? null : value.toString();
    }

    /**
     * Groups chunks (already sorted best first) by docId. Insertion order is kept, so documents end up ordered by
     * their best chunk, and chunks within a document stay best first.
     */
    private static List<SourceDocument> groupByDocument(List<Document> sortedChunks) {
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
