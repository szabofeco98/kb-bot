package com.szabofeco.kb_bot.search;

import com.szabofeco.kb_bot.common.Product;
import com.szabofeco.kb_bot.common.QuestionLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Retrieval only, no LLM: for threshold calibration and measuring retrieval separately from generation. */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(@RequestParam @NotBlank @Size(max = QuestionLimits.MAX_LENGTH) String question,
                                 @RequestParam(required = false) Product product) {
        return searchService.search(question.strip(), product);
    }
}
