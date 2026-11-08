package com.szabofeco.kb_bot.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownFileValidatorTest {

    private final MarkdownFileValidator validator = new MarkdownFileValidator();

    private static MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "text/markdown", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void nonEmptyMarkdownFileIsValid() {
        assertThat(validator.isValid(file("kondiciok.md", "# Title"), null)).isTrue();
        assertThat(validator.isValid(file("KONDICIOK.MD", "# Title"), null)).isTrue();
    }

    @Test
    void otherFormatsAreInvalid() {
        assertThat(validator.isValid(file("kondiciok.pdf", "%PDF"), null)).isFalse();
        assertThat(validator.isValid(file(null, "# Title"), null)).isFalse();
    }

    @Test
    void emptyOrMissingFileIsInvalid() {
        assertThat(validator.isValid(file("kondiciok.md", ""), null)).isFalse();
        assertThat(validator.isValid(null, null)).isFalse();
    }
}
