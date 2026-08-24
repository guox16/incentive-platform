package com.incentive.activity.application;

import com.incentive.activity.domain.PrizeType;
import java.util.List;

/** 管理端奖池只依赖奖品目录的只读快照，跨服务细节由适配器封装。 */
public interface AwardCatalog {
  List<Item> list(String authorization);

  record Item(Long id, String code, String name, PrizeType type, String status,
              String coverUrl, String awardPayload, long totalStock, long availableStock) {}
}
