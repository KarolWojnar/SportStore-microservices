package com.shop.paymentservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shop.paymentservice.model.DeliveryTime;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderDto {
    private String id;
    private String email;
    private BigDecimal totalPrice;
    private DeliveryTime deliveryTime;
    private SessionCreateParams.PaymentMethodType paymentMethod;
}
