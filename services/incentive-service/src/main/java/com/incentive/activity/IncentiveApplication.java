package com.incentive.activity;

import com.incentive.common.trace.TraceIdFilter;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IncentiveApplication {
  public static void main(String[] args) {
    SpringApplication.run(IncentiveApplication.class, args);
  }

  @Bean
  TraceIdFilter traceIdFilter() { return new TraceIdFilter(); }

  @Bean
  Clock businessClock() { return Clock.system(ZoneId.of("Asia/Shanghai")); }
}
