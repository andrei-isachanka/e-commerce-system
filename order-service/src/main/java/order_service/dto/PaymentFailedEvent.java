package order_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentFailedEvent {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("order_id")
    private Long orderId;

}
