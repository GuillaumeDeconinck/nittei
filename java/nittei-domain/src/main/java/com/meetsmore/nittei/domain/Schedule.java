package com.meetsmore.nittei.domain;

import java.util.List;

public record Schedule(ID id, ID userId, ID accountId, List<ScheduleRule> rules, String timezone, Object metadata) {
}
