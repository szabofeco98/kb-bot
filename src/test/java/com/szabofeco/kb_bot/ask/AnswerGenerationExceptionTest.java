package com.szabofeco.kb_bot.ask;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerGenerationExceptionTest {

    @Test
    void responseIsAGeneric502WhileTheLogKeepsTheDetails() {
        RuntimeException cause = new RuntimeException("connection refused: localhost:11434");

        AnswerGenerationException ex = new AnswerGenerationException("The answer could not be generated.", cause);

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ex.getBody().getDetail()).isEqualTo(AnswerGenerationException.PUBLIC_DETAIL)
                .doesNotContain("11434");
        assertThat(ex.getMessage()).isEqualTo("The answer could not be generated.");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
