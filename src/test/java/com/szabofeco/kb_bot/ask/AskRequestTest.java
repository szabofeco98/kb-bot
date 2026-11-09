package com.szabofeco.kb_bot.ask;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AskRequestTest {

    @Test
    void questionIsStrippedAtTheBoundary() {
        assertThat(new AskRequest("  Mennyi a folyósítási díj? \n", null).question())
                .isEqualTo("Mennyi a folyósítási díj?");
    }

    /** A missing question must stay null so that {@code @NotBlank} turns it into a 400, not an NPE. */
    @Test
    void nullQuestionStaysNull() {
        assertThat(new AskRequest(null, null).question()).isNull();
    }
}
