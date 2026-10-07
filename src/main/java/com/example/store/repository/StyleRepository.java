package com.example.store.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import com.example.store.entity.Style;
import org.springframework.stereotype.Repository;

@Repository
public interface StyleRepository extends JpaRepository<Style, UUID> {

    Optional<Style> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    boolean existsByCategoriesId(UUID categoryId);

    @Query("""
        SELECT s
        FROM Style s
        WHERE
            (:q IS NULL OR :q = '' OR
                LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:isDeleted IS NULL OR s.isDeleted = :isDeleted)
        """)
    Page<Style> search(
            @Param("q") String q,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT s
        FROM Style s
        JOIN s.categories c
        WHERE c.id = :categoryId
            AND s.isDeleted = false
        """)
    Page<Style> findActiveByCategoryId(
            @Param("categoryId") UUID categoryId,
            Pageable pageable
    );

    @Query("""
        SELECT s
        FROM Style s
        WHERE s.isDeleted = false
        """)
    Page<Style> findAllActive(Pageable pageable);
}