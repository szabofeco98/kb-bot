package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.RagProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Splits a document into "##" sections. A section that is too long is split at blank lines, so paragraphs, tables and
 * lists without blank lines stay whole (a list with blank lines between its items may be split). Every chunk starts
 * with a breadcrumb ("[Title > Section]") so it is understandable on its own.
 */
@Component
class MarkdownChunker {

    private static final Pattern BLANK_LINE = Pattern.compile("\\n\\s*\\n");

    private final int maxTokens;

    @Autowired
    MarkdownChunker(RagProperties rag) {
        this(rag.chunk().maxTokens());
    }

    MarkdownChunker(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    List<Chunk> chunk(String title, String markdownBody) {
        List<Chunk> chunks = new ArrayList<>();
        String section = "";
        StringBuilder content = new StringBuilder();
        for (String line : markdownBody.lines().toList()) {
            if (line.startsWith("# ")) {
                continue; // H1: the title comes from the frontmatter
            }
            if (line.startsWith("## ")) {
                addSection(chunks, title, section, content.toString());
                section = line.substring(3).strip();
                content.setLength(0);
            } else {
                content.append(line).append('\n');
            }
        }
        addSection(chunks, title, section, content.toString());
        return chunks;
    }

    private void addSection(List<Chunk> chunks, String title, String section, String content) {
        if (content.isBlank()) {
            return;
        }
        String breadcrumb = section.isEmpty() ? "[" + title + "]" : "[" + title + " > " + section + "]";
        for (String part : split(content.strip())) {
            chunks.add(new Chunk(section, breadcrumb + "\n\n" + part));
        }
    }

    /** Greedy packing of paragraphs; a single paragraph (e.g. a big table) that is too long on its own stays whole. */
    private List<String> split(String content) {
        if (fits(content)) {
            return List.of(content);
        }
        List<String> parts = new ArrayList<>();
        String current = "";
        for (String paragraph : BLANK_LINE.split(content)) {
            String candidate = current.isEmpty() ? paragraph : current + "\n\n" + paragraph;
            if (!current.isEmpty() && !fits(candidate)) {
                parts.add(current);
                current = paragraph;
            } else {
                current = candidate;
            }
        }
        parts.add(current);
        return parts;
    }

    private boolean fits(String text) {
        return estimateTokens(text) <= maxTokens;
    }

    /** ~3 characters per token for Hungarian; good enough for chunk sizing. */
    private static int estimateTokens(String text) {
        return (text.length() + 2) / 3;
    }
}
