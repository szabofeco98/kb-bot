package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;

import java.util.List;

/**
 * @param product the product filter that was applied, or {@code null} if all products were searched
 * @param documents documents with relevant chunks, best match first; empty for {@link AskStatus#NO_RELEVANT_SOURCE}
 */
public record AskResponse(AskStatus status, Product product, List<SourceDocument> documents) {
}
