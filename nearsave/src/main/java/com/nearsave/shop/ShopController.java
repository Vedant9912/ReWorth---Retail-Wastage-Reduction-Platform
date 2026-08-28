package com.nearsave.shop;

import com.nearsave.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/{id}")
    public ResponseEntity<ShopDto.ShopResponse> getShop(@PathVariable Long id) {
        Shop shop = shopService.getShopById(id);
        return ResponseEntity.ok(ShopDto.ShopResponse.fromEntity(shop));
    }
    
    @GetMapping("/mine")
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<ShopDto.ShopResponse> getMyShop(
            @AuthenticationPrincipal UserDetails userDetails) {
        Shop shop = shopService.getShopByRetailer(userDetails.getUsername());
        return ResponseEntity.ok(ShopDto.ShopResponse.fromEntity(shop));
    }

    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<ApiResponse> toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(shopService.toggleStatus(id, userDetails.getUsername()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> approveShop(
            @PathVariable Long id,
            @RequestParam boolean approve,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(shopService.approveShop(id, approve, userDetails.getUsername()));
    }
}
