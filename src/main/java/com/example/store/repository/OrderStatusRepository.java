
package com.example.store.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.entity.OrderStatus;

public interface OrderStatusRepository extends JpaRepository<OrderStatus, Integer> {

	Optional<OrderStatus> findByNameIgnoreCase(String name);
}
