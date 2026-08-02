package com.incentive.common.trace;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter implements Filter {
  public static final String HEADER = "X-Trace-Id";
  @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    String traceId = ((HttpServletRequest) request).getHeader(HEADER);
    if (traceId == null || traceId.isBlank()) traceId = UUID.randomUUID().toString();
    ((HttpServletResponse) response).setHeader(HEADER, traceId);
    request.setAttribute(HEADER, traceId);
    chain.doFilter(request, response);
  }
}

