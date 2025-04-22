package com.shop.orderservice.model.entity;

import com.shop.orderservice.model.OrderStatus;
import com.shop.orderservice.model.ProductInOrder;
import com.shop.orderservice.model.ShippingAddress;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Builder
@Entity(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = {"products"})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull(message = "User id is required.")
    private String userId;
    @NotNull(message = "Products are required.")
    @ElementCollection
    @Builder.Default
    @CollectionTable(name = "order_products", joinColumns = @JoinColumn(name = "order_id"))
    private List<ProductInOrder> products = new ArrayList<>();
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;
    @Embedded
    private ShippingAddress orderAddress;
    @Builder.Default
    private Date orderDate = Date.from(java.time.Instant.now());
    @Builder.Default
    private Date lastModified = Date.from(java.time.Instant.now());
    private Date deliveryDate;
    private String paymentMethod;
    private BigDecimal totalPrice;
    private String sessionId;
    @Builder.Default
    private boolean emailSent = false;

    public Order(List<ProductInOrder> products, String userId, ShippingAddress address, BigDecimal price, String paymentMethod) {
        this.products = products;
        this.userId = userId;
        this.orderAddress = address;
        this.totalPrice = price;
        this.paymentMethod = paymentMethod;
        this.status = OrderStatus.CREATED;
    }

    public void setNewStatus(OrderStatus status) {
        this.status = status;
        this.lastModified = Date.from(java.time.Instant.now());
    }

    public void setNextStatus() {
        int random = (int) (Math.random() * 13.0) + 1;
        switch (this.status) {
            case PROCESSING -> this.setNewStatus(random == 13 ? OrderStatus.ANNULLED : OrderStatus.SHIPPING);
            case SHIPPING -> this.setNewStatus(random == 13 ? OrderStatus.ANNULLED : OrderStatus.DELIVERED);
            case DELIVERED -> {
                if (random == 13) {
                    this.setNewStatus(OrderStatus.REFUNDED);
                }
                this.setDeliveryDate(Date.from(java.time.Instant.now()));
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id != null && id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return id == null ?  0 : id.hashCode();
    }

}
