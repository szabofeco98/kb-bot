package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceDocumentTest {

    private static DocumentRef lh002(String version) {
        return new DocumentRef("LH-002", version, "Kondíciós lista", null, Product.LAKASHITEL, "lh.md");
    }

    private static RetrievedChunk chunk(String id, DocumentRef document, double score) {
        return new RetrievedChunk(id, "text of " + id, score, "Díjak", document);
    }

    /** Leftovers of an interrupted re-ingestion must not be cited under the other version's number. */
    @Test
    void twoVersionsOfTheSameDocIdStaySeparateSources() {
        List<SourceDocument> sources = SourceDocument.fromChunks(List.of(
                chunk("new", lh002("2026-10"), 0.9),
                chunk("old", lh002("2025-01"), 0.8)));

        assertThat(sources).extracting(s -> s.document().version()).containsExactly("2026-10", "2025-01");
    }

    @Test
    void inputOrderDoesNotMatter() {
        DocumentRef other = new DocumentRef("LH-001", "2026-10", "Feltételek", null, Product.LAKASHITEL, "f.md");

        List<SourceDocument> sources = SourceDocument.fromChunks(List.of(
                chunk("c1", lh002("2026-10"), 0.5),
                chunk("c2", other, 0.7),
                chunk("c3", lh002("2026-10"), 0.9)));

        assertThat(sources).extracting(s -> s.document().docId()).containsExactly("LH-002", "LH-001");
        assertThat(sources.getFirst().bestScore()).isEqualTo(0.9);
        assertThat(sources.getFirst().chunks()).extracting(SourceChunk::chunkId).containsExactly("c3", "c1");
    }

    /** The API shape stays flat: the document fields sit next to bestScore, not under a nested "document". */
    @Test
    void documentFieldsAreUnwrappedInJson() {
        String json = JsonMapper.builder().build().writeValueAsString(
                SourceDocument.fromChunks(List.of(chunk("c1", lh002("2026-10"), 0.9))).getFirst());

        assertThat(json).contains("\"docId\":\"LH-002\"", "\"version\":\"2026-10\"", "\"bestScore\":0.9")
                .doesNotContain("\"document\"");
    }
}
