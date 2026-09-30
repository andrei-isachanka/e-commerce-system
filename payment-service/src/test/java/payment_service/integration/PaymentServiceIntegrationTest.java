package payment_service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import payment_service.dto.InventoryReservedEvent;
import payment_service.dto.PaymentApprovedEvent;
import payment_service.dto.PaymentFailedEvent;
import payment_service.kafka.PaymentProducer;
import payment_service.models.UserBalance;
import payment_service.repository.UserBalanceRepository;
import payment_service.service.PaymentService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.fail-fast=false"
})
@Testcontainers
class PaymentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("payment_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserBalanceRepository userBalanceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PaymentProducer paymentProducer;

    @Test
    void processPayment_shouldDeductBalanceInPostgreSQL() {
        UserBalance userBalance = new UserBalance();
        userBalance.setUserId(101L);
        userBalance.setBalance(new BigDecimal("1000.00"));
        userBalanceRepository.saveAndFlush(userBalance);

        InventoryReservedEvent event = new InventoryReservedEvent(
                101L,
                2001L,
                new BigDecimal("250.00")
        );

        paymentService.processPayment(event);

        BigDecimal balanceFromDatabase = jdbcTemplate.queryForObject(
                "SELECT balance FROM user_balances WHERE user_id = ?",
                BigDecimal.class,
                101L
        );

        assertEquals(
                0,
                new BigDecimal("750.00").compareTo(balanceFromDatabase)
        );

        verify(paymentProducer).sendPaymentApproved(
                any(PaymentApprovedEvent.class)
        );
        verify(paymentProducer, never()).sendPaymentFailed(
                any(PaymentFailedEvent.class)
        );
    }

    @Test
    void processPayment_shouldKeepBalanceWhenFundsAreInsufficient() {
        UserBalance userBalance = new UserBalance();
        userBalance.setUserId(102L);
        userBalance.setBalance(new BigDecimal("100.00"));
        userBalanceRepository.saveAndFlush(userBalance);

        InventoryReservedEvent event = new InventoryReservedEvent(
                102L,
                2002L,
                new BigDecimal("250.00")
        );

        paymentService.processPayment(event);

        BigDecimal balanceFromDatabase = jdbcTemplate.queryForObject(
                "SELECT balance FROM user_balances WHERE user_id = ?",
                BigDecimal.class,
                102L
        );

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(balanceFromDatabase)
        );

        verify(paymentProducer).sendPaymentFailed(
                any(PaymentFailedEvent.class)
        );
        verify(paymentProducer, never()).sendPaymentApproved(
                any(PaymentApprovedEvent.class)
        );
    }
}