package com.shop.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderSentEmailDto {
    private String firstName;
    private String lastName;
    private String email;
    private String orderId;
    private Date orderDate;
    private String address;
    private String zipCode;
    private String city;
    private String country;
    private BigDecimal totalPrice;
    private List<ProductInfoEmail> products;
}
