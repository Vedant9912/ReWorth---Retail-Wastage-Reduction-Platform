package com.reworth.dto.response;
import com.reworth.enums.Role;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Boolean alertsEnabled;
}
