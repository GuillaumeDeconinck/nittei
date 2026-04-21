package com.meetsmore.nittei.domain;

import java.time.Instant;

public record DateTimeQueryRange(Instant gte, Instant lte, Instant gt, Instant lt) {}
