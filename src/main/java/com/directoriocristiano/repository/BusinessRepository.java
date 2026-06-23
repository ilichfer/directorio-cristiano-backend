package com.directoriocristiano.repository;

import com.directoriocristiano.model.entity.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    Page<Business> findByFeaturedTrue(Pageable pageable);

    List<Business> findByOwnerId(UUID ownerId);

    @Query(value = """
        SELECT * FROM businesses b 
        WHERE (:search IS NULL OR 
               b.name ILIKE CONCAT('%', :search, '%') OR 
               b.description ILIKE CONCAT('%', :search, '%') OR 
               b.owner_name ILIKE CONCAT('%', :search, '%')) 
        AND (:category IS NULL OR b.category = :category) 
        AND (:zone IS NULL OR b.zone = :zone) 
        ORDER BY b.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM businesses b 
        WHERE (:search IS NULL OR 
               b.name ILIKE CONCAT('%', :search, '%') OR 
               b.description ILIKE CONCAT('%', :search, '%') OR 
               b.owner_name ILIKE CONCAT('%', :search, '%')) 
        AND (:category IS NULL OR b.category = :category) 
        AND (:zone IS NULL OR b.zone = :zone) 
        """,
            nativeQuery = true)
    Page<Business> searchBusinesses(
            @Param("search") String search,
            @Param("category") String category,
            @Param("zone") String zone,
            Pageable pageable);

    long countByCategory(String category);
}
