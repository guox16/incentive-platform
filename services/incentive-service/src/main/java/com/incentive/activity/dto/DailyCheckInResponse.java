package com.incentive.activity.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyCheckInResponse(
    Long userId,
    LocalDate businessDate,
    boolean checkedInToday,
    int currentStreak,
    long rewardPoints,
    String rewardStatus,
    Long checkInId,
    Long pointTransactionId,
    Long balanceAfter,
    List<LocalDate> signedDates) {}
