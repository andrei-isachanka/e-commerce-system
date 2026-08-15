package order_service.service;

import jakarta.transaction.Transactional;
import order_service.dto.OrderDTO;
import order_service.enums.OrderStatus;
import order_service.kafka.OrderProducer;
import order_service.models.Order;
import order_service.models.OrderItem;
import order_service.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {
    private final OrderProducer orderProducer;
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository, OrderProducer orderProducer){
        this.orderProducer = orderProducer;
        this.orderRepository = orderRepository;
    }


    @Transactional
    public OrderDTO createOrder(OrderDTO requestOrder) {
        Order order = new Order();
        order.setUserId(requestOrder.getUser_id());
        order.setOrderStatus(OrderStatus.PENDING);

        if (requestOrder.getItems() != null){
            List<OrderItem> items = requestOrder.getItems().stream().map(itemDTO -> {
                OrderItem item = new OrderItem();
                item.setItemId(itemDTO.getItem_id());
                item.setQuantity(itemDTO.getQuantity());
                item.setOrder(order);
                return item;
            }).toList();

            order.setItems(items);
        }

        Order savedOrder = orderRepository.save(order);
        requestOrder.setOrder_id(savedOrder.getId());
        requestOrder.setStatus(savedOrder.getOrderStatus());

        orderProducer.sendOrderCreatedMessage(requestOrder);
        return requestOrder;
    }

    @Transactional
    public void ProcessInventoryFailed(OrderDTO response) {

        Order order = orderRepository.findById(response.getOrder_id())
                .orElseThrow(() -> new RuntimeException("order not found. order id: " + response.getOrder_id()));

        order.setOrderStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
    }

    @Transactional
    public void ProcessPaymentFailed(OrderDTO response) {

        Order order = orderRepository.findById(response.getOrder_id())
                .orElseThrow(() -> new RuntimeException("order not found. order id: " + response.getOrder_id()));

        order.setOrderStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);
    }

    @Transactional
    public void ProcessPaymentApproved(OrderDTO response) {

        Order order = orderRepository.findById(response.getOrder_id())
                .orElseThrow(() -> new RuntimeException("order not found. order id: " + response.getOrder_id()));

        order.setOrderStatus(OrderStatus.APPROVED);

        orderRepository.save(order);
    }
}
