package com.nearsave.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userService.getUserProfile(userDetails.getUsername()));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getCustomers() {
        return ResponseEntity.ok(userService.getUsersByRole(User.Role.CUSTOMER));
    }

    @GetMapping("/retailers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getRetailers() {
        return ResponseEntity.ok(userService.getUsersByRole(User.Role.RETAILER));
    }
}
