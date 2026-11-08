package com.szabofeco.kb_bot.ask;

import com.szabofeco.kb_bot.common.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Retrieval only for now: returns the relevant source chunks for a question. GET, because it is a safe, idempotent
 * read. LLM answer generation will come as {@code POST /api/ask} on top of {@link AskService}.
 */
@RestController
@RequestMapping("/api/search")
public class AskController {

    /** Keeps the embedding call bounded; a real question is far shorter. */
    static final int MAX_QUESTION_LENGTH = 1000;

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    /** Constraint violations → {@code HandlerMethodValidationException} → 400 (see {@code ApiExceptionHandler}). */
    @GetMapping
    public AskResponse search(@RequestParam @NotBlank @Size(max = MAX_QUESTION_LENGTH) String question,
                              @RequestParam(required = false) Product product) {
        return askService.ask(question, product);
    }
}
