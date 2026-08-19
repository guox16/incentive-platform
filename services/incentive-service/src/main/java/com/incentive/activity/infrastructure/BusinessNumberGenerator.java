package com.incentive.activity.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BusinessNumberGenerator {
  private static final long EPOCH = 1735689600000L;
  private static final long WORKER_ID_BITS = 10L;
  private static final long SEQUENCE_BITS = 12L;
  private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
  private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
  private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

  private final long workerId;
  private long sequence;
  private long lastTimestamp = -1L;

  public BusinessNumberGenerator(@Value("${id-generator.worker-id:1}") long workerId) {
    if (workerId < 0 || workerId > MAX_WORKER_ID) {
      throw new IllegalArgumentException("Snowflake workerId 必须在0到1023之间");
    }
    this.workerId = workerId;
  }

  public synchronized long next() {
    long timestamp = System.currentTimeMillis();
    if (timestamp < lastTimestamp) {
      throw new IllegalStateException("系统时钟回拨，暂时无法生成Snowflake ID");
    }
    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & SEQUENCE_MASK;
      if (sequence == 0) timestamp = waitUntilNextMillis(lastTimestamp);
    } else {
      sequence = 0;
    }
    lastTimestamp = timestamp;
    return ((timestamp - EPOCH) << TIMESTAMP_SHIFT) | (workerId << SEQUENCE_BITS) | sequence;
  }

  private long waitUntilNextMillis(long timestamp) {
    long current = System.currentTimeMillis();
    while (current <= timestamp) {
      Thread.onSpinWait();
      current = System.currentTimeMillis();
    }
    return current;
  }
}
