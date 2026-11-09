package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.Product;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class IngestionServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final IngestionService service = new IngestionService(vectorStore, new MarkdownChunker(500));

    private static String markdown(String frontMatter, String body) {
        return "---\n" + frontMatter + "\n---\n\n" + body;
    }

    @Test
    void chunksCarryTheExpectedMetadata() {
        String content = markdown(
                "id: LH-002\nversion: 2026-10\ntitle: Folyósítási díjak\neffective_from: 2026-10-01",
                "## Díjak\n\nA folyósítási díj a hitelösszeg 1%-a.");

        IngestionResult result = service.ingest("lakashitel-dijak.md", content, Product.LAKASHITEL);

        assertThat(result).isEqualTo(new IngestionResult("LH-002", "2026-10", Product.LAKASHITEL, 1));

        ArgumentCaptor<List<Document>> added = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(added.capture());
        List<Document> documents = added.getValue();
        assertThat(documents).hasSize(1);

        Document document = documents.get(0);
        assertThat(document.getText()).contains("A folyósítási díj a hitelösszeg 1%-a.");
        assertThat(document.getMetadata())
                .containsEntry("product", "LAKASHITEL")
                .containsEntry("docId", "LH-002")
                .containsEntry("version", "2026-10")
                .containsEntry("title", "Folyósítási díjak")
                .containsEntry("section", "Díjak")
                .containsEntry("chunkIndex", 0)
                .containsEntry("sourceFile", "lakashitel-dijak.md")
                .containsEntry("effectiveFrom", "2026-10-01")
                .containsKey("ingestionId")
                .doesNotContainValue(null);
    }

    @Test
    void effectiveFromIsOmittedRatherThanStoredAsNull() {
        String content = markdown("id: LH-002\nversion: 2026-10", "## Díjak\n\nSzöveg.");

        service.ingest("doc.md", content, Product.LAKASHITEL);

        ArgumentCaptor<List<Document>> added = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(added.capture());
        assertThat(added.getValue().get(0).getMetadata()).doesNotContainKey("effectiveFrom");
    }

    @Test
    void addsTheNewVersionBeforeDeletingTheOldOneWithTheCorrectFilter() {
        String content = markdown("id: LH-002\nversion: 2026-10", "## Díjak\n\nSzöveg.");

        service.ingest("doc.md", content, Product.LAKASHITEL);

        ArgumentCaptor<List<Document>> added = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Filter.Expression> filter = ArgumentCaptor.forClass(Filter.Expression.class);
        InOrder order = inOrder(vectorStore);
        order.verify(vectorStore).add(added.capture());
        order.verify(vectorStore).delete(filter.capture());

        String ingestionId = (String) added.getValue().get(0).getMetadata().get("ingestionId");
        FilterExpressionBuilder f = new FilterExpressionBuilder();
        Filter.Expression expected = f.and(f.eq("docId", "LH-002"), f.ne("ingestionId", ingestionId)).build();
        assertThat(filter.getValue()).isEqualTo(expected);
    }

    @Test
    void rejectsAProductThatDoesNotMatchTheFrontmatter() {
        String content = markdown("id: LH-002\nversion: 2026-10\nproduct: SZEMELYI_KOLCSON", "## Díjak\n\nSzöveg.");

        assertThatThrownBy(() -> service.ingest("doc.md", content, Product.LAKASHITEL))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("LAKASHITEL")
                .hasMessageContaining("SZEMELYI_KOLCSON");
        verifyNoInteractions(vectorStore);
    }

    @Test
    void rejectsAMissingId() {
        String content = markdown("version: 2026-10", "## Díjak\n\nSzöveg.");

        assertThatThrownBy(() -> service.ingest("doc.md", content, Product.LAKASHITEL))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("id");
        verifyNoInteractions(vectorStore);
    }

    @Test
    void rejectsAFilterExpressionInjectionAttemptInTheId() {
        String content = markdown("id: LH-002' || '1'=='1\nversion: 2026-10", "## Díjak\n\nSzöveg.");

        assertThatThrownBy(() -> service.ingest("doc.md", content, Product.LAKASHITEL))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("Invalid characters");
        verifyNoInteractions(vectorStore);
    }

    @Test
    void rejectsADocumentWithNoProcessableText() {
        String content = markdown("id: LH-002\nversion: 2026-10", "");

        assertThatThrownBy(() -> service.ingest("doc.md", content, Product.LAKASHITEL))
                .isInstanceOf(InvalidDocumentException.class);
        verifyNoInteractions(vectorStore);
    }
}
