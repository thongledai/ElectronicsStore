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

import com.example.store.entity.Style;

@Repository
public interface StyleRepository extends JpaRepository<Style, UUID> {

    Optional<Style> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    boolean existsByCategoriesId(UUID categoryId);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Style s
            JOIN s.categories c
            WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name))
              AND c.id IN :categoryIds
              AND s.isDeleted = false
              AND s.id <> :excludeId
            """)
    boolean existsByNameAndCategoryIds(
            @Param("name") String name,
            @Param("categoryIds") java.util.Collection<UUID> categoryIds,
            @Param("excludeId") UUID excludeId);

    @Query("""
            SELECT COUNT(s) > 0
            FROM Style s
            WHERE LOWER(TRIM(s.name)) = LOWER(TRIM(:name))
              AND s.categories IS EMPTY
              AND s.isDeleted = false
              AND s.id <> :excludeId
            """)
    boolean existsByNameAndCategoriesIsEmpty(
            @Param("name") String name,
            @Param("excludeId") UUID excludeId);

    List<Style> findByIsDeletedFalse();

    @Query("""
            SELECT DISTINCT s
            FROM Style s
            LEFT JOIN FETCH s.categories
            WHERE
                (:q IS NULL OR :q = '' OR
                    LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
                )
                AND (:isDeleted IS NULL OR s.isDeleted = :isDeleted)
            """)
    Page<Style> search(
            @Param("q") String q,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT s
            FROM Style s
            JOIN s.categories c
            WHERE c.id = :categoryId
                AND s.isDeleted = false
            """)
    List<Style> findAllActiveByCategoryId(@Param("categoryId") UUID categoryId);

    @Query("""
            SELECT DISTINCT s
            FROM Style s
            JOIN s.categories c
            WHERE c.id = :categoryId
                AND s.isDeleted = false
            """)
    Page<Style> findActiveByCategoryId(
            @Param("categoryId") UUID categoryId,
            Pageable pageable);

    @Query("""
            SELECT s
            FROM Style s
            WHERE s.isDeleted = false
            """)
    Page<Style> findAllActive(Pageable pageable);
}