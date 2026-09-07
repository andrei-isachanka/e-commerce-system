package inventory_service.kafka;


import inventory_service.dto.InventoryFailedEvent;
import inventory_service.dto.InventoryReservedEvent;
import inventory_service.dto.OrderCreatedEvent;
import inventory_service.dto.ReservationResult;
import inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreatedConsumer {
    private final InventoryProducer inventoryProducer;
    private final InventoryService inventoryService;

    @KafkaListener(
            topics = "order",
            groupId = "order-created-consumer-group"
    )
    public void listenOrderCreated(OrderCreatedEvent event) {
        try {
            log.info("received order, orderId={}", event.getOrderId());

            ReservationResult reservationResult = inventoryService.reserve(event);

            InventoryReservedEvent inventoryReservedEvent = new InventoryReservedEvent(
                    event.getUserId(),
                    reservationResult.getOrderId(),
                    reservationResult.getTotalCost()
            );

            inventoryProducer.sendInventoryReserved(inventoryReservedEvent);

            if (reservationResult.isAlreadyReserved()) {
                log.info("Duplicate order event processed. orderId={}", reservationResult.getOrderId());
                return;
            }

            log.info("products reserved successfully. OrderId={}, totalCost={}", reservationResult.getOrderId(),
                    reservationResult.getTotalCost());
        } catch (RuntimeException exception) {
            log.error(
                    "Reservation failed. OrderId={}, reason={}",
                    event.getOrderId(),
                    exception.getMessage()
            );

            InventoryFailedEvent inventoryFailedEvent =
                    new InventoryFailedEvent(event.getOrderId());

            inventoryProducer.sendInventoryFailed(inventoryFailedEvent);
        }
    }
}
