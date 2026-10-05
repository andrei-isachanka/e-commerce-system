package order_service.integration;

import com.jayway.jsonpath.JsonPath;
import order_service.kafka.OrderProducer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.enabled=false"
})
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("order_test_db")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private OrderProducer orderProducer;

    @Test
    void createOrder_shouldSaveOrderAndItemsInPostgreSQL() throws Exception {
        String requestBody = """
                {
                  "user_id": 10,
                  "items": [
                    {"item_id": 1, "quantity": 2},
                    {"item_id": 7, "quantity": 3}
                  ]
                }
                """;

        MvcResult result = mockMvc.perform(
                        post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user_id").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.order_id").isNumber())
                .andReturn();

        Number orderIdNumber = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.order_id"
        );
        Long orderId = orderIdNumber.longValue();

        Map<String, Object> savedOrder = jdbcTemplate.queryForMap(
                "SELECT user_id, status FROM orders WHERE id = ?",
                orderId
        );

        assertEquals(10L, ((Number) savedOrder.get("user_id")).longValue());
        assertEquals("PENDING", savedOrder.get("status"));

        List<Map<String, Object>> savedItems = jdbcTemplate.queryForList(
                """
                SELECT item_id, quantity, order_id
                FROM order_items
                WHERE order_id = ?
                ORDER BY item_id
                """,
                orderId
        );

        assertEquals(2, savedItems.size());

        assertEquals(1L, ((Number) savedItems.get(0).get("item_id")).longValue());
        assertEquals(2L, ((Number) savedItems.get(0).get("quantity")).longValue());
        assertEquals(orderId, ((Number) savedItems.get(0).get("order_id")).longValue());

        assertEquals(7L, ((Number) savedItems.get(1).get("item_id")).longValue());
        assertEquals(3L, ((Number) savedItems.get(1).get("quantity")).longValue());
        assertEquals(orderId, ((Number) savedItems.get(1).get("order_id")).longValue());

        verify(orderProducer).sendOrderCreatedMessage(any());
    }
}