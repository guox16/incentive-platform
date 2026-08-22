package com.incentive.activity.application.lottery;

import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** 固定后置规则：预占命中奖品库存，库存耗尽时降级为幸运奖。 */
@Service
public class LotteryPostDrawStockRule {
  private final LotteryStockReservationStore stockStore;

  public LotteryPostDrawStockRule(LotteryStockReservationStore stockStore) {
    this.stockStore = stockStore;
  }

  public Resolution resolve(Long lotteryOrderId, Long activityId, LotteryPrize selected,
      List<LotteryPrize> prizes, Long configuredLuckyPrizeId) {
    Long stockNo = reserve(lotteryOrderId, activityId, selected);
    if (stockNo != null || selected.getPrizeType() == PrizeType.NONE) {
      return new Resolution(selected, stockNo, false);
    }

    LotteryPrize lucky = findLuckyPrize(configuredLuckyPrizeId, prizes, selected);
    Long luckyStockNo = reserve(lotteryOrderId, activityId, lucky);
    if (luckyStockNo == null && lucky.getPrizeType() != PrizeType.NONE) {
      lucky = prizes.stream()
          .filter(prize -> prize.getPrizeType() == PrizeType.NONE)
          .findFirst()
          .orElseThrow(() -> invalid("幸运奖库存不足且奖池没有NONE类型兜底奖"));
      luckyStockNo = null;
    }
    return new Resolution(lucky, luckyStockNo, true);
  }

  public void release(Long lotteryOrderId, Long activityId, Long prizeId, Long stockNo) {
    if (stockNo != null) stockStore.release(activityId, prizeId, lotteryOrderId, stockNo);
  }

  public void discard(Long lotteryOrderId, Long activityId, Long prizeId, Long stockNo) {
    if (stockNo != null) stockStore.discard(activityId, prizeId, lotteryOrderId, stockNo);
  }

  private Long reserve(Long orderId, Long activityId, LotteryPrize prize) {
    if (prize.getPrizeType() == PrizeType.NONE) return null;
    if (prize.getCampaignQuota() == null) {
      throw invalid("非NONE奖品必须配置campaignQuota: " + prize.getPrizeId());
    }
    return stockStore.reserve(
        activityId, prize.getPrizeId(), orderId, prize.getCampaignQuota());
  }

  private LotteryPrize findLuckyPrize(
      Long configuredPrizeId, List<LotteryPrize> prizes, LotteryPrize depletedPrize) {
    LotteryPrize lucky = configuredPrizeId == null
        ? prizes.stream().filter(prize -> prize.getPrizeType() == PrizeType.NONE).findFirst()
            .orElseThrow(() -> invalid("活动没有配置幸运奖"))
        : prizes.stream().filter(prize -> prize.getPrizeId().equals(configuredPrizeId)).findFirst()
            .orElseThrow(() -> invalid("奖池中不存在幸运奖prizeId: " + configuredPrizeId));
    if (lucky.getId().equals(depletedPrize.getId())) {
      return prizes.stream()
          .filter(prize -> prize.getPrizeType() == PrizeType.NONE)
          .filter(prize -> !prize.getId().equals(depletedPrize.getId()))
          .findFirst()
          .orElseThrow(() -> invalid("库存耗尽奖品不能同时作为唯一幸运奖"));
    }
    return lucky;
  }

  private IncentiveBusinessException invalid(String message) {
    return new IncentiveBusinessException(
        "LOTTERY_POST_RULE_INVALID", message, HttpStatus.CONFLICT);
  }

  public record Resolution(LotteryPrize prize, Long stockNo, boolean fallback) {}
}
