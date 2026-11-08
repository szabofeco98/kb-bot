package com.szabofeco.kb_bot.ask;

/** The LLM call failed or returned unusable output (results in a 502 response – no partial answer goes out). */
public class AnswerGenerationException extends RuntimeException {
    public AnswerGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
