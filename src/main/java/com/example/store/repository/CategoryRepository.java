package com.example.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.store.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findBySlugAndIsDeletedFalse(String slug);

    List<Category> findByIsDeletedFalse();

    List<Category> findByIsDeletedFalseOrderByCreatedAtDesc();

    List<Category> findByParentIdAndIsDeletedFalse(UUID parentId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByParentId(UUID parentId);

    boolean existsByParentIdAndIsDeletedFalse(UUID parentId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    @Query("""
        SELECT c
        FROM Category c
        LEFT JOIN FETCH c.parent
        WHERE
            (:q IS NULL OR :q = '' OR
                LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(c.slug) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:isDeleted IS NULL OR c.isDeleted = :isDeleted)
        """)
    Page<Category> search(
            @Param("q") String q,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable
    );

    @Query("""
        SELECT c
        FROM Category c
        WHERE c.isDeleted = false
        """)
    Page<Category> findAllActive(Pageable pageable);
}