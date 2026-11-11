package com.incentive.award;

import com.incentive.common.trace.TraceIdFilter;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AwardApplication {
  public static void main(String[] args) {
    SpringApplication.run(AwardApplication.class, args);
  }

  @Bean
  TraceIdFilter traceIdFilter() { return new TraceIdFilter(); }

  @Bean
  Clock clock() { return Clock.systemUTC(); }
}
