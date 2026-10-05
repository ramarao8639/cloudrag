package com.cloudrag.cloudrag.controller;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "http://localhost:5173")

public class SearchController {

    private final VectorStore vectorStore;

    public SearchController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @GetMapping
    public List<String> search(@RequestParam String query) {

        List<Document> results = vectorStore.similaritySearch(query);

        return results.stream()
                .map(Document::getText)
                .toList();
    }
}