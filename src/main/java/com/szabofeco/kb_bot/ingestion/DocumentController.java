package com.szabofeco.kb_bot.ingestion;

import com.szabofeco.kb_bot.common.Product;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final IngestionService ingestionService;

    public DocumentController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public IngestionResult upload(@RequestParam("file") @MarkdownFile MultipartFile file,
                                  @RequestParam("product") Product product) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return ingestionService.ingest(file.getOriginalFilename(), content, product);
    }
}
