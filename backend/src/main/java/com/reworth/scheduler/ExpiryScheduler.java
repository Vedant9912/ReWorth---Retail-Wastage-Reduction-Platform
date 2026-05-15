package com.reworth.scheduler;

import com.reworth.entity.Product;
import com.reworth.entity.User;
import com.reworth.enums.ProductStatus;
import com.reworth.repository.ProductRepository;
import com.reworth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Component @RequiredArgsConstructor @Slf4j
public class ExpiryScheduler {

    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    /** Runs daily at midnight IST */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void runExpiryCheck() {
        log.info("⏰ Expiry check started — {}", LocalDate.now());

        // 1. Mark expiring soon + send alerts
        List<Product> expiring = productRepo.findExpiringUnalerted(LocalDate.now().plusDays(2));
        expiring.forEach(p -> {
            p.setStatus(ProductStatus.EXPIRING_SOON);
            sendAlerts(p);
            p.setAlertSent(true);
        });
        productRepo.saveAll(expiring);
        log.info("Marked {} products EXPIRING_SOON", expiring.size());

        // 2. Mark fully expired
        List<Product> expired = productRepo.findAlreadyExpired(LocalDate.now());
        expired.forEach(p -> p.setStatus(ProductStatus.EXPIRED));
        productRepo.saveAll(expired);
        log.info("Marked {} products EXPIRED", expired.size());
    }

    private void sendAlerts(Product product) {
        List<User> subscribers = userRepo.findAll().stream()
            .filter(u -> Boolean.TRUE.equals(u.getAlertsEnabled())).toList();

        for (User user : subscribers) {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo(user.getEmail());
                msg.setSubject("🔔 ReWorth: " + product.getName() + " expiring soon!");
                msg.setText("""
                    Hi %s,
                    
                    "%s" at %s is expiring on %s.
                    Get it for ₹%.2f (%.0f%% OFF — was ₹%.2f)
                    
                    Grab it before it's gone!
                    — Team ReWorth
                    """.formatted(
                        user.getFullName(), product.getName(),
                        product.getShop().getName(), product.getExpiryDate(),
                        product.getDiscountedPrice(), (double) product.getDiscountPercent(),
                        product.getOriginalPrice()
                    ));
                mailSender.send(msg);
            } catch (Exception e) {
                log.warn("Mail failed for {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }
}
