package com.nearsave.product;

import com.nearsave.product.ProductDto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/nearby")
    public ResponseEntity<List<ProductResponse>> getNearbyProducts(
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        return ResponseEntity.ok(productService.getNearbyProducts(lat, lng));
    }

    @GetMapping("/explore")
    public ResponseEntity<PaginatedResponse<ProductResponse>> exploreProducts(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Integer minDiscount,
            @RequestParam(required = false) Double maxDistance,
            @RequestParam(required = false) Integer maxExpiryDays,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(productService.exploreProducts(
                lat, lng, category, minPrice, maxPrice, minDiscount, maxDistance, maxExpiryDays, shopId, sortBy, page, size
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<ProductResponse> addProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(
                productService.addProduct(userDetails.getUsername(), request)
        );
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RETAILER')")
    public ResponseEntity<List<ProductResponse>> getMyProducts(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(productService.getMyProducts(userDetails.getUsername()));
    }
}
