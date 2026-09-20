package com.stockguard.shared;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
@Configuration @ConditionalOnProperty(name="stockguard.events.enabled",havingValue="true")
public class KafkaConfiguration {
 @Bean KafkaAdmin.NewTopics topics() {
  return new KafkaAdmin.NewTopics(java.util.stream.Stream.of("OrderCreated","InventoryReserved","InventoryReservationFailed","LowStockDetected").flatMap(t->java.util.stream.Stream.of("stockguard."+t,"stockguard."+t+".DLT")).map(t->TopicBuilder.name(t).partitions(3).replicas(1).build()).toArray(NewTopic[]::new));
 }
 @Bean DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String,String> template) {
  var recoverer=new DeadLetterPublishingRecoverer(template,(record,error)->new TopicPartition(record.topic()+".DLT",record.partition()));
  recoverer.setFailIfSendResultIsError(true);
  var handler=new DefaultErrorHandler(recoverer,new FixedBackOff(1000L,3));
  handler.addNotRetryableExceptions(IllegalArgumentException.class);
  return handler;
 }
}
