package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentHeaderTest {

    @Test
    void readsFrontmatterAndUploaderData() {
        DocumentRef header = DocumentHeader.parse(
                Map.of("id", "LH-002", "version", "2026-10", "title", "Díjak", "effective_from", "2026-10-01",
                        "product", "LAKASHITEL"),
                "doc.md", Product.LAKASHITEL);

        assertThat(header).isEqualTo(
                new DocumentRef("LH-002", "2026-10", "Díjak", "2026-10-01", Product.LAKASHITEL, "doc.md"));
    }

    /** "title:" with nothing after it must not produce an empty title (breadcrumb "[ > Díjak]"). */
    @Test
    void blankTitleFallsBackToTheFileName() {
        DocumentRef header = DocumentHeader.parse(
                Map.of("id", "LH-002", "version", "2026-10", "title", " "), "doc.md", Product.LAKASHITEL);

        assertThat(header.title()).isEqualTo("doc.md");
    }

    /** "effective_from:" with nothing after it must not reach the prompt as "effective from: ". */
    @Test
    void blankEffectiveFromIsAbsent() {
        DocumentRef header = DocumentHeader.parse(
                Map.of("id", "LH-002", "version", "2026-10", "effective_from", ""), "doc.md", Product.LAKASHITEL);

        assertThat(header.effectiveFrom()).isNull();
    }

    @Test
    void blankDeclaredProductIsNotAMismatch() {
        DocumentRef header = DocumentHeader.parse(
                Map.of("id", "LH-002", "version", "2026-10", "product", ""), "doc.md", Product.LAKASHITEL);

        assertThat(header.product()).isEqualTo(Product.LAKASHITEL);
    }

    @Test
    void blankVersionIsMissing() {
        assertThatThrownBy(() -> DocumentHeader.parse(
                Map.of("id", "LH-002", "version", "  "), "doc.md", Product.LAKASHITEL))
                .isInstanceOf(InvalidDocumentException.class)
                .hasMessageContaining("version");
    }
}
