package com.stockguard.inventory;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class InventoryController {
 private final InventoryService service;
 public InventoryController(InventoryService service) {this.service=service;}
 @GetMapping("/warehouses") public List<InventoryService.WarehouseView> warehouses() {return service.warehouses();}
 @GetMapping("/inventory") public List<InventoryService.View> list(@RequestParam(required=false) String warehouse,@RequestParam(defaultValue="false") boolean lowStock) {return service.list(warehouse,lowStock);}
 @GetMapping("/inventory/{productId}") public InventoryService.View get(@PathVariable UUID productId,@RequestParam String warehouse) {return service.get(productId,warehouse);}
}
