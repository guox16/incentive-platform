package com.incentive.activity.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incentive.activity.domain.ActivityStatus;
import com.incentive.activity.domain.ActivityType;
import com.incentive.activity.domain.IncentiveActivity;
import com.incentive.activity.domain.ParticipationRule;
import com.incentive.activity.dto.AdminActivityResponse;
import com.incentive.activity.dto.CreateActivityRequest;
import com.incentive.activity.dto.UpdateActivityRequest;
import com.incentive.activity.repository.IncentiveActivityRepository;
import com.incentive.activity.repository.ParticipationRuleRepository;
import com.incentive.activity.support.IncentiveBusinessException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminActivityService {
  private final IncentiveActivityRepository activityRepository;
  private final ParticipationRuleRepository ruleRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public AdminActivityService(IncentiveActivityRepository activityRepository,
      ParticipationRuleRepository ruleRepository, ObjectMapper objectMapper, Clock clock) {
    this.activityRepository = activityRepository;
    this.ruleRepository = ruleRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public List<AdminActivityResponse> list(ActivityType type, ActivityStatus status) {
    return activityRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt", "id")).stream()
        .filter(activity -> activity.getType() != ActivityType.CHECK_IN)
        .filter(activity -> type == null || activity.getType() == type)
        .filter(activity -> status == null || activity.getStatus() == status)
        .map(this::response)
        .toList();
  }

  public AdminActivityResponse get(Long id) {
    return response(find(id));
  }

  @Transactional
  public AdminActivityResponse create(CreateActivityRequest request) {
    ensureManageable(request.type());
    validateTime(request.startsAt(), request.endsAt());
    String code = request.code().trim();
    if (activityRepository.existsByCode(code)) {
      throw conflict("ACTIVITY_CODE_ALREADY_EXISTS", "活动编码已存在");
    }
    String qualificationRule = normalizeJson(request.qualificationRule());
    IncentiveActivity activity = activityRepository.save(new IncentiveActivity(
        code, request.type(), request.name().trim(), request.startsAt(), request.endsAt()));
    ruleRepository.save(new ParticipationRule(activity.getId(), 1, request.pointsCost(),
        request.dailyLimit(), qualificationRule, clock.instant()));
    return response(activity);
  }

  @Transactional
  public AdminActivityResponse update(Long id, UpdateActivityRequest request) {
    IncentiveActivity activity = find(id);
    ensureManageable(activity.getType());
    validateTime(request.startsAt(), request.endsAt());
    String qualificationRule = normalizeJson(request.qualificationRule());
    ParticipationRule currentRule = latestRule(activity.getId());
    activity.update(request.name().trim(), request.status(), request.startsAt(), request.endsAt());
    if (ruleChanged(currentRule, request, qualificationRule)) {
      ruleRepository.save(new ParticipationRule(activity.getId(), currentRule.getRuleVersion() + 1,
          request.pointsCost(), request.dailyLimit(), qualificationRule, clock.instant()));
    }
    return response(activity);
  }

  private AdminActivityResponse response(IncentiveActivity activity) {
    ParticipationRule rule = latestRule(activity.getId());
    return new AdminActivityResponse(activity.getId(), activity.getCode(), activity.getType(),
        activity.getName(), activity.getStatus(), activity.getStartsAt(), activity.getEndsAt(),
        rule.getRuleVersion(), rule.getPointsCost(), rule.getDailyLimit(),
        rule.getQualificationRule(), activity.getCreatedAt(), activity.getUpdatedAt());
  }

  private IncentiveActivity find(Long id) {
    return activityRepository.findById(id).orElseThrow(() ->
        new IncentiveBusinessException("ACTIVITY_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND));
  }

  private ParticipationRule latestRule(Long activityId) {
    return ruleRepository.findFirstByActivityIdOrderByRuleVersionDesc(activityId)
        .orElseThrow(() -> conflict("ACTIVITY_RULE_NOT_FOUND", "活动未配置参与规则"));
  }

  private void ensureManageable(ActivityType type) {
    if (type == ActivityType.CHECK_IN) {
      throw new IncentiveBusinessException("ACTIVITY_TYPE_NOT_MANAGEABLE",
          "签到活动请使用签到规则管理", HttpStatus.BAD_REQUEST);
    }
  }

  private void validateTime(Instant startsAt, Instant endsAt) {
    if (endsAt != null && !endsAt.isAfter(startsAt)) {
      throw new IncentiveBusinessException("ACTIVITY_TIME_INVALID",
          "结束时间必须晚于开始时间", HttpStatus.BAD_REQUEST);
    }
  }

  private String normalizeJson(String value) {
    if (value == null || value.isBlank()) return null;
    try {
      return objectMapper.writeValueAsString(objectMapper.readTree(value));
    } catch (JsonProcessingException ex) {
      throw new IncentiveBusinessException("QUALIFICATION_RULE_INVALID",
          "资格规则必须是合法 JSON", HttpStatus.BAD_REQUEST);
    }
  }

  private boolean ruleChanged(ParticipationRule rule, UpdateActivityRequest request,
      String qualificationRule) {
    return rule.getPointsCost() != request.pointsCost()
        || !java.util.Objects.equals(rule.getDailyLimit(), request.dailyLimit())
        || !java.util.Objects.equals(rule.getQualificationRule(), qualificationRule);
  }

  private IncentiveBusinessException conflict(String code, String message) {
    return new IncentiveBusinessException(code, message, HttpStatus.CONFLICT);
  }
}
