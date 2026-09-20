package com.stockguard.inventory;
import com.stockguard.shared.*;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;
@Service
public class AuditService {
 public record Adjustment(@NotBlank @Pattern(regexp="WH-(MUM|PUN|JAI)") String warehouse,@Min(0) @Max(1000000) int newQuantity,@Min(0) long expectedVersion,@NotBlank @Size(min=5,max=300) String reason) {}
 private final InventoryRepository repo;private final InventoryService inventory;private final JdbcTemplate jdbc;private final EventStore events;
 public AuditService(InventoryRepository repo,InventoryService inventory,JdbcTemplate jdbc,EventStore events) {this.repo=repo;this.inventory=inventory;this.jdbc=jdbc;this.events=events;}
 @Transactional public InventoryService.View adjust(UUID productId,Adjustment in,String actor,String role,String correlationId) {
  var row=repo.lockStock(productId,inventory.warehouse(in.warehouse())).orElseThrow(()->ApiException.missing("Inventory not found"));
  if(row.version!=in.expectedVersion()) throw ApiException.conflict("Stock changed. Refresh before adjusting.");
  int before=row.availableQuantity;row.availableQuantity=in.newQuantity();row.updatedAt=Instant.now();
  jdbc.update("INSERT INTO inventory_audit VALUES (?,?,?,now(),?,?,?,?,?,?,?)",UUID.randomUUID(),actor,role,"ADJUST",row.productId,row.warehouseId,before,row.availableQuantity,correlationId,in.reason());
  if(row.availableQuantity<=row.reorderPoint) events.publish("LowStockDetected",productId.toString(),correlationId,Map.of("productId",productId,"warehouse",in.warehouse(),"availableQuantity",row.availableQuantity,"reorderPoint",row.reorderPoint));
  repo.flush();return InventoryService.view(row);
 }
 public List<Map<String,Object>> history(UUID productId,String warehouse,int days,boolean adjustmentsOnly) {
  String sql="SELECT a.id,a.actor,a.role,a.timestamp,a.operation,a.product_id AS \"productId\",w.code AS warehouse,a.previous_quantity AS \"previousQuantity\",a.new_quantity AS \"newQuantity\",a.correlation_id AS \"correlationId\",a.reason FROM inventory_audit a JOIN warehouses w ON a.warehouse_id=w.id WHERE a.timestamp>=?";
  List<Object> args=new ArrayList<>();args.add(java.sql.Timestamp.from(Instant.now().minusSeconds(days*86400L)));
  if(productId!=null) {sql+=" AND a.product_id=?";args.add(productId);}if(warehouse!=null) {sql+=" AND w.code=?";args.add(warehouse);}if(adjustmentsOnly) sql+=" AND a.operation='ADJUST'";
  return jdbc.queryForList(sql+" ORDER BY a.timestamp DESC LIMIT 200",args.toArray());
 }
}
