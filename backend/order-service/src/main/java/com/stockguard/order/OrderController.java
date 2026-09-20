package com.stockguard.order;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/orders")
public class OrderController {
 private final OrderService service;
 public OrderController(OrderService service) {this.service=service;}
 @GetMapping public List<OrderService.View> list() {return service.list();}
 @GetMapping("/{id}") public OrderService.View get(@PathVariable UUID id) {return service.get(id);}
 @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public OrderService.View create(@Valid @RequestBody OrderService.Input in,@RequestHeader("Idempotency-Key") UUID key,@RequestHeader(value="Authorization",defaultValue="") String authorization,org.springframework.security.core.Authentication auth) {return service.create(in,key,auth.getName(),auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_",""),authorization);}
}
