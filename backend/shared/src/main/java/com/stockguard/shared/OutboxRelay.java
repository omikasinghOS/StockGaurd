package com.stockguard.shared;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
@Component @ConditionalOnProperty(name="stockguard.events.enabled",havingValue="true")
public class OutboxRelay {
 private record Row(UUID id,String topic,String key,String body) {}
 private final JdbcTemplate jdbc;private final KafkaTemplate<String,String> kafka;
 public OutboxRelay(JdbcTemplate jdbc,KafkaTemplate<String,String> kafka) {this.jdbc=jdbc;this.kafka=kafka;}
 @Scheduled(fixedDelayString="${stockguard.events.relay-delay:500}") @Transactional(timeout=60)
 public void relay() throws Exception {
  // A relay crash after broker ack may republish; event IDs stay unchanged.
  var rows=jdbc.query("SELECT event_id,topic,partition_key,body FROM outbox WHERE published_at IS NULL ORDER BY created_at,event_id LIMIT 10 FOR UPDATE SKIP LOCKED",(rs,n)->new Row(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4)));
  for(var row:rows) {kafka.send(row.topic(),row.key(),row.body()).get(5,TimeUnit.SECONDS);jdbc.update("UPDATE outbox SET published_at=now() WHERE event_id=?",row.id());}
 }
}
