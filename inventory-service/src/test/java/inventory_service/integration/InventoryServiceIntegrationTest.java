package inventory_service.integration;

import inventory_service.dto.OrderCreatedEvent;
import inventory_service.dto.OrderItemDTO;
import inventory_service.dto.ReservationResult;
import inventory_service.enums.ReservationStatus;
import inventory_service.models.Product;
import inventory_service.models.Reservation;
import inventory_service.repository.ProductRepository;
import inventory_service.repository.ReservationRepository;
import inventory_service.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.fail-fast=false"
})
@Testcontainers
class InventoryServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("inventory_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void reserve_shouldDecreaseStockAndSaveReservationInPostgreSQL() {
        Product product = new Product();
        product.setId(501L);
        product.setProductName("Test keyboard");
        product.setAvailableQuantity(10L);
        product.setPrice(new BigDecimal("120.00"));
        productRepository.save(product);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setUserId(42L);
        event.setOrderId(1001L);
        event.setItems(List.of(new OrderItemDTO(501L, 3L)));

        ReservationResult result = inventoryService.reserve(event);

        assertEquals(1001L, result.getOrderId());
        assertEquals(new BigDecimal("360.00"), result.getTotalCost());
        assertFalse(result.isAlreadyReserved());

        Product updatedProduct = productRepository.findById(501L).orElseThrow();
        assertEquals(7L, updatedProduct.getAvailableQuantity());

        Reservation reservation = reservationRepository.findByOrderId(1001L)
                .orElseThrow();

        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());
        assertEquals(new BigDecimal("360.00"), reservation.getTotalCost());
        assertNotNull(reservation.getCreatedAt());

        assertEquals(1, reservation.getItems().size());
        assertEquals(501L, reservation.getItems().get(0).getItemId());
        assertEquals(3L, reservation.getItems().get(0).getQuantity());
    }
}