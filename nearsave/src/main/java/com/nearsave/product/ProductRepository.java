package com.nearsave.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = """
        SELECT p.* FROM products p
        JOIN shops s ON p.shop_id = s.id
        WHERE
            ST_Distance_Sphere(p.shop_location, ST_SRID(POINT(:userLat, :userLng), 4326)) <= :radiusM
            AND p.expiry_date >= DATE(NOW())
            AND p.stock_quantity > 0
            AND p.is_active = true
            AND s.is_open = true
        ORDER BY
            ST_Distance_Sphere(p.shop_location, ST_SRID(POINT(:userLat, :userLng), 4326)) ASC
        """,
        nativeQuery = true)
    List<Product> findNearbyProducts(
            @Param("userLat") double userLat,
            @Param("userLng") double userLng,
            @Param("radiusM") double radiusM
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.shop.id = :shopId ORDER BY p.createdAt DESC")
    List<Product> findByShopId(@Param("shopId") Long shopId);
}
