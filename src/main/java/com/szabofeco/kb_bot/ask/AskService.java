package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.search.ChunkRetriever;
import com.szabofeco.kb_bot.search.RetrievedChunk;
import com.szabofeco.kb_bot.search.SourceDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final ChunkRetriever retriever;
    private final AnswerGenerator answerGenerator;

    AskService(ChunkRetriever retriever, AnswerGenerator answerGenerator) {
        this.retriever = retriever;
        this.answerGenerator = answerGenerator;
    }

    public AskResponse ask(String question, Product product) {
        List<RetrievedChunk> chunks = retriever.retrieveRelevant(question, product);
        if (chunks.isEmpty()) {
            log.warn("Ask: question=\"{}\", product={}, context=[], status={}",
                    question, product, AskStatus.NO_RELEVANT_SOURCE);
            return AskResponse.noRelevantSource(product);
        }

        LabelledContext context = LabelledContext.of(chunks);
        GeneratedAnswer generated = answerGenerator.generate(question, context);
        List<RetrievedChunk> cited = context.resolve(generated.usedSources());

        // Fail closed: no answer, or an answer without any valid citation, never goes out.
        AskResponse response = generated.hasAnswer() && !cited.isEmpty()
                ? AskResponse.answered(generated.text(), product, SourceDocument.fromChunks(cited))
                : AskResponse.noRelevantSource(product);

        log.debug("Ask: question=\"{}\", product={}, context={}, answerable={}, usedSources={}, cited={}, status={}",
                question, product, context.describe(), generated.answerable(),
                generated.usedSources(), cited.stream().map(RetrievedChunk::id).toList(), response.status());
        return response;
    }
}
