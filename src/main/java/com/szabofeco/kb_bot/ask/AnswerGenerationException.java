package com.szabofeco.kb_bot.ask;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class AnswerGenerationException extends ErrorResponseException {

    static final String PUBLIC_DETAIL = "The answer could not be generated. Please try again later.";

    private final String internalMessage;

    public AnswerGenerationException(String internalMessage, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, PUBLIC_DETAIL), cause);
        this.internalMessage = internalMessage;
    }

    public AnswerGenerationException(String internalMessage) {
        this(internalMessage, null);
    }

    @Override
    public String getMessage() {
        return internalMessage;
    }
}
