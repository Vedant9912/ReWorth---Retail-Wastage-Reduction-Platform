package com.nearsave.pickup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PickupEventRepository extends JpaRepository<PickupEvent, Long> {
    Optional<PickupEvent> findByReservationId(Long reservationId);
}
