package com.stockguard.inventory;
import com.stockguard.shared.ApiException;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.Instant;
@Service
public class InventoryService {
 public record View(UUID id,UUID productId,UUID warehouseId,int availableQuantity,int reservedQuantity,int reorderPoint,int safetyStock,long version,Instant updatedAt) {}
 public record WarehouseView(UUID id,String code,String name,String city) {}
 private final InventoryRepository repo; private final WarehouseRepository warehouses;
 public InventoryService(InventoryRepository repo,WarehouseRepository warehouses) {this.repo=repo;this.warehouses=warehouses;}
 static View view(Inventory i) {return new View(i.id,i.productId,i.warehouseId,i.availableQuantity,i.reservedQuantity,i.reorderPoint,i.safetyStock,i.version,i.updatedAt);}
 public UUID warehouse(String code) {return warehouses.findByCode(code).orElseThrow(()->ApiException.missing("Warehouse not found")).id;}
 public List<View> list(String warehouse,boolean low) {return (warehouse==null?repo.findAll():repo.findByWarehouseId(warehouse(warehouse))).stream().filter(i->!low||i.availableQuantity<=i.reorderPoint).map(InventoryService::view).toList();}
 public View get(UUID productId,String warehouse) {return view(repo.findByProductIdAndWarehouseId(productId,warehouse(warehouse)).orElseThrow(()->ApiException.missing("Inventory not found")));}
 public List<WarehouseView> warehouses() {return warehouses.findAll().stream().map(w->new WarehouseView(w.id,w.code,w.name,w.city)).toList();}
}
