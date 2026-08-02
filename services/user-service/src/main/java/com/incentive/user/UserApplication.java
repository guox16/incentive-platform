package com.incentive.user;

import com.incentive.common.trace.TraceIdFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class UserApplication {
  public static void main(String[] args) {
    SpringApplication.run(UserApplication.class, args);
  }

  @Bean
  TraceIdFilter traceIdFilter() {
    return new TraceIdFilter();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    // 即使暂不引入 JWT，也不能以明文形式保存演示账户的密码。
    return new BCryptPasswordEncoder();
  }
}
