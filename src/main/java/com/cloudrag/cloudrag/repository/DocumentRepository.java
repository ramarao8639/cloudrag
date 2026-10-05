package com.cloudrag.cloudrag.repository;

import com.cloudrag.cloudrag.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository
        extends JpaRepository<Document, UUID> {

    List<Document> findByConversationId(UUID conversationId);
}