package com.incentive.activity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.incentive.activity.support.IncentiveBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class LotteryRetryPolicyTest {
  private final LotteryRetryPolicy policy = new LotteryRetryPolicy();

  @Test
  void identifiesTransientPointsFailure() {
    var decision = policy.decide(failure("POINTS_SERVICE_UNAVAILABLE"));

    assertThat(decision.transientFailure()).isTrue();
    assertThat(decision.failureCode()).isEqualTo("POINTS_SERVICE_UNAVAILABLE");
  }

  @Test
  void identifiesPermanentBusinessFailure() {
    var decision = policy.decide(failure("INSUFFICIENT_POINTS"));

    assertThat(decision.transientFailure()).isFalse();
    assertThat(decision.failureCode()).isEqualTo("INSUFFICIENT_POINTS");
  }

  private IncentiveBusinessException failure(String code) {
    return new IncentiveBusinessException(code, "测试失败", HttpStatus.BAD_GATEWAY);
  }
}
