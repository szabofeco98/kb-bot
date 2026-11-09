package com.szabofeco.kb_bot.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * {@code rag.*} settings; the single source of defaults. {@code similarityThreshold} is a placeholder – calibrate it
 * on a test question set.
 */
@Validated
@ConfigurationProperties("rag")
public record RagProperties(@DefaultValue("5") @Positive int topK,
                            @DefaultValue("0.5") @DecimalMin("0.0") @DecimalMax("1.0") double similarityThreshold,
                            @DefaultValue @Valid Chunk chunk) {

    public record Chunk(@DefaultValue("500") @Positive int maxTokens) {
    }
}
