package com.nexaedi.portal.repository;

import com.nexaedi.portal.model.ConnectedPlatform;
import com.nexaedi.portal.model.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectedPlatformRepository extends JpaRepository<ConnectedPlatform, Long> {

    /**
     * Finds the first connected platform of the given type that has a stored access token.
     * Uses a direct query to avoid LazyInitializationException when called outside a session.
     */
    @Query("SELECT p FROM ConnectedPlatform p WHERE p.platformType = :type AND p.accessToken IS NOT NULL AND p.accessToken <> '' ORDER BY p.connectedAt DESC")
    List<ConnectedPlatform> findAllByPlatformTypeWithToken(PlatformType type);

    @Query("SELECT p FROM ConnectedPlatform p WHERE p.seller.id = :sellerId AND p.platformType = :type AND p.accessToken IS NOT NULL AND p.accessToken <> ''")
    Optional<ConnectedPlatform> findBySeller_IdAndPlatformTypeWithToken(Long sellerId, PlatformType type);
}
