package com.cloudrag.cloudrag.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudrag.cloudrag.entity.Conversation;
import com.cloudrag.cloudrag.entity.Message;
import com.cloudrag.cloudrag.repository.ConversationRepository;
import com.cloudrag.cloudrag.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "http://localhost:5173")
public class RagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public RagController(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ObjectMapper objectMapper
    ) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public Map<String, Object> ask(
            @RequestParam String question,
            @RequestParam String conversationId,
            @RequestParam(required = false) String fileId
    ) {

        UUID conversationUuid =
                UUID.fromString(conversationId);

        /*
         * Make sure the conversation exists.
         */
        Conversation conversation =
                conversationRepository
                        .findById(conversationUuid)
                        .orElseGet(() ->
                                conversationRepository.save(
                                        new Conversation(conversationUuid)
                                )
                        );

        /*
         * Save the user's question.
         */
        Message userMessage =
                new Message(
                        UUID.randomUUID(),
                        conversationUuid,
                        Message.Role.USER,
                        question
                );

        messageRepository.save(userMessage);

        /*
         * Update conversation activity time.
         */
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        /*
         * Build Qdrant search.
         */
        SearchRequest.Builder searchBuilder =
                SearchRequest.builder()
                        .query(question)
                        .topK(5);

        if (fileId != null && !fileId.isBlank()) {

            searchBuilder.filterExpression(
                    "conversation_id == '" +
                    conversationId +
                    "' AND file_id == '" +
                    fileId +
                    "'"
            );

        } else {

            searchBuilder.filterExpression(
                    "conversation_id == '" +
                    conversationId +
                    "'"
            );
        }

        SearchRequest searchRequest =
                searchBuilder.build();

        /*
         * Search Qdrant.
         */
        List<Document> documents =
                vectorStore.similaritySearch(searchRequest);
        documents.forEach(document ->
        System.out.println(
                "RAG DOCUMENT: " + document.getText()
        )
);
        /*
         * No relevant documents.
         */
        if (documents == null || documents.isEmpty()) {

            String noInformationAnswer =
                    "I could not find enough information in the documents available in this conversation.";

            Message assistantMessage =
                    new Message(
                            UUID.randomUUID(),
                            conversationUuid,
                            Message.Role.ASSISTANT,
                            noInformationAnswer
                    );

            assistantMessage.setSources("[]");

            messageRepository.save(assistantMessage);

            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);

            return Map.of(
                    "question",
                    question,

                    "conversationId",
                    conversationId,

                    "answer",
                    noInformationAnswer,

                    "sources",
                    List.of()
            );
        }

        /*
         * Build context from retrieved documents.
         */
        String context =
                documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n\n"));

        /*
         * Enterprise RAG prompt.
         */
        String prompt = """
                You are an enterprise knowledge assistant.

                Answer the user's question using ONLY the
                provided context.

                If the context does not contain enough information,
                say:

                "I could not find enough information in the
                documents available in this conversation."

                Do not use information from outside the provided context.

                Context:
                %s

                Question:
                %s
                """.formatted(
                        context,
                        question
                );

        
        
        System.out.println("========== RAG PROMPT ==========");
        System.out.println(prompt);
        System.out.println("========== END RAG PROMPT ==========");
        
        /*
         * Generate answer using Ollama.
         */
        String answer =
                chatClient
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();

        /*
         * Build source information.
         */
        List<Map<String, Object>> sources =
                documents.stream()
                        .map(document -> {

                            Map<String, Object> metadata =
                                    document.getMetadata();

                            Map<String, Object> source =
                                    new HashMap<>();

                            source.put(
                                    "id",
                                    document.getId()
                            );

                            source.put(
                                    "fileName",
                                    metadata.getOrDefault(
                                            "file_name",
                                            "Unknown"
                                    )
                            );

                            source.put(
                                    "pageNumber",
                                    metadata.getOrDefault(
                                            "page_number",
                                            "Unknown"
                                    )
                            );

                            source.put(
                                    "chunkIndex",
                                    metadata.getOrDefault(
                                            "chunk_index",
                                            "Unknown"
                                    )
                            );

                            Object distance =
                                    metadata.get("distance");

                            if (distance != null) {

                                source.put(
                                        "distance",
                                        distance
                                );
                            }

                            return source;
                        })
                        .toList();

        /*
         * Convert sources to JSON for PostgreSQL.
         */
        String sourcesJson;

        try {

            sourcesJson =
                    objectMapper.writeValueAsString(
                            sources
                    );

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to serialize RAG sources",
                    exception
            );
        }

        /*
         * Save assistant answer + sources.
         */
        Message assistantMessage =
                new Message(
                        UUID.randomUUID(),
                        conversationUuid,
                        Message.Role.ASSISTANT,
                        answer
                );

        assistantMessage.setSources(sourcesJson);

        messageRepository.save(assistantMessage);

        /*
         * Update conversation activity time.
         */
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        /*
         * Return response to frontend.
         */
        return Map.of(
                "question",
                question,

                "conversationId",
                conversationId,

                "answer",
                answer,

                "sources",
                sources
        );
    }
}