package com.reworth.entity;

import com.reworth.enums.ProductCategory;
import com.reworth.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "products",
    indexes = {
        @Index(name = "idx_product_shop",     columnList = "shop_id"),
        @Index(name = "idx_product_expiry",   columnList = "expiryDate"),
        @Index(name = "idx_product_status",   columnList = "status"),
        @Index(name = "idx_product_category", columnList = "category")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountedPrice;

    @Column(nullable = false)
    private Integer discountPercent;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Builder.Default
    private Integer stockQuantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProductCategory category = ProductCategory.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(length = 255)
    private String imageUrl;

    @Builder.Default
    private Boolean alertSent = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
