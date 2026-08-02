package com.incentive.points;

import com.incentive.common.trace.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PointsApplication {
  public static void main(String[] args) {
    SpringApplication.run(PointsApplication.class, args);
  }

  @Bean
  TraceIdFilter traceIdFilter() {
    return new TraceIdFilter();
  }
}
