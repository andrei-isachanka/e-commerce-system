package inventory_service.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryReservedEvent {
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("order_id")
    private Long orderId;
    @JsonProperty("total_cost")
    private BigDecimal totalCost;
}
