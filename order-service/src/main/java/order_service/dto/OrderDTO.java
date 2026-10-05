package order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import order_service.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private Long user_id;
    private Long order_id;
    private BigDecimal total_cost;
    private OrderStatus status;
    private List<OrderDTO.OrderItemDTO> items;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemDTO{
        private long item_id;
        private long quantity;
    }

}
