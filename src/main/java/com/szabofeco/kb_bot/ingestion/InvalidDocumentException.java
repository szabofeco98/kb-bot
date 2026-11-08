package com.szabofeco.kb_bot.ingestion;

/** The uploaded document does not satisfy the ingestion rules (results in a 400 response). */
public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(String message) {
        super(message);
    }
}
