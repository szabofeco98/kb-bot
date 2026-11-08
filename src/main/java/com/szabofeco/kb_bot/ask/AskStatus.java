package com.szabofeco.kb_bot.ask;

/** Outcome of {@code POST /api/ask}. Will be extended with UNCLASSIFIED once the product classifier is added. */
public enum AskStatus {
    /** The answer was generated from the retrieved chunks and cites at least one of them. */
    ANSWERED,
    /**
     * No answer goes out: nothing passed the relevance threshold, the model said the excerpts don't contain the
     * answer, or it cited no valid source.
     */
    NO_RELEVANT_SOURCE
}
