package inventory_service.kafka;

import inventory_service.dto.PaymentFailedEvent;
import inventory_service.enums.ReservationStatus;
import inventory_service.models.Reservation;
import inventory_service.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFailedConsumer {

    private final ReservationRepository reservationRepository;

    @KafkaListener(
            topics = "payment-failed",
            groupId = "inventory-service-payment-group",
            properties = "spring.json.value.default.type=inventory_service.dto.PaymentFailedEvent"
    )
    @Transactional
    public void listenPaymentFailed(PaymentFailedEvent event) {
        Reservation reservation = reservationRepository
                .findByOrderId(event.getOrderId())
                .orElse(null);

        if (reservation == null) {
            log.error(
                    "Reservation not found for orderId={}",
                    event.getOrderId()
            );
            return;
        }

        if (reservation.getStatus() == ReservationStatus.RELEASED) {
            log.info(
                    "Reservation already released. orderId={}",
                    event.getOrderId()
            );
            return;
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setReleasedAt(OffsetDateTime.now());
        reservationRepository.save(reservation);

        log.info(
                "Reservation released after payment failure. orderId={}",
                event.getOrderId()
        );
    }
}