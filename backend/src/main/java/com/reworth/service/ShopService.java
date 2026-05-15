package com.reworth.service;
import com.reworth.dto.request.CreateShopRequest;
import com.reworth.dto.response.ShopResponse;
import java.util.List;

public interface ShopService {
    ShopResponse create(CreateShopRequest req, String ownerEmail);
    ShopResponse getById(Long id);
    List<ShopResponse> myShops(String ownerEmail);
    List<ShopResponse> nearby(double lat, double lng, double radiusKm);
    ShopResponse update(Long id, CreateShopRequest req, String ownerEmail);
    void delete(Long id, String ownerEmail);
}
