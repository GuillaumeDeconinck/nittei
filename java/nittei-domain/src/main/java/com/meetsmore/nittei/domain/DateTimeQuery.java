package com.meetsmore.nittei.domain;

import java.time.Instant;

public record DateTimeQuery(Instant eq, DateTimeQueryRange range) {
}
