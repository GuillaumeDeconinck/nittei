package com.meetsmore.nittei.infra.system;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class RealSystemClock implements SystemClock {
  @Override
  public long getTimestampMillis() {
    return Instant.now().toEpochMilli();
  }

  @Override
  public Instant getTimestamp() {
    return Instant.now();
  }
}
