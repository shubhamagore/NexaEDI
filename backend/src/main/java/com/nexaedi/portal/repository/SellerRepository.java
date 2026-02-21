package com.nexaedi.portal.repository;

import com.nexaedi.portal.model.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByEmail(String email);

    /**
     * Fetch seller with platforms eagerly (one JOIN FETCH per query to avoid MultipleBagFetchException).
     * Retailers are loaded lazily within the calling @Transactional scope.
     */
    @Query("SELECT s FROM Seller s LEFT JOIN FETCH s.platforms WHERE s.id = :id")
    Optional<Seller> findByIdWithPlatforms(Long id);

    @Query("SELECT s FROM Seller s LEFT JOIN FETCH s.retailers WHERE s.id = :id")
    Optional<Seller> findByIdWithRetailers(Long id);
}
