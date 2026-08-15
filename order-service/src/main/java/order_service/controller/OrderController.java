package order_service.controller;


import order_service.dto.OrderDTO;
import order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private final OrderService orderService;

    public OrderController(OrderService orderService) {this.orderService = orderService;}

    @PostMapping
    public ResponseEntity<OrderDTO> orderCreated(@RequestBody OrderDTO requestOrder){
        log.info("called createOrder method");
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(requestOrder));
    }
}
