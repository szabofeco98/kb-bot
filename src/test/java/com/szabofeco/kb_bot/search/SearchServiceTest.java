package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.Product;
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

class SearchServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final SearchService service = new SearchService(vectorStore, 5, 0.5);

    private static Document hit(String id, String docId, String section, double score) {
        return Document.builder()
                .id(id)
                .text("[" + section + "] text of " + id)
                .metadata(Map.of("product", "LAKASHITEL", "docId", docId, "title", "Title of " + docId,
                        "version", "2026-10", "section", section, "effectiveFrom", "2026-10-01",
                        "sourceFile", docId + ".md"))
                .score(score)
                .build();
    }

    private SearchRequest capturedRequest() {
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        return captor.getValue();
    }

    @Test
    void productFilterAndTopKArePassedToTheStore() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        service.search("  Mennyi a folyósítási díj?  ", Product.LAKASHITEL);

        SearchRequest request = capturedRequest();
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

        List<Document> relevant = service.retrieveRelevant("Milyen kamatperiódusok elérhetőek?", null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore, times(3)).similaritySearch(captor.capture());
        assertThat(captor.getAllValues()).extracting(SearchRequest::getFilterExpression).containsExactly(
                b.eq("product", "LAKASHITEL").build(),
                b.eq("product", "SZEMELYI_KOLCSON").build(),
                b.eq("product", "BANKSZAMLA").build());
        assertThat(captor.getAllValues()).extracting(SearchRequest::getTopK).containsOnly(5);
        // Lakáshitel is kept even though személyi kölcsön has more (and better) hits; merged best first.
        assertThat(relevant).extracting(Document::getId).containsExactly("c2", "c3", "c1");
    }

    @Test
    void allHitsBelowThresholdMeansNoRelevantSource() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(hit("c1", "LH-002", "Díjak", 0.31), hit("c2", "LH-001", "Feltételek", 0.2)));

        SearchResponse response = service.search("Mennyi a CSOK összege?", Product.LAKASHITEL);

        assertThat(response.status()).isEqualTo(SearchStatus.NO_RELEVANT_SOURCE);
        assertThat(response.documents()).isEmpty();
        assertThat(response.product()).isEqualTo(Product.LAKASHITEL);
    }

    @Test
    void relevantChunksAreGroupedByDocumentBestFirst() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                hit("c1", "LH-001", "Feltételek", 0.55),
                hit("c2", "LH-002", "Díjak", 0.2),       // below threshold – dropped
                hit("c3", "LH-002", "Díjak", 0.82),
                hit("c4", "LH-002", "Kamatok", 0.6)));

        SearchResponse response = service.search("Mennyi a folyósítási díj?", Product.LAKASHITEL);

        assertThat(response.status()).isEqualTo(SearchStatus.SOURCES_FOUND);
        assertThat(response.documents()).extracting(SourceDocument::docId).containsExactly("LH-002", "LH-001");

        SourceDocument best = response.documents().get(0);
        assertThat(best).isEqualTo(new SourceDocument("LH-002", "Title of LH-002", "2026-10", "2026-10-01",
                "LAKASHITEL", "LH-002.md", 0.82, List.of(
                        new SourceChunk("c3", "Díjak", 0.82, "[Díjak] text of c3"),
                        new SourceChunk("c4", "Kamatok", 0.6, "[Kamatok] text of c4"))));
        assertThat(response.documents().get(1).chunks()).extracting(SourceChunk::chunkId).containsExactly("c1");
    }
}
