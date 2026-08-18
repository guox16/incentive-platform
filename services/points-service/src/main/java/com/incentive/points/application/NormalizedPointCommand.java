package com.incentive.points.application;

import com.incentive.points.domain.PointTransactionType;

/** 经过字符串标准化、可直接进入积分本地事务的命令。 */
record NormalizedPointCommand(Long businessId, Long userId, PointTransactionType type,
                              long amount, String source, String remark) {}
