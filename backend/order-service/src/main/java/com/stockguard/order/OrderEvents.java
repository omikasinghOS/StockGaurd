package com.stockguard.order;
import com.stockguard.shared.*;
import jakarta.validation.constraints.*;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Component @ConditionalOnProperty(name="stockguard.events.enabled",havingValue="true")
public class OrderEvents {
 public record Outcome(@NotNull UUID orderId,boolean reserved,@NotBlank String reason) {}
 private final OrderRepository orders;private final EventStore events;
 public OrderEvents(OrderRepository orders,EventStore events) {this.orders=orders;this.events=events;}
 @KafkaListener(topics={"stockguard.InventoryReserved","stockguard.InventoryReservationFailed"},groupId="order-results-v1") @Transactional
 public void receive(String json) {
  var event=events.read(json);var result=events.payload(event,Outcome.class);
  if(!event.eventType().equals(result.reserved()?"InventoryReserved":"InventoryReservationFailed")) throw new IllegalArgumentException("Contradictory outcome");
  try {MDC.put("correlationId",event.correlationId());if(!events.claim(event)) return;
   var o=orders.lockById(result.orderId()).orElseThrow(()->new IllegalArgumentException("Unknown order"));
   if(o.status!=PurchaseOrder.Status.PENDING) return;
   if(result.reserved()) {o.status=PurchaseOrder.Status.INVENTORY_RESERVED;orders.flush();o.status=PurchaseOrder.Status.CONFIRMED;}
   else o.status=PurchaseOrder.Status.REJECTED;
  } finally {MDC.remove("correlationId");}
 }
}
