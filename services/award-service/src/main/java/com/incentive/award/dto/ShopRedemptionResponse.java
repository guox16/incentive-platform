package com.incentive.award.dto;

import com.incentive.award.domain.ShopRedemption;
import java.time.Instant;

public record ShopRedemptionResponse(String id, String awardName, long pointsCost,
                                     Status status, Instant createdAt) {
  public enum Status { PROCESSING, COMPLETED, REJECTED }

  public static ShopRedemptionResponse from(ShopRedemption redemption) {
    Status status = switch (redemption.getStatus()) {
      case RESERVED, PAID -> Status.PROCESSING;
      case COMPLETED -> Status.COMPLETED;
      case REJECTED -> Status.REJECTED;
    };
    return new ShopRedemptionResponse(redemption.getId().toString(), redemption.getAwardName(),
        redemption.getPointsCost(), status, redemption.getCreatedAt());
  }
}
