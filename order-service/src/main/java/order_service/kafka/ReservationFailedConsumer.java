package order_service.kafka;

import order_service.dto.InventoryFailedEvent;
import order_service.dto.OrderDTO;
import order_service.models.Order;
import order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ReservationFailedConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReservationFailedConsumer.class);
    private final OrderService orderService;

    public ReservationFailedConsumer(OrderService orderService){
        this.orderService = orderService;
    }

    //@KafkaListener(topics = "inventory-failed", groupId = "inventory-failed-group")
    @KafkaListener(
            topics = "inventory-failed",
            groupId = "inventory-failed-group",
            properties = {
                    "spring.json.value.default.type=order_service.dto.InventoryFailedEvent",
                    "spring.json.use.type.headers=false"
            })
    public void ListenInventoryFailedEvent(InventoryFailedEvent event){
        orderService.ProcessInventoryFailed(event);
        log.info("received order with id {}, inventory service response: out of stock", event.getOrder_id());
    }
}
