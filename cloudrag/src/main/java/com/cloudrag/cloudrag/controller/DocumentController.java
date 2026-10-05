package com.cloudrag.cloudrag.controller;

import com.cloudrag.cloudrag.entity.Document;
import com.cloudrag.cloudrag.repository.DocumentRepository;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;

    public DocumentController(
            DocumentRepository documentRepository,
            VectorStore vectorStore
    ) {
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
    }

    /*
     * Get documents for a conversation.
     */
    @GetMapping("/conversations/{conversationId}/documents")
    public ResponseEntity<List<Map<String, Object>>> getDocuments(
            @PathVariable UUID conversationId
    ) {

        List<Map<String, Object>> documents =
                documentRepository
                        .findByConversationId(conversationId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(documents);
    }

    /*
     * Delete a document from both:
     *
     * 1. Qdrant
     * 2. PostgreSQL
     */
    @DeleteMapping("/documents")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @RequestParam UUID conversationId,
            @RequestParam UUID fileId
    ) {

        /*
         * Delete all vectors belonging to this file
         * and conversation from Qdrant.
         */
        Filter.Expression filter =
                new Filter.Expression(
                        Filter.ExpressionType.AND,
                        new Filter.Expression(
                                Filter.ExpressionType.EQ,
                                new Filter.Key("conversation_id"),
                                new Filter.Value(conversationId.toString())
                        ),
                        new Filter.Expression(
                                Filter.ExpressionType.EQ,
                                new Filter.Key("file_id"),
                                new Filter.Value(fileId.toString())
                        )
                );

        vectorStore.delete(
                filter
        );

        /*
         * Delete document metadata from PostgreSQL.
         */
        if (documentRepository.existsById(fileId)) {

            documentRepository.deleteById(fileId);
        }

        return ResponseEntity.ok(
                Map.of(
                        "success",
                        true,

                        "conversationId",
                        conversationId,

                        "fileId",
                        fileId,

                        "message",
                        "Document deleted successfully"
                )
        );
    }

    /*
     * Convert database document to frontend response.
     */
    private Map<String, Object> toResponse(Document document) {

        return Map.of(
                "fileId",
                document.getId(),

                "fileName",
                document.getFileName(),

                "chunks",
                document.getTotalChunks(),

                "pages",
                document.getTotalPages(),

                "uploadedAt",
                document.getUploadedAt()
        );
    }
}