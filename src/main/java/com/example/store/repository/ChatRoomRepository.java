package com.example.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.store.entity.ChatRoom;
import com.example.store.enums.ChatRoomStatus;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByCustomerIdAndStatus(UUID customerId, ChatRoomStatus status);

    Optional<ChatRoom> findFirstByCustomerIdOrderByUpdatedAtDesc(UUID customerId);

    List<ChatRoom> findByStatusOrderByUpdatedAtDesc(ChatRoomStatus status);

    List<ChatRoom> findAllByOrderByUpdatedAtDesc();

    @Query("SELECT r FROM ChatRoom r WHERE r.status = :status AND (r.employee.id = :employeeId OR r.employee IS NULL) ORDER BY r.updatedAt DESC")
    List<ChatRoom> findOpenRoomsForEmployee(@Param("status") ChatRoomStatus status, @Param("employeeId") UUID employeeId);

    long countByStatus(ChatRoomStatus status);
}
