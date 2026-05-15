package com.reworth.repository;

import com.reworth.entity.Product;
import com.reworth.enums.ProductCategory;
import com.reworth.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByShopIdAndStatus(Long shopId, ProductStatus status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.expiryDate <= :target AND p.status = 'ACTIVE' AND p.alertSent = false")
    List<Product> findExpiringUnalerted(@Param("target") LocalDate target);

    @Query("SELECT p FROM Product p WHERE p.expiryDate < :today AND p.status = 'ACTIVE'")
    List<Product> findAlreadyExpired(@Param("today") LocalDate today);

    @Query("""
        SELECT p FROM Product p
        WHERE p.shop.id IN :shopIds
          AND p.status IN ('ACTIVE','EXPIRING_SOON')
          AND p.stockQuantity > 0
          AND (:category IS NULL OR p.category = :category)
        ORDER BY p.expiryDate ASC
        """)
    Page<Product> findActiveByShops(
        @Param("shopIds") List<Long> shopIds,
        @Param("category") ProductCategory category,
        Pageable pageable
    );

    List<Product> findByShopOwnerId(Long ownerId);
}
