package com.szabofeco.kb_bot.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownChunkerTest {

    @Test
    void splitsByH2SectionsAndPrependsBreadcrumb() {
        MarkdownChunker chunker = new MarkdownChunker(500);
        String markdown = """
                # Doc Title
                ## Heading One
                Content one.

                ## Heading Two
                Content two.
                """;

        List<Chunk> chunks = chunker.chunk("Doc Title", markdown);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).section()).isEqualTo("Heading One");
        assertThat(chunks.get(0).text())
                .startsWith("[Doc Title > Heading One]")
                .contains("Content one.");
        assertThat(chunks.get(1).section()).isEqualTo("Heading Two");
        assertThat(chunks.get(1).text())
                .startsWith("[Doc Title > Heading Two]")
                .contains("Content two.");
    }

    @Test
    void skipsSectionsWithNoContent() {
        MarkdownChunker chunker = new MarkdownChunker(500);
        String markdown = """
                # Doc Title
                ## Empty Heading
                ## Heading With Content
                Content two.
                """;

        List<Chunk> chunks = chunker.chunk("Doc Title", markdown);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().section()).isEqualTo("Heading With Content");
    }

    @Test
    void keepsSmallTableWhole() {
        MarkdownChunker chunker = new MarkdownChunker(500);
        String markdown = """
                # Doc Title
                ## Small Table
                | A | B |
                | - | - |
                | 1 | 2 |
                """;

        List<Chunk> chunks = chunker.chunk("Doc Title", markdown);

        assertThat(chunks).hasSize(1);
        Chunk chunk = chunks.getFirst();
        assertThat(chunk.section()).isEqualTo("Small Table");
        assertThat(countOccurrences(chunk.text(), "| A | B |")).isEqualTo(1);
        assertThat(chunk.text()).contains("| 1 | 2 |");
    }

    @Test
    void textBeforeTheFirstSectionGetsTheTitleOnlyBreadcrumb() {
        MarkdownChunker chunker = new MarkdownChunker(500);
        String markdown = """
                # Doc Title
                Intro text.

                ## Heading One
                Content one.
                """;

        List<Chunk> chunks = chunker.chunk("Doc Title", markdown);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.getFirst().section()).isEmpty();
        assertThat(chunks.getFirst().text()).startsWith("[Doc Title]").contains("Intro text.");
    }

    @Test
    void splitsOversizedSectionAtBlankLinesAndKeepsTheTableWhole() {
        MarkdownChunker chunker = new MarkdownChunker(10);
        String markdown = """
                # Doc Title
                ## Big Section
                AAAAAAAAAAAAAAAAAAAA

                | A | B |
                | - | - |
                | 1 | 2 |
                | 3 | 4 |

                BBBBBBBBBBBBBBBBBBBB
                """;

        List<Chunk> chunks = chunker.chunk("Doc Title", markdown);

        assertThat(chunks).hasSize(3);
        for (Chunk chunk : chunks) {
            assertThat(chunk.section()).isEqualTo("Big Section");
            assertThat(chunk.text()).startsWith("[Doc Title > Big Section]");
        }
        assertThat(chunks.get(0).text()).contains("AAAAAAAAAAAAAAAAAAAA");
        assertThat(chunks.get(1).text()).contains("| A | B |", "| 1 | 2 |", "| 3 | 4 |");
        assertThat(chunks.get(2).text()).contains("BBBBBBBBBBBBBBBBBBBB");
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
