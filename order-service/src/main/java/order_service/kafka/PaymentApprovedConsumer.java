package order_service.kafka;

import order_service.dto.OrderDTO;
import order_service.dto.PaymentApprovedEvent;
import order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentApprovedConsumer {
    private static final Logger log = LoggerFactory.getLogger(PaymentApprovedConsumer.class);
    private final OrderService orderService;

    public PaymentApprovedConsumer(OrderService orderService){
        this.orderService = orderService;
    }

    @KafkaListener(topics = "payment-approved", groupId = "payment-approved-group",
            properties = "spring.json.value.default.type=order_service.dto.PaymentApprovedEvent"
    )
    public void ListenPaymentApprovedEvent(PaymentApprovedEvent event){
        orderService.ProcessPaymentApproved(event);
        log.info("received order with id {}, payment service response: payment approved", event.getOrderId());
    }
}
