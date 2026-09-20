package com.stockguard.shared;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.UUID;
@Component
public class EventStore {
 private final JdbcTemplate jdbc;private final ObjectMapper mapper;private final Validator validator;
 public EventStore(JdbcTemplate jdbc,ObjectMapper mapper,Validator validator) {this.jdbc=jdbc;this.mapper=mapper;this.validator=validator;}
 @Transactional(propagation=Propagation.MANDATORY)
 public void publish(String type,String key,String correlationId,Object payload) {
  var event=new EventEnvelope(UUID.randomUUID(),type,Instant.now(),correlationId,mapper.valueToTree(payload));
  try {jdbc.update("INSERT INTO outbox(event_id,topic,partition_key,body) VALUES (?,?,?,?)",event.eventId(),"stockguard."+type,key,mapper.writeValueAsString(event));} catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new IllegalArgumentException("Invalid event",e);}
 }
 @Transactional(propagation=Propagation.MANDATORY)
 public boolean claim(EventEnvelope event) {return jdbc.update("INSERT INTO processed_events(event_id,event_type) VALUES (?,?) ON CONFLICT DO NOTHING",event.eventId(),event.eventType())==1;}
 public EventEnvelope read(String json) {
  try {var event=mapper.readValue(json,EventEnvelope.class);validate(event);return event;} catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new IllegalArgumentException("Malformed event",e);}
 }
 public <T> T payload(EventEnvelope e,Class<T> type) {T value=mapper.convertValue(e.payload(),type);validate(value);return value;}
 private void validate(Object value) {if(value==null||!validator.validate(value).isEmpty()) throw new IllegalArgumentException("Event validation failed");}
}
