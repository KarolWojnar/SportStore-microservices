package com.shop.productservice.repository;

import com.shop.productservice.model.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByNameIn(List<String> names);
    Optional<Category> findByName(String name);
}
