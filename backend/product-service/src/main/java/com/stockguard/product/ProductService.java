package com.stockguard.product;
import com.stockguard.shared.ApiException;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ProductService {
 public record Input(@NotBlank @Pattern(regexp="[A-Z0-9-]{3,40}") String sku,@NotBlank @Size(max=120) String name,@NotBlank @Size(max=80) String category,@NotNull @DecimalMin("0.01") @DecimalMax("10000000") @Digits(integer=8,fraction=2) BigDecimal price,@NotBlank @Size(max=120) String supplier) {}
 public record View(UUID id,String sku,String name,String category,BigDecimal price,String supplier,Instant createdAt,Instant updatedAt) {}
 private final ProductRepository repo;
 public ProductService(ProductRepository repo) { this.repo=repo; }
 static View view(Product p) { return new View(p.id,p.sku,p.name,p.category,p.price,p.supplier,p.createdAt,p.updatedAt); }
 public List<View> list(int page) { return repo.findAll(PageRequest.of(page,100,Sort.by("sku"))).stream().map(ProductService::view).toList(); }
 public View get(UUID id) { return view(repo.findById(id).orElseThrow(()->ApiException.missing("Product not found"))); }
 public View sku(String sku) { return view(repo.findBySku(sku).orElseThrow(()->ApiException.missing("SKU not found"))); }
 @Transactional public View save(UUID id,Input in) {
  Product p=id==null?new Product():repo.findById(id).orElseThrow(()->ApiException.missing("Product not found"));
  if(id==null) { p.id=UUID.randomUUID(); p.createdAt=Instant.now(); }
  if(id!=null && !p.sku.equals(in.sku())) throw ApiException.conflict("SKU is immutable");
  p.sku=in.sku();p.name=in.name();p.category=in.category();p.price=in.price();p.supplier=in.supplier();p.updatedAt=Instant.now();return view(repo.save(p));
 }
}
