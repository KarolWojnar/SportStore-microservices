package com.shop.paymentservice.model.dto;

import com.shop.paymentservice.model.ShippingAddress;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderBaseInfo {
    private Map<String, Integer> products;
    private String userId;
    private ShippingAddress shippingAddress;
    private BigDecimal totalPrice;
    private SessionCreateParams.PaymentMethodType paymentMethod;
}
