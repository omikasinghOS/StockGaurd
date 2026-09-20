package com.stockguard.inventory;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="inventory")
public class Inventory {
 @Id public UUID id; public UUID productId; public UUID warehouseId;
 public int availableQuantity; public int reservedQuantity; public int reorderPoint; public int safetyStock;
 @Version public long version; public Instant updatedAt;
 protected Inventory() {}
}
