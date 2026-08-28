package com.nearsave.common;

import com.nearsave.user.User;
import com.nearsave.user.UserRepository;
import com.nearsave.shop.Shop;
import com.nearsave.shop.ShopRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🏁 Database seeder checking demo accounts...");

        // Fix MySQL ENUM column issue if users table was created with old schema
        try {
            entityManager.createNativeQuery("ALTER TABLE users MODIFY COLUMN role VARCHAR(50) NOT NULL").executeUpdate();
            log.info("Successfully modified users.role column to VARCHAR(50) to support ADMIN role.");
        } catch (Exception e) {
            log.debug("users.role alter command skipped (already migrated or VARCHAR): {}", e.getMessage());
        }

        // 1. Retailer
        Optional<User> retailerOpt = userRepository.findByEmail("retailer@demo.com");
        User retailer;
        if (retailerOpt.isPresent()) {
            retailer = retailerOpt.get();
            retailer.setPassword(passwordEncoder.encode("password123"));
            retailer.setRole(User.Role.RETAILER);
            userRepository.save(retailer);
            log.info("Reset password for existing retailer@demo.com to 'password123'");
        } else {
            retailer = User.builder()
                    .name("Krishna Dairy Owner")
                    .email("retailer@demo.com")
                    .password(passwordEncoder.encode("password123"))
                    .phone("9876543210")
                    .role(User.Role.RETAILER)
                    .build();
            retailer = userRepository.save(retailer);
            log.info("Seeded new retailer@demo.com");
        }

        // Ensure shop exists for retailer
        if (shopRepository.findByUserId(retailer.getId()).isEmpty()) {
            Point shopLocation = GF.createPoint(new Coordinate(77.4272, 23.2332));
            shopLocation.setSRID(4326);

            Shop shop = Shop.builder()
                    .user(retailer)
                    .shopName("Krishna Dairy")
                    .address("Plot 12, MP Nagar Zone 1, Bhopal")
                    .shopLocation(shopLocation)
                    .isOpen(true)
                    .isApproved(true)
                    .build();
            shopRepository.save(shop);
            log.info("Seeded shop for retailer@demo.com");
        } else {
            // Ensure existing shop is approved for testing
            shopRepository.findByUserId(retailer.getId()).ifPresent(shop -> {
                if (!shop.isApproved()) {
                    shop.setApproved(true);
                    shopRepository.save(shop);
                    log.info("Approved existing shop for retailer@demo.com");
                }
            });
        }

        // 2. Customer
        Optional<User> customerOpt = userRepository.findByEmail("customer@demo.com");
        if (customerOpt.isPresent()) {
            User customer = customerOpt.get();
            customer.setPassword(passwordEncoder.encode("password123"));
            customer.setRole(User.Role.CUSTOMER);
            userRepository.save(customer);
            log.info("Reset password for existing customer@demo.com to 'password123'");
        } else {
            User customer = User.builder()
                    .name("Rahul Customer")
                    .email("customer@demo.com")
                    .password(passwordEncoder.encode("password123"))
                    .phone("9123456780")
                    .role(User.Role.CUSTOMER)
                    .build();
            userRepository.save(customer);
            log.info("Seeded new customer@demo.com");
        }

        // 3. Admin
        Optional<User> adminOpt = userRepository.findByEmail("admin@demo.com");
        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            log.info("Reset password for existing admin@demo.com to 'password123'");
        } else {
            User admin = User.builder()
                    .name("NearSave Admin")
                    .email("admin@demo.com")
                    .password(passwordEncoder.encode("password123"))
                    .phone("9999999999")
                    .role(User.Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("Seeded new admin@demo.com");
        }

        log.info("✅ Database seeder completed.");
    }
}
