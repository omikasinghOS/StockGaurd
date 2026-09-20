package com.stockguard.product;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties="security.jwt-secret=test-only-signing-key-at-least-32-characters") @AutoConfigureMockMvc @Testcontainers
class AuthSecurityTest {
 @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc;@Autowired ObjectMapper mapper;@Autowired JwtDecoder decoder;
 @BeforeEach void setup() {jdbc.update("INSERT INTO app_users VALUES (?,?,?) ON CONFLICT DO NOTHING","testviewer",new BCryptPasswordEncoder().encode("test-password-only"),"VIEWER");}
 @Test void loginIssuesVerifiedJwtAndRoleIsEnforced() throws Exception {
  var result=mvc.perform(post("/auth/login").contentType("application/json").content("{\"username\":\"testviewer\",\"password\":\"test-password-only\"}")).andExpect(status().isOk()).andReturn();
  String token=mapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();assertThat(decoder.decode(token).getSubject()).isEqualTo("testviewer");
  mvc.perform(get("/products").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  mvc.perform(post("/products").header("Authorization","Bearer "+token).contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
 @Test void failedLoginRecordedWithoutPasswordAndSqlInjectionRejected() throws Exception {
  mvc.perform(post("/auth/login").contentType("application/json").content("{\"username\":\"testviewer\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
  mvc.perform(get("/auth/security/failed-logins").with(user("manager").roles("WAREHOUSE_MANAGER"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].username").value("testviewer"));
  mvc.perform(post("/auth/login").contentType("application/json").content("{\"username\":\"' OR 1=1--\",\"password\":\"wrong\"}")).andExpect(status().isBadRequest());
 }
 @Test void rateLimitSensitiveEndpoint() throws Exception {
  for(int i=0;i<10;i++) mvc.perform(post("/auth/login").with(req->{req.setRemoteAddr("192.0.2.15");return req;}).contentType("application/json").content("{}"));
  mvc.perform(post("/auth/login").with(req->{req.setRemoteAddr("192.0.2.15");return req;}).contentType("application/json").content("{}")).andExpect(status().isTooManyRequests());
 }
}
