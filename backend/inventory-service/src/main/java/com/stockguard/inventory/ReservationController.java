package com.stockguard.inventory;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/inventory/reservations")
public class ReservationController {
 private final ReservationService service;
 public ReservationController(ReservationService service) {this.service=service;}
 @PostMapping public org.springframework.http.ResponseEntity<ReservationService.Result> reserve(@Valid @RequestBody ReservationService.Request request,org.springframework.security.core.Authentication auth) {
  var result=service.reserve(request,auth.getName(),auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_",""),MDC.get("correlationId"));
  return org.springframework.http.ResponseEntity.status(result.reserved()?200:409).body(result);
 }
}
