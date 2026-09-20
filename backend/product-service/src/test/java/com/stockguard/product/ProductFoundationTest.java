package com.stockguard.product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties="security.jwt-secret=test-only-signing-key-at-least-32-characters") @AutoConfigureMockMvc @Testcontainers
@org.springframework.security.test.context.support.WithMockUser(username="admin",roles="ADMIN")
class ProductFoundationTest {
 @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @Autowired MockMvc mvc;
 @Test void healthAndSeededRead() throws Exception {
  mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  mvc.perform(get("/products")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").exists());
 }
 @Autowired ProductService service;
 @Test void catalogCreatesAndPreservesUniqueSku() {
  var in=new ProductService.Input("TEST-001","Test Sensor","Sensors",new java.math.BigDecimal("12.50"),"Test Supplier");
  var p=service.save(null,in);assertThat(service.get(p.id()).name()).isEqualTo("Test Sensor");
  assertThatThrownBy(()->service.save(null,in)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
 }
 @Test void validatesPrice() throws Exception {
  mvc.perform(post("/products").contentType("application/json").content("{\"sku\":\"BAD-001\",\"name\":\"Bad\",\"category\":\"Sensors\",\"price\":-1,\"supplier\":\"Supplier\"}")).andExpect(status().isBadRequest());
 }
}
