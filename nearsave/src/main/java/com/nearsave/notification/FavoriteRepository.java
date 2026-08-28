package com.nearsave.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdAndTargetType(Long userId, Favorite.TargetType targetType);
    Optional<Favorite> findByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, Favorite.TargetType targetType);
    boolean existsByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, Favorite.TargetType targetType);
    void deleteByUserIdAndTargetIdAndTargetType(Long userId, Long targetId, Favorite.TargetType targetType);
}
