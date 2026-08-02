package com.incentive.points.dto;

import java.util.List;

public record PointTransactionPageResponse(
    List<PointTransactionResponse> items,
    int page,
    int size,
    long totalElements,
    int totalPages) {}
