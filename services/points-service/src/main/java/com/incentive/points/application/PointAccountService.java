package com.incentive.points.application;

import com.incentive.points.domain.PointTransaction;
import com.incentive.points.domain.PointTransactionType;
import com.incentive.points.dto.PointBalanceResponse;
import com.incentive.points.dto.PointCommandRequest;
import com.incentive.points.dto.PointTransactionPageResponse;
import com.incentive.points.dto.PointTransactionResponse;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointReservationRepository;
import com.incentive.points.repository.PointTransactionRepository;
import com.incentive.points.support.PointBusinessException;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 积分账户的命令和查询入口，统一维护业务号幂等与响应语义。 */
@Service
public class PointAccountService {
  private final PointAccountRepository accountRepository;
  private final PointReservationRepository reservationRepository;
  private final PointTransactionRepository transactionRepository;
  private final PointAccountCommandExecutor commandExecutor;

  /** 创建积分账户应用服务。 */
  public PointAccountService(PointAccountRepository accountRepository,
      PointReservationRepository reservationRepository,
      PointTransactionRepository transactionRepository,
      PointAccountCommandExecutor commandExecutor) {
    this.accountRepository = accountRepository;
    this.reservationRepository = reservationRepository;
    this.transactionRepository = transactionRepository;
    this.commandExecutor = commandExecutor;
  }

  /** 查询用户当前积分余额；未开户时返回零余额。 */
  @Transactional(readOnly = true)
  public PointBalanceResponse getBalance(Long userId) {
    return accountRepository.findById(userId)
        .map(account -> new PointBalanceResponse(userId, account.getBalance(), true, account.getUpdatedAt()))
        // 未建账等同于零余额，查询操作本身绝不创建数据库记录。
        .orElseGet(() -> new PointBalanceResponse(userId, 0, false, null));
  }

  /** 分页查询用户的积分流水。 */
  @Transactional(readOnly = true)
  public PointTransactionPageResponse getTransactions(Long userId, int page, int size) {
    var result = transactionRepository.findByUserIdOrderByCreatedAtDesc(
        userId, PageRequest.of(page, size));
    var items = result.getContent().stream().map(transaction -> toResponse(transaction, false)).toList();
    return new PointTransactionPageResponse(items, result.getNumber(), result.getSize(),
        result.getTotalElements(), result.getTotalPages());
  }

  /** 为用户增加积分，并保证业务号幂等。 */
  public PointTransactionResponse credit(PointCommandRequest request) {
    NormalizedPointCommand command = normalize(request, PointTransactionType.CREDIT);
    ensureBusinessIdNotReserved(command.businessId());
    PointTransaction existing = findExisting(command);
    if (existing != null) return toResponse(existing, true);

    try {
      return toResponse(commandExecutor.credit(command), false);
    } catch (PointCommandRaceException ex) {
      return replayAfterRace(command);
    }
  }

  /** 扣减用户积分，并保证业务号幂等和余额充足。 */
  public PointTransactionResponse debit(PointCommandRequest request) {
    NormalizedPointCommand command = normalize(request, PointTransactionType.DEBIT);
    ensureBusinessIdNotReserved(command.businessId());
    PointTransaction existing = findExisting(command);
    if (existing != null) return toResponse(existing, true);

    try {
      return toResponse(commandExecutor.debit(command), false);
    } catch (PointCommandRaceException ex) {
      return replayAfterRace(command);
    }
  }

  /** 查找已处理的同业务号命令，并校验命令内容一致。 */
  private PointTransaction findExisting(NormalizedPointCommand command) {
    return transactionRepository.findByBusinessId(command.businessId()).map(existing -> {
      boolean same = existing.getUserId().equals(command.userId())
          && existing.getType() == command.type()
          && existing.getAmount() == command.amount()
          && existing.getSource().equals(command.source())
          && Objects.equals(existing.getRemark(), command.remark());
      if (!same) {
        throw new PointBusinessException("IDEMPOTENCY_KEY_REUSED", "业务号已被其他积分命令使用", HttpStatus.CONFLICT);
      }
      return existing;
    }).orElse(null);
  }

  /** 并发失败事务回滚后，读取胜出请求已经提交的流水。 */
  private PointTransactionResponse replayAfterRace(NormalizedPointCommand command) {
    ensureBusinessIdNotReserved(command.businessId());
    PointTransaction existing = findExisting(command);
    if (existing != null) return toResponse(existing, true);
    throw new PointBusinessException(
        "POINTS_COMMAND_CONFLICT", "积分命令正在处理或已完成，请安全重试", HttpStatus.CONFLICT);
  }

  private void ensureBusinessIdNotReserved(Long businessId) {
    if (reservationRepository.findByBusinessId(businessId).isPresent()) {
      throw new PointBusinessException(
          "IDEMPOTENCY_KEY_REUSED", "业务号已被积分预占命令使用", HttpStatus.CONFLICT);
    }
  }

  /** 标准化积分命令中的字符串字段。 */
  private NormalizedPointCommand normalize(PointCommandRequest request, PointTransactionType type) {
    String source = request.source().trim().toUpperCase(Locale.ROOT);
    String remark = request.remark() == null || request.remark().isBlank() ? null : request.remark().trim();
    return new NormalizedPointCommand(request.businessId(), request.userId(),
        type, request.amount(), source, remark);
  }

  /** 将积分流水实体转换为接口响应。 */
  private PointTransactionResponse toResponse(PointTransaction transaction, boolean replayed) {
    return new PointTransactionResponse(transaction.getId(),
        transaction.getBusinessId(), transaction.getUserId(),
        transaction.getType(), transaction.getAmount(), transaction.getBalanceBefore(),
        transaction.getBalanceAfter(), transaction.getSource(), transaction.getRemark(),
        transaction.getCreatedAt(), replayed);
  }
}
