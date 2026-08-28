package com.nearsave.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenExpiryScheduler {

    private final ReservationService reservationService;

    @Scheduled(cron = "0 * * * * ?")
    public void sweepExpiredTokens() {
        log.debug("⏰ Token expiry sweep running at {}", java.time.LocalTime.now());
        try {
            reservationService.expireOldTokens();
        } catch (Exception e) {
            log.error("Error during token expiry sweep: {}", e.getMessage(), e);
        }
    }
}
