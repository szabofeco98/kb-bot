package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.szabofeco.kb_bot.search.ChunkMetadata.meta;

/**
 * Generates a grounded answer from the retrieved chunks. The model only sees the excerpts – it knows nothing
 * about the bank – and must say so when they don't contain the answer.
 */
@Component
public class AnswerGenerator {

    static final String SYSTEM_PROMPT = """
            You are an assistant for bank experts. Answer the question using ONLY the excerpts provided by the user.
            Rules:
            - Do not use any outside knowledge. If the excerpts do not contain the answer, set "answerable" to false
              and leave "answer" empty. A partial or guessed answer is worse than no answer.
            - Copy numbers, amounts, percentages and dates exactly as they appear in the excerpts.
            - If the excerpts contradict each other, say so in the answer instead of choosing one.
            - Never answer with only "yes" or "no". Start with the direct answer, then explain why: the reasons,
              conditions, exceptions and limits the excerpts give (e.g. "No, because ...", "Yes, if ...").
            - If the excerpts give no reason, state the answer in a full sentence and do not invent one.
            - Always name the product the answer refers to (each excerpt states its product and document).
              If the relevant excerpts belong to more than one product, answer separately for each product,
              one short paragraph per product, starting with the product name. Never merge different products'
              conditions into one statement.
            - Answer in the language of the question, in a few clear sentences.
            - In "usedSources" list the labels (e.g. "S1") of every excerpt the answer is based on, and only those.
            """;

    private final ChatClient chatClient;

    public AnswerGenerator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /** @param chunks retrieved chunks; chunk {@code i} gets the label {@code S(i+1)} */
    public GeneratedAnswer generate(String question, List<Document> chunks) {
        try {
            // Messages instead of .system(String)/.user(String): those are rendered as templates,
            // and curly braces in a chunk would break the rendering.
            GeneratedAnswer answer = chatClient.prompt()
                    .messages(new SystemMessage(SYSTEM_PROMPT), new UserMessage(buildUserMessage(question, chunks)))
                    .call()
                    .entity(GeneratedAnswer.class);
            if (answer == null) {
                throw new IllegalStateException("The model returned no content.");
            }
            return answer;
        } catch (RuntimeException e) {
            throw new AnswerGenerationException("The answer could not be generated.", e);
        }
    }

    /** Display name from the chunk's product metadata; falls back to the raw value for unknown ones. */
    static String productName(String product) {
        if (product == null) {
            return "unknown";
        }
        try {
            return Product.valueOf(product).displayName();
        } catch (IllegalArgumentException e) {
            return product;
        }
    }

    static String label(int index) {
        return "S" + (index + 1);
    }

    /**
     * Short labels instead of chunk UUIDs: small models copy long IDs unreliably. Product and document title are
     * stated explicitly so the model can name them (and keep products apart when no product filter was given);
     * the version / effective date helps tell document versions apart.
     */
    static String buildUserMessage(String question, List<Document> chunks) {
        StringBuilder sb = new StringBuilder("Excerpts:\n\n");
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            sb.append('[').append(label(i)).append("] (product: ").append(productName(meta(chunk, "product")))
                    .append(", document: ").append(meta(chunk, "title"))
                    .append(", version: ").append(meta(chunk, "version"));
            String effectiveFrom = meta(chunk, "effectiveFrom");
            if (effectiveFrom != null) {
                sb.append(", effective from: ").append(effectiveFrom);
            }
            sb.append(")\n").append(chunk.getText().strip()).append("\n\n");
        }
        sb.append("Question: ").append(question.strip());
        return sb.toString();
    }
}
