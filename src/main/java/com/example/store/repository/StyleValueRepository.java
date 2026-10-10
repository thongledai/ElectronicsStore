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

	List<StyleValue> findByStyleId(UUID styleId);

	Optional<StyleValue> findByNameAndStyleId(String name, UUID styleId);

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, UUID id);

	boolean existsByNameAndStyleId(String name, UUID styleId);

	boolean existsByNameAndStyleIdAndIdNot(String name, UUID styleId, UUID id);

	boolean existsByStyleId(UUID styleId);

	boolean existsByStyleIdAndIsActiveTrue(UUID styleId);

	List<StyleValue> findByStyleIdAndIsActiveTrue(UUID styleId);

	List<StyleValue> findByIsActiveTrue();

	@Query("""
			SELECT sv
			FROM StyleValue sv
			JOIN FETCH sv.style
			WHERE
			    (:q IS NULL OR :q = '' OR
			        LOWER(sv.name) LIKE LOWER(CONCAT('%', :q, '%'))
			    )
			    AND (:styleId IS NULL OR sv.style.id = :styleId)
			    AND (:isActive IS NULL OR sv.isActive = :isActive)
			""")
	Page<StyleValue> search(@Param("q") String q, @Param("styleId") UUID styleId, @Param("isActive") Boolean isActive,
			Pageable pageable);

	@Query("""
			SELECT sv
			FROM StyleValue sv
			WHERE sv.style.id = :styleId
			    AND sv.isActive = true
			""")
	Page<StyleValue> findActiveByStyleId(@Param("styleId") UUID styleId, Pageable pageable);

	@Query("""
			SELECT sv
			FROM StyleValue sv
			WHERE sv.isActive = true
			""")
	Page<StyleValue> findAllActive(Pageable pageable);

	@Query("""
			SELECT sv
			FROM StyleValue sv
			JOIN FETCH sv.style s
			JOIN s.categories c
			WHERE c.id = :categoryId
			    AND sv.isActive = true
			    AND s.isActive = true
			ORDER BY s.name ASC, sv.name ASC
			""")
	List<StyleValue> findActiveByCategoryId(@Param("categoryId") UUID categoryId);
}