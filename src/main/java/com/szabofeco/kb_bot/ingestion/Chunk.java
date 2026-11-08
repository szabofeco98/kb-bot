package com.szabofeco.kb_bot.ingestion;

/**
 * @param section the section path, e.g. "Fees" or "Fees > Other fees" (empty for the part before the first heading)
 * @param text    the chunk text together with the breadcrumb prefix – this is what goes into the embedding
 */
public record Chunk(String section, String text) {
}
