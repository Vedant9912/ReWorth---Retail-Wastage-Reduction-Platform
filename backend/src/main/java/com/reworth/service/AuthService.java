package com.reworth.service;
import com.reworth.dto.request.*;
import com.reworth.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
}
