package com.example.store.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import com.example.store.entity.StyleValue;
import org.springframework.stereotype.Repository;

@Repository
public interface StyleValueRepository
        extends JpaRepository<StyleValue, UUID> {

    Optional<StyleValue> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    boolean existsByStyleId(UUID styleId);

    @Query("""
        SELECT sv
        FROM StyleValue sv
        WHERE
            (:q IS NULL OR :q = '' OR
                LOWER(sv.name) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:styleId IS NULL OR sv.style.id = :styleId)
            AND (:isDeleted IS NULL OR sv.isDeleted = :isDeleted)
        """)
    Page<StyleValue> search(
            @Param("q") String q,
            @Param("styleId") UUID styleId,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable
    );

    @Query("""
        SELECT sv
        FROM StyleValue sv
        WHERE sv.style.id = :styleId
            AND sv.isDeleted = false
        """)
    Page<StyleValue> findActiveByStyleId(
            @Param("styleId") UUID styleId,
            Pageable pageable
    );

    @Query("""
        SELECT sv
        FROM StyleValue sv
        WHERE sv.isDeleted = false
        """)
    Page<StyleValue> findAllActive(Pageable pageable);
}