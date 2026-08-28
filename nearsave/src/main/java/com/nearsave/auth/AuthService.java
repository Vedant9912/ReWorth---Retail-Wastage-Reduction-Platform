package com.nearsave.auth;

import com.nearsave.auth.AuthDto.*;
import com.nearsave.shop.Shop;
import com.nearsave.user.User;
import com.nearsave.common.AppException;
import com.nearsave.shop.ShopRepository;
import com.nearsave.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final com.nearsave.admin.AuditService auditService;

    private static final GeometryFactory GF =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new AppException("An account with this email already exists", HttpStatus.CONFLICT);
        }

        User.Role role;
        try {
            role = User.Role.valueOf(req.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("Role must be RETAILER, CUSTOMER, or ADMIN", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(role)
                .build();
        user = userRepository.save(user);

        if (role == User.Role.RETAILER) {
            validateShopFields(req);

            Point shopPoint = GF.createPoint(
                    new Coordinate(req.getShopLongitude(), req.getShopLatitude())
            );
            shopPoint.setSRID(4326);

            Shop shop = Shop.builder()
                    .user(user)
                    .shopName(req.getShopName())
                    .address(req.getShopAddress())
                    .shopLocation(shopPoint)
                    .isOpen(true)
                    .isApproved(false)
                    .build();
            shopRepository.save(shop);
            
            auditService.logEvent(
                user.getEmail(), "SHOP_CREATED", "Shop", shop.getId(),
                "Shop: " + shop.getShopName() + " created. Status: pending approval."
            );
        }

        auditService.logEvent(
            user.getEmail(), "USER_REGISTERED", "User", user.getId(),
            "User registered with role: " + user.getRole().name()
        );

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name(), user.getName(), user.getId());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        auditService.logEvent(
            user.getEmail(), "USER_LOGIN", "User", user.getId(),
            "User successfully logged in"
        );

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name(), user.getName(), user.getId());
    }

    private void validateShopFields(RegisterRequest req) {
        if (req.getShopName() == null || req.getShopName().isBlank()) {
            throw new AppException("Shop name is required for retailer accounts", HttpStatus.BAD_REQUEST);
        }
        if (req.getShopLatitude() == null || req.getShopLongitude() == null) {
            throw new AppException("Shop GPS coordinates are required for retailer accounts", HttpStatus.BAD_REQUEST);
        }
    }
}
