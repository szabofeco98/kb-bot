package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.search.SourceDocument;

import java.util.List;

public record AskResponse(AskStatus status, String answer, Product product, List<SourceDocument> sources) {

    static AskResponse answered(String answer, Product product, List<SourceDocument> sources) {
        return new AskResponse(AskStatus.ANSWERED, answer, product, sources);
    }

    static AskResponse noRelevantSource(Product product) {
        return new AskResponse(AskStatus.NO_RELEVANT_SOURCE, null, product, List.of());
    }
}
