package com.stockguard.shared;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
@Configuration
public class SecurityConfiguration {
 @Bean SecretKeySpec signingKey(@Value("${security.jwt-secret}") String secret) {
  if(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
  return new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA256");
 }
 @Bean JwtDecoder decoder(SecretKeySpec key) {
  var decoder=NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  OAuth2TokenValidator<Jwt> audience=jwt->jwt.getAudience().contains("stockguard")&&List.of("ADMIN","WAREHOUSE_MANAGER","VIEWER").contains(jwt.getClaimAsString("role"))&&jwt.getSubject()!=null?OAuth2TokenValidatorResult.success():OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
  decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer("stockguard-auth"),audience));return decoder;
 }
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  var converter=new JwtAuthenticationConverter();converter.setJwtGrantedAuthoritiesConverter(jwt->List.of(new SimpleGrantedAuthority("ROLE_"+jwt.getClaimAsString("role"))));
  return http.csrf(csrf->csrf.disable()).cors(cors->{}).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'")))
   .authorizeHttpRequests(a->a
    .requestMatchers("/actuator/health/**","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll()
    .requestMatchers(HttpMethod.POST,"/auth/login").permitAll()
    .requestMatchers("/audit/**","/auth/security/**").hasAnyRole("ADMIN","WAREHOUSE_MANAGER")
    .requestMatchers(HttpMethod.GET,"/**").authenticated()
    .requestMatchers(HttpMethod.POST,"/products").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT,"/products/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.POST,"/inventory/**","/orders").hasAnyRole("ADMIN","WAREHOUSE_MANAGER")
    .anyRequest().denyAll())
   .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(converter)))
   .build();
 }
 @Bean CorsConfigurationSource cors(@Value("${security.cors-origins}") String origins) {
  var config=new CorsConfiguration();config.setAllowedOrigins(Arrays.asList(origins.split(",")));config.setAllowedMethods(List.of("GET","POST","PUT","OPTIONS"));config.setAllowedHeaders(List.of("Authorization","Content-Type","X-Correlation-ID","Idempotency-Key"));config.setExposedHeaders(List.of("X-Correlation-ID"));config.setAllowCredentials(false);
  var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",config);return source;
 }
}
