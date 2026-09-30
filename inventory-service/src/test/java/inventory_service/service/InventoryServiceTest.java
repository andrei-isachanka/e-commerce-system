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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Captor
    private ArgumentCaptor<Reservation> reservationCaptor;

    private Product productOne;
    private Product productTwo;

    @BeforeEach
    void setUp() {
        productOne = createProduct(
                1L,
                "Keyboard",
                10L,
                new BigDecimal("100.00")
        );

        productTwo = createProduct(
                2L,
                "Mouse",
                20L,
                new BigDecimal("50.00")
        );
    }

    @Test
    void reserve_ReserveProductsCalculateCostAndSaveReservation() {
        OrderCreatedEvent event = createOrderEvent(
                100L,
                List.of(
                        createOrderItem(1L, 2L),
                        createOrderItem(2L, 3L)
                )
        );

        when(reservationRepository.findByOrderId(100L))
                .thenReturn(Optional.empty());

        when(productRepository.findAllByIdForUpdate(anyList()))
                .thenReturn(List.of(productOne, productTwo));

        ReservationResult result = inventoryService.reserve(event);

        assertEquals(8L, productOne.getAvailableQuantity());
        assertEquals(17L, productTwo.getAvailableQuantity());

        verify(reservationRepository).save(reservationCaptor.capture());

        Reservation savedReservation = reservationCaptor.getValue();

        assertEquals(100L, savedReservation.getOrderId());
        assertEquals(ReservationStatus.RESERVED, savedReservation.getStatus());
        assertEquals(new BigDecimal("350.00"), savedReservation.getTotalCost());
        assertNotNull(savedReservation.getCreatedAt());

        assertEquals(2, savedReservation.getItems().size());

        assertEquals(1L, savedReservation.getItems().get(0).getItemId());
        assertEquals(2L, savedReservation.getItems().get(0).getQuantity());

        assertEquals(2L, savedReservation.getItems().get(1).getItemId());
        assertEquals(3L, savedReservation.getItems().get(1).getQuantity());

        assertEquals(100L, result.getOrderId());
        assertEquals(new BigDecimal("350.00"), result.getTotalCost());
        assertFalse(result.isAlreadyReserved());
    }

    @Test
    void reserve_MergeQuantitiesForSameProduct() {
        OrderCreatedEvent event = createOrderEvent(
                101L,
                List.of(
                        createOrderItem(1L, 2L),
                        createOrderItem(1L, 3L)
                )
        );

        when(reservationRepository.findByOrderId(101L))
                .thenReturn(Optional.empty());

        when(productRepository.findAllByIdForUpdate(anyList()))
                .thenReturn(List.of(productOne));

        ReservationResult result = inventoryService.reserve(event);

        assertEquals(5L, productOne.getAvailableQuantity());
        assertEquals(new BigDecimal("500.00"), result.getTotalCost());

        verify(reservationRepository).save(reservationCaptor.capture());

        Reservation savedReservation = reservationCaptor.getValue();

        assertEquals(1, savedReservation.getItems().size());
        assertEquals(1L, savedReservation.getItems().get(0).getItemId());
        assertEquals(5L, savedReservation.getItems().get(0).getQuantity());
    }

    @Test
    void reserve_ReturnExistingReservationWithoutDecreasingStockAgain() {
        OrderCreatedEvent event = createOrderEvent(
                102L,
                List.of(createOrderItem(1L, 2L))
        );

        Reservation existingReservation = new Reservation();
        existingReservation.setOrderId(102L);
        existingReservation.setTotalCost(new BigDecimal("200.00"));
        existingReservation.setStatus(ReservationStatus.RESERVED);

        when(reservationRepository.findByOrderId(102L))
                .thenReturn(Optional.of(existingReservation));

        ReservationResult result = inventoryService.reserve(event);

        assertEquals(102L, result.getOrderId());
        assertEquals(new BigDecimal("200.00"), result.getTotalCost());
        assertTrue(result.isAlreadyReserved());

        assertEquals(10L, productOne.getAvailableQuantity());

        verify(productRepository, never()).findAllByIdForUpdate(anyList());
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserve_ThrowExceptionWhenProductDoesNotExist() {
        OrderCreatedEvent event = createOrderEvent(
                103L,
                List.of(createOrderItem(999L, 1L))
        );

        when(reservationRepository.findByOrderId(103L))
                .thenReturn(Optional.empty());

        when(productRepository.findAllByIdForUpdate(anyList()))
                .thenReturn(List.of());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> inventoryService.reserve(event)
        );

        assertEquals("product not found", exception.getMessage());

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserve_ThrowExceptionWhenStockIsNotEnough() {
        OrderCreatedEvent event = createOrderEvent(
                104L,
                List.of(createOrderItem(1L, 11L))
        );

        when(reservationRepository.findByOrderId(104L))
                .thenReturn(Optional.empty());

        when(productRepository.findAllByIdForUpdate(anyList()))
                .thenReturn(List.of(productOne));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> inventoryService.reserve(event)
        );

        assertEquals(
                "not enough products in inventory",
                exception.getMessage()
        );

        assertEquals(10L, productOne.getAvailableQuantity());

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserve_ThrowExceptionWhenItemsAreEmpty() {
        OrderCreatedEvent event = createOrderEvent(105L, List.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryService.reserve(event)
        );

        assertEquals(
                "Items list must not be null or empty. Order id=105",
                exception.getMessage()
        );

        verify(reservationRepository, never()).findByOrderId(any());
        verify(productRepository, never()).findAllByIdForUpdate(anyList());
    }

    @Test
    void releaseReservation_RestoreProductQuantitiesAndMarkReservationReleased() {
        Reservation reservation = new Reservation();
        reservation.setOrderId(200L);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setItems(List.of(
                new ReservationItem(1L, 2L),
                new ReservationItem(2L, 3L)
        ));

        when(reservationRepository.findByOrderId(200L))
                .thenReturn(Optional.of(reservation));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(productOne));

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(productTwo));

        inventoryService.releaseReservation(200L);

        assertEquals(12L, productOne.getAvailableQuantity());
        assertEquals(23L, productTwo.getAvailableQuantity());

        verify(productRepository).save(productOne);
        verify(productRepository).save(productTwo);

        assertEquals(ReservationStatus.RELEASED, reservation.getStatus());
        assertNotNull(reservation.getReleasedAt());

        verify(reservationRepository).save(reservation);
    }

    @Test
    void releaseReservation_NotReleaseProductsAgainWhenAlreadyReleased() {
        Reservation reservation = new Reservation();
        reservation.setOrderId(201L);
        reservation.setStatus(ReservationStatus.RELEASED);
        reservation.setItems(List.of(
                new ReservationItem(1L, 2L)
        ));

        when(reservationRepository.findByOrderId(201L))
                .thenReturn(Optional.of(reservation));

        inventoryService.releaseReservation(201L);

        assertEquals(10L, productOne.getAvailableQuantity());

        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void releaseReservation_DoNothingWhenReservationDoesNotExist() {
        when(reservationRepository.findByOrderId(202L))
                .thenReturn(Optional.empty());

        inventoryService.releaseReservation(202L);

        verify(productRepository, never()).findById(any());
        verify(productRepository, never()).save(any(Product.class));
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void releaseReservation_ThrowExceptionWhenReservedProductDoesNotExist() {
        Reservation reservation = new Reservation();
        reservation.setOrderId(203L);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setItems(List.of(
                new ReservationItem(999L, 2L)
        ));

        when(reservationRepository.findByOrderId(203L))
                .thenReturn(Optional.of(reservation));

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> inventoryService.releaseReservation(203L)
        );

        assertEquals(
                "Product not found, id=999",
                exception.getMessage()
        );

        assertEquals(ReservationStatus.RESERVED, reservation.getStatus());

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    private Product createProduct(
            Long id,
            String productName,
            Long availableQuantity,
            BigDecimal price
    ) {
        Product product = new Product();
        product.setId(id);
        product.setProductName(productName);
        product.setAvailableQuantity(availableQuantity);
        product.setPrice(price);

        return product;
    }

    private OrderCreatedEvent createOrderEvent(
            Long orderId,
            List<OrderItemDTO> items
    ) {
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(orderId);
        event.setItems(items);

        return event;
    }

    private OrderItemDTO createOrderItem(
            Long itemId,
            Long quantity
    ) {
        OrderItemDTO item = new OrderItemDTO();
        item.setItemId(itemId);
        item.setQuantity(quantity);

        return item;
    }
}