package com.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import payment_service.dto.InventoryReservedEvent;
import payment_service.dto.PaymentApprovedEvent;
import payment_service.dto.PaymentFailedEvent;
import payment_service.kafka.PaymentProducer;
import payment_service.models.UserBalance;
import payment_service.repository.UserBalanceRepository;
import payment_service.service.PaymentService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserBalanceRepository userBalanceRepository;

    @Mock
    private PaymentProducer paymentProducer;

    @InjectMocks
    private PaymentService paymentService;

    @Captor
    private ArgumentCaptor<PaymentFailedEvent> paymentFailedCaptor;

    @Captor
    private ArgumentCaptor<PaymentApprovedEvent> paymentApprovedCaptor;

    @Test
    void processPayment_SendPaymentFailedWhenUserBalanceDoesNotExist() {
        InventoryReservedEvent event = new InventoryReservedEvent(
                1L,
                100L,
                new BigDecimal("250.00")
        );

        when(userBalanceRepository.findByUserId(1L))
                .thenReturn(Optional.empty());

        paymentService.processPayment(event);

        verify(paymentProducer).sendPaymentFailed(
                paymentFailedCaptor.capture()
        );

        PaymentFailedEvent failedEvent = paymentFailedCaptor.getValue();

        assertEquals(1L, failedEvent.getUserId());
        assertEquals(100L, failedEvent.getOrderId());

        verify(userBalanceRepository, never()).save(any(UserBalance.class));
        verify(paymentProducer, never()).sendPaymentApproved(
                any(PaymentApprovedEvent.class)
        );
    }

    @Test
    void processPayment_SendPaymentFailedWhenBalanceIsNotEnough() {
        InventoryReservedEvent event = new InventoryReservedEvent(
                2L,
                101L,
                new BigDecimal("250.00")
        );

        UserBalance userBalance = createUserBalance(
                2L,
                new BigDecimal("100.00")
        );

        when(userBalanceRepository.findByUserId(2L))
                .thenReturn(Optional.of(userBalance));

        paymentService.processPayment(event);

        assertEquals(
                new BigDecimal("100.00"),
                userBalance.getBalance()
        );

        verify(paymentProducer).sendPaymentFailed(
                paymentFailedCaptor.capture()
        );

        PaymentFailedEvent failedEvent = paymentFailedCaptor.getValue();

        assertEquals(2L, failedEvent.getUserId());
        assertEquals(101L, failedEvent.getOrderId());

        verify(userBalanceRepository, never()).save(any(UserBalance.class));
        verify(paymentProducer, never()).sendPaymentApproved(
                any(PaymentApprovedEvent.class)
        );
    }

    @Test
    void processPayment_DeductBalanceSaveUserAndSendPaymentApproved() {
        InventoryReservedEvent event = new InventoryReservedEvent(
                3L,
                102L,
                new BigDecimal("250.00")
        );

        UserBalance userBalance = createUserBalance(
                3L,
                new BigDecimal("1000.00")
        );

        when(userBalanceRepository.findByUserId(3L))
                .thenReturn(Optional.of(userBalance));

        paymentService.processPayment(event);

        assertEquals(
                new BigDecimal("750.00"),
                userBalance.getBalance()
        );

        verify(userBalanceRepository).save(userBalance);

        verify(paymentProducer).sendPaymentApproved(
                paymentApprovedCaptor.capture()
        );

        PaymentApprovedEvent approvedEvent = paymentApprovedCaptor.getValue();

        assertEquals(3L, approvedEvent.getUserId());
        assertEquals(102L, approvedEvent.getOrderId());

        verify(paymentProducer, never()).sendPaymentFailed(
                any(PaymentFailedEvent.class)
        );
    }

    @Test
    void processPayment_AllowPaymentWhenBalanceEqualsTotalCost() {
        InventoryReservedEvent event = new InventoryReservedEvent(
                4L,
                103L,
                new BigDecimal("500.00")
        );

        UserBalance userBalance = createUserBalance(
                4L,
                new BigDecimal("500.00")
        );

        when(userBalanceRepository.findByUserId(4L))
                .thenReturn(Optional.of(userBalance));

        paymentService.processPayment(event);

        assertEquals(
                new BigDecimal("0.00"),
                userBalance.getBalance()
        );

        verify(userBalanceRepository).save(userBalance);

        verify(paymentProducer).sendPaymentApproved(
                paymentApprovedCaptor.capture()
        );

        PaymentApprovedEvent approvedEvent = paymentApprovedCaptor.getValue();

        assertEquals(4L, approvedEvent.getUserId());
        assertEquals(103L, approvedEvent.getOrderId());

        verify(paymentProducer, never()).sendPaymentFailed(
                any(PaymentFailedEvent.class)
        );
    }

    private UserBalance createUserBalance(
            Long userId,
            BigDecimal balance
    ) {
        UserBalance userBalance = new UserBalance();
        userBalance.setUserId(userId);
        userBalance.setBalance(balance);

        return userBalance;
    }
}
