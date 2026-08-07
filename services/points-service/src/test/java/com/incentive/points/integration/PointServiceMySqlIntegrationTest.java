package com.incentive.points.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incentive.points.application.PointAccountService;
import com.incentive.points.domain.InsufficientPointsException;
import com.incentive.points.dto.PointCommandRequest;
import com.incentive.points.dto.PointTransactionResponse;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointTransactionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实 MySQL 验证原生懒建账 SQL、事务和数据库锁。 */
@SpringBootTest(properties = {
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers(disabledWithoutDocker = true)
class PointServiceMySqlIntegrationTest {
  @Container
  static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("points_db")
      .withUsername("points")
      .withPassword("points");

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
  }

  @Autowired private PointAccountService service;
  @Autowired private PointTransactionRepository transactionRepository;
  @Autowired private PointAccountRepository accountRepository;

  @BeforeEach
  void cleanDatabase() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
  }

  @Test
  void persistsBalanceLedgerIdempotencyAndRollbackTogether() {
    Long userId = 1L;
    UUID creditBusinessId = UUID.randomUUID();
    PointCommandRequest credit = command(creditBusinessId, userId, 100, "award");

    assertThat(service.getBalance(userId).accountCreated()).isFalse();
    assertThat(service.credit(credit).balanceAfter()).isEqualTo(100);
    assertThat(service.credit(credit).replayed()).isTrue();
    assertThat(transactionRepository.count()).isEqualTo(1);

    PointCommandRequest excessiveDebit = command(UUID.randomUUID(), userId, 101, "exchange");
    assertThatThrownBy(() -> service.debit(excessiveDebit)).isInstanceOf(InsufficientPointsException.class);
    assertThat(service.getBalance(userId).balance()).isEqualTo(100);
    assertThat(transactionRepository.count()).isEqualTo(1);
  }

  @Test
  void serializesConcurrentCommandsWithoutLostUpdates() throws Exception {
    Long userId = 1L;
    service.credit(command(UUID.randomUUID(), userId, 10, "seed"));

    var executor = Executors.newFixedThreadPool(8);
    try {
      List<Callable<Void>> tasks = new ArrayList<>();
      for (int index = 0; index < 8; index++) {
        tasks.add(() -> {
          service.credit(command(UUID.randomUUID(), userId, 1, "task"));
          return null;
        });
      }
      for (var future : executor.invokeAll(tasks)) future.get();
    } finally {
      executor.shutdownNow();
    }

    assertThat(service.getBalance(userId).balance()).isEqualTo(18);
    assertThat(transactionRepository.count()).isEqualTo(9);
  }

  @Test
  void concurrentDuplicateBusinessIdChangesBalanceOnlyOnce() throws Exception {
    Long userId = 1L;
    PointCommandRequest request = command(UUID.randomUUID(), userId, 5, "award");
    var executor = Executors.newFixedThreadPool(2);
    try {
      List<Callable<PointTransactionResponse>> duplicateTasks = List.of(
          () -> service.credit(request),
          () -> service.credit(request));
      var results = executor.invokeAll(duplicateTasks);
      for (var result : results) assertThat(result.get().balanceAfter()).isEqualTo(5);
    } finally {
      executor.shutdownNow();
    }

    assertThat(service.getBalance(userId).balance()).isEqualTo(5);
    assertThat(transactionRepository.count()).isEqualTo(1);
  }

  private PointCommandRequest command(UUID businessId, Long userId, long amount, String source) {
    return new PointCommandRequest(businessId, userId, amount, source, null);
  }
}
