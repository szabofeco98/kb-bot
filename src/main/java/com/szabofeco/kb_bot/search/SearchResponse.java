package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.Product;

import java.util.List;

public record SearchResponse(SearchStatus status, Product product, List<SourceDocument> documents) {
}
