package com.stockguard.product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/products") @Validated
public class ProductController {
 private final ProductService service;
 public ProductController(ProductService service) {this.service=service;}
 @GetMapping public List<ProductService.View> list(@RequestParam(defaultValue="0") @Min(0) int page) {return service.list(page);}
 @GetMapping("/{id}") public ProductService.View get(@PathVariable UUID id) {return service.get(id);}
 @GetMapping("/sku/{sku}") public ProductService.View sku(@PathVariable String sku) {return service.sku(sku);}
 @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public ProductService.View create(@Valid @RequestBody ProductService.Input in) {return service.save(null,in);}
 @PutMapping("/{id}") public ProductService.View update(@PathVariable UUID id,@Valid @RequestBody ProductService.Input in) {return service.save(id,in);}
}
