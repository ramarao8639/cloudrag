package com.cloudrag.cloudrag.controller;

import com.cloudrag.cloudrag.entity.Conversation;
import com.cloudrag.cloudrag.entity.Message;
import com.cloudrag.cloudrag.repository.ConversationRepository;
import com.cloudrag.cloudrag.repository.MessageRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "http://localhost:5173")
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationController(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @DeleteMapping("/{conversationId}")
    @Transactional
    public ResponseEntity<Void> deleteConversation(
            @PathVariable UUID conversationId
    ) {

        // Check conversation exists
        if (!conversationRepository.existsById(conversationId)) {
            return ResponseEntity.notFound().build();
        }

        // Delete messages belonging to the conversation
        messageRepository.deleteByConversationId(conversationId);

        // Delete conversation
        conversationRepository.deleteById(conversationId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getConversations() {

        List<Map<String, Object>> conversations =
                conversationRepository
                        .findAllByOrderByUpdatedAtDesc()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(conversations);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createConversation() {

        UUID conversationId = UUID.randomUUID();

        Conversation conversation =
                new Conversation(conversationId);

        Conversation saved =
                conversationRepository.save(conversation);

        return ResponseEntity.ok(
                toResponse(saved)
        );
    }

    private Map<String, Object> toResponse(
            Conversation conversation
    ) {

        Message firstMessage =
                messageRepository
                        .findFirstByConversationIdOrderByCreatedAtAsc(
                                conversation.getId()
                        );

        String title = "New conversation";

        if (firstMessage != null
                && firstMessage.getRole() == Message.Role.USER
                && firstMessage.getContent() != null
                && !firstMessage.getContent().isBlank()) {

            title = firstMessage.getContent().trim();

            if (title.length() > 40) {
                title = title.substring(0, 40) + "...";
            }
        }

        return Map.of(
                "id", conversation.getId(),
                "title", title,
                "createdAt", conversation.getCreatedAt(),
                "updatedAt", conversation.getUpdatedAt()
        );
    }
}