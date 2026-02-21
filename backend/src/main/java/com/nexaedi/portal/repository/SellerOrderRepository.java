package com.nexaedi.portal.repository;

import com.nexaedi.portal.model.OrderSyncStatus;
import com.nexaedi.portal.model.SellerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface SellerOrderRepository extends JpaRepository<SellerOrder, Long> {

    List<SellerOrder> findBySellerIdOrderByReceivedAtDesc(Long sellerId);

    List<SellerOrder> findBySellerIdAndStatusOrderByReceivedAtDesc(Long sellerId, OrderSyncStatus status);

    List<SellerOrder> findBySellerIdAndRetailerIdOrderByReceivedAtDesc(Long sellerId, String retailerId);

    long countBySellerIdAndStatus(Long sellerId, OrderSyncStatus status);

    long countBySellerId(Long sellerId);

    @Query("SELECT COALESCE(SUM(o.orderValue), 0) FROM SellerOrder o WHERE o.seller.id = :sellerId AND o.receivedAt >= :since")
    BigDecimal sumOrderValueSince(Long sellerId, Instant since);

    @Query("SELECT COALESCE(SUM(o.orderValue), 0) FROM SellerOrder o WHERE o.seller.id = :sellerId AND o.retailerId = :retailerId AND o.receivedAt >= :since")
    BigDecimal sumOrderValueByRetailerSince(Long sellerId, String retailerId, Instant since);

    @Query("SELECT COUNT(o) FROM SellerOrder o WHERE o.seller.id = :sellerId AND o.receivedAt >= :since")
    long countOrdersSince(Long sellerId, Instant since);

    List<SellerOrder> findBySellerIdOrderByReceivedAtDesc(Long sellerId,
            org.springframework.data.domain.Pageable pageable);
}
