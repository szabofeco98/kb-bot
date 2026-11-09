package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.ChunkMetadataKeys;
import com.szabofeco.kb_bot.common.DocumentRef;
import com.szabofeco.kb_bot.common.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final VectorStore vectorStore;
    private final MarkdownChunker chunker;
    private final FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();

    IngestionService(VectorStore vectorStore, MarkdownChunker chunker) {
        this.vectorStore = vectorStore;
        this.chunker = chunker;
    }

    public IngestionResult ingest(String fileName, String content, Product product) {
        ParsedMarkdown parsed = ParsedMarkdown.parse(content);
        DocumentRef document = DocumentHeader.parse(parsed.frontMatter(), fileName, product);

        List<Chunk> chunks = chunker.chunk(document.title(), parsed.body());
        if (chunks.isEmpty()) {
            throw new InvalidDocumentException("The document contains no processable text.");
        }

        String ingestionId = UUID.randomUUID().toString();
        List<Document> documents = IntStream.range(0, chunks.size())
                .mapToObj(i -> chunks.get(i).toDocument(document, i, ingestionId))
                .toList();
        replaceVersion(document.docId(), documents, ingestionId);

        log.debug("Document ingested: docId={}, version={}, product={}, chunks={}",
                document.docId(), document.version(), document.product(), documents.size());
        return new IngestionResult(document.docId(), document.version(), document.product(), documents.size());
    }

    /** Add first, then delete the old version: no window without the document, and a failed add keeps the old one. */
    private void replaceVersion(String docId, List<Document> documents, String ingestionId) {
        vectorStore.add(documents);
        vectorStore.delete(filterBuilder.and(
                filterBuilder.eq(ChunkMetadataKeys.DOC_ID, docId),
                filterBuilder.ne(ChunkMetadataKeys.INGESTION_ID, ingestionId)).build());
    }
}
