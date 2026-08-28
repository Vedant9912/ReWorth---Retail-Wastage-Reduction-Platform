package com.nearsave.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class AuthDto {
    @Getter @Setter
    public static class LoginRequest {
        @Email(message = "Valid email required")
        @NotBlank
        private String email;

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }

    @Getter @Setter
    public static class RegisterRequest {
        @NotBlank
        private String name;

        @Email @NotBlank
        private String email;

        @NotBlank @Size(min = 6)
        private String password;

        private String phone;

        @NotNull
        private String role; // "RETAILER", "CUSTOMER", or "ADMIN"

        // Only required when role = RETAILER
        private String shopName;
        private String shopAddress;
        private Double shopLatitude;
        private Double shopLongitude;
    }

    @Getter @Setter @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String role;
        private String name;
        private Long userId;
    }
}
