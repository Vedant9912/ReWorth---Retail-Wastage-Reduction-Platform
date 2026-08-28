package com.nearsave.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByTokenCode(String tokenCode);

    boolean existsByTokenCode(String tokenCode);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status IN (com.nearsave.reservation.ReservationStatus.RESERVED, com.nearsave.reservation.ReservationStatus.READY_FOR_PICKUP)
        AND r.expiresAt < :now
        """)
    List<Reservation> findExpiredPendingReservations(@Param("now") LocalDateTime now);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.customer.id = :customerId
        ORDER BY r.createdAt DESC
        """)
    List<Reservation> findByCustomerId(@Param("customerId") Long customerId);

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.product.shop.id = :shopId
        ORDER BY r.createdAt DESC
        """)
    List<Reservation> findByShopId(@Param("shopId") Long shopId);

    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r
        WHERE r.customer.id = :customerId
        AND r.product.id = :productId
        AND r.status IN (com.nearsave.reservation.ReservationStatus.RESERVED, com.nearsave.reservation.ReservationStatus.READY_FOR_PICKUP)
        """)
    boolean existsPendingReservationForCustomerAndProduct(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId
    );
}
