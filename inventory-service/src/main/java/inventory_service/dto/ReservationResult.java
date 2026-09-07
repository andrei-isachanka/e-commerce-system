package inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReservationResult {
    private final Long orderId;
    private final BigDecimal totalCost;
    private final boolean alreadyReserved;
}
