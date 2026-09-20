package com.stockguard.inventory;
import com.stockguard.shared.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Service
public class ReservationService {
 public record Line(@NotNull UUID productId,@Min(1) @Max(100000) int quantity) {}
 public record Request(@NotNull UUID orderId,@NotBlank @Pattern(regexp="WH-(MUM|PUN|JAI)") String warehouse,@NotEmpty @Size(max=20) List<@Valid Line> items) {}
 public record Result(UUID orderId,boolean reserved,String reason) {}
 private final com.stockguard.shared.EventStore events;private final InventoryRepository inventory;private final InventoryService service;private final JdbcTemplate jdbc;
 public ReservationService(InventoryRepository inventory,InventoryService service,JdbcTemplate jdbc,com.stockguard.shared.EventStore events) {this.inventory=inventory;this.service=service;this.jdbc=jdbc;this.events=events;}
 @Transactional(timeout=10)
 public Result reserve(Request request,String actor,String role,String correlationId) {
  if(request.items().stream().map(Line::productId).distinct().count()!=request.items().size()) throw ApiException.conflict("Combine duplicate product lines");
  var lines=request.items().stream().sorted(Comparator.comparing(Line::productId)).toList();
  String hash=fingerprint(request.warehouse()+lines);
  // ON CONFLICT waits for a concurrent owner, then exposes its committed outcome.
  int claimed=jdbc.update("INSERT INTO reservations(order_id,request_hash,status,reason) VALUES (?,?,'PENDING','Processing') ON CONFLICT DO NOTHING",request.orderId(),hash);
  if(claimed==0) return jdbc.queryForObject("SELECT request_hash,status,reason FROM reservations WHERE order_id=?",(rs,n)->{
   if(!hash.equals(rs.getString(1))) throw ApiException.conflict("Order ID was already used with different reservation details");
   return new Result(request.orderId(),rs.getString(2).equals("RESERVED"),rs.getString(3));
  },request.orderId());
  UUID warehouse=service.warehouse(request.warehouse());
  List<Inventory> locked=new ArrayList<>();
  for(var line:lines) {
   var row=inventory.lockStock(line.productId(),warehouse);
   if(row.isEmpty()||row.get().availableQuantity<line.quantity()) return finish(request.orderId(),false,"Insufficient inventory");
   locked.add(row.get());
  }
  for(int n=0;n<lines.size();n++) {
   var row=locked.get(n);int before=row.availableQuantity;
   row.availableQuantity-=lines.get(n).quantity();row.reservedQuantity+=lines.get(n).quantity();row.updatedAt=Instant.now();
   if(row.availableQuantity<=row.reorderPoint) events.publish("LowStockDetected",row.productId.toString(),correlationId,Map.of("productId",row.productId,"warehouse",request.warehouse(),"availableQuantity",row.availableQuantity,"reorderPoint",row.reorderPoint));
   jdbc.update("INSERT INTO inventory_audit VALUES (?,?,?,now(),?,?,?,?,?,?,?)",UUID.randomUUID(),actor,role,"RESERVE",row.productId,row.warehouseId,before,row.availableQuantity,correlationId,"Order "+request.orderId());
  }
  inventory.flush();
  return finish(request.orderId(),true,"Inventory reserved");
 }
 private Result finish(UUID orderId,boolean success,String reason) {
  jdbc.update("UPDATE reservations SET status=?,reason=? WHERE order_id=?",success?"RESERVED":"REJECTED",reason,orderId);return new Result(orderId,success,reason);
 }
 private static String fingerprint(String text) {
  try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));} catch(java.security.NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
 }
}
