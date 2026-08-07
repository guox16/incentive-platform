package com.incentive.points;

import com.incentive.common.trace.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PointsApplication {
  /** 启动积分服务。 */
  public static void main(String[] args) {
    SpringApplication.run(PointsApplication.class, args);
  }

  /** 注册请求链路追踪过滤器。 */
  @Bean
  TraceIdFilter traceIdFilter() {
    return new TraceIdFilter();
  }
}
