package com.stockguard.shared;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.util.UUID;
import java.time.Instant;
public record EventEnvelope(@NotNull UUID eventId,@NotBlank String eventType,@NotNull Instant timestamp,@NotBlank @Size(max=64) String correlationId,@NotNull JsonNode payload) {}
