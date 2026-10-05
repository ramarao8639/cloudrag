package com.cloudrag.cloudrag.controller;

import com.cloudrag.cloudrag.dto.MessageResponse;
import com.cloudrag.cloudrag.entity.Message;
import com.cloudrag.cloudrag.repository.MessageRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {

    private final MessageRepository messageRepository;

    public MessageController(
            MessageRepository messageRepository
    ) {
        this.messageRepository = messageRepository;
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable UUID conversationId
    ) {

        List<MessageResponse> messages =
                messageRepository
                        .findByConversationIdOrderByCreatedAtAsc(
                                conversationId
                        )
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(messages);
    }

    private MessageResponse toResponse(Message message) {

        /*
         * Sources are currently stored as JSON text.
         *
         * For now, return an empty source list when reading
         * historical messages. The RAG response still returns
         * live sources to the frontend.
         */
        List<Map<String, Object>> sources =
                Collections.emptyList();

        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole().name(),
                message.getContent(),
                message.getCreatedAt(),
                sources
        );
    }
}