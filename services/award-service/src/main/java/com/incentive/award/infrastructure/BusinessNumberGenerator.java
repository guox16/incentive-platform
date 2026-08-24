package com.incentive.award.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BusinessNumberGenerator {
  private static final long EPOCH = 1735689600000L;
  private static final long SEQUENCE_MASK = (1L << 12) - 1;
  private final long workerId;
  private long sequence;
  private long lastTimestamp = -1L;

  public BusinessNumberGenerator(@Value("${id-generator.worker-id:2}") long workerId) {
    if (workerId < 0 || workerId > 1023) {
      throw new IllegalArgumentException("Snowflake workerId必须在0到1023之间");
    }
    this.workerId = workerId;
  }

  public synchronized long next() {
    long timestamp = System.currentTimeMillis();
    if (timestamp < lastTimestamp) throw new IllegalStateException("系统时钟回拨");
    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & SEQUENCE_MASK;
      while (sequence == 0 && timestamp <= lastTimestamp) {
        Thread.onSpinWait();
        timestamp = System.currentTimeMillis();
      }
    } else {
      sequence = 0;
    }
    lastTimestamp = timestamp;
    return ((timestamp - EPOCH) << 22) | (workerId << 12) | sequence;
  }
}
