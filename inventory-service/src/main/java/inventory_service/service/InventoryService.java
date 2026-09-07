package inventory_service.service;

import inventory_service.dto.OrderCreatedEvent;
import inventory_service.dto.OrderItemDTO;
import inventory_service.dto.ReservationItem;
import inventory_service.dto.ReservationResult;
import inventory_service.enums.ReservationStatus;
import inventory_service.models.Product;
import inventory_service.models.Reservation;
import inventory_service.repository.ProductRepository;
import inventory_service.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InventoryService {
    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ReservationResult reserve(OrderCreatedEvent event){
        validateEvent(event);

        Reservation existingReservation = reservationRepository.findByOrderId(event.getOrderId())
                .orElse(null);

        if (existingReservation != null){
            return new ReservationResult(
                    existingReservation.getOrderId(),
                    existingReservation.getTotalCost(),
                    true
            );
        }

        Map<Long, Long> requestedQuantities = mergeItems(event.getItems());
        List<Product> products = productRepository.findAllByIdForUpdate(
                new ArrayList<>(requestedQuantities.keySet())
        );
        Map<Long, Product> productById = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        Function.identity()
                ));

        validateStock(requestedQuantities, productById);
        BigDecimal totalCost = calculateTotalCost(requestedQuantities, productById);
        decreaseProductQuantities(requestedQuantities, productById);
        
        Reservation reservation = new Reservation();
        reservation.setOrderId(event.getOrderId());
        reservation.setItems(toReservationItems(requestedQuantities));
        reservation.setTotalCost(totalCost);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setCreatedAt(OffsetDateTime.now());

        reservationRepository.save(reservation);
        return new ReservationResult(
                event.getOrderId(),
                totalCost,
                false
        );
    }

    private List<ReservationItem> toReservationItems(Map<Long, Long> requestedQuantities) {
        return requestedQuantities.entrySet().stream().map(item ->
                new ReservationItem(item.getKey(), item.getValue())).toList();
    }

    private void decreaseProductQuantities(Map<Long, Long> requestedQuantities, Map<Long, Product> productById) {
        for (Map.Entry<Long, Long> requestedItem : requestedQuantities.entrySet()){
             Product product = productById.get(requestedItem.getKey());
             long newAvailableQuantity = product.getAvailableQuantity() - requestedItem.getValue();
             product.setAvailableQuantity(newAvailableQuantity);
        }
    }

    private BigDecimal calculateTotalCost(Map<Long, Long> requestedQuantities, Map<Long, Product> productById) {
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map.Entry<Long, Long> requestedItem : requestedQuantities.entrySet()){
            Product product = productById.get(requestedItem.getKey());
            BigDecimal itemTotalCost = product.getPrice().multiply(BigDecimal.valueOf(requestedItem.getValue()));
            totalCost = totalCost.add(itemTotalCost);
        }
        return totalCost;
    }

    private void validateStock(Map<Long, Long> requestedQuantities, Map<Long, Product> productById) {
        for (Map.Entry<Long, Long> requestedItem : requestedQuantities.entrySet()){
            Long productId = requestedItem.getKey();
            Long requestedQuantity = requestedItem.getValue();

            Product product = productById.get(productId);

            if (product == null){
                throw new RuntimeException("product not found");
            }

            if (product.getAvailableQuantity() < requestedQuantity){
                throw new RuntimeException("not enough products in inventory");
            }
        }
    }

    private Map<Long, Long> mergeItems(List<OrderItemDTO> items) {
        Map<Long, Long> requestedQuantities = new HashMap<>();

        for(var item : items){
            requestedQuantities.merge(
                    item.getItemId(),
                    item.getQuantity(),
                    Long::sum
            );
        }
        return requestedQuantities;
    }

    private void validateEvent(OrderCreatedEvent event) {
        if (event == null ||  event.getOrderId() == 0){
            throw new IllegalArgumentException("Order id must not be null");
        }
        if (event.getItems() == null || event.getItems().isEmpty()){
            throw new IllegalArgumentException("Items list must not be null or empty. Order id=" + event.getOrderId());
        }
        for (var item : event.getItems()){
            if (item.getItemId() == null || item.getQuantity() == null){
                throw new IllegalArgumentException("item id and quantity must not be null");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater then 0. Item id=" + item.getItemId());
            }
        }
    }
}
