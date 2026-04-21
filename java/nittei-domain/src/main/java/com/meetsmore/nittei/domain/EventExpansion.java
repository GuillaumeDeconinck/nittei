package com.meetsmore.nittei.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EventExpansion {

  private static final int MAX_INSTANCES = 100;

  private EventExpansion() {}

  public static List<EventInstance> expandAllEventsAndRemoveExceptions(
      Map<ID, Calendar> calendarsById, List<CalendarEvent> events, TimeSpan timespan) {
    Map<ID, List<Instant>> exceptionsByRecurringEventId = generateExceptionsMap(events);
    List<EventInstance> allInstances = new ArrayList<>();

    for (CalendarEvent event : events) {
      Calendar calendar = calendarsById.get(event.calendarId());
      if (calendar == null) {
        continue;
      }
      List<Instant> exceptions = exceptionsByRecurringEventId.getOrDefault(event.id(), List.of());
      allInstances.addAll(expandEventAndRemoveExceptions(calendar, event, exceptions, timespan));
    }
    return allInstances;
  }

  public static List<EventInstance> toCompatibleInstances(List<EventInstance> instances) {
    if (instances.isEmpty()) {
      return List.of();
    }
    List<EventInstance> sorted = new ArrayList<>(instances);
    sorted.sort(Comparator.comparing(EventInstance::startTime));

    List<EventInstance> merged = new ArrayList<>();
    for (EventInstance current : sorted) {
      if (merged.isEmpty()) {
        merged.add(current);
        continue;
      }

      EventInstance previous = merged.get(merged.size() - 1);
      if (previous.busy() == current.busy()
          && !previous.endTime().isBefore(current.startTime())
          && !current.endTime().isBefore(previous.startTime())) {
        Instant mergedStart =
            previous.startTime().isBefore(current.startTime())
                ? previous.startTime()
                : current.startTime();
        Instant mergedEnd =
            previous.endTime().isAfter(current.endTime()) ? previous.endTime() : current.endTime();
        merged.set(merged.size() - 1, new EventInstance(mergedStart, mergedEnd, previous.busy()));
      } else {
        merged.add(current);
      }
    }
    return merged;
  }

  private static Map<ID, List<Instant>> generateExceptionsMap(List<CalendarEvent> events) {
    Map<ID, List<Instant>> exceptions = new HashMap<>();
    for (CalendarEvent event : events) {
      if (event.recurringEventId() != null && event.originalStartTime() != null) {
        exceptions
            .computeIfAbsent(event.recurringEventId(), ignored -> new ArrayList<>())
            .add(event.originalStartTime());
      }
    }
    return exceptions;
  }

  public static List<EventInstance> expandEventAndRemoveExceptions(
      Calendar calendar, CalendarEvent event, List<Instant> exceptions, TimeSpan timespan) {
    List<EventInstance> expanded = expandEvent(calendar, event, timespan);
    if (exceptions.isEmpty()) {
      return expanded;
    }
    Set<Instant> exceptionStarts = new HashSet<>(exceptions);
    return expanded.stream().filter(i -> !exceptionStarts.contains(i.startTime())).toList();
  }

  private static List<EventInstance> expandEvent(
      Calendar calendar, CalendarEvent event, TimeSpan timespan) {
    if (event.recurrence() == null) {
      if (event.exdates() != null && event.exdates().contains(event.startTime())) {
        return List.of();
      }
      return List.of(new EventInstance(event.startTime(), computeEndTime(event), event.busy()));
    }

    ZoneId zoneId =
        ZoneId.of(
            calendar.settings() == null || calendar.settings().timezone() == null
                ? "UTC"
                : calendar.settings().timezone());

    RRuleOptions recurrence = event.recurrence();
    int interval =
        recurrence.interval() == null || recurrence.interval() <= 0 ? 1 : recurrence.interval();
    Integer maxByCount = recurrence.count();
    Instant until = recurrence.until();

    ZonedDateTime occurrence = event.startTime().atZone(zoneId);
    List<EventInstance> results = new ArrayList<>();
    int generated = 0;
    while (generated < MAX_INSTANCES) {
      Instant occurrenceStart = occurrence.toInstant();
      if (until != null && occurrenceStart.isAfter(until)) {
        break;
      }
      if (maxByCount != null && generated >= maxByCount) {
        break;
      }

      if (!occurrenceStart.isBefore(timespan.start()) && !occurrenceStart.isAfter(timespan.end())) {
        if (event.exdates() == null || !event.exdates().contains(occurrenceStart)) {
          results.add(
              new EventInstance(
                  occurrenceStart,
                  occurrenceStart.plusMillis(computeDurationMs(event)),
                  event.busy()));
        }
      }

      generated++;
      occurrence = advance(occurrence, recurrence.freq(), interval);
    }
    return results;
  }

  private static ZonedDateTime advance(ZonedDateTime start, RRuleFrequency freq, int interval) {
    return switch (freq == null ? RRuleFrequency.DAILY : freq) {
      case DAILY -> start.plusDays(interval);
      case WEEKLY -> start.plusWeeks(interval);
      case MONTHLY -> start.plusMonths(interval);
      case YEARLY -> start.plusYears(interval);
    };
  }

  private static Instant computeEndTime(CalendarEvent event) {
    if (event.endTime() != null) {
      return event.endTime();
    }
    return event.startTime().plusMillis(computeDurationMs(event));
  }

  private static long computeDurationMs(CalendarEvent event) {
    if (event.duration() != null) {
      return event.duration();
    }
    if (event.endTime() != null) {
      return Math.max(0L, event.endTime().toEpochMilli() - event.startTime().toEpochMilli());
    }
    return 0L;
  }
}
