package payment_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "user_balances")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
}
