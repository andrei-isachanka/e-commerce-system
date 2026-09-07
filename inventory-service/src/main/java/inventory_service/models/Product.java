package inventory_service.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {

    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String productName;

    @Column(nullable = false)
    private Long availableQuantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;
}
