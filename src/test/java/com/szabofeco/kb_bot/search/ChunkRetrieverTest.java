package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.common.RagProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChunkRetrieverTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final ChunkRetriever retriever =
            new ChunkRetriever(vectorStore, new RagProperties(5, 0.5, new RagProperties.Chunk(500)));

    static Document hit(String id, String docId, String section, double score) {
        return Document.builder()
                .id(id)
                .text("[" + section + "] text of " + id)
                .metadata(Map.of("product", "LAKASHITEL", "docId", docId, "title", "Title of " + docId,
                        "version", "2026-10", "section", section, "effectiveFrom", "2026-10-01",
                        "sourceFile", docId + ".md"))
                .score(score)
                .build();
    }

    @Test
    void productFilterAndTopKArePassedToTheStore() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        retriever.retrieveRelevant("Mennyi a folyósítási díj?", Product.LAKASHITEL);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        SearchRequest request = captor.getValue();
        assertThat(request.getQuery()).isEqualTo("Mennyi a folyósítási díj?");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL);
        assertThat(request.getFilterExpression())
                .isEqualTo(new FilterExpressionBuilder().eq("product", "LAKASHITEL").build());
    }

    @Test
    void withoutProductEachProductIsSearchedSeparatelyAndMerged() {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenAnswer(invocation -> {
            SearchRequest request = invocation.getArgument(0);
            if (request.getFilterExpression().equals(b.eq("product", "LAKASHITEL").build())) {
                return List.of(hit("c1", "LH-002", "Kamatperiódusok", 0.7));
            }
            if (request.getFilterExpression().equals(b.eq("product", "SZEMELYI_KOLCSON").build())) {
                return List.of(hit("c2", "SZK-001", "Kamatperiódusok", 0.9), hit("c3", "SZK-001", "Kamatok", 0.8));
            }
            return List.of(hit("c4", "BSZ-001", "Díjak", 0.3)); // BANKSZAMLA: below threshold
        });

        List<RetrievedChunk> relevant =
                retriever.retrieveRelevant("Milyen kamatperiódusok elérhetőek?", null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore, times(3)).similaritySearch(captor.capture());
        assertThat(captor.getAllValues()).extracting(SearchRequest::getFilterExpression).containsExactly(
                b.eq("product", "LAKASHITEL").build(),
                b.eq("product", "SZEMELYI_KOLCSON").build(),
                b.eq("product", "BANKSZAMLA").build());
        assertThat(captor.getAllValues()).extracting(SearchRequest::getTopK).containsOnly(5);
        assertThat(relevant).extracting(RetrievedChunk::id).containsExactly("c2", "c3", "c1");
    }

    @Test
    void hitsBelowTheThresholdAreDropped() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(hit("c1", "LH-002", "Díjak", 0.31), hit("c2", "LH-001", "Feltételek", 0.5)));

        List<RetrievedChunk> relevant =
                retriever.retrieveRelevant("Mennyi a CSOK összege?", Product.LAKASHITEL);

        assertThat(relevant).extracting(RetrievedChunk::id).containsExactly("c2");
    }
}
