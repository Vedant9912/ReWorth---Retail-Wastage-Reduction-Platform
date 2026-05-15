package com.reworth.repository;

import com.reworth.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findByOwnerIdAndActiveTrue(Long ownerId);

    /**
     * Haversine geo-search — returns shops within radiusKm ordered by distance.
     * Formula: 6371 * acos(cos(lat1)*cos(lat2)*cos(lng2-lng1)+sin(lat1)*sin(lat2))
     */
    @Query(value = """
        SELECT s.* FROM shops s
        WHERE s.active = true
        HAVING (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(s.latitude))
            * COS(RADIANS(s.longitude) - RADIANS(:lng))
            + SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
        )) <= :radiusKm
        ORDER BY (6371 * ACOS(
            COS(RADIANS(:lat)) * COS(RADIANS(s.latitude))
            * COS(RADIANS(s.longitude) - RADIANS(:lng))
            + SIN(RADIANS(:lat)) * SIN(RADIANS(s.latitude))
        )) ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Shop> findShopsWithinRadius(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusKm") double radiusKm,
        @Param("limit") int limit
    );
}
