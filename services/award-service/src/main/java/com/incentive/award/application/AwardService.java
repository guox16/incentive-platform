package com.incentive.award.application;

import com.incentive.award.domain.Award;
import com.incentive.award.domain.AwardStatus;
import com.incentive.award.dto.AwardResponse;
import com.incentive.award.dto.AwardUpsertRequest;
import com.incentive.award.repository.AwardRepository;
import com.incentive.award.support.AwardBusinessException;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AwardService {
  private final AwardRepository repository;
  private final Clock clock;

  public AwardService(AwardRepository repository, Clock clock) {
    this.repository = repository;
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
    if (repository.existsByCode(request.code().trim())) {
      throw new AwardBusinessException("AWARD_CODE_EXISTS", "奖品编码已存在", HttpStatus.CONFLICT);
    }
    return response(repository.save(new Award(request, clock.instant())));
  }

  @Transactional
  public AwardResponse update(Long id, AwardUpsertRequest request) {
    Award award = find(id);
    if (!award.getCode().equals(request.code().trim())) {
      throw new AwardBusinessException("AWARD_CODE_IMMUTABLE", "奖品编码创建后不可修改", HttpStatus.CONFLICT);
    }
    award.update(request, clock.instant());
    return response(award);
  }

  @Transactional
  public void delete(Long id) {
    find(id).softDelete(clock.instant());
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
}
