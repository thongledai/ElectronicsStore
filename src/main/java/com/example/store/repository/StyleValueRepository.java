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

import com.example.store.entity.StyleValue;

@Repository
public interface StyleValueRepository extends JpaRepository<StyleValue, UUID> {

        Optional<StyleValue> findByName(String name);

        Optional<StyleValue> findByNameAndStyleId(String name, UUID styleId);

        boolean existsByName(String name);

        boolean existsByNameAndIdNot(String name, UUID id);

        boolean existsByNameAndStyleId(String name, UUID styleId);

        boolean existsByNameAndStyleIdAndIdNot(String name, UUID styleId, UUID id);

        boolean existsByStyleId(UUID styleId);

        boolean existsByStyleIdAndIsDeletedFalse(UUID styleId);

        List<StyleValue> findByStyleIdAndIsDeletedFalse(UUID styleId);

        List<StyleValue> findByIsDeletedFalse();

        @Query("""
                        SELECT sv
                        FROM StyleValue sv
                        JOIN FETCH sv.style
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
                        Pageable pageable);

        @Query("""
                        SELECT sv
                        FROM StyleValue sv
                        WHERE sv.style.id = :styleId
                            AND sv.isDeleted = false
                        """)
        Page<StyleValue> findActiveByStyleId(
                        @Param("styleId") UUID styleId,
                        Pageable pageable);

        @Query("""
                        SELECT sv
                        FROM StyleValue sv
                        WHERE sv.isDeleted = false
                        """)
        Page<StyleValue> findAllActive(Pageable pageable);

        @Query("""
                        SELECT sv
                        FROM StyleValue sv
                        JOIN FETCH sv.style s
                        JOIN s.categories c
                        WHERE c.id = :categoryId
                            AND sv.isDeleted = false
                            AND s.isDeleted = false
                        ORDER BY s.name ASC, sv.name ASC
                        """)
        List<StyleValue> findActiveByCategoryId(@Param("categoryId") UUID categoryId);
}