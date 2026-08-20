package com.incentive.activity.application;

import com.incentive.activity.support.IncentiveBusinessException;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/** 只负责识别异常性质；业务链路是否重试由执行入口固定控制为一次。 */
@Component
public class LotteryRetryPolicy {
  private static final Set<String> TRANSIENT_CODES = Set.of(
      "POINTS_SERVICE_UNAVAILABLE",
      "POINTS_SERVICE_ERROR",
      "POINTS_SERVICE_INVALID_RESPONSE",
      "POINTS_COMMAND_CONFLICT");

  public Decision decide(Throwable failure) {
    IncentiveBusinessException businessFailure = findBusinessFailure(failure);
    if (businessFailure != null) {
      String code = businessFailure.getCode();
      return new Decision(code, TRANSIENT_CODES.contains(code));
    }
    if (findDataAccessFailure(failure)) {
      return new Decision("DATABASE_TEMPORARY_ERROR", true);
    }
    return new Decision("LOTTERY_PROCESSING_ERROR", false);
  }

  private IncentiveBusinessException findBusinessFailure(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof IncentiveBusinessException businessException) {
        return businessException;
      }
    }
    return null;
  }

  private boolean findDataAccessFailure(Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof DataAccessException) return true;
    }
    return false;
  }

  public record Decision(String failureCode, boolean transientFailure) {}
}
