package com.cloudrag.cloudrag.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cloudrag.cloudrag.entity.Conversation;
import com.cloudrag.cloudrag.entity.Document;
import com.cloudrag.cloudrag.repository.ConversationRepository;
import com.cloudrag.cloudrag.repository.DocumentRepository;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin(origins = "http://localhost:5173")
public class PdfController {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final ConversationRepository conversationRepository;

    public PdfController(
            VectorStore vectorStore,
            DocumentRepository documentRepository,
            ConversationRepository conversationRepository
    ) {
        this.vectorStore = vectorStore;
        this.documentRepository = documentRepository;
        this.conversationRepository = conversationRepository;
    }

    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") String conversationId
    ) throws IOException {

        UUID conversationUuid = UUID.fromString(conversationId);
        UUID fileUuid = UUID.randomUUID();

        String fileId = fileUuid.toString();

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "document.pdf";
        }

        Path tempFile =
                Files.createTempFile("cloudrag-", ".pdf");

        try {

            /*
             * Make sure the conversation exists in PostgreSQL.
             */
            conversationRepository
                    .findById(conversationUuid)
                    .orElseGet(() ->
                            conversationRepository.save(
                                    new Conversation(conversationUuid)
                            )
                    );

            file.transferTo(tempFile);

            /*
             * Extract text from PDF using Apache PDFBox directly.
             *
             * This avoids Spring AI's PagePdfDocumentReader
             * and its ForkPDFLayoutTextStripper sorting issue.
             */
            List<org.springframework.ai.document.Document> documents =
                    new ArrayList<>();

            int totalPages;

            try (PDDocument pdfDocument =
                         Loader.loadPDF(tempFile.toFile())) {

                PDFTextStripper stripper =
                        new PDFTextStripper();

                totalPages =
                        pdfDocument.getNumberOfPages();

                for (int page = 1; page <= totalPages; page++) {

                    stripper.setStartPage(page);
                    stripper.setEndPage(page);

                    String text =
                            stripper.getText(pdfDocument);

                    if (text != null && !text.isBlank()) {

                        org.springframework.ai.document.Document document =
                                new org.springframework.ai.document.Document(text);

                        document.getMetadata().put(
                                "page_number",
                                page
                        );

                        document.getMetadata().put(
                                "total_pages",
                                totalPages
                        );

                        documents.add(document);
                    }
                }
            }

            /*
             * Split extracted PDF text into smaller chunks
             * before creating embeddings.
             */
            TokenTextSplitter splitter =
                    TokenTextSplitter.builder().build();

            List<org.springframework.ai.document.Document> chunks =
                    splitter.apply(documents);

            /*
             * Add conversation and file information
             * to every vector stored in Qdrant.
             */
            for (org.springframework.ai.document.Document chunk : chunks) {

                chunk.getMetadata().put(
                        "conversation_id",
                        conversationId
                );

                chunk.getMetadata().put(
                        "file_id",
                        fileId
                );

                chunk.getMetadata().put(
                        "file_name",
                        originalFileName
                );
            }

            /*
             * Generate embeddings and store chunks
             * in Qdrant.
             */
            vectorStore.add(chunks);

            /*
             * Store document metadata in PostgreSQL.
             */
            Document databaseDocument =
                    new Document(
                            fileUuid,
                            conversationUuid,
                            originalFileName,
                            chunks.size(),
                            totalPages
                    );

            documentRepository.save(databaseDocument);

            /*
             * Return upload information to frontend.
             */
            return Map.of(
                    "success", true,
                    "fileId", fileId,
                    "conversationId", conversationId,
                    "fileName", originalFileName,
                    "pages", totalPages,
                    "chunks", chunks.size()
            );

        } finally {

            Files.deleteIfExists(tempFile);
        }
    }
}