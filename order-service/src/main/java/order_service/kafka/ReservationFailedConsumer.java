package order_service.kafka;

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

    @KafkaListener(topics = "inventory-failed", groupId = "inventory-failed-group")
    public void ListenInventoryFailedEvent(OrderDTO response){
        orderService.ProcessInventoryFailed(response);
        log.info("received order with id {}, inventory service response: out of stock", response.getOrder_id());
    }
}
