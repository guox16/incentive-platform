package com.incentive.award.application;

import com.incentive.award.domain.Prize;
import com.incentive.award.domain.PrizeInventoryLedger;
import com.incentive.award.domain.PrizeStatus;
import com.incentive.award.dto.AdjustInventoryRequest;
import com.incentive.award.dto.CreatePrizeRequest;
import com.incentive.award.dto.PrizeInventoryLedgerResponse;
import com.incentive.award.dto.PrizeResponse;
import com.incentive.award.dto.UpdatePrizeRequest;
import com.incentive.award.repository.PrizeInventoryLedgerRepository;
import com.incentive.award.repository.PrizeRepository;
import com.incentive.award.support.PrizeBusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrizeService {
  private final PrizeRepository prizeRepository;
  private final PrizeInventoryLedgerRepository ledgerRepository;
  public PrizeService(PrizeRepository prizeRepository, PrizeInventoryLedgerRepository ledgerRepository) {
    this.prizeRepository = prizeRepository; this.ledgerRepository = ledgerRepository;
  }
  @Transactional
  public PrizeResponse create(CreatePrizeRequest request) {
    String code = request.code().trim();
    if (prizeRepository.existsByCode(code)) throw conflict("PRIZE_CODE_ALREADY_EXISTS", "奖品编码已存在");
    Prize prize = new Prize(code, request.name().trim(), request.type(), request.availableStock(), request.awardPayload());
    return response(prizeRepository.save(prize));
  }
  public PrizeResponse get(Long id) { return response(find(id)); }
  public List<PrizeResponse> list(PrizeStatus status) {
    List<Prize> prizes = status == null ? prizeRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
        : prizeRepository.findByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(status);
    return prizes.stream().map(this::response).toList();
  }
  @Transactional
  public PrizeResponse update(Long id, UpdatePrizeRequest request) {
    Prize prize = find(id);
    if (request.status() == PrizeStatus.DELETED) throw new IllegalArgumentException("请使用删除接口下架奖品");
    prize.update(request.name().trim(), request.type(), request.status(), request.awardPayload());
    return response(prize);
  }
  @Transactional
  public void delete(Long id) { find(id).delete(); }
  @Transactional
  public PrizeInventoryLedgerResponse adjustInventory(Long id, AdjustInventoryRequest request) {
    PrizeInventoryLedger existing = ledgerRepository.findByBusinessNo(request.businessNo().trim()).orElse(null);
    if (existing != null) {
      if (!existing.getPrizeId().equals(id)) throw conflict("INVENTORY_BUSINESS_NO_CONFLICT", "库存业务号已用于其他奖品");
      return ledgerResponse(existing);
    }
    Prize prize = find(id);
    prize.adjustStock(request.changeAmount());
    PrizeInventoryLedger ledger = new PrizeInventoryLedger(id, request.businessNo().trim(), request.changeAmount(),
        prize.getAvailableStock(), request.remark());
    return ledgerResponse(ledgerRepository.save(ledger));
  }
  public List<PrizeInventoryLedgerResponse> inventoryLedgers(Long id) {
    find(id);
    return ledgerRepository.findByPrizeIdOrderByCreatedAtDesc(id).stream().map(this::ledgerResponse).toList();
  }
  private Prize find(Long id) { return prizeRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() ->
      new PrizeBusinessException("PRIZE_NOT_FOUND", "奖品不存在", HttpStatus.NOT_FOUND)); }
  private PrizeBusinessException conflict(String code, String message) { return new PrizeBusinessException(code, message, HttpStatus.CONFLICT); }
  private PrizeResponse response(Prize prize) { return new PrizeResponse(prize.getId(), prize.getCode(), prize.getName(), prize.getType(), prize.getStatus(), prize.getAvailableStock(), prize.getAwardPayload(), prize.getCreatedAt(), prize.getUpdatedAt()); }
  private PrizeInventoryLedgerResponse ledgerResponse(PrizeInventoryLedger ledger) { return new PrizeInventoryLedgerResponse(ledger.getId(), ledger.getPrizeId(), ledger.getBusinessNo(), ledger.getOperationType(), ledger.getChangeAmount(), ledger.getBalanceAfter(), ledger.getRemark(), ledger.getCreatedAt()); }
}
