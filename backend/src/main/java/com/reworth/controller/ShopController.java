package com.reworth.controller;

import com.reworth.dto.request.CreateShopRequest;
import com.reworth.dto.response.*;
import com.reworth.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Shops")
@SecurityRequirement(name = "bearerAuth")
public class ShopController {
    private final ShopService shopService;

    @GetMapping("/shops/nearby")
    @Operation(summary = "Find shops by geo-radius (Haversine)")
    public ApiResponse<List<ShopResponse>> nearby(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam(defaultValue = "10.0") double radius) {
        return ApiResponse.ok(shopService.nearby(lat, lng, radius));
    }

    @GetMapping("/shops/{id}")
    @Operation(summary = "Get shop by ID")
    public ApiResponse<ShopResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(shopService.getById(id));
    }

    @PostMapping("/shop")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SHOP_OWNER','ADMIN')")
    @Operation(summary = "Create shop")
    public ApiResponse<ShopResponse> create(@Valid @RequestBody CreateShopRequest req, Principal p) {
        return ApiResponse.ok(shopService.create(req, p.getName()), "Shop created");
    }

    @GetMapping("/shop/my-shops")
    @PreAuthorize("hasAnyRole('SHOP_OWNER','ADMIN')")
    @Operation(summary = "My shops")
    public ApiResponse<List<ShopResponse>> mine(Principal p) {
        return ApiResponse.ok(shopService.myShops(p.getName()));
    }

    @PutMapping("/shop/{id}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER','ADMIN')")
    @Operation(summary = "Update shop")
    public ApiResponse<ShopResponse> update(@PathVariable Long id,
            @Valid @RequestBody CreateShopRequest req, Principal p) {
        return ApiResponse.ok(shopService.update(id, req, p.getName()), "Shop updated");
    }

    @DeleteMapping("/shop/{id}")
    @PreAuthorize("hasAnyRole('SHOP_OWNER','ADMIN')")
    @Operation(summary = "Delete shop (soft)")
    public ApiResponse<Void> delete(@PathVariable Long id, Principal p) {
        shopService.delete(id, p.getName());
        return ApiResponse.ok(null, "Shop deleted");
    }
}
