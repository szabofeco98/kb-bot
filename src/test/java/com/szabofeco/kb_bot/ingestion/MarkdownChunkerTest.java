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
    void splitsOversizedH2SectionByH3Subsections() {
        MarkdownChunker chunker = new MarkdownChunker(10);
        String markdown = """
                # Doc Title
                ## Big Section
                ### Sub One
                AAAAAAAAAAAAAAAAAAAA
                ### Sub Two
                BBBBBBBBBBBBBBBBBBBB
                """;

        List<Chunk> chunks = chunker.chunk("Doc Title", markdown);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).section()).isEqualTo("Big Section > Sub One");
        assertThat(chunks.get(0).text())
                .startsWith("[Doc Title > Big Section > Sub One]")
                .contains("AAAAAAAAAAAAAAAAAAAA");
        assertThat(chunks.get(1).section()).isEqualTo("Big Section > Sub Two");
        assertThat(chunks.get(1).text())
                .startsWith("[Doc Title > Big Section > Sub Two]")
                .contains("BBBBBBBBBBBBBBBBBBBB");
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
    void splitsOversizedTableByRowsAndRepeatsHeaderInEveryPart() {
        MarkdownChunker chunker = new MarkdownChunker(8);
        String markdown = """
                # Doc Title
                ## Big Table
                | A | B |
                | - | - |
                | 1 | 2 |
                | 3 | 4 |
                | 5 | 6 |
                | 7 | 8 |
                """;

        List<Chunk> chunks = chunker.chunk("Doc Title", markdown);

        assertThat(chunks).hasSizeGreaterThan(1);
        for (Chunk chunk : chunks) {
            assertThat(chunk.section()).isEqualTo("Big Table");
            assertThat(chunk.text()).contains("| A | B |").contains("| - | - |");
        }

        String allChunkText = chunks.stream().map(Chunk::text).reduce("", String::concat);
        assertThat(allChunkText)
                .contains("| 1 | 2 |").contains("| 3 | 4 |")
                .contains("| 5 | 6 |").contains("| 7 | 8 |");
        assertThat(countOccurrences(allChunkText, "| 1 | 2 |")).isEqualTo(1);
        assertThat(countOccurrences(allChunkText, "| 7 | 8 |")).isEqualTo(1);
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
