package com.stockguard.inventory;
import com.stockguard.shared.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Component @ConditionalOnProperty(name="stockguard.events.enabled",havingValue="true")
public class InventoryEvents {
 public record Created(@NotNull UUID orderId,@NotBlank @Pattern(regexp="WH-(MUM|PUN|JAI)") String warehouse,@NotEmpty @Size(max=20) @Valid List<ReservationService.Line> items,@NotBlank @Size(max=120) String actor,@NotBlank @Pattern(regexp="ADMIN|WAREHOUSE_MANAGER") String role) {}
 private final EventStore events;private final ReservationService reservations;
 public InventoryEvents(EventStore events,ReservationService reservations) {this.events=events;this.reservations=reservations;}
 @KafkaListener(topics="stockguard.OrderCreated",groupId="inventory-reservations-v1") @Transactional(timeout=15)
 public void receive(String json) {
  var event=events.read(json);if(!event.eventType().equals("OrderCreated")) throw new IllegalArgumentException("Unexpected event type");
  var p=events.payload(event,Created.class);
  try {MDC.put("correlationId",event.correlationId());if(!events.claim(event)) return;
   var result=reservations.reserve(new ReservationService.Request(p.orderId(),p.warehouse(),p.items()),p.actor(),p.role(),event.correlationId());
   events.publish(result.reserved()?"InventoryReserved":"InventoryReservationFailed",p.orderId().toString(),event.correlationId(),result);
  } finally {MDC.remove("correlationId");}
 }
}
