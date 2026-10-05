package payment_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import payment_service.dto.InventoryReservedEvent;
import payment_service.service.PaymentService;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryReservedConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "inventory-reserved",
            groupId = "payment-service-group",
            properties = "spring.json.value.default.type=payment_service.dto.InventoryReservedEvent"
    )
    public void listenInventoryReserved(InventoryReservedEvent event) {
        log.info(
                "Inventory reserved received. userId={}, orderId={}, totalCost={}",
                event.getUserId(),
                event.getOrderId(),
                event.getTotalCost()
        );

        paymentService.processPayment(event);
    }
}