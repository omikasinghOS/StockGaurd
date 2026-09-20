package com.stockguard.product;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/auth")
public class AuthController {
 public record Login(@NotBlank @Pattern(regexp="[a-zA-Z0-9._-]{1,80}") String username,@NotBlank @Size(max=72) String password) {}
 private final AuthService service;
 public AuthController(AuthService service) {this.service=service;}
 @PostMapping("/login") public AuthService.Session login(@Valid @RequestBody Login in) {return service.login(in.username(),in.password());}
 @GetMapping("/security/failed-logins") public List<Map<String,Object>> failures() {return service.failures();}
}
