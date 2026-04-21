package com.meetsmore.nittei.api.service;

import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.api.shared.auth.RequestContextResolver;
import com.meetsmore.nittei.api.structs.service.ServiceApi;
import com.meetsmore.nittei.domain.BusyCalendarProvider;
import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Service;
import com.meetsmore.nittei.domain.ServiceMultiPersonOptions;
import com.meetsmore.nittei.domain.ServiceResource;
import com.meetsmore.nittei.domain.ServiceWithUsers;
import com.meetsmore.nittei.domain.TimeSpan;
import com.meetsmore.nittei.domain.User;
import com.meetsmore.nittei.domain.booking.ServiceBookingSlot;
import com.meetsmore.nittei.domain.booking.ServiceBookingSlots;
import com.meetsmore.nittei.domain.booking.ServiceBookingSlotsDate;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.repos.EventRepository;
import com.meetsmore.nittei.utils.config.AppConfig;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class ServiceBookingService {

  private static final long MIN_SLOT_INTERVAL_MS = 1000L * 60 * 5;
  private static final long MAX_SLOT_INTERVAL_MS = 1000L * 60 * 60 * 2;

  private final NitteiContext ctx;
  private final RequestContextResolver auth;
  private final AppConfig appConfig;

  public ServiceBookingService(
      NitteiContext ctx, RequestContextResolver auth, AppConfig appConfig) {
    this.ctx = ctx;
    this.auth = auth;
    this.appConfig = appConfig;
  }

  public ResponseEntity<ServiceApi.GetServiceBookingSlotsAPIResponse> getServiceBookingSlots(
      HttpHeaders headers, String serviceId, ServiceApi.GetServiceBookingSlotsQueryParams query) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID sid = auth.parseId(serviceId, "Malformed serviceId");
    auth.requireAccountService(account, sid, ctx);

    if (query == null) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA,
          "Invalid datetime: null. Should be YYYY-MM-DD, e.g. January 1. 2020 => 2020-1-1");
    }

    BookingWindow bookingWindow = parseBookingWindow(query);
    ServiceWithUsers service =
        ctx.repos()
            .services()
            .findWithUsers(sid)
            .orElseThrow(
                () -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service was not found"));

    if (isGroupWithSize(service.multiPerson(), 0)) {
      return ResponseEntity.ok(
          ServiceApi.GetServiceBookingSlotsAPIResponse.from(new ServiceBookingSlots(List.of())));
    }

    List<ID> requestedHosts = parseHostUserIds(query.hostUserIds());
    List<ServiceResource> targetUsers = service.users();
    if (requestedHosts != null) {
      Set<ID> requested = new HashSet<>(requestedHosts);
      targetUsers = service.users().stream().filter(u -> requested.contains(u.userId())).toList();
    }

    List<ServiceBookingSlot> slots =
        computeServiceBookingSlots(
            service, targetUsers, bookingWindow, query.duration(), query.interval());

    if (requiresAllHostsAvailable(service.multiPerson())) {
      int hostCount = service.users().size();
      slots = slots.stream().filter(slot -> slot.userIds().size() == hostCount).toList();
    }

    return ResponseEntity.ok(
        ServiceApi.GetServiceBookingSlotsAPIResponse.from(groupBookingSlotsByDate(slots)));
  }

  public ResponseEntity<ServiceApi.CreateServiceEventIntendAPIResponse> createServiceEventIntend(
      HttpHeaders headers, String serviceId, ServiceApi.CreateServiceEventIntendRequestBody body) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID sid = auth.parseId(serviceId, "Malformed serviceId");
    auth.requireAccountService(account, sid, ctx);
    if (body == null || body.timestamp() == null) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA, "Missing booking intend timestamp");
    }

    Instant start = body.timestamp().truncatedTo(ChronoUnit.DAYS);
    Instant end = start.plus(1, ChronoUnit.DAYS);

    ServiceWithUsers service =
        ctx.repos()
            .services()
            .findWithUsers(sid)
            .orElseThrow(
                () -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service was not found"));

    BookingWindow bookingWindow = new BookingWindow(start, end);

    List<ServiceResource> targetUsers = service.users();
    if (body.hostUserIds() != null && !body.hostUserIds().isEmpty()) {
      Set<ID> requested = new HashSet<>(body.hostUserIds());
      targetUsers = service.users().stream().filter(u -> requested.contains(u.userId())).toList();
    }

    List<ServiceBookingSlot> slots =
        computeServiceBookingSlots(
            service, targetUsers, bookingWindow, body.duration(), body.interval());

    if (requiresAllHostsAvailable(service.multiPerson())) {
      int hostCount = service.users().size();
      slots = slots.stream().filter(slot -> slot.userIds().size() == hostCount).toList();
    }

    boolean createEventForHosts = true;
    ServiceBookingSlot slot =
        slots.stream()
            .filter(s -> s.start().equals(body.timestamp()))
            .findFirst()
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.BAD_CLIENT_DATA,
                        "The user is not available at the given time"));

    Integer reservations = null;
    if ((body.hostUserIds() == null || body.hostUserIds().isEmpty())
        && "group".equals(getMultiPersonVariant(service.multiPerson()))) {
      reservations = ctx.repos().reservations().count(service.id(), body.timestamp());
    }

    HostSelection selection = resolveHostSelection(service, slot, body.hostUserIds(), reservations);

    if (selection.incrementReservation()) {
      ctx.repos().reservations().increment(service.id(), body.timestamp());
    }
    createEventForHosts = selection.createEventForHosts();

    List<ID> selectedHostUserIds =
        selection.useRoundRobin()
            ? selectRoundRobinHost(service, slot.userIds())
            : selection.selectedHostUserIds();

    List<User> selectedHosts = ctx.repos().users().findMany(selectedHostUserIds);
    return ResponseEntity.ok(
        ServiceApi.CreateServiceEventIntendAPIResponse.from(selectedHosts, createEventForHosts));
  }

  public ResponseEntity<ServiceApi.RemoveServiceEventIntendAPIResponse> removeServiceEventIntend(
      HttpHeaders headers, String serviceId, ServiceApi.RemoveServiceEventIntendQueryParams query) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID sid = auth.parseId(serviceId, "Malformed serviceId");
    Service service = auth.requireAccountService(account, sid, ctx);
    if (query == null || query.timestamp() == null) {
      throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Missing timestamp");
    }
    ctx.repos().reservations().decrement(service.id(), query.timestamp());
    return ResponseEntity.ok(ServiceApi.RemoveServiceEventIntendAPIResponse.defaultValue());
  }

  private BookingWindow parseBookingWindow(ServiceApi.GetServiceBookingSlotsQueryParams query) {
    if (query == null) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA,
          "Invalid datetime: null. Should be YYYY-MM-DD, e.g. January 1. 2020 => 2020-1-1");
    }
    if (query.interval() < MIN_SLOT_INTERVAL_MS || query.interval() > MAX_SLOT_INTERVAL_MS) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA,
          "Invalid interval specified. It should be between 10 - 60 minutes inclusively and be specified as milliseconds.");
    }

    ZoneId timezone = ZoneId.of("UTC");
    if (query.timezone() != null && !query.timezone().isBlank()) {
      try {
        timezone = ZoneId.of(query.timezone());
      } catch (RuntimeException ignored) {
        throw new NitteiApiException(
            NitteiErrorCode.BAD_CLIENT_DATA,
            "Invalid datetime: "
                + query.timezone()
                + ". Should be YYYY-MM-DD, e.g. January 1. 2020 => 2020-1-1");
      }
    }

    LocalDate startDate = parseDateOrThrow(query.startDate());
    LocalDate endDate = parseDateOrThrow(query.endDate());

    Instant startTime = ZonedDateTime.of(startDate.atStartOfDay(), timezone).toInstant();
    Instant endTime = ZonedDateTime.of(endDate.plusDays(1).atStartOfDay(), timezone).toInstant();
    if (!startTime.isBefore(endTime)) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA, "The provided start and end is invalid");
    }

    if (ChronoUnit.MILLIS.between(startTime, endTime)
        > appConfig.getBookingSlotsQueryDurationLimit()) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA, "The provided start and end is invalid");
    }

    return new BookingWindow(startTime, endTime);
  }

  private LocalDate parseDateOrThrow(String value) {
    if (value == null || value.isBlank()) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA,
          "Invalid datetime: "
              + value
              + ". Should be YYYY-MM-DD, e.g. January 1. 2020 => 2020-1-1");
    }
    String[] parts = value.split("-");
    if (parts.length != 3) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA,
          "Invalid datetime: "
              + value
              + ". Should be YYYY-MM-DD, e.g. January 1. 2020 => 2020-1-1");
    }
    try {
      int year = Integer.parseInt(parts[0]);
      int month = Integer.parseInt(parts[1]);
      int day = Integer.parseInt(parts[2]);
      if (year < 1970 || year > 2100) {
        throw new IllegalArgumentException("Invalid year");
      }
      return LocalDate.of(year, month, day);
    } catch (NumberFormatException | DateTimeException e) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA,
          "Invalid datetime: "
              + value
              + ". Should be YYYY-MM-DD, e.g. January 1. 2020 => 2020-1-1");
    }
  }

  private List<ID> parseHostUserIds(String hostUserIds) {
    if (hostUserIds == null || hostUserIds.isBlank()) {
      return null;
    }
    List<ID> parsed = new ArrayList<>();
    for (String raw : hostUserIds.split(",")) {
      try {
        parsed.add(auth.parseId(raw.trim(), "Malformed host user id"));
      } catch (RuntimeException ignored) {
        // Keep parity with Rust parse_vec_query_value: invalid IDs are ignored.
      }
    }
    return parsed;
  }

  private List<ServiceBookingSlot> computeServiceBookingSlots(
      ServiceWithUsers service,
      List<ServiceResource> users,
      BookingWindow bookingWindow,
      long duration,
      long interval) {
    List<UserBookingAvailability> userAvailabilities = new ArrayList<>();

    for (ServiceResource resource : users) {
      BookingWindow userWindow = adjustWindowForUser(resource, bookingWindow);
      if (userWindow == null) {
        continue;
      }

      List<TimeSpan> freeIntervals = getUserAvailabilityIntervals(resource, userWindow);
      if (freeIntervals.isEmpty()) {
        continue;
      }
      List<TimeSpan> busyIntervals = getUserBusyIntervals(resource, userWindow, service.id());
      freeIntervals = subtractBusyFromFree(freeIntervals, busyIntervals);

      userAvailabilities.add(
          new UserBookingAvailability(
              resource.userId(),
              freeIntervals,
              new TimeSpan(userWindow.start(), userWindow.end())));
    }

    return aggregateServiceBookingSlots(userAvailabilities, duration, interval);
  }

  private BookingWindow adjustWindowForUser(ServiceResource user, BookingWindow bookingWindow) {
    Instant start = bookingWindow.start();
    Instant end = bookingWindow.end();

    Instant firstAvailable =
        ctx.sys().getTimestamp().plusMillis(user.closestBookingTime() * 60 * 1000);
    if (start.isBefore(firstAvailable)) {
      start = firstAvailable;
    }

    if (user.furthestBookingTime() != null) {
      Instant lastAvailable =
          ctx.sys().getTimestamp().plusMillis(user.furthestBookingTime() * 60 * 1000);
      if (lastAvailable.isBefore(end)) {
        end = lastAvailable;
      }
    }

    if (!start.isBefore(end)) {
      return null;
    }
    if (ChronoUnit.MILLIS.between(start, end) > appConfig.getBookingSlotsQueryDurationLimit()) {
      return null;
    }

    return new BookingWindow(start, end);
  }

  private List<TimeSpan> getUserAvailabilityIntervals(
      ServiceResource user, BookingWindow bookingWindow) {
    String variant =
        user.availability() == null || user.availability().variant() == null
            ? "empty"
            : user.availability().variant().toLowerCase(Locale.ROOT);

    if ("calendar".equals(variant) && user.availability().id() != null) {
      ID calendarId = user.availability().id();
      Calendar calendar = ctx.repos().calendars().find(calendarId).orElse(null);
      if (calendar == null || !calendar.userId().equals(user.userId())) {
        return List.of();
      }
      List<CalendarEvent> events =
          ctx.repos()
              .events()
              .findByCalendar(calendarId, new TimeSpan(bookingWindow.start(), bookingWindow.end()));
      List<TimeSpan> free = new ArrayList<>();
      List<TimeSpan> busy = new ArrayList<>();
      for (CalendarEvent e : events) {
        TimeSpan eventInterval = toTimeSpan(e);
        if (eventInterval == null) {
          continue;
        }
        if (e.busy()) {
          busy.add(eventInterval);
        } else {
          free.add(eventInterval);
        }
      }
      return subtractBusyFromFree(free, busy);
    }

    if ("schedule".equals(variant) && user.availability().id() != null) {
      var schedule = ctx.repos().schedules().find(user.availability().id()).orElse(null);
      if (schedule == null || !schedule.userId().equals(user.userId())) {
        return List.of();
      }

      ZoneId scheduleZone;
      try {
        scheduleZone =
            ZoneId.of(
                schedule.timezone() == null || schedule.timezone().isBlank()
                    ? "UTC"
                    : schedule.timezone());
      } catch (Exception ignored) {
        scheduleZone = ZoneId.of("UTC");
      }

      List<TimeSpan> free = new ArrayList<>();
      LocalDate startDate = bookingWindow.start().atZone(scheduleZone).toLocalDate().minusDays(1);
      LocalDate endDate = bookingWindow.end().atZone(scheduleZone).toLocalDate().plusDays(1);
      for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
        for (com.meetsmore.nittei.domain.ScheduleRule rule : schedule.rules()) {
          if (!scheduleRuleMatchesDate(rule, date)) {
            continue;
          }
          if (rule.intervals() == null) {
            continue;
          }
          for (Map<String, Object> rawInterval : rule.intervals()) {
            TimeSpan computed =
                parseScheduleInterval(date, scheduleZone, rawInterval, bookingWindow);
            if (computed != null) {
              free.add(computed);
            }
          }
        }
      }
      return mergeIntervals(free);
    }

    return List.of();
  }

  static boolean scheduleRuleMatchesDate(
      com.meetsmore.nittei.domain.ScheduleRule rule, LocalDate date) {
    if (rule == null) {
      return false;
    }
    String type = rule.type();
    Object value = rule.value();
    if (type == null || value == null) {
      return true;
    }
    if ("date".equalsIgnoreCase(type)) {
      LocalDate normalizedDate = parseScheduleRuleDate(value);
      return normalizedDate != null && normalizedDate.equals(date);
    }
    if (!"wday".equalsIgnoreCase(type)) {
      return true;
    }
    String day =
        switch (date.getDayOfWeek()) {
          case MONDAY -> "Mon";
          case TUESDAY -> "Tue";
          case WEDNESDAY -> "Wed";
          case THURSDAY -> "Thu";
          case FRIDAY -> "Fri";
          case SATURDAY -> "Sat";
          case SUNDAY -> "Sun";
        };
    return day.equalsIgnoreCase(String.valueOf(value));
  }

  private static LocalDate parseScheduleRuleDate(Object value) {
    if (value == null) {
      return null;
    }
    String[] parts = String.valueOf(value).trim().split("-");
    if (parts.length != 3) {
      return null;
    }
    try {
      return LocalDate.of(
          Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private TimeSpan parseScheduleInterval(
      LocalDate date, ZoneId zone, Map<String, Object> raw, BookingWindow bookingWindow) {
    if (raw == null) {
      return null;
    }
    LocalTime start = parseHourMinute(raw.get("start"));
    LocalTime end = parseHourMinute(raw.get("end"));
    if (start == null || end == null || !start.isBefore(end)) {
      return null;
    }
    Instant intervalStart = date.atTime(start).atZone(zone).toInstant();
    Instant intervalEnd = date.atTime(end).atZone(zone).toInstant();
    if (!intervalStart.isBefore(intervalEnd)) {
      return null;
    }
    Instant clippedStart =
        intervalStart.isBefore(bookingWindow.start()) ? bookingWindow.start() : intervalStart;
    Instant clippedEnd =
        intervalEnd.isAfter(bookingWindow.end()) ? bookingWindow.end() : intervalEnd;
    if (!clippedStart.isBefore(clippedEnd)) {
      return null;
    }
    return new TimeSpan(clippedStart, clippedEnd);
  }

  @SuppressWarnings("unchecked")
  private LocalTime parseHourMinute(Object raw) {
    if (!(raw instanceof Map<?, ?> map)) {
      return null;
    }
    Object hoursRaw = ((Map<String, Object>) map).get("hours");
    Object minutesRaw = ((Map<String, Object>) map).get("minutes");
    int hours = toInt(hoursRaw);
    int minutes = toInt(minutesRaw);
    if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
      return null;
    }
    return LocalTime.of(hours, minutes);
  }

  private int toInt(Object raw) {
    if (raw instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(raw));
    } catch (Exception e) {
      return -1;
    }
  }

  static List<TimeSpan> mergeIntervals(List<TimeSpan> intervals) {
    if (intervals.isEmpty()) {
      return List.of();
    }
    List<TimeSpan> sorted =
        intervals.stream()
            .sorted(Comparator.comparing(TimeSpan::start).thenComparing(TimeSpan::end))
            .toList();
    List<TimeSpan> merged = new ArrayList<>();
    TimeSpan current = sorted.get(0);
    for (int i = 1; i < sorted.size(); i++) {
      TimeSpan next = sorted.get(i);
      if (!next.start().isAfter(current.end())) {
        Instant maxEnd = current.end().isAfter(next.end()) ? current.end() : next.end();
        current = new TimeSpan(current.start(), maxEnd);
      } else {
        merged.add(current);
        current = next;
      }
    }
    merged.add(current);
    return merged;
  }

  private List<TimeSpan> getUserBusyIntervals(
      ServiceResource user, BookingWindow bookingWindow, ID currentServiceId) {
    List<TimeSpan> busy = new ArrayList<>();

    List<ServiceResource> allServiceResources =
        ctx.repos().serviceUsers().findByUser(user.userId());

    List<CalendarEvent> otherServiceEvents =
        ctx.repos()
            .events()
            .findUserServiceEvents(
                user.userId(), false, bookingWindow.start(), bookingWindow.end());
    for (CalendarEvent e : otherServiceEvents) {
      if (e.serviceId() != null && !e.serviceId().equals(currentServiceId)) {
        TimeSpan interval = toTimeSpan(e);
        if (interval != null) {
          busy.add(interval);
        }
      }
    }

    List<BusyCalendarProvider> busyCalendars =
        ctx.repos().serviceUserBusyCalendars().find(user.serviceId(), user.userId());

    List<ID> nitteiBusyCalendarIds =
        busyCalendars.stream()
            .filter(c -> "nittei".equalsIgnoreCase(c.provider()))
            .map(c -> auth.parseId(c.id(), "Malformed busy calendar id"))
            .toList();

    List<Calendar> nitteiBusyCalendars =
        ctx.repos().calendars().findByUser(user.userId()).stream()
            .filter(c -> nitteiBusyCalendarIds.contains(c.id()))
            .toList();

    for (Calendar calendar : nitteiBusyCalendars) {
      List<CalendarEvent> events =
          ctx.repos()
              .events()
              .findByCalendar(
                  calendar.id(), new TimeSpan(bookingWindow.start(), bookingWindow.end()));
      for (CalendarEvent event : events) {
        if (!event.busy()) {
          continue;
        }
        TimeSpan interval = toTimeSpan(event);
        if (interval == null) {
          continue;
        }

        if (event.serviceId() != null) {
          ServiceResource sourceService =
              allServiceResources.stream()
                  .filter(s -> s.serviceId().equals(event.serviceId()))
                  .findFirst()
                  .orElse(null);
          if (sourceService != null) {
            interval =
                new TimeSpan(
                    interval.start().minusMillis(sourceService.bufferBefore() * 60 * 1000),
                    interval.end().plusMillis(sourceService.bufferAfter() * 60 * 1000));
          }
        }

        busy.add(interval);
      }
    }

    // External provider busy calendars can be added here once provider freebusy mappings are
    // completed.
    return busy;
  }

  static ServiceBookingSlots groupBookingSlotsByDate(List<ServiceBookingSlot> slots) {
    Map<String, List<ServiceBookingSlot>> grouped = new HashMap<>();
    for (ServiceBookingSlot slot : slots) {
      ZonedDateTime zdt = ZonedDateTime.ofInstant(slot.start(), ZoneId.of("UTC"));
      String date = zdt.getYear() + "-" + zdt.getMonthValue() + "-" + zdt.getDayOfMonth();
      grouped.computeIfAbsent(date, ignored -> new ArrayList<>()).add(slot);
    }
    List<ServiceBookingSlotsDate> dates =
        grouped.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(
                e ->
                    new ServiceBookingSlotsDate(
                        e.getKey(),
                        e.getValue().stream()
                            .sorted(Comparator.comparing(ServiceBookingSlot::start))
                            .toList()))
            .toList();
    return new ServiceBookingSlots(dates);
  }

  static List<TimeSpan> subtractBusyFromFree(List<TimeSpan> free, List<TimeSpan> busy) {
    if (free.isEmpty()) {
      return List.of();
    }
    if (busy.isEmpty()) {
      return free;
    }

    List<TimeSpan> result = new ArrayList<>();
    for (TimeSpan freeInterval : free) {
      List<TimeSpan> remainders = List.of(freeInterval);
      for (TimeSpan busyInterval : busy) {
        List<TimeSpan> next = new ArrayList<>();
        for (TimeSpan candidate : remainders) {
          next.addAll(subtract(candidate, busyInterval));
        }
        remainders = next;
        if (remainders.isEmpty()) {
          break;
        }
      }
      result.addAll(remainders);
    }
    return result;
  }

  static List<TimeSpan> subtract(TimeSpan free, TimeSpan busy) {
    if (!busy.start().isBefore(free.end()) || !busy.end().isAfter(free.start())) {
      return List.of(free);
    }

    List<TimeSpan> parts = new ArrayList<>();
    if (busy.start().isAfter(free.start())) {
      parts.add(new TimeSpan(free.start(), busy.start()));
    }
    if (busy.end().isBefore(free.end())) {
      parts.add(new TimeSpan(busy.end(), free.end()));
    }
    return parts;
  }

  static List<Instant> computeSlotStarts(
      TimeSpan freeInterval, TimeSpan bookingWindow, long duration, long interval) {
    if (freeInterval == null || bookingWindow == null || duration <= 0 || interval <= 0) {
      return List.of();
    }

    List<Instant> slotStarts = new ArrayList<>();
    Instant cursor = bookingWindow.start();
    while (!cursor.plusMillis(interval).isAfter(bookingWindow.end())) {
      if (!cursor.isBefore(freeInterval.start())
          && !cursor.plusMillis(duration).isAfter(freeInterval.end())) {
        slotStarts.add(cursor);
      }
      cursor = cursor.plusMillis(interval);
    }
    return slotStarts;
  }

  static List<ServiceBookingSlot> aggregateServiceBookingSlots(
      List<UserBookingAvailability> userAvailabilities, long duration, long interval) {
    Map<Instant, Set<ID>> slotUsers = new LinkedHashMap<>();
    for (UserBookingAvailability availability : userAvailabilities) {
      for (TimeSpan free : availability.freeIntervals()) {
        for (Instant slotStart :
            computeSlotStarts(free, availability.bookingWindow(), duration, interval)) {
          slotUsers
              .computeIfAbsent(slotStart, ignored -> new LinkedHashSet<>())
              .add(availability.userId());
        }
      }
    }

    return slotUsers.entrySet().stream()
        .map(e -> new ServiceBookingSlot(e.getKey(), duration, e.getValue().stream().toList()))
        .sorted(Comparator.comparing(ServiceBookingSlot::start))
        .toList();
  }

  static HostSelection resolveHostSelection(
      ServiceWithUsers service,
      ServiceBookingSlot slot,
      List<ID> requestedHostUserIds,
      Integer reservationsAtTimestamp) {
    if (slot == null || slot.userIds().isEmpty()) {
      throw userNotAvailable();
    }

    if (requestedHostUserIds != null && !requestedHostUserIds.isEmpty()) {
      for (ID hostId : requestedHostUserIds) {
        if (!slot.userIds().contains(hostId)) {
          throw userNotAvailable();
        }
      }
      return new HostSelection(requestedHostUserIds, true, false, false);
    }

    return switch (getMultiPersonVariant(service.multiPerson())) {
      case "roundrobinalgorithm" -> new HostSelection(slot.userIds(), true, false, true);
      case "collective" ->
          new HostSelection(allHostsOrThrow(service, slot.userIds()), true, false, false);
      case "group" -> {
        List<ID> allHosts = allHostsOrThrow(service, slot.userIds());
        int reservations = reservationsAtTimestamp == null ? 0 : reservationsAtTimestamp;
        int maxGroup = parseGroupSize(service.multiPerson());
        boolean createEventForHosts = reservations + 1 >= maxGroup;
        yield new HostSelection(allHosts, createEventForHosts, true, false);
      }
      default -> new HostSelection(slot.userIds(), true, false, false);
    };
  }

  private static List<ID> allHostsOrThrow(ServiceWithUsers service, List<ID> userIdsAtSlot) {
    List<ID> allHosts = service.users().stream().map(ServiceResource::userId).toList();
    if (userIdsAtSlot.size() < allHosts.size()) {
      throw userNotAvailable();
    }
    return allHosts;
  }

  private static NitteiApiException userNotAvailable() {
    return new NitteiApiException(
        NitteiErrorCode.BAD_CLIENT_DATA, "The user is not available at the given time");
  }

  private TimeSpan toTimeSpan(CalendarEvent event) {
    if (event.startTime() == null || event.endTime() == null) {
      return null;
    }
    return new TimeSpan(event.startTime(), event.endTime());
  }

  private List<ID> selectRoundRobinHost(ServiceWithUsers service, List<ID> userIdsAtSlot) {
    String algorithm =
        service.multiPerson() == null || service.multiPerson().data() == null
            ? "availability"
            : String.valueOf(service.multiPerson().data()).toLowerCase(Locale.ROOT);

    if (userIdsAtSlot.size() == 1) {
      return List.of(userIdsAtSlot.get(0));
    }

    if (algorithm.contains("equaldistribution")) {
      Instant now = Instant.now();
      Instant inTwoMonths = now.plus(61, ChronoUnit.DAYS);
      List<CalendarEvent> events =
          ctx.repos().events().findByService(service.id(), userIdsAtSlot, now, inTwoMonths);
      return List.of(randomPick(selectEqualDistributionCandidates(userIdsAtSlot, events)));
    }

    List<EventRepository.MostRecentCreatedServiceEvents> events =
        ctx.repos().events().findMostRecentlyCreatedServiceEvents(service.id(), userIdsAtSlot);
    return List.of(randomPick(selectAvailabilityCandidates(userIdsAtSlot, events)));
  }

  static List<ID> selectEqualDistributionCandidates(
      List<ID> userIdsAtSlot, List<CalendarEvent> events) {
    if (userIdsAtSlot == null || userIdsAtSlot.isEmpty()) {
      return List.of();
    }

    Map<ID, Long> counts = new LinkedHashMap<>();
    for (ID uid : userIdsAtSlot) {
      counts.put(uid, 0L);
    }
    for (CalendarEvent event : events) {
      counts.computeIfPresent(event.userId(), (k, v) -> v + 1);
    }

    long min = counts.values().stream().min(Long::compareTo).orElse(0L);
    return counts.entrySet().stream()
        .filter(e -> e.getValue() == min)
        .map(Map.Entry::getKey)
        .toList();
  }

  static List<ID> selectAvailabilityCandidates(
      List<ID> userIdsAtSlot, List<EventRepository.MostRecentCreatedServiceEvents> events) {
    if (userIdsAtSlot == null || userIdsAtSlot.isEmpty()) {
      return List.of();
    }

    Instant oldest = null;
    List<ID> candidates = new ArrayList<>();
    for (EventRepository.MostRecentCreatedServiceEvents e : events) {
      Instant created = e.created();
      if (candidates.isEmpty() || compareNullableInstant(created, oldest) < 0) {
        oldest = created;
        candidates = new ArrayList<>(List.of(e.userId()));
      } else if (compareNullableInstant(created, oldest) == 0) {
        candidates.add(e.userId());
      }
    }
    if (candidates.isEmpty()) {
      return userIdsAtSlot;
    }
    return candidates;
  }

  static int compareNullableInstant(Instant a, Instant b) {
    if (a == null && b == null) {
      return 0;
    }
    if (a == null) {
      return -1;
    }
    if (b == null) {
      return 1;
    }
    return a.compareTo(b);
  }

  private ID randomPick(List<ID> values) {
    if (values.isEmpty()) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA, "The user is not available at the given time");
    }
    int index = ThreadLocalRandom.current().nextInt(values.size());
    return values.get(index);
  }

  private static boolean requiresAllHostsAvailable(ServiceMultiPersonOptions options) {
    String variant = getMultiPersonVariant(options);
    return "collective".equals(variant) || "group".equals(variant);
  }

  private static boolean isGroupWithSize(ServiceMultiPersonOptions options, int expected) {
    if (!"group".equals(getMultiPersonVariant(options))) {
      return false;
    }
    return parseGroupSize(options) == expected;
  }

  private static int parseGroupSize(ServiceMultiPersonOptions options) {
    if (options == null || options.data() == null) {
      return 0;
    }
    if (options.data() instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(options.data()));
    } catch (Exception e) {
      return 0;
    }
  }

  private static String getMultiPersonVariant(ServiceMultiPersonOptions options) {
    if (options == null || options.variant() == null) {
      return "roundrobinalgorithm";
    }
    return options.variant().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
  }

  private record BookingWindow(Instant start, Instant end) {}

  record UserBookingAvailability(ID userId, List<TimeSpan> freeIntervals, TimeSpan bookingWindow) {}

  record HostSelection(
      List<ID> selectedHostUserIds,
      boolean createEventForHosts,
      boolean incrementReservation,
      boolean useRoundRobin) {}
}
