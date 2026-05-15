package com.reworth.service;
import com.reworth.dto.request.CreateProductRequest;
import com.reworth.dto.response.ProductResponse;
import com.reworth.enums.ProductCategory;
import org.springframework.data.domain.*;
import java.util.List;

public interface ProductService {
    ProductResponse create(CreateProductRequest req, String ownerEmail);
    ProductResponse getById(Long id);
    Page<ProductResponse> nearby(double lat, double lng, double radiusKm, ProductCategory cat, Pageable p);
    Page<ProductResponse> byShop(Long shopId, Pageable p);
    List<ProductResponse> mine(String ownerEmail);
    ProductResponse update(Long id, CreateProductRequest req, String ownerEmail);
    void delete(Long id, String ownerEmail);
}
