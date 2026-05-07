package com.meetsmore.nittei.domain.booking;

import com.meetsmore.nittei.domain.ID;
import java.time.Instant;
import java.util.List;

public record ServiceBookingSlot(Instant start, long duration, List<ID> userIds) {}
