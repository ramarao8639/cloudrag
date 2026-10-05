package com.cloudrag.cloudrag.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessageResponse {

    private UUID id;
    private UUID conversationId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private List<Map<String, Object>> sources;

    public MessageResponse() {
    }

    public MessageResponse(
            UUID id,
            UUID conversationId,
            String role,
            String content,
            LocalDateTime createdAt,
            List<Map<String, Object>> sources
    ) {
        this.id = id;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
        this.sources = sources;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Map<String, Object>> getSources() {
        return sources;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setSources(List<Map<String, Object>> sources) {
        this.sources = sources;
    }
}