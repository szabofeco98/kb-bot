package com.szabofeco.kb_bot.ask;

/** Outcome of a knowledge-base question. Will be extended with ANSWERED / UNCLASSIFIED once generation is added. */
public enum AskStatus {
    /** At least one chunk passed the relevance threshold. */
    SOURCES_FOUND,
    /** Nothing relevant enough was found – better to say so than to answer from weak sources. */
    NO_RELEVANT_SOURCE
}
