package com.example.store.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import com.example.store.entity.Brand;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    @Query("""
        SELECT b
        FROM Brand b
        WHERE
            (:q IS NULL OR :q = '' OR
                LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(b.slug) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:isActive IS NULL OR b.isActive = :isActive)
        """)
    Page<Brand> search(
            @Param("q") String q,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );

    @Query("""
        SELECT b
        FROM Brand b
        WHERE b.isActive = true
        """)
    Page<Brand> findAllActive(Pageable pageable);
}