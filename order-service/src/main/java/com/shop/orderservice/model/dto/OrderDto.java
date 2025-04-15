package com.shop.orderservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shop.orderservice.model.DeliveryTime;
import com.shop.orderservice.model.OrderStatus;
import com.shop.orderservice.model.ShippingAddress;
import com.stripe.param.InvoiceUpdateParams;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private List<OrderProductDto> productsDto;
    private OrderStatus status;
    private ShippingAddress shippingAddress;
    private BigDecimal totalPrice;
    private Date deliveryDate;
    private Date orderDate;
    private DeliveryTime deliveryTime;
    private InvoiceUpdateParams.PaymentSettings.PaymentMethodType paymentMethod;
}

