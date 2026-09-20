package com.stockguard.inventory;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface InventoryRepository extends JpaRepository<Inventory,UUID> {
 List<Inventory> findByWarehouseId(UUID warehouseId);
 Optional<Inventory> findByProductIdAndWarehouseId(UUID productId,UUID warehouseId);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select i from Inventory i where i.productId = :productId and i.warehouseId = :warehouseId")
 Optional<Inventory> lockStock(UUID productId,UUID warehouseId);
}
