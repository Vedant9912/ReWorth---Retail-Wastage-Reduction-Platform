package com.reworth.dto.response;
import com.reworth.enums.ProductCategory;
import com.reworth.enums.ProductStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal originalPrice;
    private BigDecimal discountedPrice;
    private Integer discountPercent;
    private LocalDate expiryDate;
    private Integer daysUntilExpiry;
    private Integer stockQuantity;
    private ProductCategory category;
    private ProductStatus status;
    private String imageUrl;
    private Long shopId;
    private String shopName;
    private String shopAddress;
    private Double shopLatitude;
    private Double shopLongitude;
    private LocalDateTime createdAt;
}
