package com.stockguard.order;
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
@SpringBootTest(properties={"stockguard.events.enabled=false","security.jwt-secret=test-only-signing-key-at-least-32-characters"}) @AutoConfigureMockMvc @Testcontainers
@org.springframework.security.test.context.support.WithMockUser(username="admin",roles="ADMIN")
class OrderFoundationTest {
 @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @Autowired MockMvc mvc;
 @Test void healthAndSeededRead() throws Exception {
  mvc.perform(get("/actuator/health")).andExpect(status().isOk());
  mvc.perform(get("/orders")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").exists());
 }
 
}
