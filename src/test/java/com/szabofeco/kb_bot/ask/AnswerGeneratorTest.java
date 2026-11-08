package com.szabofeco.kb_bot.ask;

import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerGeneratorTest {

    @Test
    void userMessageLabelsChunksWithProductDocumentAndVersion() {
        List<Document> chunks = List.of(
                new Document("[Lakáshitel – Kondíciós lista > Díjak]\nFolyósítási díj: 1%, max. 100 000 Ft",
                        Map.of("product", "LAKASHITEL", "title", "Lakáshitel – Kondíciós lista",
                                "version", "2026-10", "effectiveFrom", "2026-10-01")),
                new Document("[Személyi kölcsön – Termékleírás > Díjak]\nFolyósítási díj: díjmentes",
                        Map.of("product", "SZEMELYI_KOLCSON", "title", "Személyi kölcsön – Termékleírás",
                                "version", "2026-09")));

        String message = AnswerGenerator.buildUserMessage("  Mennyi a folyósítási díj? ", chunks);

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
    void productNamesAreHumanReadable() {
        assertThat(AnswerGenerator.productName("SZEMELYI_KOLCSON")).isEqualTo("személyi kölcsön");
        assertThat(AnswerGenerator.productName("AUTOHITEL")).isEqualTo("AUTOHITEL");
        assertThat(AnswerGenerator.productName(null)).isEqualTo("unknown");
    }

    @Test
    void labelsAreOneBased() {
        assertThat(AnswerGenerator.label(0)).isEqualTo("S1");
        assertThat(AnswerGenerator.label(9)).isEqualTo("S10");
    }

    /** The field descriptions must reach the model via the JSON schema in the format instructions. */
    @Test
    void formatInstructionsCarryTheFieldDescriptions() {
        String format = new BeanOutputConverter<>(GeneratedAnswer.class).getFormat();

        assertThat(format)
                .contains("Never only yes or no")
                .contains("usedSources")
                .contains("answerable");
    }
}
