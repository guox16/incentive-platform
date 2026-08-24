package com.incentive.award.messaging;

import com.incentive.award.domain.AwardSourceType;
import com.incentive.award.domain.AwardType;

public record AwardCommandMessage(
    Long pendingAwardId,
    String commandKey,
    AwardSourceType sourceType,
    Long sourceRecordId,
    Long userId,
    Long awardId,
    String awardName,
    AwardType awardType,
    String awardPayload,
    Long stockNo) {}
