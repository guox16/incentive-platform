package com.incentive.activity.application.lottery;

public record LotteryPreDrawContext(
    Long activityId,
    Long userId,
    long pointsCost,
    long drawNumber) {}
