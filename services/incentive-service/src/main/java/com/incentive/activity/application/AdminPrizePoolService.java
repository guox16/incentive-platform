package com.incentive.activity.application;

import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.LotteryPrize;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.domain.PrizeType;
import com.incentive.activity.dto.AdminPrizePoolResponse;
import com.incentive.activity.dto.UpdatePrizePoolRequest;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.LotteryPrizeRepository;
import com.incentive.activity.repository.ParticipationRuleRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminPrizePoolService {
  private final IncentiveActivityRepository activityRepository;
  private final ParticipationRuleRepository ruleRepository;
  private final LotteryPrizeRepository prizeRepository;
  private final AwardCatalog awardCatalog;

  public AdminPrizePoolService(IncentiveActivityRepository activityRepository,
      ParticipationRuleRepository ruleRepository, LotteryPrizeRepository prizeRepository,
      AwardCatalog awardCatalog) {
    this.activityRepository = activityRepository;
    this.ruleRepository = ruleRepository;
    this.prizeRepository = prizeRepository;
    this.awardCatalog = awardCatalog;
  }

  public AdminPrizePoolResponse get(Long activityId, String authorization) {
    IncentiveActivity activity = lotteryActivity(activityId);
    ParticipationRule rule = latestRule(activityId);
    List<LotteryPrize> configured = prizeRepository
        .findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(activityId, rule.getId());
    List<AwardCatalog.Item> awards = awardCatalog.list(authorization);
    Map<Long, AwardCatalog.Item> awardById = index(awards);
    Set<Long> occupied = prizeRepository.findPrizeIdsAssignedToOtherLatestPools(activityId);

    List<AdminPrizePoolResponse.ConfiguredPrize> configuredResponse = configured.stream()
        .map(prize -> configured(prize, awardById.get(prize.getPrizeId())))
        .toList();
    List<AdminPrizePoolResponse.PrizeCandidate> candidates = awards.stream()
        .filter(this::hasUsableStock)
        .filter(award -> award.type() == PrizeType.NONE || !occupied.contains(award.id()))
        .map(award -> new AdminPrizePoolResponse.PrizeCandidate(
            award.id(), award.code(), award.name(), award.type(), award.availableStock()))
        .toList();
    return new AdminPrizePoolResponse(configuredResponse, candidates);
  }

  @Transactional
  public AdminPrizePoolResponse update(
      Long activityId, UpdatePrizePoolRequest request, String authorization) {
    IncentiveActivity activity = lotteryActivity(activityId);
    if (activity.getStatus() != ActivityStatus.DRAFT) {
      throw conflict("PRIZE_POOL_LOCKED", "只有草稿活动可以修改奖池");
    }
    ParticipationRule rule = latestRule(activityId);
    Map<Long, AwardCatalog.Item> awardById = index(awardCatalog.list(authorization));
    Set<Long> occupied = prizeRepository.findPrizeIdsAssignedToOtherLatestPools(activityId);
    Set<Long> selected = new HashSet<>();
    List<LotteryPrize> replacements = new ArrayList<>();

    for (int index = 0; index < request.prizes().size(); index++) {
      UpdatePrizePoolRequest.Entry entry = request.prizes().get(index);
      if (!selected.add(entry.prizeId())) {
        throw conflict("PRIZE_POOL_DUPLICATE", "同一奖品不能在奖池中重复配置");
      }
      AwardCatalog.Item award = awardById.get(entry.prizeId());
      if (award == null || !hasUsableStock(award)) {
        throw conflict("PRIZE_NOT_AVAILABLE", "所选奖品已下架或库存不足，请重新选择");
      }
      if (award.type() != PrizeType.NONE && occupied.contains(entry.prizeId())) {
        throw conflict("PRIZE_ALREADY_ASSIGNED", "所选奖品已参与其他活动，请重新选择");
      }
      if (award.type() == PrizeType.NONE && entry.campaignQuota() != null) {
        throw conflict("PRIZE_QUOTA_INVALID", "未中奖奖项不需要设置活动库存");
      }
      if (award.type() != PrizeType.NONE && entry.campaignQuota() == null) {
        throw conflict("PRIZE_QUOTA_REQUIRED", "请设置奖品的活动库存");
      }
      if (entry.campaignQuota() != null && entry.campaignQuota() > award.availableStock()) {
        throw conflict("PRIZE_QUOTA_EXCEEDED", "活动库存不能超过奖品可用库存");
      }
      replacements.add(new LotteryPrize(activityId, rule.getId(), award.id(), award.name(),
          award.type(), award.coverUrl(), award.awardPayload(), entry.weight(),
          entry.campaignQuota(), (index + 1) * 10));
    }

    List<LotteryPrize> existing = prizeRepository
        .findByActivityIdAndRuleIdOrderByDisplayOrderAscIdAsc(activityId, rule.getId());
    prizeRepository.deleteAll(existing);
    prizeRepository.flush();
    prizeRepository.saveAll(replacements);
    return get(activityId, authorization);
  }

  private AdminPrizePoolResponse.ConfiguredPrize configured(
      LotteryPrize prize, AwardCatalog.Item award) {
    return new AdminPrizePoolResponse.ConfiguredPrize(
        prize.getPrizeId(), award == null ? null : award.code(), prize.getPrizeName(),
        prize.getPrizeType(), award == null ? 0 : award.availableStock(), prize.getWeight(),
        prize.getCampaignQuota(), prize.getDisplayOrder());
  }

  private boolean hasUsableStock(AwardCatalog.Item award) {
    return "ACTIVE".equals(award.status())
        && (award.type() == PrizeType.NONE || award.availableStock() > 0);
  }

  private Map<Long, AwardCatalog.Item> index(List<AwardCatalog.Item> awards) {
    Map<Long, AwardCatalog.Item> result = new HashMap<>();
    awards.forEach(award -> result.put(award.id(), award));
    return result;
  }

  private IncentiveActivity lotteryActivity(Long activityId) {
    IncentiveActivity activity = activityRepository.findById(activityId).orElseThrow(() ->
        new IncentiveBusinessException(
            "ACTIVITY_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND));
    if (activity.getType() != ActivityType.LOTTERY) {
      throw new IncentiveBusinessException(
          "PRIZE_POOL_TYPE_MISMATCH", "只有抽奖活动可以配置奖池", HttpStatus.BAD_REQUEST);
    }
    return activity;
  }

  private ParticipationRule latestRule(Long activityId) {
    return ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(activityId)
        .orElseThrow(() -> conflict("ACTIVITY_RULE_NOT_FOUND", "活动未配置参与规则"));
  }

  private IncentiveBusinessException conflict(String code, String message) {
    return new IncentiveBusinessException(code, message, HttpStatus.CONFLICT);
  }
}
