package order_service.kafka;

import order_service.dto.OrderDTO;
import order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentFailedConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentFailedConsumer.class);
    private final OrderService orderService;

    public PaymentFailedConsumer(OrderService orderService){
        this.orderService = orderService;
    }

    @KafkaListener(topics = "payment-failed", groupId = "payment-failed-group")
    public void ListenPaymentFailedEvent(OrderDTO response){
        orderService.ProcessPaymentFailed(response);
        log.info("received order with id {}, payment service response: payment rejected", response.getOrder_id());
    }
}
