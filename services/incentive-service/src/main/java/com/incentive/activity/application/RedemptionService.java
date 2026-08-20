package com.incentive.activity.application;

import com.incentive.activity.domain.RedemptionRecord;
import com.incentive.activity.domain.RedemptionStatus;
import com.incentive.activity.dto.RedemptionResponse;
import com.incentive.activity.infrastructure.BusinessNumberGenerator;
import com.incentive.activity.infrastructure.PointsClient;
import com.incentive.activity.repository.RedemptionRecordRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RedemptionService {
  private final RedemptionRecordRepository recordRepository;
  private final RedemptionTransactions transactions;
  private final PointsClient pointsClient;
  private final BusinessNumberGenerator businessNumberGenerator;

  public RedemptionService(RedemptionRecordRepository recordRepository,
      RedemptionTransactions transactions, PointsClient pointsClient,
      BusinessNumberGenerator businessNumberGenerator) {
    this.recordRepository = recordRepository;
    this.transactions = transactions;
    this.pointsClient = pointsClient;
    this.businessNumberGenerator = businessNumberGenerator;
  }

  public RedemptionResponse redeem(String activityCode, Long itemId, Long userId,
      String requestId) {
    String normalizedRequestId = requestId.trim();
    RedemptionRecord record = recordRepository.findByRequestId(normalizedRequestId).orElse(null);
    if (record == null) {
      try {
        record = transactions.createPending(normalizedRequestId, activityCode, itemId, userId,
            businessNumberGenerator.next());
      } catch (DataIntegrityViolationException ex) {
        record = recordRepository.findByRequestId(normalizedRequestId).orElse(null);
        if (record == null) throw ex;
      }
    }
    validateSameRequest(record, activityCode, itemId, userId);

    if (record.getStatus() == RedemptionStatus.PENDING) {
      PointsClient.PointDebitResult debit = pointsClient.debit(record.getPointBusinessId(), userId,
          record.getPointsCost(), "REDEMPTION", "兑换商品：" + record.getItemCode());
      record = transactions.complete(record.getId(), debit.transactionId(), debit.balanceAfter());
    }
    return response(record);
  }

  private void validateSameRequest(RedemptionRecord record, String activityCode, Long itemId,
      Long userId) {
    if (!record.getUserId().equals(userId)
        || !record.getActivityCode().equals(activityCode)
        || !record.getItemId().equals(itemId)) {
      throw new IncentiveBusinessException(
          "REDEMPTION_REQUEST_ID_REUSED", "兑换请求号已用于其他请求", HttpStatus.CONFLICT);
    }
  }

  private RedemptionResponse response(RedemptionRecord record) {
    return new RedemptionResponse(record.getId(), record.getActivityCode(), record.getItemId(),
        record.getItemCode(), record.getUserId(), record.getPrizeId(), record.getPrizeName(),
        record.getPrizeType(), record.getCoverUrl(), record.getPointsCost(),
        record.getPointTransactionId(), record.getBalanceAfter(), true, record.getCreatedAt());
  }
}
