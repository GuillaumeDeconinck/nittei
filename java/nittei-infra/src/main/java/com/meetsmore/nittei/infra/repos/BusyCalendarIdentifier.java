package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.ID;

public record BusyCalendarIdentifier(ID serviceId, ID userId, ID calendarId) {
}
