package com.nearsave.admin;

import com.nearsave.shop.Shop;
import com.nearsave.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ShopRepository shopRepository;
    private final AuditEventRepository auditEventRepository;

    @GetMapping("/shops")
    public ResponseEntity<List<Shop>> getAllShops() {
        return ResponseEntity.ok(shopRepository.findAll());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditEvent>> getAuditLogs() {
        return ResponseEntity.ok(auditEventRepository.findAllByOrderByTimestampDesc());
    }
}
