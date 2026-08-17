package com.incentive.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewaySecurityErrorWriter {
  private static final String TRACE_HEADER = "X-Trace-Id";
  private final ObjectMapper objectMapper;

  public GatewaySecurityErrorWriter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String message) {
    String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_HEADER);
    if (traceId == null || traceId.isBlank()) traceId = UUID.randomUUID().toString();
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    exchange.getResponse().getHeaders().set(TRACE_HEADER, traceId);
    try {
      byte[] body = objectMapper.writeValueAsBytes(Map.of(
          "code", code,
          "message", message,
          "traceId", traceId,
          "timestamp", Instant.now().toString()));
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
      return exchange.getResponse().writeWith(Mono.just(buffer));
    } catch (JsonProcessingException exception) {
      return exchange.getResponse().setComplete();
    }
  }
}
