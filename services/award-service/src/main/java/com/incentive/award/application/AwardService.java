package com.incentive.award.application;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardInventoryLedger;
import com.incentive.award.domain.AwardStatus;
import com.incentive.award.domain.AwardType;
import com.incentive.award.dto.AdjustInventoryRequest;
import com.incentive.award.dto.AwardInventoryLedgerResponse;
import com.incentive.award.dto.AwardResponse;
import com.incentive.award.dto.AwardUpsertRequest;
import com.incentive.award.infrastructure.BusinessNumberGenerator;
import com.incentive.award.repository.AwardInventoryLedgerRepository;
import com.incentive.award.repository.AwardRepository;
import com.incentive.award.support.AwardBusinessException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AwardService {
  private final AwardRepository repository;
  private final AwardInventoryLedgerRepository ledgerRepository;
  private final BusinessNumberGenerator businessNumberGenerator;
  private final Clock clock;

  public AwardService(AwardRepository repository,
      AwardInventoryLedgerRepository ledgerRepository,
      BusinessNumberGenerator businessNumberGenerator, Clock clock) {
    this.repository = repository;
    this.ledgerRepository = ledgerRepository;
    this.businessNumberGenerator = businessNumberGenerator;
    this.clock = clock;
  }

  public List<AwardResponse> list() {
    return repository.findByStatusNotOrderByIdAsc(AwardStatus.DELETED).stream()
        .map(this::response).toList();
  }

  public AwardResponse get(Long id) {
    return response(find(id));
  }

  @Transactional
  public AwardResponse create(AwardUpsertRequest request) {
    String code = nextCode(request.type());
    return response(repository.save(new Award(code, request, clock.instant())));
  }

  @Transactional
  public AwardResponse update(Long id, AwardUpsertRequest request) {
    Award award = find(id);
    if (award.getTotalStock() != request.totalStock()
        || award.getAvailableStock() != request.availableStock()) {
      throw new AwardBusinessException(
          "AWARD_INVENTORY_IMMUTABLE", "请使用库存调整接口修改库存", HttpStatus.CONFLICT);
    }
    award.update(request, clock.instant());
    return response(award);
  }

  @Transactional
  public void delete(Long id) {
    find(id).softDelete(clock.instant());
  }

  @Transactional
  public AwardInventoryLedgerResponse adjustInventory(
      Long id, AdjustInventoryRequest request) {
    String businessNo = request.businessNo().trim();
    AwardInventoryLedger existing = ledgerRepository.findByBusinessNo(businessNo).orElse(null);
    if (existing != null) {
      if (!existing.getAwardId().equals(id)
          || existing.getChangeAmount() != request.changeAmount()) {
        throw new AwardBusinessException(
            "INVENTORY_BUSINESS_NO_CONFLICT", "库存业务号对应的调整命令不一致", HttpStatus.CONFLICT);
      }
      return ledgerResponse(existing);
    }
    if (request.changeAmount() == 0) {
      throw new AwardBusinessException(
          "INVENTORY_CHANGE_ZERO", "库存调整数量不能为0", HttpStatus.BAD_REQUEST);
    }
    Award award = find(id);
    try {
      award.adjustInventory(request.changeAmount(), clock.instant());
    } catch (ArithmeticException | IllegalArgumentException ex) {
      throw new AwardBusinessException(
          "AWARD_INVENTORY_INVALID", "库存调整后不能小于0或超出数值范围", HttpStatus.CONFLICT);
    }
    AwardInventoryLedger ledger = new AwardInventoryLedger(
        id, businessNo, request.changeAmount(), award.getAvailableStock(),
        request.remark(), clock.instant());
    return ledgerResponse(ledgerRepository.save(ledger));
  }

  public List<AwardInventoryLedgerResponse> inventoryLedgers(Long id) {
    find(id);
    return ledgerRepository.findByAwardIdOrderByCreatedAtDesc(id).stream()
        .map(this::ledgerResponse).toList();
  }

  private Award find(Long id) {
    return repository.findByIdAndStatusNot(id, AwardStatus.DELETED)
        .orElseThrow(() -> new AwardBusinessException(
            "AWARD_NOT_FOUND", "奖品不存在", HttpStatus.NOT_FOUND));
  }

  private AwardResponse response(Award award) {
    return new AwardResponse(award.getId(), award.getCode(), award.getName(), award.getType(),
        award.getStatus(), award.getCoverUrl(), award.getAwardPayload(), award.getTotalStock(),
        award.getAvailableStock(), award.getCreatedAt(), award.getUpdatedAt());
  }

  private String nextCode(AwardType type) {
    return "PRIZE_" + type.name() + "_"
        + Long.toUnsignedString(businessNumberGenerator.next(), 36).toUpperCase(Locale.ROOT);
  }

  private AwardInventoryLedgerResponse ledgerResponse(AwardInventoryLedger ledger) {
    return new AwardInventoryLedgerResponse(
        ledger.getId(), ledger.getAwardId(), ledger.getBusinessNo(),
        ledger.getOperationType(), ledger.getChangeAmount(), ledger.getAvailableAfter(),
        ledger.getRemark(), ledger.getCreatedAt());
  }
}
