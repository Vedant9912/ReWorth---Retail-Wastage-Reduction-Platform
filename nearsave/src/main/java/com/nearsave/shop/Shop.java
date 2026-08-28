package com.nearsave.shop;

import com.nearsave.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "shops")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 150)
    private String shopName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "POINT NOT NULL SRID 4326")
    private Point shopLocation;

    @Builder.Default
    @Column(nullable = false)
    private boolean isOpen = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean isApproved = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
