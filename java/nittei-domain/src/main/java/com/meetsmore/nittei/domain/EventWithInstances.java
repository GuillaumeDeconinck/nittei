package com.meetsmore.nittei.domain;

import java.util.List;

public record EventWithInstances(CalendarEvent event, List<EventInstance> instances) {
}
