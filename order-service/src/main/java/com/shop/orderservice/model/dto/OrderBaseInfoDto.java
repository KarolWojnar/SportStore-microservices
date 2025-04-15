package com.shop.orderservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shop.orderservice.model.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderBaseInfoDto {
    private Long id;
    private Date orderDate;
    private Date deliveryDate;
    private String status;
    private BigDecimal totalPrice;

    public static OrderBaseInfoDto mapToDto(Order order) {
        return OrderBaseInfoDto.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate() == null ? null : order.getOrderDate())
                .deliveryDate(order.getDeliveryDate() == null ? null : order.getDeliveryDate())
                .status(order.getStatus().toString())
                .totalPrice(order.getTotalPrice())
                .build();
    }
}
