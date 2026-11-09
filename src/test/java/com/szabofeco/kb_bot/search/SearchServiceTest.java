package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private static final String QUESTION = "Mennyi a folyósítási díj?";
    private static final Product LAKASHITEL = Product.LAKASHITEL;

    private final ChunkRetriever retriever = mock(ChunkRetriever.class);
    private final SearchService service = new SearchService(retriever);

    private static RetrievedChunk chunk(String id, String docId, String section, double score) {
        return new RetrievedChunk(id, "[" + section + "] text of " + id, score, section, new DocumentRef(docId,
                "2026-10", "Title of " + docId, "2026-10-01", Product.LAKASHITEL, docId + ".md"));
    }

    @Test
    void noRelevantChunkMeansNoRelevantSource() {
        when(retriever.retrieveRelevant(QUESTION, LAKASHITEL)).thenReturn(List.of());

        SearchResponse response = service.search(QUESTION, LAKASHITEL);

        assertThat(response).isEqualTo(new SearchResponse(SearchStatus.NO_RELEVANT_SOURCE, Product.LAKASHITEL,
                List.of()));
    }

    @Test
    void relevantChunksAreGroupedByDocumentBestFirst() {
        when(retriever.retrieveRelevant(QUESTION, LAKASHITEL)).thenReturn(List.of(
                chunk("c3", "LH-002", "Díjak", 0.82),
                chunk("c4", "LH-002", "Kamatok", 0.6),
                chunk("c1", "LH-001", "Feltételek", 0.55)));

        SearchResponse response = service.search(QUESTION, LAKASHITEL);

        assertThat(response.status()).isEqualTo(SearchStatus.SOURCES_FOUND);
        assertThat(response.documents()).extracting(d -> d.document().docId()).containsExactly("LH-002", "LH-001");
        assertThat(response.documents().getFirst()).isEqualTo(new SourceDocument(
                new DocumentRef("LH-002", "2026-10", "Title of LH-002", "2026-10-01", Product.LAKASHITEL,
                        "LH-002.md"),
                0.82, List.of(
                new SourceChunk("c3", "Díjak", 0.82, "[Díjak] text of c3"),
                new SourceChunk("c4", "Kamatok", 0.6, "[Kamatok] text of c4"))));
    }

    @Test
    void allProductsScopeEchoesNullProduct() {
        when(retriever.retrieveRelevant(QUESTION, null)).thenReturn(List.of());

        assertThat(service.search(QUESTION, null).product()).isNull();
    }
}
