package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;
import java.time.Instant;

public interface ReservationRepository {
  void increment(ID serviceId, Instant timestamp);

  void decrement(ID serviceId, Instant timestamp);

  int count(ID serviceId, Instant timestamp);
}
