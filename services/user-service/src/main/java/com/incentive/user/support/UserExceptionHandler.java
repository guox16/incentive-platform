package com.incentive.user.support;

import com.incentive.common.api.ApiError;
import com.incentive.common.trace.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {
  /** 将用户业务异常转换为标准错误响应。 */
  @ExceptionHandler(UserBusinessException.class)
  ResponseEntity<ApiError> handleBusiness(UserBusinessException ex, HttpServletRequest request) {
    return ResponseEntity.status(ex.getStatus()).body(error(ex.getCode(), ex.getMessage(), request));
  }

  /** 将请求体校验失败转换为错误响应。 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "请求参数不符合要求", request));
  }

  /** 组装包含链路追踪 ID 的标准错误对象。 */
  private ApiError error(String code, String message, HttpServletRequest request) {
    return new ApiError(code, message, (String) request.getAttribute(TraceIdFilter.HEADER), Instant.now());
  }
}
