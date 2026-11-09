package com.szabofeco.kb_bot.common;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * One ingested document version. Equality includes the version on purpose: two stored versions of a docId stay two
 * separate sources instead of being merged.
 */
public record DocumentRef(String docId, String version, String title, @Nullable String effectiveFrom,
                          Product product, String sourceFile) {

    /** No null values: Chroma rejects them. */
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(ChunkMetadataKeys.PRODUCT, product.name());
        metadata.put(ChunkMetadataKeys.DOC_ID, docId);
        metadata.put(ChunkMetadataKeys.VERSION, version);
        metadata.put(ChunkMetadataKeys.TITLE, title);
        metadata.put(ChunkMetadataKeys.SOURCE_FILE, sourceFile);
        if (effectiveFrom != null) {
            metadata.put(ChunkMetadataKeys.EFFECTIVE_FROM, effectiveFrom);
        }
        return metadata;
    }

    public static DocumentRef fromMetadata(String chunkId, Map<String, Object> metadata) {
        return new DocumentRef(
                requiredMetadata(chunkId, metadata, ChunkMetadataKeys.DOC_ID),
                requiredMetadata(chunkId, metadata, ChunkMetadataKeys.VERSION),
                requiredMetadata(chunkId, metadata, ChunkMetadataKeys.TITLE),
                optionalMetadata(metadata, ChunkMetadataKeys.EFFECTIVE_FROM),
                product(chunkId, requiredMetadata(chunkId, metadata, ChunkMetadataKeys.PRODUCT)),
                requiredMetadata(chunkId, metadata, ChunkMetadataKeys.SOURCE_FILE));
    }

    public static String requiredMetadata(String chunkId, Map<String, Object> metadata, String key) {
        String value = optionalMetadata(metadata, key);
        if (value == null) {
            throw new IllegalStateException("Chunk " + chunkId + " has no '" + key + "' metadata.");
        }
        return value;
    }

    private static @Nullable String optionalMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }

    private static Product product(String chunkId, String value) {
        try {
            return Product.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Chunk " + chunkId + " has an unknown product: " + value, e);
        }
    }
}
