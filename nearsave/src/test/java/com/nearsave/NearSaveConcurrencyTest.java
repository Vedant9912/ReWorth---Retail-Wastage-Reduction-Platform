package com.nearsave;

import com.nearsave.product.Product;
import com.nearsave.product.ProductRepository;
import com.nearsave.reservation.ReservationDto.ReservationRequest;
import com.nearsave.reservation.ReservationService;
import com.nearsave.shop.Shop;
import com.nearsave.shop.ShopRepository;
import com.nearsave.user.User;
import com.nearsave.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
public class NearSaveConcurrencyTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private com.nearsave.reservation.ReservationRepository reservationRepository;

    @Autowired
    private com.nearsave.admin.AuditEventRepository auditEventRepository;

    @Autowired
    private com.nearsave.notification.NotificationRepository notificationRepository;

    @Autowired
    private com.nearsave.notification.FavoriteRepository favoriteRepository;

    @Autowired
    private com.nearsave.pickup.PickupEventRepository pickupEventRepository;

    @org.junit.jupiter.api.BeforeEach
    public void setup() {
        pickupEventRepository.deleteAll();
        reservationRepository.deleteAll();
        favoriteRepository.deleteAll();
        notificationRepository.deleteAll();
        auditEventRepository.deleteAll();
        productRepository.deleteAll();
        shopRepository.deleteAll();
        userRepository.deleteAll();
    }

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    public void testConcurrentReservationsOnSingleStockProduct() throws Exception {
        // 1. Create a retailer and shop
        User retailer = User.builder()
                .name("Retailer Concurrency")
                .email("retailer_test@concurrency.com")
                .password("pass123")
                .role(User.Role.RETAILER)
                .build();
        retailer = userRepository.save(retailer);

        Point shopPoint = GF.createPoint(new Coordinate(77.4272, 23.2332));
        shopPoint.setSRID(4326);

        Shop shop = Shop.builder()
                .user(retailer)
                .shopName("Concurrency Shop")
                .address("Bhopal MP Nagar")
                .shopLocation(shopPoint)
                .isOpen(true)
                .isApproved(true)
                .build();
        shop = shopRepository.save(shop);

        // 2. Create a product with stock = 1
        Product product = Product.builder()
                .shop(shop)
                .shopName(shop.getShopName())
                .shopLocation(shop.getShopLocation())
                .name("Limited Item")
                .mrp(BigDecimal.valueOf(100.0))
                .discountedPrice(BigDecimal.valueOf(50.0))
                .stockQuantity(1)
                .expiryDate(LocalDate.now().plusDays(40))
                .category(Product.Category.DAIRY)
                .isActive(true)
                .build();
        product = productRepository.save(product);
        final Long productId = product.getId();

        // 3. Create 100 customer users
        List<User> customers = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            User customer = User.builder()
                    .name("Customer " + i)
                    .email("customer" + i + "@concurrency.com")
                    .password("pass123")
                    .role(User.Role.CUSTOMER)
                    .build();
            customers.add(userRepository.save(customer));
        }

        // 4. Fire 100 threads concurrently
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (final User customer : customers) {
            futures.add(executorService.submit(() -> {
                try {
                    latch.await(); // wait for start signal
                    ReservationRequest req = new ReservationRequest();
                    req.setProductId(productId);
                    reservationService.reserveProduct(customer.getEmail(), req);
                    successCount.incrementAndGet();
                    return true;
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    return false;
                }
            }));
        }

        latch.countDown(); // Start all threads at once!
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);

        // 5. Assertions: Only 1 thread must succeed, and 99 must fail!
        assertEquals(1, successCount.get(), "Only 1 customer should succeed in reserving the last stock");
        assertEquals(99, failureCount.get(), "99 customers should fail due to lock contention/out-of-stock");

        // Verify remaining stock is 0
        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertEquals(0, updatedProduct.getStockQuantity());
    }
}
