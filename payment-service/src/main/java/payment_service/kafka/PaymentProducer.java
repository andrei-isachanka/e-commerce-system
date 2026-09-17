package payment_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import payment_service.dto.PaymentApprovedEvent;
import payment_service.dto.PaymentFailedEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private static final String PAYMENT_APPROVED_TOPIC = "payment-approved";
    private static final String PAYMENT_FAILED_TOPIC = "payment-failed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentApproved(PaymentApprovedEvent paymentApprovedEvent) {
        kafkaTemplate.send(
                PAYMENT_APPROVED_TOPIC,
                String.valueOf(paymentApprovedEvent.getOrderId()),
                paymentApprovedEvent
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to send payment-approved. orderId={}",
                        paymentApprovedEvent.getOrderId(),
                        exception
                );
            }
        });
    }

    public void sendPaymentFailed(PaymentFailedEvent paymentFailedEvent) {
        kafkaTemplate.send(
                PAYMENT_FAILED_TOPIC,
                String.valueOf(paymentFailedEvent.getOrderId()),
                paymentFailedEvent
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(
                        "Failed to send payment-failed. orderId={}",
                        paymentFailedEvent.getOrderId(),
                        exception
                );
            }
        });
    }
}