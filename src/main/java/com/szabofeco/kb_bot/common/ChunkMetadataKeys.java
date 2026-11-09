package com.szabofeco.kb_bot.common;

/** Persisted in the vector store: renaming a key orphans every stored chunk, so re-ingest after a change. */
public final class ChunkMetadataKeys {

    public static final String PRODUCT = "product";
    public static final String DOC_ID = "docId";
    public static final String VERSION = "version";
    public static final String TITLE = "title";
    public static final String SECTION = "section";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String SOURCE_FILE = "sourceFile";
    public static final String INGESTION_ID = "ingestionId";
    public static final String EFFECTIVE_FROM = "effectiveFrom";

    private ChunkMetadataKeys() {
    }
}
