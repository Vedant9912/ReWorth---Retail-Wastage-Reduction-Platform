package com.reworth.service.impl;

import com.reworth.dto.request.*;
import com.reworth.dto.response.*;
import com.reworth.entity.User;
import com.reworth.enums.Role;
import com.reworth.exception.BadRequestException;
import com.reworth.repository.UserRepository;
import com.reworth.security.service.JwtService;
import com.reworth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
    private final UserDetailsService uds;

    @Override @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new BadRequestException("Email already registered: " + req.getEmail());
        if (req.getRole() == Role.ADMIN)
            throw new BadRequestException("Cannot self-register as ADMIN");

        User user = User.builder()
            .fullName(req.getFullName()).email(req.getEmail())
            .password(encoder.encode(req.getPassword()))
            .phone(req.getPhone()).role(req.getRole())
            .build();
        userRepo.save(user);
        return tokens(user);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> new BadRequestException("User not found"));
        return tokens(user);
    }

    private AuthResponse tokens(User user) {
        UserDetails ud = uds.loadUserByUsername(user.getEmail());
        return AuthResponse.builder()
            .accessToken(jwtService.generateToken(ud))
            .refreshToken(jwtService.generateRefreshToken(ud))
            .user(UserResponse.builder()
                .id(user.getId()).fullName(user.getFullName())
                .email(user.getEmail()).phone(user.getPhone())
                .role(user.getRole()).alertsEnabled(user.getAlertsEnabled())
                .build())
            .build();
    }
}
