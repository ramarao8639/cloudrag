package com.cloudrag.cloudrag.repository;

import com.cloudrag.cloudrag.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository
        extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(
            UUID conversationId
    );

    Message findFirstByConversationIdOrderByCreatedAtAsc(
            UUID conversationId
    );
    
    void deleteByConversationId(UUID conversationId);

}