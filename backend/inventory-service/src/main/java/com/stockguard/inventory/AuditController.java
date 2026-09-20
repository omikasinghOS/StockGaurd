package com.stockguard.inventory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @Validated
public class AuditController {
 private final AuditService service;
 public AuditController(AuditService service) {this.service=service;}
 @GetMapping("/audit") public List<Map<String,Object>> history(@RequestParam(required=false) UUID productId,@RequestParam(required=false) String warehouse,@RequestParam(defaultValue="30") @Min(1) @Max(90) int days,@RequestParam(defaultValue="false") boolean adjustmentsOnly) {return service.history(productId,warehouse,days,adjustmentsOnly);}
 @PostMapping("/inventory/{productId}/adjustments") public InventoryService.View adjust(@PathVariable UUID productId,@Valid @RequestBody AuditService.Adjustment in,Authentication auth) {return service.adjust(productId,in,auth.getName(),auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_",""),MDC.get("correlationId"));}
}
