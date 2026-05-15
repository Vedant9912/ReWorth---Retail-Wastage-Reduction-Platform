package com.reworth.service.impl;

import com.reworth.dto.request.CreateShopRequest;
import com.reworth.dto.response.ShopResponse;
import com.reworth.entity.*;
import com.reworth.exception.*;
import com.reworth.repository.*;
import com.reworth.service.ShopService;
import com.reworth.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class ShopServiceImpl implements ShopService {

    private final ShopRepository shopRepo;
    private final UserRepository userRepo;

    @Override @Transactional
    public ShopResponse create(CreateShopRequest req, String email) {
        User owner = findUser(email);
        Shop shop = Shop.builder()
            .name(req.getName()).description(req.getDescription())
            .address(req.getAddress()).latitude(req.getLatitude())
            .longitude(req.getLongitude()).phone(req.getPhone())
            .email(req.getEmail()).logoUrl(req.getLogoUrl())
            .owner(owner).build();
        return map(shopRepo.save(shop), null);
    }

    @Override
    public ShopResponse getById(Long id) {
        return map(shopRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shop", id)), null);
    }

    @Override
    public List<ShopResponse> myShops(String email) {
        User owner = findUser(email);
        return shopRepo.findByOwnerIdAndActiveTrue(owner.getId())
            .stream().map(s -> map(s, null)).collect(Collectors.toList());
    }

    @Override
    public List<ShopResponse> nearby(double lat, double lng, double radiusKm) {
        if (radiusKm <= 0 || radiusKm > 50) radiusKm = 10.0;
        return shopRepo.findShopsWithinRadius(lat, lng, radiusKm, 50)
            .stream().map(s -> map(s,
                HaversineUtil.distanceKm(lat, lng, s.getLatitude(), s.getLongitude())))
            .collect(Collectors.toList());
    }

    @Override @Transactional
    public ShopResponse update(Long id, CreateShopRequest req, String email) {
        Shop shop = owned(id, email);
        shop.setName(req.getName()); shop.setDescription(req.getDescription());
        shop.setAddress(req.getAddress()); shop.setLatitude(req.getLatitude());
        shop.setLongitude(req.getLongitude()); shop.setPhone(req.getPhone());
        shop.setEmail(req.getEmail());
        if (req.getLogoUrl() != null) shop.setLogoUrl(req.getLogoUrl());
        return map(shopRepo.save(shop), null);
    }

    @Override @Transactional
    public void delete(Long id, String email) {
        Shop shop = owned(id, email);
        shop.setActive(false);
        shopRepo.save(shop);
    }

    // ── helpers ──────────────────────────────────────────
    private Shop owned(Long id, String email) {
        Shop s = shopRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Shop", id));
        if (!s.getOwner().getEmail().equals(email))
            throw new BadRequestException("You do not own this shop");
        return s;
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User: " + email));
    }

    private ShopResponse map(Shop s, Double dist) {
        int count = s.getProducts() == null ? 0 :
            (int) s.getProducts().stream().filter(p -> p.getStockQuantity() > 0).count();
        return ShopResponse.builder()
            .id(s.getId()).name(s.getName()).description(s.getDescription())
            .address(s.getAddress()).latitude(s.getLatitude()).longitude(s.getLongitude())
            .phone(s.getPhone()).email(s.getEmail()).logoUrl(s.getLogoUrl())
            .active(s.getActive()).distanceKm(dist != null ? HaversineUtil.round2(dist) : null)
            .ownerId(s.getOwner().getId()).ownerName(s.getOwner().getFullName())
            .activeProductCount(count).build();
    }
}
