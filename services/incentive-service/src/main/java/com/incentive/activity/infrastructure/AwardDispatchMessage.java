package com.incentive.activity.infrastructure;

import com.incentive.activity.domain.PendingAward;

public record AwardDispatchMessage(
    Long pendingAwardId,
    String commandKey,
    PendingAward.SourceType sourceType,
    Long sourceRecordId,
    Long userId,
    Long awardId,
    String awardName,
    String awardType,
    String awardPayload,
    Long stockNo) {

  public static AwardDispatchMessage from(PendingAward award) {
    return new AwardDispatchMessage(
        award.getId(), award.getSourceType() + ":" + award.getSourceRecordId(),
        award.getSourceType(), award.getSourceRecordId(), award.getUserId(),
        award.getPrizeId(), award.getPrizeName(), award.getPrizeType().name(),
        award.getAwardPayload(), award.getStockNo());
  }
}
