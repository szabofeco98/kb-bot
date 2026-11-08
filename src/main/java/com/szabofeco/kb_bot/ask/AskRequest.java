package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.common.QuestionLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param product optional product filter; {@code null} searches all products */
public record AskRequest(@NotBlank @Size(max = QuestionLimits.MAX_LENGTH) String question,
                         Product product) {
}
