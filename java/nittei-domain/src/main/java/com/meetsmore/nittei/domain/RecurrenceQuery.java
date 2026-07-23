package com.meetsmore.nittei.domain;

import java.time.Instant;

public record RecurrenceQuery(Boolean exists, Instant existsAndRecurringAt) {}
