package com.stockguard.inventory;
import com.stockguard.shared.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import java.util.*;
import java.time.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties="security.jwt-secret=test-only-signing-key-at-least-32-characters") @Testcontainers
class KafkaReservationTest {
 @Container @ServiceConnection static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
 @Container @ServiceConnection static KafkaContainer kafka=new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));
 @Autowired KafkaTemplate<String,String> template; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc;
 @Test void duplicateEventReservesOnceAndFailurePublishesOutcome() throws Exception {
  UUID product=UUID.fromString("00000000-0000-0000-0000-000000000101");UUID order=UUID.randomUUID();UUID eventId=UUID.randomUUID();
  var payload=Map.of("orderId",order,"warehouse","WH-PUN","items",List.of(Map.of("productId",product,"quantity",1)),"actor","kafka-test","role","ADMIN");
  var event=new EventEnvelope(eventId,"OrderCreated",Instant.now(),"kafka-duplicate-test",mapper.valueToTree(payload));String json=mapper.writeValueAsString(event);
  template.send("stockguard.OrderCreated",order.toString(),json).get();template.send("stockguard.OrderCreated",order.toString(),json).get();
  await().atMost(Duration.ofSeconds(40)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE topic='stockguard.InventoryReserved' AND published_at IS NOT NULL",Integer.class)).isEqualTo(1));
  assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_events WHERE event_id=?",Integer.class,eventId)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT reserved_quantity FROM inventory i JOIN warehouses w ON w.id=i.warehouse_id WHERE product_id=? AND w.code='WH-PUN'",Integer.class,product)).isEqualTo(1);
  UUID failedOrder=UUID.randomUUID();var failedPayload=new HashMap<String,Object>(payload);failedPayload.put("orderId",failedOrder);
  template.send("stockguard.OrderCreated",failedOrder.toString(),mapper.writeValueAsString(new EventEnvelope(UUID.randomUUID(),"OrderCreated",Instant.now(),"kafka-failure-test",mapper.valueToTree(failedPayload)))).get();
  await().atMost(Duration.ofSeconds(30)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox WHERE topic='stockguard.InventoryReservationFailed' AND published_at IS NOT NULL",Integer.class)).isEqualTo(1));
 }
 @Test void malformedEventReachesDeadLetterTopic() throws Exception {
  template.send("stockguard.OrderCreated","bad","{not-json").get();
  Properties props=new Properties();props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,kafka.getBootstrapServers());props.put(ConsumerConfig.GROUP_ID_CONFIG,"dlt-test-"+UUID.randomUUID());props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
  try(var consumer=new KafkaConsumer<String,String>(props,new StringDeserializer(),new StringDeserializer())) {
   consumer.subscribe(List.of("stockguard.OrderCreated.DLT"));
   await().atMost(Duration.ofSeconds(40)).until(()-> {for(var record:consumer.poll(Duration.ofMillis(500))) if(record.value().equals("{not-json")) return true;return false;});
  }
 }
}
