package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.search.SearchService;
import com.szabofeco.kb_bot.search.SourceDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fixed RAG pipeline: retrieval → relevance gate → grounded generation → citations built in code.
 * Every path that cannot be backed by a cited source ends in {@link AskStatus#NO_RELEVANT_SOURCE}.
 */
@Service
public class AskService {

    private static final Logger log = LoggerFactory.getLogger(AskService.class);

    private final SearchService searchService;
    private final AnswerGenerator answerGenerator;

    public AskService(SearchService searchService, AnswerGenerator answerGenerator) {
        this.searchService = searchService;
        this.answerGenerator = answerGenerator;
    }

    public AskResponse ask(String question, Product product) {
        List<Document> chunks = searchService.retrieveRelevant(question, product);
        if (chunks.isEmpty()) {
            // Nothing relevant enough: the LLM is not called at all.
            audit(question, product, chunks, null, List.of(), List.of(), AskStatus.NO_RELEVANT_SOURCE);
            return AskResponse.noRelevantSource(product);
        }

        GeneratedAnswer generated = answerGenerator.generate(question, chunks);

        // Citations: map the labels the model used back to the chunks it was given. Labels that were not in the
        // context are dropped – the model cannot cite anything we did not retrieve.
        Map<String, Document> byLabel = new LinkedHashMap<>();
        for (int i = 0; i < chunks.size(); i++) {
            byLabel.put(AnswerGenerator.label(i), chunks.get(i));
        }
        List<String> cited = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        List<String> usedSources = generated.usedSources() == null ? List.of() : generated.usedSources();
        for (String raw : usedSources) {
            String label = raw == null ? "" : raw.replaceAll("[\\[\\]\\s]", "").toUpperCase(Locale.ROOT);
            if (byLabel.containsKey(label)) {
                if (!cited.contains(label)) {
                    cited.add(label);
                }
            } else {
                dropped.add(raw);
            }
        }

        boolean hasAnswer = generated.answerable() && generated.answer() != null && !generated.answer().isBlank();
        if (!hasAnswer || cited.isEmpty()) {
            // Not answerable, or an answer without any valid source: fail closed, nothing uncited goes out.
            audit(question, product, chunks, generated, cited, dropped, AskStatus.NO_RELEVANT_SOURCE);
            return AskResponse.noRelevantSource(product);
        }

        // Keep the retrieval order (best first): sources are ordered by relevance, not by citation order.
        List<Document> citedChunks = byLabel.entrySet().stream()
                .filter(e -> cited.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        audit(question, product, chunks, generated, cited, dropped, AskStatus.ANSWERED);
        return new AskResponse(AskStatus.ANSWERED, generated.answer().strip(), product,
                SourceDocument.fromChunks(citedChunks));
    }

    private static void audit(String question, Product product, List<Document> chunks, GeneratedAnswer generated,
                              List<String> cited, List<String> dropped, AskStatus status) {
        List<String> context = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            context.add(AnswerGenerator.label(i) + "=" + chunks.get(i).getId());
        }
        log.info("Ask: question=\"{}\", product={}, context={}, answerable={}, cited={}, dropped={}, status={}",
                question.strip(), product == null ? "ALL" : product, context,
                generated == null ? "-" : generated.answerable(), cited, dropped, status);
    }
}
