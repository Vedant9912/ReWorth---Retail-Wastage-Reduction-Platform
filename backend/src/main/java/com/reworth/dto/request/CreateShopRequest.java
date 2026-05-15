package com.reworth.dto.request;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateShopRequest {
    @NotBlank @Size(min=2, max=120)                      public String name;
    @Size(max=300)                                        public String description;
    @NotBlank @Size(max=300)                              public String address;
    @NotNull @DecimalMin("-90") @DecimalMax("90")         public Double latitude;
    @NotNull @DecimalMin("-180") @DecimalMax("180")       public Double longitude;
    @Pattern(regexp="^[6-9]\\d{9}$", message="Invalid mobile") public String phone;
    @Email                                                public String email;
    public String logoUrl;
}
