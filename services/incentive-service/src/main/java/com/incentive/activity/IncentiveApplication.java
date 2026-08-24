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
    SpringApplication application = new SpringApplication(IncentiveApplication.class);
    // Temporary: use the local profile while the service runs outside Docker.
    application.setAdditionalProfiles("local");
    application.run(args);
  }

  @Bean
  TraceIdFilter traceIdFilter() { return new TraceIdFilter(); }

  @Bean
  Clock businessClock() { return Clock.system(ZoneId.of("Asia/Shanghai")); }
}
