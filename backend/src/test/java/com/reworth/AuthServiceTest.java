package com.reworth;

import com.reworth.dto.request.RegisterRequest;
import com.reworth.dto.response.AuthResponse;
import com.reworth.entity.User;
import com.reworth.enums.Role;
import com.reworth.exception.BadRequestException;
import com.reworth.repository.UserRepository;
import com.reworth.security.service.JwtService;
import com.reworth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepo;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authManager;
    @Mock UserDetailsService uds;
    @InjectMocks AuthServiceImpl authService;

    @Test
    void register_duplicateEmail_throwsBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setRole(Role.USER);
        when(userRepo.existsByEmail("test@example.com")).thenReturn(true);
        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already registered");
    }

    @Test
    void register_adminRole_throwsBadRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("admin@test.com");
        req.setRole(Role.ADMIN);
        when(userRepo.existsByEmail(any())).thenReturn(false);
        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("ADMIN");
    }
}
