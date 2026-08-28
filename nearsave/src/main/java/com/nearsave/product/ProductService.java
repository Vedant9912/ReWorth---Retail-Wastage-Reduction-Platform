package com.nearsave.product;

import com.nearsave.product.ProductDto.*;
import com.nearsave.shop.Shop;
import com.nearsave.user.User;
import com.nearsave.common.AppException;
import com.nearsave.shop.ShopRepository;
import com.nearsave.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final com.nearsave.admin.AuditService auditService;

    @Value("${app.search.radius-metres}")
    private double searchRadiusMetres;

    @Value("${app.product.min-expiry-days}")
    private int minExpiryDays;

    @Transactional(readOnly = true)
    public PaginatedResponse<ProductResponse> exploreProducts(
            double userLat, double userLng,
            String category,
            BigDecimal minPrice, BigDecimal maxPrice,
            Integer minDiscountPercent,
            Double maxDistanceMetres,
            Integer maxExpiryDays,
            Long shopId,
            String sortBy,
            int page, int size
    ) {
        StringBuilder sql = new StringBuilder("SELECT p.* FROM products p JOIN shops s ON p.shop_id = s.id WHERE p.is_active = true AND s.is_open = true AND p.stock_quantity > 0 ");
        String distanceSql = "ST_Distance_Sphere(p.shop_location, ST_SRID(POINT(:userLat, :userLng), 4326))";

        if (category != null && !category.isBlank()) {
            sql.append("AND p.category = :category ");
        }
        if (minPrice != null) {
            sql.append("AND p.discounted_price >= :minPrice ");
        }
        if (maxPrice != null) {
            sql.append("AND p.discounted_price <= :maxPrice ");
        }
        if (minDiscountPercent != null) {
            sql.append("AND ((p.mrp - p.discounted_price) / p.mrp * 100) >= :minDiscountPercent ");
        }
        
        double maxDist = maxDistanceMetres != null ? maxDistanceMetres : searchRadiusMetres;
        sql.append("AND ").append(distanceSql).append(" <= :maxDist ");

        if (maxExpiryDays != null) {
            sql.append("AND p.expiry_date <= :maxExpiryDate ");
        }
        sql.append("AND p.expiry_date >= :today ");

        if (shopId != null) {
            sql.append("AND p.shop_id = :shopId ");
        }

        // Sorting
        String orderSort = sortBy != null ? sortBy : "nearest";
        switch (orderSort.toLowerCase()) {
            case "biggest_discount":
                sql.append("ORDER BY ((p.mrp - p.discounted_price) / p.mrp) DESC ");
                break;
            case "lowest_price":
                sql.append("ORDER BY p.discounted_price ASC ");
                break;
            case "newest":
                sql.append("ORDER BY p.created_at DESC ");
                break;
            case "expiry_soonest":
                sql.append("ORDER BY p.expiry_date ASC ");
                break;
            case "nearest":
            default:
                sql.append("ORDER BY ").append(distanceSql).append(" ASC ");
                break;
        }

        // Execute raw query
        Query query = entityManager.createNativeQuery(sql.toString(), Product.class);
        query.setParameter("userLat", userLat);
        query.setParameter("userLng", userLng);
        query.setParameter("today", LocalDate.now());
        query.setParameter("maxDist", maxDist);

        if (category != null && !category.isBlank()) {
            query.setParameter("category", category);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        if (minDiscountPercent != null) {
            query.setParameter("minDiscountPercent", minDiscountPercent);
        }
        if (maxExpiryDays != null) {
            query.setParameter("maxExpiryDate", LocalDate.now().plusDays(maxExpiryDays));
        }
        if (shopId != null) {
            query.setParameter("shopId", shopId);
        }

        // Count query for pagination total elements
        String countSql = "SELECT COUNT(*) FROM (" + sql.toString() + ") AS count_temp";
        Query countQuery = entityManager.createNativeQuery(countSql);
        countQuery.setParameter("userLat", userLat);
        countQuery.setParameter("userLng", userLng);
        countQuery.setParameter("today", LocalDate.now());
        countQuery.setParameter("maxDist", maxDist);

        if (category != null && !category.isBlank()) {
            countQuery.setParameter("category", category);
        }
        if (minPrice != null) {
            countQuery.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            countQuery.setParameter("maxPrice", maxPrice);
        }
        if (minDiscountPercent != null) {
            countQuery.setParameter("minDiscountPercent", minDiscountPercent);
        }
        if (maxExpiryDays != null) {
            countQuery.setParameter("maxExpiryDate", LocalDate.now().plusDays(maxExpiryDays));
        }
        if (shopId != null) {
            countQuery.setParameter("shopId", shopId);
        }

        long totalElements = ((Number) countQuery.getSingleResult()).longValue();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Apply pagination offsets
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        @SuppressWarnings("unchecked")
        List<Product> products = query.getResultList();

        List<ProductResponse> content = products.stream()
                .map(p -> {
                    double distanceM = calculateDistance(userLat, userLng, p);
                    return mapToResponse(p, distanceM);
                })
                .collect(Collectors.toList());

        return PaginatedResponse.<ProductResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    @Transactional
    public ProductResponse addProduct(String retailerEmail, ProductRequest req) {
        User retailer = userRepository.findByEmail(retailerEmail)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));

        Shop shop = shopRepository.findByUserId(retailer.getId())
                .orElseThrow(() -> new AppException(
                        "No shop found. Please complete shop setup first.", HttpStatus.NOT_FOUND
                ));

        LocalDate minExpiry = LocalDate.now().plusDays(minExpiryDays);
        if (req.getExpiryDate().isBefore(minExpiry)) {
            throw new AppException(
                    "Expiry date must be at least " + minExpiryDays + " days from today",
                    HttpStatus.BAD_REQUEST
            );
        }

        BigDecimal minPrice = req.getMrp().multiply(BigDecimal.valueOf(0.40));
        BigDecimal maxPrice = req.getMrp().multiply(BigDecimal.valueOf(0.60));
        if (req.getDiscountedPrice().compareTo(minPrice) < 0
                || req.getDiscountedPrice().compareTo(maxPrice) > 0) {
            throw new AppException(
                    "Discounted price must be between 40% and 60% of MRP",
                    HttpStatus.BAD_REQUEST
            );
        }

        Product product = Product.builder()
                .shop(shop)
                .shopName(shop.getShopName())
                .shopLocation(shop.getShopLocation())
                .name(req.getName())
                .category(req.getCategory())
                .mrp(req.getMrp())
                .discountedPrice(req.getDiscountedPrice())
                .stockQuantity(req.getStockQuantity())
                .expiryDate(req.getExpiryDate())
                .isActive(true)
                .build();

        product = productRepository.save(product);

        auditService.logEvent(
                retailerEmail, "PRODUCT_ADDED", "Product", product.getId(),
                "Product: " + product.getName() + " listed with qty: " + product.getStockQuantity()
        );

        return mapToResponse(product, 0.0);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getNearbyProducts(double userLat, double userLng) {
        List<Product> products = productRepository.findNearbyProducts(
                userLat, userLng, searchRadiusMetres
        );

        // Fallback for demo/testing: if no products exist within 1 km, expand search radius
        if (products.isEmpty()) {
            products = productRepository.findNearbyProducts(
                    userLat, userLng, 10000000.0 // 10,000 km fallback
            );
        }

        return products.stream()
                .map(p -> {
                    double distanceM = calculateDistance(userLat, userLng, p);
                    return mapToResponse(p, distanceM);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts(String retailerEmail) {
        User retailer = userRepository.findByEmail(retailerEmail)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));

        Shop shop = shopRepository.findByUserId(retailer.getId())
                .orElseThrow(() -> new AppException("Shop not found", HttpStatus.NOT_FOUND));

        return productRepository.findByShopId(shop.getId())
                .stream()
                .map(p -> mapToResponse(p, 0.0))
                .collect(Collectors.toList());
    }

    private ProductResponse mapToResponse(Product p, double distanceM) {
        int discountPct = p.getMrp().compareTo(BigDecimal.ZERO) > 0
                ? p.getMrp().subtract(p.getDiscountedPrice())
                        .divide(p.getMrp(), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .intValue()
                : 0;

        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .shopName(p.getShopName())
                .category(p.getCategory().name())
                .mrp(p.getMrp())
                .discountedPrice(p.getDiscountedPrice())
                .discountPercent(discountPct)
                .stockQuantity(p.getStockQuantity())
                .expiryDate(p.getExpiryDate())
                .distanceMetres(Math.round(distanceM))
                .latitude(p.getShopLocation().getY())
                .longitude(p.getShopLocation().getX())
                .build();
    }

    private double calculateDistance(double userLat, double userLng, Product p) {
        double shopLat = p.getShopLocation().getY();
        double shopLng = p.getShopLocation().getX();
        final int EARTH_RADIUS = 6371000;
        double dLat = Math.toRadians(shopLat - userLat);
        double dLng = Math.toRadians(shopLng - userLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(shopLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_RADIUS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
