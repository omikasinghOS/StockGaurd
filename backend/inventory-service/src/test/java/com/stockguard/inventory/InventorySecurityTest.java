package com.stockguard.inventory;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
@SpringBootTest(properties={"stockguard.events.enabled=false","security.jwt-secret=test-only-signing-key-at-least-32-characters"}) @AutoConfigureMockMvc @Testcontainers
class InventorySecurityTest {
 @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @Autowired MockMvc mvc;
 static final String URL="/inventory/00000000-0000-0000-0000-000000000102/adjustments";
 static final String BODY="{\"warehouse\":\"WH-PUN\",\"newQuantity\":12,\"expectedVersion\":0,\"reason\":\"Verified cycle count\"}";
 @Test void anonymousAndForgedTokenRejected() throws Exception {
  mvc.perform(get("/inventory")).andExpect(status().isUnauthorized());
  mvc.perform(get("/inventory").header("Authorization","Bearer forged-token")).andExpect(status().isUnauthorized());
 }
 @Test void viewerCannotWriteOrReadAudit() throws Exception {
  mvc.perform(get("/inventory").with(user("viewer").roles("VIEWER"))).andExpect(status().isOk());
  mvc.perform(post(URL).with(user("viewer").roles("VIEWER")).contentType("application/json").content(BODY)).andExpect(status().isForbidden());
  mvc.perform(get("/audit").with(user("viewer").roles("VIEWER"))).andExpect(status().isForbidden());
 }
 @Test void managerAdjustmentAuditedAndStaleVersionRejected() throws Exception {
  mvc.perform(post(URL).with(user("manager").roles("WAREHOUSE_MANAGER")).header("X-Correlation-ID","security-test").contentType("application/json").content(BODY)).andExpect(status().isOk()).andExpect(jsonPath("$.availableQuantity").value(12));
  mvc.perform(get("/audit?productId=00000000-0000-0000-0000-000000000102").with(user("manager").roles("WAREHOUSE_MANAGER"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].actor").value("manager")).andExpect(jsonPath("$[0].correlationId").value("security-test"));
  mvc.perform(post(URL).with(user("manager").roles("WAREHOUSE_MANAGER")).contentType("application/json").content(BODY)).andExpect(status().isConflict());
 }
}
