package inventory_service.repository;

import inventory_service.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>
{
    Optional<Reservation> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
