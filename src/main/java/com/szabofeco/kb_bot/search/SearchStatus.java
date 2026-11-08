package com.szabofeco.kb_bot.search;

/** Outcome of a retrieval-only search ({@code GET /api/search}). */
public enum SearchStatus {
    /** At least one chunk passed the relevance threshold. */
    SOURCES_FOUND,
    /** Nothing relevant enough was found – better to say so than to answer from weak sources. */
    NO_RELEVANT_SOURCE
}
