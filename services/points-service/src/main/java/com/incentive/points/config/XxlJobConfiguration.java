package com.incentive.points.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** XXL-JOB 积分任务执行器配置。 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "xxl.job.executor.enabled", havingValue = "true")
public class XxlJobConfiguration {
  /** 创建并注册积分服务 XXL-JOB 执行器。 */
  @Bean
  XxlJobSpringExecutor xxlJobExecutor(
      @Value("${xxl.job.admin.addresses}") String adminAddresses,
      @Value("${xxl.job.admin.timeout:3}") int timeout,
      @Value("${xxl.job.executor.appname}") String appName,
      @Value("${xxl.job.executor.access-token:}") String accessToken,
      @Value("${xxl.job.executor.ip:}") String ip,
      @Value("${xxl.job.executor.port:9999}") int port,
      @Value("${xxl.job.executor.address:}") String address,
      @Value("${xxl.job.executor.log-path:./logs/xxl-job}") String logPath,
      @Value("${xxl.job.executor.log-retention-days:30}") int logRetentionDays) {
    XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
    executor.setAdminAddresses(adminAddresses);
    executor.setTimeout(timeout);
    executor.setEnabled(true);
    executor.setAppname(appName);
    executor.setAccessToken(accessToken);
    executor.setIp(ip);
    executor.setPort(port);
    executor.setAddress(address);
    executor.setLogPath(logPath);
    executor.setLogRetentionDays(logRetentionDays);
    return executor;
  }
}
