package com.nearsave.analytics;

import lombok.*;
import java.math.BigDecimal;

public class AnalyticsDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RetailerStats {
        private BigDecimal totalSales;
        private long totalItemsSold;
        private long totalExpiredItems;
        private long totalCancelledItems;
        private long totalNoShows;
        private double cancellationRate;
        private double expiryRate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AdminStats {
        private long totalUsers;
        private long totalShops;
        private long totalActiveProducts;
        private long totalReservations;
        private BigDecimal totalSalesPlatform;
        private double cancellationRate;
        private double expiryRate;
    }
}
