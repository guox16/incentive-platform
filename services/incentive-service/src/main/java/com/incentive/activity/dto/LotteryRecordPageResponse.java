package com.incentive.activity.dto;

import java.util.List;

public record LotteryRecordPageResponse(
    List<LotteryRecordResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages) {
}
