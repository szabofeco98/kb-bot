package com.szabofeco.kb_bot.ingestion;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidDocumentException extends ErrorResponseException {

    public InvalidDocumentException(String message) {
        super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message), null);
    }

    @Override
    public @Nullable String getMessage() {
        return getBody().getDetail();
    }
}
