package com.incentive.award.support;

import com.incentive.common.api.ApiError;
import com.incentive.common.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PrizeExceptionHandler {
  @ExceptionHandler(PrizeBusinessException.class)
  ResponseEntity<ApiError> business(PrizeBusinessException ex, HttpServletRequest request) {
    return ResponseEntity.status(ex.getStatus()).body(error(ex.getCode(), ex.getMessage(), request));
  }
  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
  ResponseEntity<ApiError> validation(Exception ex, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", ex.getMessage(), request));
  }
  private ApiError error(String code, String message, HttpServletRequest request) {
    return new ApiError(code, message, (String) request.getAttribute(TraceIdFilter.HEADER), Instant.now());
  }
}
