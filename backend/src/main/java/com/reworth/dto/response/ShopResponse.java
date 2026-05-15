package com.reworth.dto.response;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShopResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
    private String logoUrl;
    private Boolean active;
    private Double distanceKm;
    private Long ownerId;
    private String ownerName;
    private Integer activeProductCount;
}
