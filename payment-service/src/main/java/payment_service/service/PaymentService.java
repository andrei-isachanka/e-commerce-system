package payment_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import payment_service.dto.InventoryReservedEvent;
import payment_service.dto.PaymentApprovedEvent;
import payment_service.dto.PaymentFailedEvent;
import payment_service.kafka.PaymentProducer;
import payment_service.models.UserBalance;
import payment_service.repository.UserBalanceRepository;

import java.math.BigDecimal;

@Slf4j
@Service
public class PaymentService {

    private final UserBalanceRepository userBalanceRepository;
    private final PaymentProducer paymentProducer;

    public PaymentService (PaymentProducer paymentProducer, UserBalanceRepository userBalanceRepository){
        this.paymentProducer = paymentProducer;
        this.userBalanceRepository = userBalanceRepository;
    }

    @Transactional
    public void processPayment(InventoryReservedEvent event) {
        Long userId = event.getUserId();
        Long orderId = event.getOrderId();
        BigDecimal totalCost = event.getTotalCost();

        UserBalance userBalance = userBalanceRepository.findByUserId(userId)
                .orElse(null);

        if (userBalance == null) {
            log.warn("User balance not found. userId={}, orderId={}", userId, orderId);
            paymentProducer.sendPaymentFailed(new PaymentFailedEvent(userId, orderId));
            return;
        }

        if (userBalance.getBalance().compareTo(totalCost) < 0) {
            log.info(
                    "Not enough balance. userId={}, orderId={}, balance={}, totalCost={}",
                    userId, orderId, userBalance.getBalance(), totalCost
            );
            paymentProducer.sendPaymentFailed(new PaymentFailedEvent(userId, orderId));
            return;
        }

        userBalance.setBalance(userBalance.getBalance().subtract(totalCost));
        userBalanceRepository.save(userBalance);

        log.info(
                "Payment approved. userId={}, orderId={}, newBalance={}",
                userId, orderId, userBalance.getBalance()
        );
        paymentProducer.sendPaymentApproved(new PaymentApprovedEvent(userId, orderId));
    }
}