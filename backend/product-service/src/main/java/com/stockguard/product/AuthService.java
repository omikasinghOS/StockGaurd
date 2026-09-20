package com.stockguard.product;
import com.stockguard.shared.ApiException;
import java.util.*;
import java.time.*;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.MDC;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
@Service
public class AuthService implements ApplicationRunner {
 private final JdbcTemplate jdbc;private final Environment env;private final JwtEncoder encoder;
 private final BCryptPasswordEncoder passwords=new BCryptPasswordEncoder(12);
 private final String dummyHash=passwords.encode(UUID.randomUUID().toString());
 public record Session(String accessToken,String tokenType,long expiresIn,String username,String role) {}
 public AuthService(JdbcTemplate jdbc,Environment env,SecretKeySpec key) {this.jdbc=jdbc;this.env=env;this.encoder=new NimbusJwtEncoder(new ImmutableSecret<>(key));}
 @Override public void run(ApplicationArguments args) {
  for(var entry:Map.of("admin","ADMIN","manager","WAREHOUSE_MANAGER","viewer","VIEWER").entrySet()) {
   String secret=env.getProperty("DEMO_"+entry.getKey().toUpperCase()+"_PASSWORD");
   if(secret==null||secret.isBlank()) continue;
   if(secret.length()<12||secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalStateException("Demo passwords must be 12-72 UTF-8 bytes");
   jdbc.update("INSERT INTO app_users(username,password_hash,role) VALUES (?,?,?) ON CONFLICT DO NOTHING",entry.getKey(),passwords.encode(secret),entry.getValue());
  }
 }
 public Session login(String username,String password) {
  var rows=jdbc.queryForList("SELECT password_hash,role FROM app_users WHERE username=?",username);
  boolean valid=passwords.matches(password,rows.isEmpty()?dummyHash:(String)rows.getFirst().get("password_hash"));
  String correlationId=Optional.ofNullable(MDC.get("correlationId")).orElse("login");
  jdbc.update("INSERT INTO login_audit(id,username,success,correlation_id) VALUES (?,?,?,?)",UUID.randomUUID(),username,valid,correlationId);
  if(!valid||rows.isEmpty()) throw new ApiException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
  String role=(String)rows.getFirst().get("role");Instant now=Instant.now();
  var claims=JwtClaimsSet.builder().issuer("stockguard-auth").subject(username).audience(List.of("stockguard")).issuedAt(now).expiresAt(now.plusSeconds(1800)).claim("role",role).id(UUID.randomUUID().toString()).build();
  var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims));return new Session(token.getTokenValue(),"Bearer",1800,username,role);
 }
 public List<Map<String,Object>> failures() {return jdbc.queryForList("SELECT username,count(*) AS attempts,max(timestamp) AS last_attempt FROM login_audit WHERE NOT success AND timestamp>now()-interval '24 hours' GROUP BY username ORDER BY attempts DESC LIMIT 100");}
}
