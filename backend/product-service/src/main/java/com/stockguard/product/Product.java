package com.stockguard.product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="products")
public class Product {
 @Id public UUID id;
 @Column(nullable=false,unique=true) public String sku;
 public String name; public String category; public BigDecimal price; public String supplier;
 public Instant createdAt; public Instant updatedAt;
 protected Product() {}
}
