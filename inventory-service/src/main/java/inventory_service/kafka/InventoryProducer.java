package inventory_service.kafka;

import inventory_service.dto.InventoryFailedEvent;
import inventory_service.dto.InventoryReservedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
        );
    }

    public void sendInventoryFailed(InventoryFailedEvent event) {
        kafkaTemplate.send(
                INVENTORY_FAILED_TOPIC,
                String.valueOf(event.getOrderId()),
                event
        );
    }
}