package com.meetsmore.nittei.api.structs.event;

import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.CalendarEventReminder;
import com.meetsmore.nittei.domain.CalendarEventStatus;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.RRuleOptions;
import java.time.Instant;
import java.util.List;

public final class EventDtos {

    private EventDtos() {
    }

    public record CalendarEventDTO(
        ID id,
        String title,
        String description,
        String eventType,
        String location,
        boolean allDay,
        CalendarEventStatus status,
        String externalParentId,
        String externalId,
        Instant startTime,
        Instant endTime,
        long duration,
        boolean busy,
        Instant updated,
        Instant created,
        RRuleOptions recurrence,
        Instant recurringUntil,
        List<Instant> exdates,
        ID recurringEventId,
        Instant originalStartTime,
        ID calendarId,
        ID userId,
        List<CalendarEventReminder> reminders,
        Object metadata
    ) {
        public static CalendarEventDTO from(CalendarEvent event) {
            return new CalendarEventDTO(
                event.id(),
                event.title(),
                event.description(),
                event.eventType(),
                event.location(),
                event.allDay(),
                event.status(),
                event.externalParentId(),
                event.externalId(),
                event.startTime(),
                event.endTime(),
                event.duration(),
                event.busy(),
                event.updated(),
                event.created(),
                event.recurrence(),
                event.recurringUntil(),
                event.exdates(),
                event.recurringEventId(),
                event.originalStartTime(),
                event.calendarId(),
                event.userId(),
                event.reminders(),
                event.metadata()
            );
        }
    }

    public record EventWithInstancesDTO(CalendarEventDTO event, List<EventInstance> instances) {
    }
}
