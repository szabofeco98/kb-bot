package com.szabofeco.kb_bot.ask;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Generated answer + the documents it is based on. POST (unlike {@code GET /api/search}): the question stays out
 * of URLs and access logs, intermediaries won't retry a paid LLM call, and the body can grow (e.g. history).
 */
@RestController
@RequestMapping("/api/ask")
public class AskController {

    private final AskService askService;

    public AskController(AskService askService) {
        this.askService = askService;
    }

    @PostMapping
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return askService.ask(request.question(), request.product());
    }
}
