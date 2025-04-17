package com.shop.productservice.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Document(collection = "products")
@Data
public class Product {

    @Id
    private String id;
    @Indexed
    private String name;
    @Indexed
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;
    private int amountLeft;
    private String description;
    private String imageUrl;
    private boolean available = true;

    /**
     * integer - amount of user who rated
     * double - rating of product
     */
    private Map<Integer, Double> ratings;
    private int orders;
    private List<Category> categories;
}
