package com.shop.orderservice.service;

import com.shop.orderservice.exception.OrderException;
import com.shop.orderservice.model.OrderStatus;
import com.shop.orderservice.model.ProductInOrder;
import com.shop.orderservice.model.ShippingAddress;
import com.shop.orderservice.model.dto.*;
import com.shop.orderservice.model.entity.Order;
import com.shop.orderservice.repository.OrderRepository;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Spy
    @InjectMocks
    private KafkaEventService kafkaEventService;

    private Order testOrder;
    private CustomerDto testCustomer;
    private Map<String, Integer> testProducts;
    private String correlationId;
    private final ShippingAddress orderAddress = new ShippingAddress("123 Main St", "Anytown", "USA", "12345");

    @BeforeEach
    void setUp() {
        correlationId = UUID.randomUUID().toString();

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId("123");
        testOrder.setOrderAddress(orderAddress);
        testOrder.setSessionId("session-123");
        testOrder.setTotalPrice(new BigDecimal("99.99"));
        testOrder.setPaymentMethod("CARD");

        List<ProductInOrder> products = new ArrayList<>();
        products.add(new ProductInOrder("product-1", 2, new BigDecimal("19.99")));
        products.add(new ProductInOrder("product-2", 1, new BigDecimal("59.99")));
        testOrder.setProducts(products);

        testCustomer = new CustomerDto();
        testCustomer.setFirstName("John");
        testCustomer.setLastName("Doe");

        testProducts = new HashMap<>();
        testProducts.put("product-1", 2);
        testProducts.put("product-2", 1);
    }


    @Test
    void optCustomer_shouldReturnCustomerDto() {
        String userId = "123";
        doReturn(null).when(kafkaTemplate).send(anyString(), any(Object.class));

        CompletableFuture<CustomerDto> future = kafkaEventService.optCustomer(userId);

        Map<String, CompletableFuture<CustomerDto>> requests = new HashMap<>();
        requests.put(correlationId, future);
        ReflectionTestUtils.setField(kafkaEventService, "requestsForCustomer", requests);

        CustomerInfoResponse response = new CustomerInfoResponse();
        response.setCorrelationId(correlationId);
        response.setCustomer(testCustomer);
        kafkaEventService.customerInfoResponse(response);

        verify(kafkaTemplate).send(eq("customer-info-request"), any(CustomerInfoRequest.class));
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());

        try {
            CustomerDto result = future.get();
            assertEquals("John", result.getFirstName());
            assertEquals("Doe", result.getLastName());
        } catch (Exception e) {
            fail("Future should complete normally: " + e.getMessage());
        }
    }

    @Test
    void getCartAndSetAsOrderProcessing_shouldReturnProductMap() {
        String userId = "123";
        doReturn(null).when(kafkaTemplate).send(anyString(), any(Object.class));

        CompletableFuture<Map<String, Integer>> future = kafkaEventService.getCartAndSetAsOrderProcessing(userId, true);

        Map<String, CompletableFuture<Map<String, Integer>>> requests = new HashMap<>();
        requests.put(correlationId, future);
        ReflectionTestUtils.setField(kafkaEventService, "requestsForCart", requests);
        ProductsInCartInfoResponse response = new ProductsInCartInfoResponse();
        response.setCorrelationId(correlationId);
        response.setProduct(testProducts);
        kafkaEventService.cartResponse(response);

        verify(kafkaTemplate).send(eq("cart-product-block-request"), any(ProductsInCartInfoRequest.class));
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());

        try {
            Map<String, Integer> result = future.get();
            assertEquals(2, result.size());
            assertEquals(2, result.get("product-1"));
            assertEquals(1, result.get("product-2"));
        } catch (Exception e) {
            fail("Future should complete normally: " + e.getMessage());
        }
    }

    @Test
    void getTotalPriceOfCart_shouldReturnTotalPrice() {
        doReturn(null).when(kafkaTemplate).send(anyString(), any(Object.class));

        CompletableFuture<BigDecimal> future = kafkaEventService.getTotalPriceOfCart(testProducts);

        Map<String, CompletableFuture<BigDecimal>> requests = new HashMap<>();
        requests.put(correlationId, future);
        ReflectionTestUtils.setField(kafkaEventService, "requestsForTotalPrice", requests);

        TotalPriceOfProductsResponse response = new TotalPriceOfProductsResponse();
        response.setCorrelationId(correlationId);
        response.setTotalPrice(new BigDecimal("99.99"));
        kafkaEventService.totalPriceResponse(response);

        verify(kafkaTemplate).send(eq("total-price-request"), any(TotalPriceOfProductsRequest.class));
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());

        try {
            BigDecimal result = future.get();
            assertEquals(new BigDecimal("99.99"), result);
        } catch (Exception e) {
            fail("Future should complete normally: " + e.getMessage());
        }
    }

    @Test
    void totalPriceResponse_shouldCompleteExceptionally_whenErrorMessageIsProvided() {
        CompletableFuture<BigDecimal> future = new CompletableFuture<>();
        Map<String, CompletableFuture<BigDecimal>> requests = new HashMap<>();
        requests.put(correlationId, future);
        ReflectionTestUtils.setField(kafkaEventService, "requestsForTotalPrice", requests);

        TotalPriceOfProductsResponse response = new TotalPriceOfProductsResponse();
        response.setCorrelationId(correlationId);
        response.setErrorMessage("Products not found");
        kafkaEventService.totalPriceResponse(response);

        assertTrue(future.isCompletedExceptionally());

        try {
            future.get();
            fail("Should have thrown an exception");
        } catch (Exception e) {
            assertInstanceOf(OrderException.class, e.getCause());
            assertEquals("Products not found", e.getCause().getMessage());
        }
    }

    @Test
    void unlockProducts_shouldCompleteSuccessfully() {
        doReturn(null).when(kafkaTemplate).send(anyString(), any(Object.class));

        CompletableFuture<Void> future = kafkaEventService.unlockProducts(testProducts);

        Map<String, CompletableFuture<Void>> requests = new HashMap<>();
        requests.put(correlationId, future);
        ReflectionTestUtils.setField(kafkaEventService, "requestsForUnlockProducts", requests);

        TotalPriceOfProductsResponse response = new TotalPriceOfProductsResponse();
        response.setCorrelationId(correlationId);
        kafkaEventService.unlockProductsResponse(response);

        verify(kafkaTemplate).send(eq("order-product-unlock-request"), any(TotalPriceOfProductsRequest.class));
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    void orderCreateResponse_shouldCreateOrderAndSendResponse() {
        OrderBaseInfo orderBaseInfo = new OrderBaseInfo();
        orderBaseInfo.setUserId("123");
        ShippingAddress shippingAddress = new ShippingAddress("Street 77", "Los Santos", "USA", "99-123");

        orderBaseInfo.setShippingAddress(shippingAddress);
        orderBaseInfo.setPaymentMethod(SessionCreateParams.PaymentMethodType.CARD);
        orderBaseInfo.setProducts(testProducts);
        orderBaseInfo.setTotalPrice(new BigDecimal("9999"));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCorrelationId("test-correlation-id");
        request.setOrderBaseInfo(orderBaseInfo);

        Map<String, BigDecimal> productPrices = new HashMap<>();
        productPrices.put("product-1", new BigDecimal("19.99"));
        productPrices.put("product-2", new BigDecimal("59.99"));

        CompletableFuture<Map<String, BigDecimal>> future = CompletableFuture.completedFuture(productPrices);
        doReturn(future).when(kafkaEventService).getPriceOfProducts(anySet());

        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        kafkaEventService.orderCreateResponse(request);

        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order-create-response"), any(CreateOrderResponse.class));
    }

    @Test
    void setSessionIdForOrder_shouldUpdateOrderWithSessionId() {
        OrderSessionRequest request = new OrderSessionRequest();
        request.setOrderId("1");
        request.setSessionId("new-session-123");
        request.setCorrelationId("test-correlation-id");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        kafkaEventService.setSessionIdForOrder(request);

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);
        assertEquals("new-session-123", testOrder.getSessionId());
        verify(kafkaTemplate).send(eq("order-session-response"), eq("test-correlation-id"));
    }

    @Test
    void orderPaid_shouldUpdateOrderStatusAndSendEmail() {
        String sessionId = "session-123";

        when(orderRepository.findBySessionId(sessionId)).thenReturn(Optional.of(testOrder));

        doAnswer(invocation -> {
            CompletableFuture<CustomerDto> future = new CompletableFuture<>();
            future.complete(testCustomer);
            return future;
        }).when(kafkaEventService).optCustomer(anyString());

        doAnswer(invocation -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.complete("john.doe@example.com");
            return future;
        }).when(kafkaEventService).getUserEmail(anyString());

        kafkaEventService.orderSessionIdAndSetAsProcessing(sessionId);

        verify(orderRepository).findBySessionId(sessionId);
        verify(orderRepository).save(testOrder);
        assertEquals(OrderStatus.PROCESSING, testOrder.getStatus());
        verify(kafkaTemplate).send(eq("order-sent-request"), any(OrderSentEmailDto.class));
        verify(kafkaTemplate).send(eq("product-sold-request"), any(Map.class));
    }

    @Test
    void sendOrderDeliveredEmail_shouldSendEmail() {
        List<ProductOrderDto> productDtos = new ArrayList<>();
        ProductOrderDto productDto1 = new ProductOrderDto();
        productDto1.setId("product-1");
        productDto1.setName("Product One");
        productDtos.add(productDto1);

        ProductOrderDto productDto2 = new ProductOrderDto();
        productDto2.setId("product-2");
        productDto2.setName("Product Two");
        productDtos.add(productDto2);

        doAnswer(invocation -> {
            CompletableFuture<CustomerDto> future = new CompletableFuture<>();
            future.complete(testCustomer);
            return future;
        }).when(kafkaEventService).optCustomer(anyString());

        doAnswer(invocation -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.complete("john.doe@example.com");
            return future;
        }).when(kafkaEventService).getUserEmail(anyString());

        doAnswer(invocation -> {
            CompletableFuture<List<ProductOrderDto>> future = new CompletableFuture<>();
            future.complete(productDtos);
            return future;
        }).when(kafkaEventService).getProductsByIds(anyList());

        kafkaEventService.sendOrderDeliveredEmail(testOrder);

        verify(kafkaTemplate).send(eq("order-delivered-request"), any(OrderSentEmailDto.class));
    }

    @Test
    void orderProductRated_shouldMarkProductAsRated() {
        OrderProductRatedRequest request = new OrderProductRatedRequest();
        request.setOrderId("1");
        request.setProductId("product-1");
        request.setCorrelationId("test-correlation-id");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        kafkaEventService.orderProductRatedEvent(request);

        verify(orderRepository).findById(1L);
        verify(orderRepository).save(testOrder);

        boolean isRated = testOrder.getProducts().stream()
                .filter(p -> p.getProductId().equals("product-1"))
                .findFirst()
                .map(ProductInOrder::isRated)
                .orElse(false);

        assertTrue(isRated);
        verify(kafkaTemplate).send(eq("order-product-rated-response"), eq("test-correlation-id"));
    }

    @Test
    void orderInfoResponse_shouldReturnOrderInfo() {
        OrderRepaymentRequest request = new OrderRepaymentRequest();
        request.setOrderId("1");
        request.setCorrelationId("test-correlation-id");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        kafkaEventService.orderInfoResponse(request);

        verify(orderRepository).findById(1L);
        verify(kafkaTemplate).send(eq("order-info-response"), any(OrderRepaymentResponse.class));
    }

    @Test
    void orderInfoResponse_shouldHandleOrderNotFound() {
        OrderRepaymentRequest request = new OrderRepaymentRequest();
        request.setOrderId("999");
        request.setCorrelationId("test-correlation-id");

        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        kafkaEventService.orderInfoResponse(request);

        verify(orderRepository).findById(999L);

        ArgumentCaptor<OrderRepaymentResponse> responseCaptor = ArgumentCaptor.forClass(OrderRepaymentResponse.class);
        verify(kafkaTemplate).send(eq("order-info-response"), responseCaptor.capture());

        OrderRepaymentResponse response = responseCaptor.getValue();
        assertEquals("Order not found.", response.getErrorMessage());
        assertNull(response.getOrderInfoRepayment());
    }
}