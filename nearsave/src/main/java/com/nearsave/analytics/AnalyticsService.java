package com.nearsave.analytics;

import com.nearsave.reservation.ReservationRepository;
import com.nearsave.reservation.ReservationStatus;
import com.nearsave.reservation.Reservation;
import com.nearsave.shop.ShopRepository;
import com.nearsave.shop.Shop;
import com.nearsave.user.UserRepository;
import com.nearsave.user.User;
import com.nearsave.product.ProductRepository;
import com.nearsave.common.AppException;
import com.nearsave.analytics.AnalyticsDto.RetailerStats;
import com.nearsave.analytics.AnalyticsDto.AdminStats;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ReservationRepository reservationRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public RetailerStats getRetailerStats(String email) {
        User retailer = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));

        Shop shop = shopRepository.findByUserId(retailer.getId())
                .orElseThrow(() -> new AppException("Shop not found", HttpStatus.NOT_FOUND));

        List<Reservation> reservations = reservationRepository.findByShopId(shop.getId());

        long totalReservations = reservations.size();
        long completedCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.COMPLETED).count();
        long expiredCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.EXPIRED).count();
        long cancelledCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CANCELLED).count();
        long noShowCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.NO_SHOW).count();

        BigDecimal totalSales = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .map(r -> r.getProduct().getDiscountedPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double cancellationRate = totalReservations > 0 ? (double) cancelledCount / totalReservations * 100 : 0.0;
        double expiryRate = totalReservations > 0 ? (double) expiredCount / totalReservations * 100 : 0.0;

        return RetailerStats.builder()
                .totalSales(totalSales)
                .totalItemsSold(completedCount)
                .totalExpiredItems(expiredCount)
                .totalCancelledItems(cancelledCount)
                .totalNoShows(noShowCount)
                .cancellationRate(cancellationRate)
                .expiryRate(expiryRate)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminStats getAdminStats() {
        long totalUsers = userRepository.count();
        long totalShops = shopRepository.count();
        long totalProducts = productRepository.count();
        long totalReservations = reservationRepository.count();

        List<Reservation> reservations = reservationRepository.findAll();
        long completedCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.COMPLETED).count();
        long expiredCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.EXPIRED).count();
        long cancelledCount = reservations.stream().filter(r -> r.getStatus() == ReservationStatus.CANCELLED).count();

        BigDecimal totalSales = reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .map(r -> r.getProduct().getDiscountedPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double cancellationRate = totalReservations > 0 ? (double) cancelledCount / totalReservations * 100 : 0.0;
        double expiryRate = totalReservations > 0 ? (double) expiredCount / totalReservations * 100 : 0.0;

        return AdminStats.builder()
                .totalUsers(totalUsers)
                .totalShops(totalShops)
                .totalActiveProducts(totalProducts)
                .totalReservations(totalReservations)
                .totalSalesPlatform(totalSales)
                .cancellationRate(cancellationRate)
                .expiryRate(expiryRate)
                .build();
    }
}
