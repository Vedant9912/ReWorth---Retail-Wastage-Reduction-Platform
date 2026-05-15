package com.reworth.dto.request;
import com.reworth.enums.ProductCategory;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateProductRequest {
    @NotBlank @Size(min=2,max=150) public String name;
    @Size(max=500)                 public String description;
    @NotNull @DecimalMin("0.01")   public BigDecimal originalPrice;
    @NotNull @Min(1) @Max(99)      public Integer discountPercent;
    @NotNull @Future               public LocalDate expiryDate;
    @Min(1)                        public Integer stockQuantity = 1;
    public ProductCategory category = ProductCategory.OTHER;
    public String imageUrl;
    @NotNull                       public Long shopId;
}
