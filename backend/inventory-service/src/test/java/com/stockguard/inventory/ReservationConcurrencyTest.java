package com.stockguard.inventory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
@SpringBootTest(properties={"stockguard.events.enabled=false","security.jwt-secret=test-only-signing-key-at-least-32-characters"}) @AutoConfigureMockMvc @Testcontainers
@org.springframework.security.test.context.support.WithMockUser(username="admin",roles="ADMIN")
class ReservationConcurrencyTest {
 @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @Autowired ReservationService service; @Autowired JdbcTemplate jdbc; @Autowired MockMvc mvc;
 static final UUID PRODUCT=UUID.fromString("00000000-0000-0000-0000-000000000101");
 static final UUID EMPTY=UUID.fromString("00000000-0000-0000-0000-000000000108");
 @BeforeEach void reset() {jdbc.update("DELETE FROM reservations");jdbc.update("DELETE FROM inventory_audit");jdbc.update("UPDATE inventory SET available_quantity=1,reserved_quantity=0 WHERE product_id=?",PRODUCT);}
 @Test void threeConcurrentHttpRequestsReserveExactlyOneUnit() throws Exception {
  var gate=new CountDownLatch(1);
  try(var executor=Executors.newFixedThreadPool(3)) {
   List<Future<Integer>> futures=new ArrayList<>();
   for(int i=0;i<3;i++) futures.add(executor.submit(()->{gate.await();return mvc.perform(post("/inventory/reservations").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")).contentType("application/json").content("{\"orderId\":\""+UUID.randomUUID()+"\",\"warehouse\":\"WH-PUN\",\"items\":[{\"productId\":\""+PRODUCT+"\",\"quantity\":1}]}")).andReturn().getResponse().getStatus();}));
   gate.countDown();List<Integer> statuses=new ArrayList<>();for(var f:futures) statuses.add(f.get(20,TimeUnit.SECONDS));
   assertThat(statuses).containsExactlyInAnyOrder(200,409,409);
  }
  assertThat(stock("available_quantity")).isZero();assertThat(stock("reserved_quantity")).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT min(available_quantity) FROM inventory",Integer.class)).isGreaterThanOrEqualTo(0);
 }
 @Test void repeatedReservationIsIdempotent() {
  var req=request(UUID.randomUUID(),List.of(new ReservationService.Line(PRODUCT,1)));
  assertThat(service.reserve(req,"test","ADMIN","test").reserved()).isTrue();
  assertThat(service.reserve(req,"test","ADMIN","test").reserved()).isTrue();assertThat(stock("reserved_quantity")).isEqualTo(1);
  assertThatThrownBy(()->service.reserve(request(req.orderId(),List.of(new ReservationService.Line(PRODUCT,2))),"test","ADMIN","test")).isInstanceOf(com.stockguard.shared.ApiException.class);
 }
 @Test void insufficientSecondItemRollsBackWholeReservation() {
  var result=service.reserve(request(UUID.randomUUID(),List.of(new ReservationService.Line(PRODUCT,1),new ReservationService.Line(EMPTY,1))),"test","ADMIN","test");
  assertThat(result.reserved()).isFalse();assertThat(stock("available_quantity")).isEqualTo(1);assertThat(stock("reserved_quantity")).isZero();
 }
 private ReservationService.Request request(UUID id,List<ReservationService.Line> items) {return new ReservationService.Request(id,"WH-PUN",items);}
 private int stock(String column) {return jdbc.queryForObject("SELECT "+column+" FROM inventory i JOIN warehouses w ON w.id=i.warehouse_id WHERE product_id=? AND w.code='WH-PUN'",Integer.class,PRODUCT);}
}
