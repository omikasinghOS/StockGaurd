package com.stockguard.shared;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component @Order(-90)
public class RateLimitFilter extends OncePerRequestFilter {
 private record Window(long start,int requests) {}
 private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  if(!request.getMethod().equals("POST")) {chain.doFilter(request,response);return;}
  boolean login=request.getRequestURI().equals("/auth/login");int limit=login?10:60;long now=System.currentTimeMillis();
  if(windows.size()>10000) windows.entrySet().removeIf(e->now-e.getValue().start()>60000);
  String actor=request.getUserPrincipal()==null?request.getRemoteAddr():request.getUserPrincipal().getName();String key=(login?"login:":"write:")+actor;
  if(windows.size()>10000&&!windows.containsKey(key)) {reject(response);return;}
  Window window=windows.compute(key,(k,w)->w==null||now-w.start()>=60000?new Window(now,1):new Window(w.start(),w.requests()+1));
  if(window.requests()>limit) {reject(response);return;}chain.doFilter(request,response);
 }
 private void reject(HttpServletResponse response) throws IOException {response.setStatus(429);response.setHeader("Retry-After","60");response.setContentType("application/problem+json");response.getWriter().write("{\"status\":429,\"detail\":\"Rate limit exceeded. Retry later.\"}");}
}
