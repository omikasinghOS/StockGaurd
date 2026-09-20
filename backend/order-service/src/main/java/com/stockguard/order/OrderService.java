package com.stockguard.order;
import com.stockguard.shared.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
@Service
public class OrderService {
 public record Line(@NotNull UUID productId,@Min(1) @Max(100000) int quantity) {}
 public record Input(@NotBlank @Pattern(regexp="WH-(MUM|PUN|JAI)") String warehouse,@NotEmpty @Size(max=20) List<@Valid Line> items) {}
 public record ItemView(UUID productId,int quantity,BigDecimal unitPrice) {}
 public record View(UUID id,String customerId,String status,BigDecimal totalAmount,Instant createdAt,String warehouse,List<ItemView> items) {}
 public record ProductPrice(UUID id,String sku,String name,String category,BigDecimal price,String supplier,Instant createdAt,Instant updatedAt) {}
 private final com.stockguard.shared.EventStore events;private final OrderRepository repo; private final RestClient products;
 public OrderService(OrderRepository repo,RestClient.Builder builder,@Value("${product.url}") String url,com.stockguard.shared.EventStore events) {this.events=events;this.repo=repo;this.products=builder.requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build())).baseUrl(url).build();}
 static View view(PurchaseOrder o) {return new View(o.id,o.customerId,o.status.name(),o.totalAmount,o.createdAt,o.warehouse,o.items.stream().map(i->new ItemView(i.productId,i.quantity,i.unitPrice)).toList());}
 public List<View> list() {return repo.findAll(PageRequest.of(0,100,Sort.by(Sort.Direction.DESC,"createdAt"))).stream().map(OrderService::view).toList();}
 public View get(UUID id) {return view(repo.findById(id).orElseThrow(()->ApiException.missing("Order not found")));}
 @Transactional public View create(Input in,UUID requestId,String user,String role,String authorization) {
  var existing=repo.findByRequestId(requestId); if(existing.isPresent()) {
   var o=existing.get();
   boolean same=o.customerId.equals(user)&&o.warehouse.equals(in.warehouse())&&o.items.size()==in.items().size()&&o.items.stream().allMatch(i->in.items().stream().anyMatch(j->j.productId().equals(i.productId)&&j.quantity()==i.quantity));
   if(!same) throw ApiException.conflict("Idempotency key already used for a different request");return view(o);
  }
  if(in.items().stream().map(Line::productId).distinct().count()!=in.items().size()) throw ApiException.conflict("Combine duplicate product lines");
  var o=new PurchaseOrder();o.id=UUID.randomUUID();o.requestId=requestId;o.customerId=user;o.status=PurchaseOrder.Status.PENDING;o.createdAt=Instant.now();o.warehouse=in.warehouse();o.totalAmount=BigDecimal.ZERO;
  for(var line:in.items()) {
   ProductPrice p=products.get().uri("/products/{id}",line.productId()).header("Authorization",authorization).retrieve().body(ProductPrice.class);
   if(p==null) throw ApiException.missing("Product unavailable");
   o.items.add(new PurchaseOrder.Item(line.productId(),line.quantity(),p.price()));o.totalAmount=o.totalAmount.add(p.price().multiply(BigDecimal.valueOf(line.quantity())));
  }
  repo.saveAndFlush(o);
  String correlationId=org.slf4j.MDC.get("correlationId");if(correlationId==null) correlationId=UUID.randomUUID().toString();
  events.publish("OrderCreated",o.id.toString(),correlationId,Map.of("orderId",o.id,"warehouse",o.warehouse,"items",in.items(),"actor",user,"role",role));
  return view(o);
 }
}
