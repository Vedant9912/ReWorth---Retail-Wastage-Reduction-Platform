package com.reworth.controller;

import com.reworth.dto.request.CreateProductRequest;
import com.reworth.dto.response.*;
import com.reworth.enums.ProductCategory;
import com.reworth.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/products/nearby")
    @Operation(summary = "Browse near-expiry products near user location")
    public ApiResponse<Page<ProductResponse>> nearby(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam(defaultValue = "10.0") double radius,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("expiryDate").ascending());
        return ApiResponse.ok(productService.nearby(lat, lng, radius, category, pg));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get product by ID")
    public ApiResponse<ProductResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(productService.getById(id));
    }

    @GetMapping("/shops/{shopId}/products")
    @Operation(summary = "Get shop's active products")
    public ApiResponse<Page<ProductResponse>> byShop(@PathVariable Long shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("expiryDate").ascending());
        return ApiResponse.ok(productService.byShop(shopId, pg));
    }

    @PostMapping("/shop/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SHOP_OWNER','ADMIN')")
    @Operation(summary = "Add product listing")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest req, Principal p) {
        return ApiResponse.ok(productService.create(req, p.getName()), "Product added");
    }

    @GetMapping("/shop/products")
    @PreAuthorize("hasAnyRole('SHOP_OWNER','ADMIN')")
    @Operation(summary = "My products")
    public ApiResponse<List<ProductResponse>> mine(Principal p) {
        return ApiResponse.ok(productService.mine(p.getName()));
    }

    @PutMapping("/shop/products/{id}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER','ADMIN')")
    @Operation(summary = "Update product")
    public ApiResponse<ProductResponse> update(@PathVariable Long id,
            @Valid @RequestBody CreateProductRequest req, Principal p) {
        return ApiResponse.ok(productService.update(id, req, p.getName()), "Updated");
    }

    @DeleteMapping("/shop/products/{id}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER','ADMIN')")
    @Operation(summary = "Remove product")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal p) {
        productService.delete(id, p.getName());
        return ApiResponse.ok(null, "Removed");
    }
}
