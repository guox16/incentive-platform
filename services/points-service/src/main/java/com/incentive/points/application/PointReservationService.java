package com.incentive.points.application;

import com.incentive.points.domain.PointReservation;
import com.incentive.points.domain.PointReservationStatus;
import com.incentive.points.dto.PointReservationRequest;
import com.incentive.points.dto.PointReservationResponse;
import com.incentive.points.repository.PointReservationRepository;
import com.incentive.points.repository.PointTransactionRepository;
import com.incentive.points.support.PointBusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 积分预占、确认和主动取消的幂等应用入口。 */
@Service
public class PointReservationService {
  private final PointReservationRepository reservationRepository;
  private final PointTransactionRepository transactionRepository;
  private final PointReservationCommandExecutor commandExecutor;

  public PointReservationService(PointReservationRepository reservationRepository,
      PointTransactionRepository transactionRepository,
      PointReservationCommandExecutor commandExecutor) {
    this.reservationRepository = reservationRepository;
    this.transactionRepository = transactionRepository;
    this.commandExecutor = commandExecutor;
  }

  /** 创建预占；相同业务号和相同请求参数重复调用时返回原结果。 */
  public PointReservationResponse reserve(PointReservationRequest request) {
    NormalizedPointReservationCommand command = normalize(request);
    PointReservation existing = reservationRepository.findByBusinessId(command.businessId()).orElse(null);
    if (existing != null) return toResponse(validateSame(existing, command), true);
    if (transactionRepository.findByBusinessId(command.businessId()).isPresent()) {
      throw idempotencyKeyReused();
    }

    try {
      return toResponse(commandExecutor.reserve(command), false);
    } catch (PointCommandRaceException ex) {
      existing = reservationRepository.findByBusinessId(command.businessId()).orElse(null);
      if (existing != null) return toResponse(validateSame(existing, command), true);
      throw commandConflict();
    }
  }

  /** 确认预占；重复确认返回已确认结果。 */
  public PointReservationResponse confirm(Long businessId) {
    PointReservation existing = requireReservation(businessId);
    if (existing.getStatus() == PointReservationStatus.CONFIRMED) {
      return toResponse(existing, true);
    }
    ensureCanConfirm(existing);
    try {
      return toResponse(commandExecutor.confirm(businessId), false);
    } catch (PointCommandRaceException ex) {
      existing = requireReservation(businessId);
      if (existing.getStatus() == PointReservationStatus.CONFIRMED) {
        return toResponse(existing, true);
      }
      ensureCanConfirm(existing);
      throw commandConflict();
    }
  }

  /** 主动取消预占并退回积分；重复取消返回已取消结果。 */
  public PointReservationResponse cancel(Long businessId) {
    PointReservation existing = requireReservation(businessId);
    if (existing.getStatus() == PointReservationStatus.CANCELLED) {
      return toResponse(existing, true);
    }
    ensureCanCancel(existing);
    try {
      return toResponse(commandExecutor.cancel(businessId), false);
    } catch (PointCommandRaceException ex) {
      existing = requireReservation(businessId);
      if (existing.getStatus() == PointReservationStatus.CANCELLED) {
        return toResponse(existing, true);
      }
      ensureCanCancel(existing);
      throw commandConflict();
    }
  }

  /** 查询预占当前状态。 */
  @Transactional(readOnly = true)
  public PointReservationResponse get(Long businessId) {
    return toResponse(requireReservation(businessId), false);
  }

  private PointReservation validateSame(
      PointReservation existing, NormalizedPointReservationCommand command) {
    boolean same = existing.getUserId().equals(command.userId())
        && existing.getAmount() == command.amount()
        && existing.getSource().equals(command.source())
        && Objects.equals(existing.getRemark(), command.remark())
        && existing.getExpiresAt().equals(command.expiresAt());
    if (!same) throw idempotencyKeyReused();
    return existing;
  }

  private void ensureCanConfirm(PointReservation reservation) {
    if (reservation.getStatus() != PointReservationStatus.RESERVED) {
      throw stateConflict("只有待确认的积分预占才能确认");
    }
    if (!reservation.getExpiresAt().isAfter(now())) {
      throw new PointBusinessException(
          "POINT_RESERVATION_EXPIRED", "积分预占已经过期", HttpStatus.CONFLICT);
    }
  }

  private void ensureCanCancel(PointReservation reservation) {
    if (reservation.getStatus() != PointReservationStatus.RESERVED) {
      throw stateConflict("只有待确认的积分预占才能取消");
    }
  }

  private PointReservation requireReservation(Long businessId) {
    return reservationRepository.findByBusinessId(businessId)
        .orElseThrow(() -> new PointBusinessException(
            "POINT_RESERVATION_NOT_FOUND", "积分预占不存在", HttpStatus.NOT_FOUND));
  }

  private NormalizedPointReservationCommand normalize(PointReservationRequest request) {
    String source = request.source().trim().toUpperCase(Locale.ROOT);
    String remark = request.remark() == null || request.remark().isBlank()
        ? null : request.remark().trim();
    return new NormalizedPointReservationCommand(
        request.businessId(), request.userId(), request.amount(), source, remark,
        request.expiresAt().truncatedTo(ChronoUnit.MILLIS));
  }

  private PointReservationResponse toResponse(PointReservation reservation, boolean replayed) {
    return new PointReservationResponse(
        reservation.getId(), reservation.getBusinessId(), reservation.getUserId(),
        reservation.getAmount(), reservation.getBalanceBefore(), reservation.getBalanceAfter(),
        reservation.getSource(), reservation.getRemark(), reservation.getStatus(),
        reservation.getConfirmedTransactionId(), reservation.getExpiresAt(),
        reservation.getConfirmedAt(), reservation.getCancelledAt(), reservation.getExpiredAt(),
        reservation.getCreatedAt(), reservation.getUpdatedAt(), replayed);
  }

  private PointBusinessException idempotencyKeyReused() {
    return new PointBusinessException(
        "IDEMPOTENCY_KEY_REUSED", "业务号已被其他积分命令使用", HttpStatus.CONFLICT);
  }

  private PointBusinessException stateConflict(String message) {
    return new PointBusinessException("POINT_RESERVATION_STATE_CONFLICT", message, HttpStatus.CONFLICT);
  }

  private PointBusinessException commandConflict() {
    return new PointBusinessException(
        "POINTS_COMMAND_CONFLICT", "积分预占命令正在处理或已完成，请安全重试", HttpStatus.CONFLICT);
  }

  private Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MILLIS);
  }
}
