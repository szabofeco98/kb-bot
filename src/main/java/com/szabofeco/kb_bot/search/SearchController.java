package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.common.QuestionLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Retrieval only: returns the relevant source documents for a question, without calling the LLM. GET, because it is
 * a safe, idempotent read. Used for threshold calibration and to measure retrieval separately from generation;
 * the generated answer is {@code POST /api/ask} (package {@code ask}, which builds on {@link SearchService}).
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** Constraint violations → {@code HandlerMethodValidationException} → 400 (see {@code ApiExceptionHandler}). */
    @GetMapping
    public SearchResponse search(@RequestParam @NotBlank @Size(max = QuestionLimits.MAX_LENGTH) String question,
                                 @RequestParam(required = false) Product product) {
        return searchService.search(question, product);
    }
}
