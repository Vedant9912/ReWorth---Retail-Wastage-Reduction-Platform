package com.reworth.service.impl;

import com.reworth.dto.request.CreateProductRequest;
import com.reworth.dto.response.ProductResponse;
import com.reworth.entity.*;
import com.reworth.enums.*;
import com.reworth.exception.*;
import com.reworth.repository.*;
import com.reworth.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepo;
    private final ShopRepository shopRepo;
    private final UserRepository userRepo;

    @Override @Transactional
    public ProductResponse create(CreateProductRequest req, String email) {
        Shop shop = shopRepo.findById(req.getShopId())
            .orElseThrow(() -> new ResourceNotFoundException("Shop", req.getShopId()));
        if (!shop.getOwner().getEmail().equals(email))
            throw new BadRequestException("You do not own this shop");

        BigDecimal discounted = req.getOriginalPrice()
            .multiply(BigDecimal.valueOf(100 - req.getDiscountPercent()))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Product p = Product.builder()
            .name(req.getName()).description(req.getDescription())
            .originalPrice(req.getOriginalPrice()).discountedPrice(discounted)
            .discountPercent(req.getDiscountPercent()).expiryDate(req.getExpiryDate())
            .stockQuantity(req.getStockQuantity()).category(req.getCategory())
            .imageUrl(req.getImageUrl()).shop(shop).build();
        return map(productRepo.save(p));
    }

    @Override
    public ProductResponse getById(Long id) {
        return map(productRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id)));
    }

    @Override
    public Page<ProductResponse> nearby(double lat, double lng, double radiusKm,
                                         ProductCategory cat, Pageable pageable) {
        List<Shop> shops = shopRepo.findShopsWithinRadius(lat, lng, radiusKm, 100);
        List<Long> ids = shops.stream().map(Shop::getId).collect(Collectors.toList());
        if (ids.isEmpty()) return Page.empty(pageable);
        return productRepo.findActiveByShops(ids, cat, pageable).map(this::map);
    }

    @Override
    public Page<ProductResponse> byShop(Long shopId, Pageable pageable) {
        return productRepo.findByShopIdAndStatus(shopId, ProductStatus.ACTIVE, pageable).map(this::map);
    }

    @Override
    public List<ProductResponse> mine(String email) {
        User owner = userRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User: " + email));
        return productRepo.findByShopOwnerId(owner.getId())
            .stream().map(this::map).collect(Collectors.toList());
    }

    @Override @Transactional
    public ProductResponse update(Long id, CreateProductRequest req, String email) {
        Product p = productRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (!p.getShop().getOwner().getEmail().equals(email))
            throw new BadRequestException("You do not own this product");

        BigDecimal discounted = req.getOriginalPrice()
            .multiply(BigDecimal.valueOf(100 - req.getDiscountPercent()))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        p.setName(req.getName()); p.setDescription(req.getDescription());
        p.setOriginalPrice(req.getOriginalPrice()); p.setDiscountedPrice(discounted);
        p.setDiscountPercent(req.getDiscountPercent()); p.setExpiryDate(req.getExpiryDate());
        p.setStockQuantity(req.getStockQuantity()); p.setCategory(req.getCategory());
        if (req.getImageUrl() != null) p.setImageUrl(req.getImageUrl());
        return map(productRepo.save(p));
    }

    @Override @Transactional
    public void delete(Long id, String email) {
        Product p = productRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (!p.getShop().getOwner().getEmail().equals(email))
            throw new BadRequestException("You do not own this product");
        p.setStatus(ProductStatus.SOLD_OUT);
        productRepo.save(p);
    }

    private ProductResponse map(Product p) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), p.getExpiryDate());
        return ProductResponse.builder()
            .id(p.getId()).name(p.getName()).description(p.getDescription())
            .originalPrice(p.getOriginalPrice()).discountedPrice(p.getDiscountedPrice())
            .discountPercent(p.getDiscountPercent()).expiryDate(p.getExpiryDate())
            .daysUntilExpiry((int) days).stockQuantity(p.getStockQuantity())
            .category(p.getCategory()).status(p.getStatus()).imageUrl(p.getImageUrl())
            .shopId(p.getShop().getId()).shopName(p.getShop().getName())
            .shopAddress(p.getShop().getAddress())
            .shopLatitude(p.getShop().getLatitude()).shopLongitude(p.getShop().getLongitude())
            .createdAt(p.getCreatedAt()).build();
    }
}
