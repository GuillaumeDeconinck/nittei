package com.meetsmore.nittei.infra.services;

import java.time.Instant;
import java.util.List;

public record FreeBusyProviderQuery(List<String> calendarIds, Instant start, Instant end) {
}
