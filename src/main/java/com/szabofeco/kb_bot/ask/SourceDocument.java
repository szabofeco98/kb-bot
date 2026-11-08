package com.szabofeco.kb_bot.ask;

import java.util.List;

/**
 * A document the answer is based on, with its relevant chunks (best first). This is what the expert sees as the
 * source: which file, which version, effective from when.
 *
 * @param bestScore score of the best-matching chunk; documents are ordered by it
 */
public record SourceDocument(String docId, String title, String version, String effectiveFrom, String product,
                             String sourceFile, double bestScore, List<SourceChunk> chunks) {
}
