package com.incentive.award.controller;

import com.incentive.award.application.AwardService;
import com.incentive.award.dto.AwardResponse;
import com.incentive.award.dto.AwardUpsertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/awards")
@Tag(name = "奖品管理")
public class AwardController {
  private final AwardService service;

  public AwardController(AwardService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "查询奖品列表")
  public List<AwardResponse> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  @Operation(summary = "查询奖品详情")
  public AwardResponse get(@PathVariable @Positive Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "创建奖品")
  public AwardResponse create(@Valid @RequestBody AwardUpsertRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @Operation(summary = "更新奖品")
  public AwardResponse update(@PathVariable @Positive Long id,
      @Valid @RequestBody AwardUpsertRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "软删除奖品")
  public void delete(@PathVariable @Positive Long id) {
    service.delete(id);
  }
}
