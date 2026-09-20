package com.stockguard.order;
import jakarta.persistence.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name="orders")
public class PurchaseOrder {
 public enum Status {PENDING,INVENTORY_RESERVED,CONFIRMED,REJECTED,CANCELLED}
 @Id public UUID id; public String customerId; @Enumerated(EnumType.STRING) public Status status;
 public BigDecimal totalAmount; public Instant createdAt; public String warehouse;
 @Column(unique=true) public UUID requestId;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="order_items",joinColumns=@JoinColumn(name="order_id")) public List<Item> items=new ArrayList<>();
 protected PurchaseOrder() {}
 @Embeddable public static class Item {
  public UUID productId; public int quantity; public BigDecimal unitPrice;
  protected Item() {} public Item(UUID productId,int quantity,BigDecimal unitPrice) {this.productId=productId;this.quantity=quantity;this.unitPrice=unitPrice;}
 }
}
