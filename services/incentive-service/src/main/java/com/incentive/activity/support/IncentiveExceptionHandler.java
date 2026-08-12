package com.incentive.activity.support;

import com.incentive.common.api.ApiError;
import com.incentive.common.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IncentiveExceptionHandler {
  @ExceptionHandler(IncentiveBusinessException.class)
  ResponseEntity<ApiError> handleBusiness(IncentiveBusinessException ex, HttpServletRequest request) {
    return ResponseEntity.status(ex.getStatus()).body(error(ex.getCode(), ex.getMessage(), request));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "请求参数不符合要求", request));
  }

  private ApiError error(String code, String message, HttpServletRequest request) {
    return new ApiError(code, message,
        (String) request.getAttribute(TraceIdFilter.HEADER), Instant.now());
  }
}
