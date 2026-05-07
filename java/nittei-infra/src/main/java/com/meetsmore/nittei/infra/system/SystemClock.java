package com.meetsmore.nittei.infra.system;

import java.time.Instant;

public interface SystemClock {
  long getTimestampMillis();

  Instant getTimestamp();
}
