package order_service.kafka;

import order_service.dto.OrderDTO;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderProducer {
    private final KafkaTemplate<String, OrderDTO> kafkaTemplate;
    private static final String TOPIC = "order";

    public OrderProducer(KafkaTemplate<String, OrderDTO> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }


    public void sendOrderCreatedMessage(OrderDTO orderDTO) {
        String key = String.valueOf(orderDTO.getOrder_id());

        kafkaTemplate.send(TOPIC, key, orderDTO);
    }




   /* public void sendOrderCreatedMessage(OrderDTO orderDTO){
        String key = String.valueOf(orderDTO.getOrder_id());

        kafkaTemplate.send(TOPIC, key, orderDTO);


    }*/
}
