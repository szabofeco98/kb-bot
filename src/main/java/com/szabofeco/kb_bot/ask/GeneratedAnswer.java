package com.szabofeco.kb_bot.ask;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Structured output of the model. The {@link JsonPropertyDescription}s end up in the JSON schema that Spring AI
 * sends as format instructions, so they steer the model field by field (a second place besides the system prompt).
 *
 * @param answerable  {@code false} if the excerpts do not contain the answer
 * @param answer      the answer text (ignored when not answerable)
 * @param usedSources labels of the excerpts the answer is based on, e.g. {@code ["S1", "S3"]}
 */
public record GeneratedAnswer(
        @JsonPropertyDescription("false if the excerpts do not contain the answer")
        boolean answerable,

        @JsonPropertyDescription("Full-sentence answer: first the direct answer, then the reasons, conditions and "
                + "exceptions from the excerpts. Never only yes or no. Empty if not answerable.")
        String answer,

        @JsonPropertyDescription("Labels of the excerpts the answer is based on, e.g. [\"S1\", \"S3\"]")
        List<String> usedSources) {
}
