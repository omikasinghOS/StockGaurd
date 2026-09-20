package com.stockguard.shared;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {
 private static final Logger log=LoggerFactory.getLogger(CorrelationFilter.class);
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
  String id=req.getHeader("X-Correlation-ID"); if(id==null||!id.matches("[a-zA-Z0-9-]{1,64}")) id=UUID.randomUUID().toString();
  MDC.put("correlationId",id); res.setHeader("X-Correlation-ID",id);
  try { chain.doFilter(req,res); } finally { log.info("http method={} path={} status={}",req.getMethod(),req.getRequestURI(),res.getStatus()); MDC.remove("correlationId"); }
 }
}
