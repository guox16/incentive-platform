package com.incentive.points.application;

import com.incentive.points.domain.PointAccount;
import com.incentive.points.domain.PointTransaction;
import com.incentive.points.domain.PointTransactionType;
import com.incentive.points.dto.PointBalanceResponse;
import com.incentive.points.dto.PointCommandRequest;
import com.incentive.points.dto.PointTransactionPageResponse;
import com.incentive.points.dto.PointTransactionResponse;
import com.incentive.points.repository.PointAccountRepository;
import com.incentive.points.repository.PointTransactionRepository;
import com.incentive.points.support.PointBusinessException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 积分账户的命令和查询入口，统一维护事务、行锁与幂等语义。 */
@Service
@Transactional(readOnly = true)
public class PointAccountService {
  private final PointAccountRepository accountRepository;
  private final PointTransactionRepository transactionRepository;

  /** 创建积分账户应用服务。 */
  public PointAccountService(PointAccountRepository accountRepository, PointTransactionRepository transactionRepository) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
  }

  /** 查询用户当前积分余额；未开户时返回零余额。 */
  public PointBalanceResponse getBalance(Long userId) {
    return accountRepository.findById(userId)
        .map(account -> new PointBalanceResponse(userId, account.getBalance(), true, account.getUpdatedAt()))
        // 未建账等同于零余额，查询操作本身绝不创建数据库记录。
        .orElseGet(() -> new PointBalanceResponse(userId, 0, false, null));
  }

  /** 分页查询用户的积分流水。 */
  public PointTransactionPageResponse getTransactions(Long userId, int page, int size) {
    var result = transactionRepository.findByUserIdOrderByCreatedAtDesc(
        userId, PageRequest.of(page, size));
    var items = result.getContent().stream().map(transaction -> toResponse(transaction, false)).toList();
    return new PointTransactionPageResponse(items, result.getNumber(), result.getSize(),
        result.getTotalElements(), result.getTotalPages());
  }

  @Transactional
  /** 为用户增加积分，并保证业务号幂等。 */
  public PointTransactionResponse credit(PointCommandRequest request) {
    NormalizedCommand command = normalize(request, PointTransactionType.CREDIT);
    PointTransaction existing = findExisting(command);
    if (existing != null) return toResponse(existing, true);

    // INSERT IGNORE 只承担并发首次建账；随后仍通过行锁串行化实际余额变更。
    accountRepository.createIfAbsent(command.userId(), Instant.now());
    PointAccount account = accountRepository.findByUserIdForUpdate(command.userId())
        .orElseThrow(() -> new IllegalStateException("积分账户创建后未找到"));
    existing = findExisting(command);
    if (existing != null) return toResponse(existing, true);

    long before;
    try {
      before = account.credit(command.amount());
    } catch (ArithmeticException ex) {
      throw new PointBusinessException("POINTS_BALANCE_OVERFLOW", "积分余额超过允许范围", HttpStatus.CONFLICT);
    }
    return saveTransaction(command, before, account.getBalance());
  }

  @Transactional
  /** 扣减用户积分，并保证业务号幂等和余额充足。 */
  public PointTransactionResponse debit(PointCommandRequest request) {
    NormalizedCommand command = normalize(request, PointTransactionType.DEBIT);
    PointTransaction existing = findExisting(command);
    if (existing != null) return toResponse(existing, true);

    PointAccount account = accountRepository.findByUserIdForUpdate(command.userId())
        .orElseThrow(() -> new com.incentive.points.domain.InsufficientPointsException());
    existing = findExisting(command);
    if (existing != null) return toResponse(existing, true);

    long before = account.debit(command.amount());
    return saveTransaction(command, before, account.getBalance());
  }

  /** 保存积分流水并返回响应结果。 */
  private PointTransactionResponse saveTransaction(NormalizedCommand command, long before, long after) {
    PointTransaction transaction = new PointTransaction(command.businessId(), command.userId(), command.type(),
        command.amount(), before, after, command.source(), command.remark());
    try {
      // 强制 flush，让全局唯一业务号冲突在当前命令内暴露；事务会同时回滚余额变更。
      return toResponse(transactionRepository.saveAndFlush(transaction), false);
    } catch (DataIntegrityViolationException ex) {
      throw new PointBusinessException("POINTS_COMMAND_CONFLICT", "积分命令正在处理或已完成，请安全重试", HttpStatus.CONFLICT);
    }
  }

  /** 查找已处理的同业务号命令，并校验命令内容一致。 */
  private PointTransaction findExisting(NormalizedCommand command) {
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

  /** 标准化积分命令中的字符串字段。 */
  private NormalizedCommand normalize(PointCommandRequest request, PointTransactionType type) {
    String source = request.source().trim().toUpperCase(Locale.ROOT);
    String remark = request.remark() == null || request.remark().isBlank() ? null : request.remark().trim();
    return new NormalizedCommand(request.businessId(), request.userId(),
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

  private record NormalizedCommand(Long businessId, Long userId, PointTransactionType type,
                                   long amount, String source, String remark) {}
}
