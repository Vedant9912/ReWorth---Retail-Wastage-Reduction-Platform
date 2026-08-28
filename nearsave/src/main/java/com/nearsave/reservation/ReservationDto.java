package com.nearsave.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservationDto {

    @Getter @Setter
    public static class ReservationRequest {
        @NotNull
        private Long productId;
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class ReservationResponse {
        private Long reservationId;
        private String tokenCode;
        private String productName;
        private String shopName;
        private BigDecimal amountToPay;
        private LocalDateTime expiresAt;
        private String status;
    }

    @Getter @Setter
    public static class VerifyTokenRequest {
        @NotBlank @Size(min = 6, max = 6, message = "Token must be exactly 6 characters")
        private String tokenCode;
    }
}
