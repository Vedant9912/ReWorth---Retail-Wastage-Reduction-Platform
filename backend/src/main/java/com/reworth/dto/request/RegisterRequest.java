package com.reworth.dto.request;

import com.reworth.enums.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank @Size(min=2, max=80)   public String fullName;
    @NotBlank @Email                  public String email;
    @NotBlank @Size(min=6)            public String password;
    @Pattern(regexp="^[6-9]\\d{9}$") public String phone;
    @NotNull                          public Role role;
}
