package com.shop.paymentservice.model.dto;

import com.stripe.param.checkout.SessionCreateParams;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderInfoRepayment {
    private SessionCreateParams.PaymentMethodType paymentMethod;
    private BigDecimal totalPrice;
}
