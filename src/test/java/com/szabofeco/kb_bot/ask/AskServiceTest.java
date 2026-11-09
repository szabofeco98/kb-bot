package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.search.ChunkRetriever;
import com.szabofeco.kb_bot.search.RetrievedChunk;
import com.szabofeco.kb_bot.search.SourceChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AskServiceTest {

    private static final String QUESTION = "Mennyi a folyósítási díj?";

    private static final Product LAKASHITEL = Product.LAKASHITEL;

    private final ChunkRetriever retriever = mock(ChunkRetriever.class);
    private final AnswerGenerator answerGenerator = mock(AnswerGenerator.class);
    private final AskService service = new AskService(retriever, answerGenerator);

    private static RetrievedChunk chunk(String id, String docId, String section, double score) {
        return new RetrievedChunk(id, "[" + section + "] text of " + id, score, section,
                new DocumentRef(docId, "2026-10", "Title of " + docId, null, Product.LAKASHITEL, docId + ".md"));
    }

    // S1 = c1 (LH-002), S2 = c2 (LH-001), S3 = c3 (LH-002) – already sorted best first, as ChunkRetriever returns them
    private final List<RetrievedChunk> retrieved = List.of(
            chunk("c1", "LH-002", "Díjak", 0.9),
            chunk("c2", "LH-001", "Feltételek", 0.8),
            chunk("c3", "LH-002", "Kamatok", 0.7));

    private void retrieves(List<RetrievedChunk> chunks) {
        when(retriever.retrieveRelevant(QUESTION, LAKASHITEL)).thenReturn(chunks);
    }

    private void modelReturns(GeneratedAnswer answer) {
        when(answerGenerator.generate(anyString(), any(LabelledContext.class))).thenReturn(answer);
    }

    @Test
    void noRelevantChunksMeansNoLlmCall() {
        retrieves(List.of());

        AskResponse response = service.ask(QUESTION, LAKASHITEL);

        assertThat(response).isEqualTo(AskResponse.noRelevantSource(LAKASHITEL));
        verifyNoInteractions(answerGenerator);
    }

    @Test
    void notAnswerableMeansNoRelevantSource() {
        retrieves(retrieved);
        modelReturns(new GeneratedAnswer(false, "", List.of()));

        AskResponse response = service.ask(QUESTION, LAKASHITEL);

        assertThat(response.status()).isEqualTo(AskStatus.NO_RELEVANT_SOURCE);
        assertThat(response.answer()).isNull();
        assertThat(response.sources()).isEmpty();
    }

    @Test
    void answerReturnsOnlyCitedChunksGroupedByDocument() {
        retrieves(retrieved);
        modelReturns(new GeneratedAnswer(true, " A folyósítási díj 1%, maximum 100 000 Ft. ", List.of("S3", "S1")));

        AskResponse response = service.ask(QUESTION, LAKASHITEL);

        assertThat(response.status()).isEqualTo(AskStatus.ANSWERED);
        assertThat(response.answer()).isEqualTo("A folyósítási díj 1%, maximum 100 000 Ft.");
        assertThat(response.product()).isEqualTo(Product.LAKASHITEL);
        // S2 (LH-001) was not cited, so it is not a source. Chunks keep retrieval order, not citation order.
        assertThat(response.sources()).extracting(d -> d.document().docId()).containsExactly("LH-002");
        assertThat(response.sources().getFirst().chunks()).extracting(SourceChunk::chunkId)
                .containsExactly("c1", "c3");
        verify(answerGenerator).generate(eq(QUESTION), argThat(context -> context.chunks().equals(retrieved)));
    }

    @Test
    void labelsAreNormalisedAndUnknownOnesDropped() {
        retrieves(retrieved);
        modelReturns(new GeneratedAnswer(true, "Answer.", List.of("[s2]", "S9", "c1")));

        AskResponse response = service.ask(QUESTION, LAKASHITEL);

        assertThat(response.status()).isEqualTo(AskStatus.ANSWERED);
        assertThat(response.sources()).extracting(d -> d.document().docId()).containsExactly("LH-001");
    }

    @Test
    void answerWithoutAnyValidSourceIsNotReturned() {
        retrieves(retrieved);
        modelReturns(new GeneratedAnswer(true, "Answer from nowhere.", List.of("S7")));

        AskResponse response = service.ask(QUESTION, LAKASHITEL);

        assertThat(response).isEqualTo(AskResponse.noRelevantSource(LAKASHITEL));
    }

    @Test
    void answerWithNullSourcesIsNotReturned() {
        retrieves(retrieved);
        modelReturns(new GeneratedAnswer(true, "Answer.", null));

        assertThat(service.ask(QUESTION, LAKASHITEL).status()).isEqualTo(AskStatus.NO_RELEVANT_SOURCE);
    }

    @Test
    void searchWithoutProductIsPassedThrough() {
        when(retriever.retrieveRelevant(any(), any())).thenReturn(List.of());

        AskResponse response = service.ask(QUESTION, null);

        verify(retriever).retrieveRelevant(QUESTION, null);
        assertThat(response.product()).isNull();
    }
}
