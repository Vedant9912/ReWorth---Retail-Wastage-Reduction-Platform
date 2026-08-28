package com.nearsave.reservation;

import com.nearsave.reservation.ReservationDto.*;
import com.nearsave.product.Product;
import com.nearsave.user.User;
import com.nearsave.shop.Shop;
import com.nearsave.common.AppException;
import com.nearsave.common.ApiResponse;
import com.nearsave.product.ProductRepository;
import com.nearsave.user.UserRepository;
import com.nearsave.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final com.nearsave.admin.AuditService auditService;

    private static final String TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public ReservationResponse reserveProduct(String customerEmail, ReservationRequest req) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new AppException("Customer not found", HttpStatus.NOT_FOUND));

        if (reservationRepository.existsPendingReservationForCustomerAndProduct(
                customer.getId(), req.getProductId())) {
            throw new AppException(
                    "You already have an active booking for this product. Collect it first!",
                    HttpStatus.CONFLICT
            );
        }

        Product product = productRepository.findByIdWithLock(req.getProductId())
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        if (!product.isActive()) {
            throw new AppException("This product is no longer available", HttpStatus.GONE);
        }
        if (product.getStockQuantity() <= 0) {
            throw new AppException(
                    "Sorry, this item just got booked by someone else. Check back soon!",
                    HttpStatus.CONFLICT
            );
        }

        product.setStockQuantity(product.getStockQuantity() - 1);
        productRepository.save(product);

        String tokenCode = generateUniqueTokenCode();

        Reservation reservation = Reservation.builder()
                .product(product)
                .customer(customer)
                .tokenCode(tokenCode)
                .status(ReservationStatus.RESERVED)
                .build();
        reservation = reservationRepository.save(reservation);

        auditService.logEvent(
                customer.getEmail(), "RESERVATION_CREATED", "Reservation", reservation.getId(),
                "Product: " + product.getName() + " reserved with token: " + tokenCode
        );

        return mapToResponse(reservation);
    }

    @Transactional
    public ApiResponse verifyToken(String retailerEmail, VerifyTokenRequest req) {
        Reservation reservation = reservationRepository.findByTokenCode(req.getTokenCode())
                .orElseThrow(() -> new AppException(
                        "Token not found. Check the code and try again.",
                        HttpStatus.NOT_FOUND
                ));

        User retailer = userRepository.findByEmail(retailerEmail)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));

        Long tokenShopUserId = reservation.getProduct().getShop().getUser().getId();
        if (!tokenShopUserId.equals(retailer.getId())) {
            throw new AppException("This token does not belong to your shop", HttpStatus.FORBIDDEN);
        }

        validateTransition(reservation.getStatus(), ReservationStatus.COMPLETED);

        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setCompletedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        auditService.logEvent(
                retailer.getEmail(), "RESERVATION_COMPLETED", "Reservation", reservation.getId(),
                "Token verified. Customer collected: " + reservation.getProduct().getName()
        );

        return ApiResponse.ok(
                "Order completed! Customer collected: " + reservation.getProduct().getName()
        );
    }

    @Transactional
    public ApiResponse markReadyForPickup(Long id, String retailerEmail) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new AppException("Reservation not found", HttpStatus.NOT_FOUND));

        User retailer = userRepository.findByEmail(retailerEmail)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));

        Long tokenShopUserId = reservation.getProduct().getShop().getUser().getId();
        if (!tokenShopUserId.equals(retailer.getId())) {
            throw new AppException("Not authorized for this shop's reservations", HttpStatus.FORBIDDEN);
        }

        validateTransition(reservation.getStatus(), ReservationStatus.READY_FOR_PICKUP);

        reservation.setStatus(ReservationStatus.READY_FOR_PICKUP);
        reservationRepository.save(reservation);

        auditService.logEvent(
                retailer.getEmail(), "RESERVATION_READY", "Reservation", reservation.getId(),
                "Reservation status marked as READY_FOR_PICKUP"
        );

        return ApiResponse.ok("Reservation status updated to READY_FOR_PICKUP");
    }

    @Transactional
    public ApiResponse markNoShow(Long id, String retailerEmail) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new AppException("Reservation not found", HttpStatus.NOT_FOUND));

        User retailer = userRepository.findByEmail(retailerEmail)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));

        Long tokenShopUserId = reservation.getProduct().getShop().getUser().getId();
        if (!tokenShopUserId.equals(retailer.getId())) {
            throw new AppException("Not authorized for this shop's reservations", HttpStatus.FORBIDDEN);
        }

        validateTransition(reservation.getStatus(), ReservationStatus.NO_SHOW);

        reservation.setStatus(ReservationStatus.NO_SHOW);
        reservationRepository.save(reservation);

        // Release inventory
        Product product = reservation.getProduct();
        product.setStockQuantity(product.getStockQuantity() + 1);
        productRepository.save(product);

        auditService.logEvent(
                retailer.getEmail(), "RESERVATION_NO_SHOW", "Reservation", reservation.getId(),
                "Reservation marked as NO_SHOW by retailer. Stock released."
        );

        return ApiResponse.ok("Reservation marked as NO_SHOW. Stock released.");
    }

    @Transactional
    public ApiResponse cancelReservation(Long id, String email) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new AppException("Reservation not found", HttpStatus.NOT_FOUND));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        boolean isCustomer = reservation.getCustomer().getId().equals(user.getId());
        boolean isRetailer = reservation.getProduct().getShop().getUser().getId().equals(user.getId());

        if (!isCustomer && !isRetailer) {
            throw new AppException("Not authorized to cancel this reservation", HttpStatus.FORBIDDEN);
        }

        validateTransition(reservation.getStatus(), ReservationStatus.CANCELLED);

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // Release stock
        Product product = reservation.getProduct();
        product.setStockQuantity(product.getStockQuantity() + 1);
        productRepository.save(product);

        auditService.logEvent(
                user.getEmail(), "RESERVATION_CANCELLED", "Reservation", reservation.getId(),
                "Reservation cancelled by " + user.getRole().name() + ". Stock released."
        );

        return ApiResponse.ok("Reservation successfully cancelled. Stock released.");
    }

    @Transactional
    public void expireOldTokens() {
        List<Reservation> expiredReservations =
                reservationRepository.findExpiredPendingReservations(LocalDateTime.now());

        if (expiredReservations.isEmpty()) return;

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);

            Product product = reservation.getProduct();
            product.setStockQuantity(product.getStockQuantity() + 1);
            productRepository.save(product);

            auditService.logEvent(
                    "SYSTEM", "RESERVATION_EXPIRED", "Reservation", reservation.getId(),
                    "Reservation expired automatically by background sweep scheduler. Stock released."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyBookings(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new AppException("Customer not found", HttpStatus.NOT_FOUND));

        return reservationRepository.findByCustomerId(customer.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getShopBookings(String retailerEmail) {
        User retailer = userRepository.findByEmail(retailerEmail)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));

        Shop shop = shopRepository.findByUserId(retailer.getId())
                .orElseThrow(() -> new AppException("Shop not found", HttpStatus.NOT_FOUND));

        return reservationRepository.findByShopId(shop.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateTransition(ReservationStatus current, ReservationStatus target) {
        boolean valid = false;
        switch (current) {
            case RESERVED:
                valid = (target == ReservationStatus.READY_FOR_PICKUP ||
                         target == ReservationStatus.CANCELLED ||
                         target == ReservationStatus.EXPIRED ||
                         target == ReservationStatus.NO_SHOW ||
                         target == ReservationStatus.COMPLETED);
                break;
            case READY_FOR_PICKUP:
                valid = (target == ReservationStatus.COMPLETED ||
                         target == ReservationStatus.CANCELLED ||
                         target == ReservationStatus.EXPIRED ||
                         target == ReservationStatus.NO_SHOW);
                break;
            case COMPLETED:
            case CANCELLED:
            case EXPIRED:
            case NO_SHOW:
                valid = false;
                break;
        }
        if (!valid) {
            throw new AppException(
                "Invalid status transition from " + current + " to " + target,
                HttpStatus.BAD_REQUEST
            );
        }
    }

    private String generateUniqueTokenCode() {
        String code;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(TOKEN_CHARS.charAt(RANDOM.nextInt(TOKEN_CHARS.length())));
            }
            code = sb.toString();
            attempts++;
            if (attempts > 100) {
                throw new AppException("Could not generate unique token. Try again.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } while (reservationRepository.existsByTokenCode(code));
        return code;
    }

    private ReservationResponse mapToResponse(Reservation b) {
        return ReservationResponse.builder()
                .reservationId(b.getId())
                .tokenCode(b.getTokenCode())
                .productName(b.getProduct().getName())
                .shopName(b.getProduct().getShopName())
                .amountToPay(b.getProduct().getDiscountedPrice())
                .expiresAt(b.getExpiresAt())
                .status(b.getStatus().name())
                .build();
    }
}
