package com.szabofeco.kb_bot.ingestion;

import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.ListBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Structure-based (hierarchical) chunking for Markdown documents, built on the commonmark AST
 * (core + GFM tables extension) instead of hand-rolled line parsing.
 *
 * <ol>
 *   <li>First splits by "##" sections – each chunk should be about one topic.</li>
 *   <li>If a section is too long, splits it into "###" subsections.</li>
 *   <li>If that's still too long, splits into blocks (paragraph, list, table) and packs them together.</li>
 *   <li>Tables and lists are never split in two; if a table alone is too large,
 *       it is split row by row, but the header is repeated before every part.</li>
 * </ol>
 *
 * A breadcrumb is prepended to every chunk (e.g. "[Lakáshitel – Kondíciós lista > Díjak]") so that the chunk
 * is understandable on its own, and the embedding "knows" which product and section it's about.
 *
 * <p>Block text is never re-rendered from the AST: for every node we slice the original source lines spanned
 * by it (via {@link IncludeSourceSpans}), so the exact original Markdown (table pipes, list markers, ...)
 * survives unchanged.
 */
@Component
public class MarkdownChunker {

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");

    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build();
    private static final TextContentRenderer TEXT_RENDERER = TextContentRenderer.builder().build();

    private final int maxTokens;

    public MarkdownChunker(@Value("${rag.chunk.max-tokens:500}") int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public List<Chunk> chunk(String title, String markdownBody) {
        List<String> sourceLines = markdownBody.lines().toList();
        Node document = PARSER.parse(markdownBody);

        List<Chunk> chunks = new ArrayList<>();
        for (Section h2 : splitByHeadingLevel(bodyNodes(document), 2)) {
            String content = sectionText(h2.nodes(), sourceLines);
            if (content.isBlank()) {
                continue;
            }
            if (fits(content)) {
                chunks.add(toChunk(title, h2.heading(), null, content));
                continue;
            }
            for (Section h3 : splitByHeadingLevel(h2.nodes(), 3)) {
                for (String part : packBlocks(h3.nodes(), sourceLines)) {
                    chunks.add(toChunk(title, h2.heading(), h3.heading(), part));
                }
            }
        }
        return chunks;
    }

    // ---------------------------------------------------------------- sections

    private record Section(String heading, List<Node> nodes) {
    }

    /** The document's top-level blocks, with the H1 title dropped (it comes from the frontmatter instead). */
    private List<Node> bodyNodes(Node document) {
        List<Node> nodes = new ArrayList<>();
        for (Node child : children(document)) {
            if (child instanceof Heading h && h.getLevel() == 1) {
                continue;
            }
            nodes.add(child);
        }
        return nodes;
    }

    /** Splits at headings of the given level. The part before the first heading becomes a headingless section. */
    private List<Section> splitByHeadingLevel(List<Node> nodes, int level) {
        List<Section> sections = new ArrayList<>();
        String heading = null;
        List<Node> current = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof Heading h && h.getLevel() == level) {
                sections.add(new Section(heading, current));
                heading = TEXT_RENDERER.render(h).strip();
                current = new ArrayList<>();
            } else {
                current.add(node);
            }
        }
        sections.add(new Section(heading, current));
        return sections;
    }

    /** The raw source text spanned by a run of sibling nodes, exactly as written (including blank lines between). */
    private String sectionText(List<Node> nodes, List<String> sourceLines) {
        return rawText(nodes, sourceLines).strip();
    }

    /** The raw source text spanned by a single node. */
    private String rawText(Node node, List<String> sourceLines) {
        return rawText(List.of(node), sourceLines);
    }

    /** The raw source text from the first line of the first node's span to the last line of the last node's span. */
    private String rawText(List<Node> nodes, List<String> sourceLines) {
        List<Node> spanned = nodes.stream().filter(n -> !n.getSourceSpans().isEmpty()).toList();
        if (spanned.isEmpty()) {
            return "";
        }
        int first = spanned.stream().mapToInt(n -> n.getSourceSpans().getFirst().getLineIndex()).min().orElseThrow();
        int last = spanned.stream().mapToInt(n -> n.getSourceSpans().getLast().getLineIndex()).max().orElseThrow();
        return String.join("\n", sourceLines.subList(first, last + 1));
    }

    private static List<Node> children(Node parent) {
        List<Node> result = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNext()) {
            result.add(child);
        }
        return result;
    }

    // ---------------------------------------------------------------- blocks

    private enum BlockType { PARAGRAPH, LIST, TABLE, CODE }

    private BlockType typeOf(Node node) {
        if (node instanceof TableBlock) {
            return BlockType.TABLE;
        }
        if (node instanceof ListBlock) {
            return BlockType.LIST;
        }
        if (node instanceof FencedCodeBlock || node instanceof IndentedCodeBlock) {
            return BlockType.CODE;
        }
        return BlockType.PARAGRAPH;
    }

    /** Packs the blocks into chunks in order, staying within the size limit. */
    private List<String> packBlocks(List<Node> nodes, List<String> sourceLines) {
        List<String> parts = new ArrayList<>();
        for (Node node : nodes) {
            String text = rawText(node, sourceLines);
            if (text.isBlank()) {
                continue;
            }
            if (fits(text)) {
                parts.add(text);
            } else {
                parts.addAll(splitOversized(node, text, sourceLines));
            }
        }
        return pack(parts, "", "\n\n");
    }

    private List<String> splitOversized(Node node, String text, List<String> sourceLines) {
        return switch (typeOf(node)) {
            case TABLE -> splitTable((TableBlock) node, sourceLines);
            case LIST -> pack(listItemTexts(node, sourceLines), "", "\n");
            case CODE -> pack(List.of(text.split("\n")), "", "\n");
            case PARAGRAPH -> pack(List.of(SENTENCE_END.split(text)), "", " ");
        };
    }

    /** Splits the table row by row, prepending the header (header row + delimiter row) to every part. */
    private List<String> splitTable(TableBlock table, List<String> sourceLines) {
        TableHead head = children(table).stream()
                .filter(TableHead.class::isInstance).map(TableHead.class::cast).findFirst().orElse(null);
        TableBody body = children(table).stream()
                .filter(TableBody.class::isInstance).map(TableBody.class::cast).findFirst().orElse(null);
        if (head == null || head.getFirstChild() == null || body == null) {
            return pack(List.of(rawText(table, sourceLines).split("\n")), "", "\n");
        }

        int headerLine = head.getFirstChild().getSourceSpans().getFirst().getLineIndex();
        String header = sourceLines.get(headerLine) + "\n" + sourceLines.get(headerLine + 1) + "\n";
        List<String> rows = children(body).stream().map(row -> rawText(row, sourceLines)).toList();
        return pack(rows, header, "\n");
    }

    private List<String> listItemTexts(Node list, List<String> sourceLines) {
        return children(list).stream().map(item -> rawText(item, sourceLines)).toList();
    }

    /**
     * Greedy packing: concatenates units as long as it stays within the limit.
     * If a single unit alone is too large, it is left as-is (not split further).
     */
    private List<String> pack(List<String> units, String prefix, String joiner) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            String candidate = current.isEmpty() ? unit : current + joiner + unit;
            if (!current.isEmpty() && !fits(prefix + candidate)) {
                result.add(prefix + current);
                current = new StringBuilder(unit);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            result.add(prefix + current);
        }
        return result;
    }

    // ---------------------------------------------------------------- helpers

    private Chunk toChunk(String title, String h2, String h3, String content) {
        StringBuilder section = new StringBuilder();
        if (h2 != null) {
            section.append(h2);
        }
        if (h3 != null) {
            section.append(section.isEmpty() ? "" : " > ").append(h3);
        }
        String breadcrumb = section.isEmpty() ? "[" + title + "]" : "[" + title + " > " + section + "]";
        return new Chunk(section.toString(), breadcrumb + "\n\n" + content.strip());
    }

    private boolean fits(String text) {
        return estimateTokens(text) <= maxTokens;
    }

    /**
     * Rough estimate: for Hungarian text, about 3 characters / token. Accurate enough for chunk sizing,
     * no need for the model's own tokenizer.
     */
    static int estimateTokens(String text) {
        return (text.length() + 2) / 3;
    }
}
