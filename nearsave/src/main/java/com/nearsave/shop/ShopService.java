package com.nearsave.shop;

import com.nearsave.common.AppException;
import com.nearsave.common.ApiResponse;
import com.nearsave.user.User;
import com.nearsave.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final com.nearsave.admin.AuditService auditService;

    @Transactional(readOnly = true)
    public Shop getShopById(Long id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new AppException("Shop not found", HttpStatus.NOT_FOUND));
    }
    
    @Transactional(readOnly = true)
    public Shop getShopByRetailer(String email) {
        User retailer = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));
        return shopRepository.findByUserId(retailer.getId())
                .orElseThrow(() -> new AppException("Shop not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public ApiResponse toggleStatus(Long id, String email) {
        Shop shop = getShopById(id);
        User retailer = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Retailer not found", HttpStatus.NOT_FOUND));
        
        if (!shop.getUser().getId().equals(retailer.getId())) {
            throw new AppException("Not authorized to toggle this shop", HttpStatus.FORBIDDEN);
        }
        
        shop.setOpen(!shop.isOpen());
        shopRepository.save(shop);

        auditService.logEvent(
                retailer.getEmail(), "SHOP_TOGGLE_STATUS", "Shop", shop.getId(),
                "Shop open status toggled to: " + shop.isOpen()
        );
        
        return ApiResponse.ok("Shop is now " + (shop.isOpen() ? "open" : "closed"));
    }

    @Transactional
    public ApiResponse approveShop(Long id, boolean approve, String adminEmail) {
        Shop shop = getShopById(id);
        shop.setApproved(approve);
        shopRepository.save(shop);

        auditService.logEvent(
                adminEmail, "SHOP_APPROVAL", "Shop", shop.getId(),
                "Shop approval status updated to: " + approve
        );

        return ApiResponse.ok("Shop approval status updated to " + approve);
    }
}
