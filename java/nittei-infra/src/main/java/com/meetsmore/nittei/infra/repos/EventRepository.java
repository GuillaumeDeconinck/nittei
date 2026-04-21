package com.meetsmore.nittei.infra.repos;

import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.TimeSpan;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import com.meetsmore.nittei.infra.repos.query.SearchEventsForAccountParams;
import com.meetsmore.nittei.infra.repos.query.SearchEventsForUserParams;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepository {

  record MostRecentCreatedServiceEvents(ID userId, Instant created) {}

  void insert(CalendarEvent event);

  void insertMany(List<CalendarEvent> events);

  void save(CalendarEvent event);

  Optional<CalendarEvent> find(ID eventId);

  List<CalendarEvent> findByIdAndRecurringEventId(ID eventId);

  List<CalendarEvent> findByRecurringEventIdsForTimespan(
      List<ID> recurringEventIds, TimeSpan timespan);

  List<CalendarEvent> getByExternalId(ID accountId, String externalId);

  List<CalendarEvent> findManyByExternalIds(ID accountId, List<String> externalIds);

  List<CalendarEvent> findMany(List<ID> eventIds);

  List<CalendarEvent> findByCalendar(ID calendarId, TimeSpan timespan);

  List<CalendarEvent> findEventsForUsersForTimespan(
      List<ID> userIds, TimeSpan timespan, boolean includeTentative, boolean includeNonBusy);

  List<CalendarEvent> findRecurringEventsForUsersForTimespan(
      List<ID> userIds, TimeSpan timespan, boolean includeTentative, boolean includeNonBusy);

  List<CalendarEvent> findByCalendars(List<ID> calendarIds, TimeSpan timespan);

  List<CalendarEvent> findBusyEventsAndRecurringEventsForCalendars(
      List<ID> calendarIds, TimeSpan timespan, boolean includeTentative);

  List<CalendarEvent> searchEventsForUser(SearchEventsForUserParams params);

  List<CalendarEvent> searchEventsForAccount(SearchEventsForAccountParams params);

  List<MostRecentCreatedServiceEvents> findMostRecentlyCreatedServiceEvents(
      ID serviceId, List<ID> userIds);

  List<CalendarEvent> findByService(
      ID serviceId, List<ID> userIds, Instant minTime, Instant maxTime);

  List<CalendarEvent> findUserServiceEvents(
      ID userId, boolean busy, Instant minTime, Instant maxTime);

  void delete(ID eventId);

  void deleteMany(List<ID> eventIds);

  void deleteByService(ID serviceId);

  List<CalendarEvent> findByMetadata(MetadataFindQuery query);
}
