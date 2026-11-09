package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Keys are literals on purpose: they are the persisted format in the vector store, so renaming a constant in
 * {@code ChunkMetadataKeys} must break this test rather than silently orphan the stored chunks.
 */
class RetrievedChunkTest {

    private static Map<String, Object> metadata() {
        Map<String, Object> metadata = new HashMap<>(Map.of("product", "LAKASHITEL", "docId", "LH-002",
                "title", "Kondíciós lista", "version", "2026-10", "section", "Díjak", "sourceFile", "lh.md",
                "chunkIndex", 3, "ingestionId", "abc"));
        metadata.put("effectiveFrom", "2026-10-01");
        return metadata;
    }

    @Test
    void mapsTheStoredMetadataToTypedFields() {
        Document document = Document.builder().id("c1").text("chunk text").metadata(metadata()).score(0.87).build();

        assertThat(RetrievedChunk.from(document)).isEqualTo(new RetrievedChunk("c1", "chunk text", 0.87, "Díjak",
                new DocumentRef("LH-002", "2026-10", "Kondíciós lista", "2026-10-01", Product.LAKASHITEL, "lh.md")));
    }

    @Test
    void effectiveFromIsOptionalAndAMissingScoreIsZero() {
        Map<String, Object> metadata = metadata();
        metadata.remove("effectiveFrom");

        RetrievedChunk chunk = RetrievedChunk.from(Document.builder().id("c1").text("t").metadata(metadata).build());

        assertThat(chunk.document().effectiveFrom()).isNull();
        assertThat(chunk.score()).isZero();
    }

    @Test
    void aMissingRequiredKeyFailsLoudly() {
        Map<String, Object> metadata = metadata();
        metadata.remove("docId");

        assertThatThrownBy(() -> RetrievedChunk.from(Document.builder().id("c1").text("t").metadata(metadata).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("docId");
    }

    @Test
    void anUnknownProductFailsLoudly() {
        Map<String, Object> metadata = metadata();
        metadata.put("product", "AUTOHITEL");

        assertThatThrownBy(() -> RetrievedChunk.from(Document.builder().id("c1").text("t").metadata(metadata).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AUTOHITEL");
    }
}
