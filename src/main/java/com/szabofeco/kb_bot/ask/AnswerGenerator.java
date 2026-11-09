package com.szabofeco.kb_bot.ask;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

@Component
class AnswerGenerator {

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

    AnswerGenerator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    GeneratedAnswer generate(String question, LabelledContext context) {
        GeneratedAnswer answer;
        try {
            answer = chatClient.prompt()
                    .messages(new SystemMessage(SYSTEM_PROMPT), new UserMessage(buildUserMessage(question, context)))
                    .call()
                    .entity(GeneratedAnswer.class);
        } catch (RuntimeException e) {
            throw new AnswerGenerationException("The answer could not be generated.", e);
        }
        if (answer == null) {
            throw new AnswerGenerationException("The model returned no content.");
        }
        return answer;
    }

    static String buildUserMessage(String question, LabelledContext context) {
        return "Excerpts:\n\n" + context.render() + "\n\nQuestion: " + question;
    }
}
