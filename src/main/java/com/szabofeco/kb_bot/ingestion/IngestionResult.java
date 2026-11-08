package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.Product;

public record IngestionResult(String docId, String version, Product product, int chunkCount) {
}
