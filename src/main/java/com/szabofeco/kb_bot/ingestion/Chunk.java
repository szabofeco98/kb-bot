package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.ChunkMetadataKeys;
import com.szabofeco.kb_bot.common.DocumentRef;
import org.springframework.ai.document.Document;

import java.util.Map;

record Chunk(String section, String text) {

    Document toDocument(DocumentRef document, int chunkIndex, String ingestionId) {
        Map<String, Object> metadata = document.toMetadata();
        metadata.put(ChunkMetadataKeys.SECTION, section);
        metadata.put(ChunkMetadataKeys.CHUNK_INDEX, chunkIndex);
        metadata.put(ChunkMetadataKeys.INGESTION_ID, ingestionId);
        return new Document(text, metadata);
    }
}
