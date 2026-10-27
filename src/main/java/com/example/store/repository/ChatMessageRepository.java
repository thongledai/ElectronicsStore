package com.example.store.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.store.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(UUID roomId);

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

    long countByChatRoomIdAndIsReadFalseAndSenderIdNot(UUID roomId, UUID userId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true WHERE m.chatRoom.id = :roomId AND m.sender.id != :userId AND m.isRead = false")
    int markMessagesAsRead(@Param("roomId") UUID roomId, @Param("userId") UUID userId);
}
