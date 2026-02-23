package com.meetsmore.nittei.domain;

import java.util.List;
import java.util.Map;

public record ScheduleRule(String type, Object value, List<Map<String, Object>> intervals) {
}
