package order_service.service;

import order_service.dto.InventoryFailedEvent;
import order_service.dto.OrderDTO;
import order_service.dto.PaymentApprovedEvent;
import order_service.dto.PaymentFailedEvent;
import order_service.enums.OrderStatus;
import order_service.kafka.OrderProducer;
import order_service.models.Order;
import order_service.models.OrderItem;
import order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderProducer orderProducer;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private OrderDTO.OrderItemDTO firstItem;
    private OrderDTO.OrderItemDTO secondItem;

    @BeforeEach
    void setUp() {
        firstItem = new OrderDTO.OrderItemDTO(1L, 2);
        secondItem = new OrderDTO.OrderItemDTO(2L, 3);
    }

    @Test
    void createOrder_shouldSavePendingOrderAndSendOrderCreatedEvent() {
        OrderDTO requestOrder = new OrderDTO(
                10L,
                null,
                null,
                null,
                List.of(firstItem, secondItem)
        );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = invocation.getArgument(0);
                    savedOrder.setId(100L);
                    return savedOrder;
                });

        OrderDTO result = orderService.createOrder(requestOrder);

        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertEquals(10L, savedOrder.getUserId());
        assertEquals(OrderStatus.PENDING, savedOrder.getOrderStatus());

        assertEquals(2, savedOrder.getItems().size());

        assertEquals(1L, savedOrder.getItems().get(0).getItemId());
        assertEquals(2, savedOrder.getItems().get(0).getQuantity());

        assertEquals(2L, savedOrder.getItems().get(1).getItemId());
        assertEquals(3, savedOrder.getItems().get(1).getQuantity());

        assertEquals(savedOrder, savedOrder.getItems().get(0).getOrder());
        assertEquals(savedOrder, savedOrder.getItems().get(1).getOrder());

        assertEquals(100L, result.getOrder_id());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        verify(orderProducer).sendOrderCreatedMessage(requestOrder);
    }

    @Test
    void createOrder_shouldCreateOrderWithoutItemsWhenItemsAreNull() {
        OrderDTO requestOrder = new OrderDTO(
                15L,
                null,
                null,
                null,
                null
        );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order savedOrder = invocation.getArgument(0);
                    savedOrder.setId(101L);
                    return savedOrder;
                });

        OrderDTO result = orderService.createOrder(requestOrder);

        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertEquals(15L, savedOrder.getUserId());
        assertEquals(OrderStatus.PENDING, savedOrder.getOrderStatus());
        assertNull(savedOrder.getItems());

        assertEquals(101L, result.getOrder_id());
        assertEquals(OrderStatus.PENDING, result.getStatus());

        verify(orderProducer).sendOrderCreatedMessage(requestOrder);
    }

    @Test
    void processInventoryFailed_shouldChangeOrderStatusToCancelled() {
        Order order = createOrder(50L, OrderStatus.PENDING);

        InventoryFailedEvent event = mock(InventoryFailedEvent.class);
        when(event.getOrder_id()).thenReturn(50L);

        when(orderRepository.findById(50L)).thenReturn(Optional.of(order));

        orderService.ProcessInventoryFailed(event);

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void processPaymentFailed_shouldChangeOrderStatusToCancelled() {
        Order order = createOrder(51L, OrderStatus.PENDING);

        PaymentFailedEvent event = mock(PaymentFailedEvent.class);
        when(event.getOrderId()).thenReturn(51L);

        when(orderRepository.findById(51L)).thenReturn(Optional.of(order));

        orderService.ProcessPaymentFailed(event);

        assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void processPaymentApproved_shouldChangeOrderStatusToApproved() {
        Order order = createOrder(52L, OrderStatus.PENDING);

        PaymentApprovedEvent event = mock(PaymentApprovedEvent.class);
        when(event.getOrderId()).thenReturn(52L);

        when(orderRepository.findById(52L)).thenReturn(Optional.of(order));

        orderService.ProcessPaymentApproved(event);

        assertEquals(OrderStatus.APPROVED, order.getOrderStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderDTOById_shouldReturnMappedOrderDTO() {
        Order order = createOrder(60L, OrderStatus.APPROVED);
        order.setUserId(25L);
        order.setTotalCost(new BigDecimal("1200.50"));

        OrderItem firstOrderItem = new OrderItem();
        firstOrderItem.setItemId(7L);
        firstOrderItem.setQuantity(2);
        firstOrderItem.setOrder(order);

        OrderItem secondOrderItem = new OrderItem();
        secondOrderItem.setItemId(9L);
        secondOrderItem.setQuantity(1);
        secondOrderItem.setOrder(order);

        order.setItems(List.of(firstOrderItem, secondOrderItem));

        when(orderRepository.findById(60L)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.getOrderDTOById(60L);

        assertEquals(25L, result.getUser_id());
        assertEquals(60L, result.getOrder_id());
        assertEquals(new BigDecimal("1200.50"), result.getTotal_cost());
        assertEquals(OrderStatus.APPROVED, result.getStatus());

        assertEquals(2, result.getItems().size());

        assertEquals(7L, result.getItems().get(0).getItem_id());
        assertEquals(2, result.getItems().get(0).getQuantity());

        assertEquals(9L, result.getItems().get(1).getItem_id());
        assertEquals(1, result.getItems().get(1).getQuantity());

        verify(orderRepository).findById(60L);
    }

    @Test
    void getOrderDTOById_shouldThrowExceptionWhenOrderDoesNotExist() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.getOrderDTOById(999L)
        );

        assertEquals(
                "order not found. order id: 999",
                exception.getMessage()
        );

        verify(orderRepository, times(1)).findById(999L);
    }

    private Order createOrder(Long id, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderStatus(status);
        return order;
    }
}