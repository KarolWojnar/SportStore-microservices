package com.shop.orderservice.service;

import com.shop.orderservice.model.OrderStatus;
import com.shop.orderservice.model.ProductInOrder;
import com.shop.orderservice.model.ShippingAddress;
import com.shop.orderservice.model.dto.*;
import com.shop.orderservice.model.entity.Order;
import com.shop.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private KafkaEventService kafkaEventService;


    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderRepository orderRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId("123");
        testOrder.setOrderDate(Date.from(Instant.now()));
        testOrder.setStatus(OrderStatus.CREATED);

        ProductInOrder p = new ProductInOrder("prod1", 1, new BigDecimal("19.99"));
        testOrder.setProducts(List.of(p));
    }

    @Test
    void getSummary_Success() {
        String userId = "123";
        CustomerDto mockCustomer = new CustomerDto();
        Map<String, Integer> mockProducts = Map.of("prod1", 2);
        BigDecimal mockTotalPrice = BigDecimal.valueOf(100.00);

        when(kafkaEventService.optCustomer(userId))
                .thenReturn(CompletableFuture.completedFuture(mockCustomer));
        when(kafkaEventService.getCartAndSetAsOrderProcessing(userId, true))
                .thenReturn(CompletableFuture.completedFuture(mockProducts));
        when(kafkaEventService.getTotalPriceOfCart(mockProducts))
                .thenReturn(CompletableFuture.completedFuture(mockTotalPrice));

        OrderDto result = orderService.getSummary(userId);

        assertNotNull(result);
        assertEquals(mockCustomer.getFirstName(), result.getFirstName());
        assertEquals(mockCustomer.getLastName(), result.getLastName());
        assertEquals(mockTotalPrice, result.getTotalPrice());
        verify(kafkaEventService, times(1)).optCustomer(userId);
        verify(kafkaEventService, times(1)).getCartAndSetAsOrderProcessing(userId, true);
        verify(kafkaEventService, times(1)).getTotalPriceOfCart(mockProducts);
    }

    @Test
    void cancelPayment_Success() {
        String userId = "123";
        Map<String, Integer> mockProducts = Map.of("prod1", 1);
        when(kafkaEventService.getCartAndSetAsOrderProcessing(userId, false))
                .thenReturn(CompletableFuture.completedFuture(mockProducts));
        doReturn(CompletableFuture.completedFuture(null))
                .when(kafkaEventService).unlockProducts(mockProducts);
        assertDoesNotThrow(() -> orderService.cancelPayment(userId));

        verify(kafkaEventService, times(1)).getCartAndSetAsOrderProcessing(userId, false);
        verify(kafkaEventService, times(1)).unlockProducts(mockProducts);
    }

    @Test
    void getUserOrders_Success() {
        String userId = "123";
        List<Order> mockOrders = List.of(new Order());
        when(orderRepository.findAllByUserId(userId)).thenReturn(mockOrders);

        List<OrderBaseInfoDto> result = orderService.getUserOrders(userId);

        assertNotEquals(0, result.size());
        assertEquals(mockOrders.size(), result.size());
        verify(orderRepository, times(1)).findAllByUserId(userId);
    }

    @Test
    void getOrderById_Success() {
        CustomerDto customer = new CustomerDto();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setShippingAddress(new ShippingAddress());

        List<ProductOrderDto> productDtos = List.of(new ProductOrderDto());

        when(orderRepository.findByIdAndUserId(1L, "123")).thenReturn(Optional.of(testOrder));
        when(kafkaEventService.optCustomer("123")).thenReturn(CompletableFuture.completedFuture(customer));
        when(kafkaEventService.getProductsByIds(anyList())).thenReturn(CompletableFuture.completedFuture(productDtos));
        when(orderMapper.mapToOrderDto(any(), any(), any(), any(), any())).thenReturn(new OrderDto());

        OrderDto result = orderService.getOrderById("123", "1", "email@test.com");

        assertNotNull(result);
        verify(orderRepository).findByIdAndUserId(1L, "123");
    }

    @Test
    void cancelOrder_asAdmin_Success() {
        testOrder.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(kafkaEventService.unlockProducts(anyMap())).thenReturn(CompletableFuture.completedFuture(null));

        assertDoesNotThrow(() -> orderService.cancelOrder("1", "ROLE_ADMIN", "any"));

        verify(orderRepository).save(testOrder);
        assertEquals(OrderStatus.ANNULLED, testOrder.getStatus());
    }

    @Test
    void refundOrder_Success() {
        testOrder.setStatus(OrderStatus.DELIVERED);
        testOrder.setOrderDate(Date.from(Instant.now().minusSeconds(10_000)));

        when(orderRepository.findByIdAndUserId(1L, "123")).thenReturn(Optional.of(testOrder));

        assertDoesNotThrow(() -> orderService.refundOrder("1", "123"));

        verify(orderRepository).save(testOrder);
        assertEquals(OrderStatus.REFUNDED, testOrder.getStatus());
    }

    @Test
    void changeOrderStatus_ShouldUpdateStatusAndSendEmail() {
        testOrder.setStatus(OrderStatus.SHIPPING);
        testOrder.setEmailSent(false);
        testOrder.setLastModified(new Date(System.currentTimeMillis() - OrderService.ORDER_CHANGE.toMillis() - 10000));

        when(orderRepository.findAllByStatusIsNotAndLastModifiedBefore(any(), any())).thenReturn(List.of(testOrder));

        orderService.changeOrderStatus();

        verify(orderRepository).saveAll(anyList());
    }

    @Test
    void deleteNotPaidOrders_ShouldDeleteOrders() {
        testOrder.setOrderDate(new Date(System.currentTimeMillis() - OrderService.ORDER_DELETE.toMillis() - 10000));

        when(orderRepository.findAllByStatusAndOrderDateBefore(eq(OrderStatus.CREATED), any())).thenReturn(List.of(testOrder));
        when(kafkaEventService.unlockProducts(anyMap())).thenReturn(CompletableFuture.completedFuture(null));

        assertDoesNotThrow(() -> orderService.deleteNotPaidOrders());

        verify(orderRepository).delete(testOrder);
    }
}