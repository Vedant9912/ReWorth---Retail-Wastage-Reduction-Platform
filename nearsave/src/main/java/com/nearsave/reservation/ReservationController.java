package com.nearsave.reservation;

import com.nearsave.reservation.ReservationDto.*;
import com.nearsave.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/reserve")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationResponse> reserve(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReservationRequest request
    ) {
        return ResponseEntity.ok(
                reservationService.reserveProduct(userDetails.getUsername(), request)
        );
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<ApiResponse> verify(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody VerifyTokenRequest request
    ) {
        return ResponseEntity.ok(
                reservationService.verifyToken(userDetails.getUsername(), request)
        );
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReservationResponse>> myBookings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.getMyBookings(userDetails.getUsername()));
    }

    @GetMapping("/shop")
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<List<ReservationResponse>> shopBookings(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.getShopBookings(userDetails.getUsername()));
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<ApiResponse> markReady(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.markReadyForPickup(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<ApiResponse> markNoShow(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.markNoShow(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RETAILER')")
    public ResponseEntity<ApiResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, userDetails.getUsername()));
    }
}
