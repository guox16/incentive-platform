package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LotteryRetryPolicyTest {
  private final LotteryRetryPolicy policy =
      new LotteryRetryPolicy(5, Duration.ofSeconds(5), Duration.ofMinutes(1));

  @Test
  void retriesTransientPointsFailureWithExponentialBackoff() {
    var first = policy.decide(failure("POINTS_SERVICE_UNAVAILABLE"), 0);
    var third = policy.decide(failure("POINTS_SERVICE_UNAVAILABLE"), 2);

    assertThat(first.retryable()).isTrue();
    assertThat(first.delay()).isEqualTo(Duration.ofSeconds(5));
    assertThat(third.delay()).isEqualTo(Duration.ofSeconds(20));
  }

  @Test
  void stopsAfterMaximumAttempt() {
    var decision = policy.decide(failure("POINTS_SERVICE_UNAVAILABLE"), 4);

    assertThat(decision.retryable()).isFalse();
    assertThat(decision.delay()).isNull();
  }

  @Test
  void doesNotRetryPermanentBusinessFailure() {
    var decision = policy.decide(failure("INSUFFICIENT_POINTS"), 0);

    assertThat(decision.retryable()).isFalse();
    assertThat(decision.failureCode()).isEqualTo("INSUFFICIENT_POINTS");
  }

  private IncentiveBusinessException failure(String code) {
    return new IncentiveBusinessException(code, "测试失败", HttpStatus.BAD_GATEWAY);
  }
}
