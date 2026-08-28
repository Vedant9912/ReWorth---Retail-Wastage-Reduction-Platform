package com.nearsave.product;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProductDto {

    @Getter @Setter
    public static class ProductRequest {
        @NotBlank
        private String name;

        @NotNull
        private Product.Category category;

        @NotNull @Positive
        private BigDecimal mrp;

        @NotNull @Positive
        private BigDecimal discountedPrice;

        @NotNull @Min(1)
        private Integer stockQuantity;

        @NotNull @Future(message = "Expiry date must be in the future")
        private LocalDate expiryDate;
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class ProductResponse {
        private Long id;
        private String name;
        private String shopName;
        private String category;
        private BigDecimal mrp;
        private BigDecimal discountedPrice;
        private int discountPercent;
        private int stockQuantity;
        private LocalDate expiryDate;
        private double distanceMetres;
        private double latitude;
        private double longitude;
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class PaginatedResponse<T> {
        private java.util.List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }
}
