package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.search.SourceDocument;

import java.util.List;

/**
 * @param answer  the generated answer; {@code null} unless {@link AskStatus#ANSWERED}
 * @param product the product filter that was applied, or {@code null} if all products were searched
 * @param sources only the documents/chunks the answer cites (built in code, never parsed from model text)
 */
public record AskResponse(AskStatus status, String answer, Product product, List<SourceDocument> sources) {

    static AskResponse noRelevantSource(Product product) {
        return new AskResponse(AskStatus.NO_RELEVANT_SOURCE, null, product, List.of());
    }
}
