package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.search.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerGeneratorTest {

    @Test
    void userMessageLabelsChunksWithProductDocumentAndVersion() {
        List<RetrievedChunk> chunks = List.of(
                new RetrievedChunk("c1", "[Lakáshitel – Kondíciós lista > Díjak]\nFolyósítási díj: 1%, max. 100 000 Ft",
                        0.9, "Díjak", new DocumentRef("LH-002", "2026-10", "Lakáshitel – Kondíciós lista",
                        "2026-10-01", Product.LAKASHITEL, "lakashitel-kondiciok.md")),
                new RetrievedChunk("c2", "[Személyi kölcsön – Termékleírás > Díjak]\nFolyósítási díj: díjmentes",
                        0.8, "Díjak", new DocumentRef("SZK-001", "2026-09", "Személyi kölcsön – Termékleírás",
                        null, Product.SZEMELYI_KOLCSON, "szemelyi-kolcson.md")));

        String message = AnswerGenerator.buildUserMessage("Mennyi a folyósítási díj?", LabelledContext.of(chunks));

        assertThat(message).isEqualTo("""
                Excerpts:

                [S1] (product: lakáshitel, document: Lakáshitel – Kondíciós lista, version: 2026-10, \
                effective from: 2026-10-01)
                [Lakáshitel – Kondíciós lista > Díjak]
                Folyósítási díj: 1%, max. 100 000 Ft

                [S2] (product: személyi kölcsön, document: Személyi kölcsön – Termékleírás, version: 2026-09)
                [Személyi kölcsön – Termékleírás > Díjak]
                Folyósítási díj: díjmentes

                Question: Mennyi a folyósítási díj?""");
    }

    @Test
    void formatInstructionsCarryTheFieldDescriptions() {
        String format = new BeanOutputConverter<>(GeneratedAnswer.class).getFormat();

        assertThat(format)
                .contains("Never only yes or no")
                .contains("usedSources")
                .contains("answerable");
    }
}
