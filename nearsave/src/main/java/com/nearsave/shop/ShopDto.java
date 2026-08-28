package com.nearsave.shop;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class ShopDto {

    @Getter @Setter @Builder
    public static class ShopResponse {
        private Long id;
        private String shopName;
        private String address;
        private boolean isOpen;
        private boolean isApproved;
        
        public static ShopResponse fromEntity(Shop shop) {
            return ShopResponse.builder()
                    .id(shop.getId())
                    .shopName(shop.getShopName())
                    .address(shop.getAddress())
                    .isOpen(shop.isOpen())
                    .isApproved(shop.isApproved())
                    .build();
        }
    }
}
