package com.example.store.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.store.entity.User;
public interface UserRepository extends JpaRepository<User, UUID> {
	User findByEmail(String email);
	User findByUsername(String username);
}
