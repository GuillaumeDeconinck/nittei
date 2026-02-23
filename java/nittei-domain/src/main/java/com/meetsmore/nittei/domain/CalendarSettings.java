package com.meetsmore.nittei.domain;

import java.time.DayOfWeek;

public record CalendarSettings(DayOfWeek weekStart, String timezone) {
}
