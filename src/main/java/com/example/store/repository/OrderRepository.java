
package com.example.store.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.entity.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	@EntityGraph(attributePaths = { "user", "status", "items", "items.variant", "items.variant.product" })
	List<Order> findByStatus_NameIgnoreCaseOrderByCreatedAtDesc(String statusName);

	@EntityGraph(attributePaths = { "user", "status", "items", "items.variant", "items.variant.product" })
	java.util.Optional<Order> findWithItemsById(UUID id);
}
