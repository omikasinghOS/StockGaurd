package com.stockguard.product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProductRepository extends JpaRepository<Product,UUID> { Optional<Product> findBySku(String sku); }
