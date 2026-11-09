package com.szabofeco.kb_bot.ask;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Structured output of the model. The {@link JsonPropertyDescription}s go into the JSON schema of the format
 * instructions, so they steer the model alongside the system prompt.
 */
public record GeneratedAnswer(
        @JsonPropertyDescription("false if the excerpts do not contain the answer")
        boolean answerable,

        @JsonPropertyDescription("Full-sentence answer: first the direct answer, then the reasons, conditions and "
                + "exceptions from the excerpts. Never only yes or no. Empty if not answerable.")
        String answer,

        @JsonPropertyDescription("Labels of the excerpts the answer is based on, e.g. [\"S1\", \"S3\"]")
        List<String> usedSources) {

    boolean hasAnswer() {
        return answerable && answer != null && !answer.isBlank();
    }

    String text() {
        return answer.strip();
    }
}
