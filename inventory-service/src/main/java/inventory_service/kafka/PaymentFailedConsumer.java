package inventory_service.kafka;

import inventory_service.dto.PaymentFailedEvent;
import inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFailedConsumer {

    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "payment-failed",
            groupId = "inventory-service-payment-group",
            properties = "spring.json.value.default.type=inventory_service.dto.PaymentFailedEvent"
    )
    public void listenPaymentFailed(PaymentFailedEvent event) {
        log.info(
                "Payment failed received. userId={}, orderId={}",
                event.getUserId(),
                event.getOrderId()
        );

        inventoryService.releaseReservation(event.getOrderId());
    }
}