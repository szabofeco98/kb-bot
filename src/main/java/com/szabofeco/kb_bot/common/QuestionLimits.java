package com.szabofeco.kb_bot.common;

/** Input limits shared by the question endpoints (GET /api/search, POST /api/ask). */
public final class QuestionLimits {

    /** Keeps the embedding and LLM calls bounded; a real question is far shorter. */
    public static final int MAX_LENGTH = 1000;

    private QuestionLimits() {
    }
}
