package inventory_service.kafka;

import inventory_service.dto.InventoryFailedEvent;
import inventory_service.dto.InventoryReservedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Slf4j
@Service
public class InventoryProducer {

    private static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved";
    private static final String INVENTORY_FAILED_TOPIC = "inventory-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendInventoryReserved(InventoryReservedEvent event) {
        kafkaTemplate.send(
                INVENTORY_RESERVED_TOPIC,
                String.valueOf(event.getOrderId()),
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to send inventory-reserved. orderId={}",
                        event.getOrderId(),
                        exception
                );
            }
        });
    }

    public void sendInventoryFailed(InventoryFailedEvent event) {
        kafkaTemplate.send(
                INVENTORY_FAILED_TOPIC,
                String.valueOf(event.getOrderId()),
                event
        );
    }
}