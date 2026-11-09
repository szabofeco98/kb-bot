package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.search.RetrievedChunk;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class LabelledContextTest {

    private static RetrievedChunk chunk(String id) {
        return new RetrievedChunk(id, "text of " + id, 0.9, "Díjak",
                new DocumentRef("LH-001", "2026-10", "Title", null, Product.LAKASHITEL, "lh.md"));
    }

    private final LabelledContext context = LabelledContext.of(List.of(chunk("c1"), chunk("c2"), chunk("c3")));

    @Test
    void labelsAreOneBasedInRetrievalOrder() {
        LabelledContext ten = LabelledContext.of(IntStream.rangeClosed(1, 10).mapToObj(i -> chunk("c" + i)).toList());

        assertThat(ten.excerpts().getFirst().label()).isEqualTo("S1");
        assertThat(ten.excerpts().getLast().label()).isEqualTo("S10");
        assertThat(context.chunks()).extracting(RetrievedChunk::id).containsExactly("c1", "c2", "c3");
    }

    @Test
    void renderPutsTheBracketedLabelAndTheDocumentBeforeEachExcerpt() {
        assertThat(LabelledContext.of(List.of(chunk("c1"), chunk("c2"))).render()).isEqualTo("""
                [S1] (product: lakáshitel, document: Title, version: 2026-10)
                text of c1

                [S2] (product: lakáshitel, document: Title, version: 2026-10)
                text of c2""");
    }

    @Test
    void describeMapsLabelsToChunkIds() {
        assertThat(context.describe()).isEqualTo("[S1=c1, S2=c2, S3=c3]");
    }

    @Test
    void labelsAreNormalised() {
        assertThat(context.resolve(List.of("[s2]", " s1 "))).extracting(RetrievedChunk::id).containsExactly("c1", "c2");
    }

    @Test
    void unknownAndNullLabelsAreIgnored() {
        assertThat(context.resolve(Arrays.asList("S9", "c1", null, "S3")))
                .extracting(RetrievedChunk::id).containsExactly("c3");
    }

    @Test
    void citedChunksKeepRetrievalOrderWithoutDuplicates() {
        assertThat(context.resolve(List.of("S3", "S1", "S3")))
                .extracting(RetrievedChunk::id).containsExactly("c1", "c3");
    }

    @Test
    void nullOrUnknownSourcesGiveNoChunks() {
        assertThat(context.resolve(null)).isEmpty();
        assertThat(context.resolve(List.of("S7"))).isEmpty();
    }
}
