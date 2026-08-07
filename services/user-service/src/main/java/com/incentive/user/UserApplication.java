package com.incentive.user;

import com.incentive.common.trace.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class UserApplication {
  /** 启动用户服务。 */
  public static void main(String[] args) {
    SpringApplication.run(UserApplication.class, args);
  }

  /** 注册请求链路追踪过滤器。 */
  @Bean
  TraceIdFilter traceIdFilter() {
    return new TraceIdFilter();
  }

  /** 注册 BCrypt 密码编码器。 */
  @Bean
  PasswordEncoder passwordEncoder() {
    // 即使暂不引入 JWT，也不能以明文形式保存演示账户的密码。
    return new BCryptPasswordEncoder();
  }
}
