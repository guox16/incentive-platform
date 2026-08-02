package com.incentive.common.api;

import com.incentive.common.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> handle(Exception ex, HttpServletRequest request) {
    String traceId = (String) request.getAttribute(TraceIdFilter.HEADER);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("INTERNAL_ERROR", "Unexpected server error", traceId, Instant.now()));
  }
}
