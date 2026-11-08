package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Document ingestion: frontmatter validation → chunking → embedding + save → deleting the previous version.
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    /**
     * General sanity check for id/version-like fields. Not an injection guard: the delete filter is built via
     * {@link FilterExpressionBuilder} (a typed expression tree, never a concatenated string), so arbitrary
     * characters in docId/version cannot affect the filter's structure.
     */
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final VectorStore vectorStore;
    private final MarkdownChunker chunker;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    public IngestionService(VectorStore vectorStore, MarkdownChunker chunker) {
        this.vectorStore = vectorStore;
        this.chunker = chunker;
    }

    public IngestionResult ingest(String fileName, String content, Product product) {
        ParsedMarkdown parsed = ParsedMarkdown.parse(content);
        Map<String, String> frontMatter = parsed.frontMatter();

        String docId = requireSafe(frontMatter, "id");
        String version = requireSafe(frontMatter, "version");
        String title = frontMatter.getOrDefault("title", fileName);

        // The product is given by the uploader; if it's also present in the file, it must match.
        String declaredProduct = frontMatter.get("product");
        if (declaredProduct != null && !declaredProduct.equals(product.name())) {
            throw new InvalidDocumentException("The given product (" + product + ") does not match the one "
                    + "in the document (" + declaredProduct + ").");
        }

        List<Chunk> chunks = chunker.chunk(title, parsed.body());
        if (chunks.isEmpty()) {
            throw new InvalidDocumentException("The document contains no processable text.");
        }

        String ingestionId = UUID.randomUUID().toString();
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("product", product.name());
            metadata.put("docId", docId);
            metadata.put("version", version);
            metadata.put("title", title);
            metadata.put("section", chunk.section());
            metadata.put("chunkIndex", i);
            metadata.put("sourceFile", fileName);
            metadata.put("ingestionId", ingestionId);
            if (frontMatter.containsKey("effective_from")) {
                metadata.put("effectiveFrom", frontMatter.get("effective_from"));
            }
            documents.add(new Document(chunk.text(), metadata));
        }

        // The new version is added first, then the old one is deleted. This way there's no moment
        // where the document is not searchable at all, and if the save fails, the old version is kept.
        vectorStore.add(documents);
        vectorStore.delete(filterBuilder.and(
                filterBuilder.eq("docId", docId),
                filterBuilder.ne("ingestionId", ingestionId)).build());

        log.info("Document ingested: docId={}, version={}, product={}, chunks={}",
                docId, version, product, documents.size());
        return new IngestionResult(docId, version, product, documents.size());
    }

    private static String requireSafe(Map<String, String> frontMatter, String key) {
        String value = frontMatter.get(key);
        if (value == null || value.isBlank()) {
            throw new InvalidDocumentException("Missing required frontmatter field: " + key);
        }
        if (!SAFE_VALUE.matcher(value).matches()) {
            throw new InvalidDocumentException("Invalid characters in field '" + key + "': " + value);
        }
        return value;
    }
}
