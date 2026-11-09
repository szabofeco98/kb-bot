package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.common.QuestionLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(@NotBlank @Size(max = QuestionLimits.MAX_LENGTH) String question,
                         Product product) {

    public AskRequest {
        // Null-safe: a missing question must still reach @NotBlank and become a 400.
        question = question == null ? null : question.strip();
    }
}
