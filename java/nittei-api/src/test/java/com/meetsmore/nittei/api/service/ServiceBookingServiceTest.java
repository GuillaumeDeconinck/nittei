package com.meetsmore.nittei.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.ScheduleRule;
import com.meetsmore.nittei.domain.ServiceMultiPersonOptions;
import com.meetsmore.nittei.domain.ServiceResource;
import com.meetsmore.nittei.domain.ServiceWithUsers;
import com.meetsmore.nittei.domain.TimeSpan;
import com.meetsmore.nittei.domain.booking.ServiceBookingSlot;
import com.meetsmore.nittei.infra.repos.EventRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServiceBookingServiceTest {

  @Test
  void scheduleDateRuleMatchesPaddedDates() {
    ScheduleRule rule = new ScheduleRule("date", "2026-04-02", List.of());

    assertTrue(ServiceBookingService.scheduleRuleMatchesDate(rule, LocalDate.of(2026, 4, 2)));
  }

  @Test
  void scheduleDateRuleMatchesUnpaddedDates() {
    ScheduleRule rule = new ScheduleRule("date", "2026-4-2", List.of());

    assertTrue(ServiceBookingService.scheduleRuleMatchesDate(rule, LocalDate.of(2026, 4, 2)));
  }

  @Test
  void scheduleDateRuleRejectsDifferentDates() {
    ScheduleRule rule = new ScheduleRule("date", "2026-4-2", List.of());

    assertFalse(ServiceBookingService.scheduleRuleMatchesDate(rule, LocalDate.of(2026, 4, 3)));
  }

  @Test
  void computeSlotStartsRejectsFreeIntervalThatIsTooShort() {
    assertEquals(
        List.of(), ServiceBookingService.computeSlotStarts(span(2, 12), span(0, 100), 10, 10));
  }

  @Test
  void computeSlotStartsMatchesSingleSlotRustCase() {
    assertEquals(
        List.of(instant(10)),
        ServiceBookingService.computeSlotStarts(span(2, 22), span(0, 100), 10, 10));
  }

  @Test
  void computeSlotStartsMatchesMultiSlotRustCase() {
    assertEquals(
        List.of(instant(10), instant(20), instant(30)),
        ServiceBookingService.computeSlotStarts(span(2, 42), span(0, 100), 10, 10));
  }

  @Test
  void computeSlotStartsHandlesWindowCrossingStart() {
    assertEquals(
        List.of(instant(10), instant(20)),
        ServiceBookingService.computeSlotStarts(span(2, 30), span(10, 100), 10, 10));
  }

  @Test
  void computeSlotStartsHandlesWindowCrossingEnd() {
    assertEquals(
        List.of(instant(90)),
        ServiceBookingService.computeSlotStarts(span(81, 120), span(0, 100), 10, 10));
  }

  @Test
  void subtractBusyFromFreeSplitsIntervalAroundBusyBlock() {
    assertEquals(
        List.of(span(0, 30), span(40, 100)),
        ServiceBookingService.subtractBusyFromFree(List.of(span(0, 100)), List.of(span(30, 40))));
  }

  @Test
  void mergeIntervalsCoalescesOverlappingAndTouchingSpans() {
    assertEquals(
        List.of(span(0, 30), span(40, 60)),
        ServiceBookingService.mergeIntervals(
            List.of(span(10, 20), span(0, 15), span(20, 30), span(40, 50), span(45, 60))));
  }

  @Test
  void groupBookingSlotsByDateMatchesRustGroupingShape() {
    ID userId = new ID("00000000-0000-0000-0000-000000000001");
    ServiceBookingSlot dayOneA =
        new ServiceBookingSlot(instant(0), 15 * 60 * 1000L, List.of(userId));
    ServiceBookingSlot dayOneB =
        new ServiceBookingSlot(instant(15 * 60 * 1000L), 15 * 60 * 1000L, List.of(userId));
    ServiceBookingSlot dayTwo =
        new ServiceBookingSlot(instant(24L * 60 * 60 * 1000), 15 * 60 * 1000L, List.of(userId));

    var grouped = ServiceBookingService.groupBookingSlotsByDate(List.of(dayTwo, dayOneB, dayOneA));

    assertEquals(2, grouped.dates().size());
    assertEquals("1970-1-1", grouped.dates().get(0).date());
    assertEquals(List.of(dayOneA, dayOneB), grouped.dates().get(0).slots());
    assertEquals("1970-1-2", grouped.dates().get(1).date());
    assertEquals(List.of(dayTwo), grouped.dates().get(1).slots());
  }

  @Test
  void aggregateServiceBookingSlotsMatchesSingleUserRustCase() {
    ID userId = new ID("00000000-0000-0000-0000-000000000011");

    assertEquals(
        List.of(
            new ServiceBookingSlot(instant(10), 10, List.of(userId)),
            new ServiceBookingSlot(instant(20), 10, List.of(userId))),
        ServiceBookingService.aggregateServiceBookingSlots(
            List.of(
                new ServiceBookingService.UserBookingAvailability(
                    userId, List.of(span(2, 30)), span(10, 100))),
            10,
            10));
  }

  @Test
  void aggregateServiceBookingSlotsMatchesTwoUserRustCase() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000021");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000022");

    assertEquals(
        List.of(
            new ServiceBookingSlot(instant(10), 10, List.of(userId1, userId2)),
            new ServiceBookingSlot(instant(20), 10, List.of(userId1, userId2)),
            new ServiceBookingSlot(instant(40), 10, List.of(userId2))),
        ServiceBookingService.aggregateServiceBookingSlots(
            List.of(
                new ServiceBookingService.UserBookingAvailability(
                    userId1, List.of(span(2, 30)), span(10, 100)),
                new ServiceBookingService.UserBookingAvailability(
                    userId2, List.of(span(2, 30), span(33, 52)), span(10, 100))),
            10,
            10));
  }

  @Test
  void aggregateServiceBookingSlotsDoesNotDuplicateUserIdsWhenIntervalsOverlap() {
    ID userId = new ID("00000000-0000-0000-0000-000000000031");

    assertEquals(
        List.of(
            new ServiceBookingSlot(instant(10), 10, List.of(userId)),
            new ServiceBookingSlot(instant(20), 10, List.of(userId))),
        ServiceBookingService.aggregateServiceBookingSlots(
            List.of(
                new ServiceBookingService.UserBookingAvailability(
                    userId, List.of(span(2, 22), span(10, 30)), span(0, 100))),
            10,
            10));
  }

  @Test
  void resolveHostSelectionKeepsExplicitHostsWhenAllRequestedHostsAreAvailable() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000041");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000042");
    ServiceWithUsers service = serviceWithUsers("round-robin-algorithm", null, userId1, userId2);
    ServiceBookingSlot slot = new ServiceBookingSlot(instant(10), 10, List.of(userId1, userId2));

    var selection =
        ServiceBookingService.resolveHostSelection(service, slot, List.of(userId2), null);

    assertEquals(List.of(userId2), selection.selectedHostUserIds());
    assertTrue(selection.createEventForHosts());
    assertFalse(selection.incrementReservation());
    assertFalse(selection.useRoundRobin());
  }

  @Test
  void resolveHostSelectionRejectsExplicitHostsWhenRequestedHostIsUnavailable() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000051");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000052");
    ServiceWithUsers service = serviceWithUsers("round-robin-algorithm", null, userId1, userId2);
    ServiceBookingSlot slot = new ServiceBookingSlot(instant(10), 10, List.of(userId1));

    assertThrows(
        NitteiApiException.class,
        () -> ServiceBookingService.resolveHostSelection(service, slot, List.of(userId2), null));
  }

  @Test
  void resolveHostSelectionRequiresAllHostsForCollectiveServices() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000061");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000062");
    ServiceWithUsers service = serviceWithUsers("collective", null, userId1, userId2);
    ServiceBookingSlot slot = new ServiceBookingSlot(instant(10), 10, List.of(userId1, userId2));

    var selection = ServiceBookingService.resolveHostSelection(service, slot, null, null);

    assertEquals(List.of(userId1, userId2), selection.selectedHostUserIds());
    assertTrue(selection.createEventForHosts());
    assertFalse(selection.incrementReservation());
    assertFalse(selection.useRoundRobin());
  }

  @Test
  void resolveHostSelectionRejectsCollectiveServicesWhenNotAllHostsAreAvailable() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000071");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000072");
    ServiceWithUsers service = serviceWithUsers("collective", null, userId1, userId2);
    ServiceBookingSlot slot = new ServiceBookingSlot(instant(10), 10, List.of(userId1));

    assertThrows(
        NitteiApiException.class,
        () -> ServiceBookingService.resolveHostSelection(service, slot, null, null));
  }

  @Test
  void resolveHostSelectionForGroupDefersEventUntilCapacityIsReached() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000081");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000082");
    ServiceWithUsers service = serviceWithUsers("group", 3, userId1, userId2);
    ServiceBookingSlot slot = new ServiceBookingSlot(instant(10), 10, List.of(userId1, userId2));

    var selection = ServiceBookingService.resolveHostSelection(service, slot, null, 1);

    assertEquals(List.of(userId1, userId2), selection.selectedHostUserIds());
    assertFalse(selection.createEventForHosts());
    assertTrue(selection.incrementReservation());
    assertFalse(selection.useRoundRobin());
  }

  @Test
  void resolveHostSelectionForGroupCreatesEventWhenCapacityIsReached() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000091");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000092");
    ServiceWithUsers service = serviceWithUsers("group", 3, userId1, userId2);
    ServiceBookingSlot slot = new ServiceBookingSlot(instant(10), 10, List.of(userId1, userId2));

    var selection = ServiceBookingService.resolveHostSelection(service, slot, null, 2);

    assertEquals(List.of(userId1, userId2), selection.selectedHostUserIds());
    assertTrue(selection.createEventForHosts());
    assertTrue(selection.incrementReservation());
    assertFalse(selection.useRoundRobin());
  }

  @Test
  void resolveHostSelectionFlagsRoundRobinServicesForLaterHostChoice() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000101");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000102");
    ServiceWithUsers service = serviceWithUsers("round-robin-algorithm", null, userId1, userId2);
    ServiceBookingSlot slot = new ServiceBookingSlot(instant(10), 10, List.of(userId1, userId2));

    var selection = ServiceBookingService.resolveHostSelection(service, slot, null, null);

    assertEquals(List.of(userId1, userId2), selection.selectedHostUserIds());
    assertTrue(selection.createEventForHosts());
    assertFalse(selection.incrementReservation());
    assertTrue(selection.useRoundRobin());
  }

  @Test
  void selectAvailabilityCandidatesPrefersNeverAssignedHostsLikeRust() {
    ID neverAssigned1 = new ID("00000000-0000-0000-0000-000000000111");
    ID assigned = new ID("00000000-0000-0000-0000-000000000112");
    ID neverAssigned2 = new ID("00000000-0000-0000-0000-000000000113");

    List<ID> candidates =
        ServiceBookingService.selectAvailabilityCandidates(
            List.of(neverAssigned1, assigned, neverAssigned2),
            List.of(
                new EventRepository.MostRecentCreatedServiceEvents(assigned, instant(10)),
                new EventRepository.MostRecentCreatedServiceEvents(neverAssigned1, null),
                new EventRepository.MostRecentCreatedServiceEvents(neverAssigned2, null)));

    assertEquals(List.of(neverAssigned1, neverAssigned2), candidates);
  }

  @Test
  void selectAvailabilityCandidatesPrefersLeastRecentlyAssignedHost() {
    ID oldest = new ID("00000000-0000-0000-0000-000000000121");
    ID newer = new ID("00000000-0000-0000-0000-000000000122");
    ID newest = new ID("00000000-0000-0000-0000-000000000123");

    List<ID> candidates =
        ServiceBookingService.selectAvailabilityCandidates(
            List.of(oldest, newer, newest),
            List.of(
                new EventRepository.MostRecentCreatedServiceEvents(newer, instant(6)),
                new EventRepository.MostRecentCreatedServiceEvents(newest, instant(12)),
                new EventRepository.MostRecentCreatedServiceEvents(oldest, instant(4))));

    assertEquals(List.of(oldest), candidates);
  }

  @Test
  void selectAvailabilityCandidatesFallsBackToAvailableHostsWhenRepositoryReturnsNothing() {
    ID userId1 = new ID("00000000-0000-0000-0000-000000000131");
    ID userId2 = new ID("00000000-0000-0000-0000-000000000132");

    assertEquals(
        List.of(userId1, userId2),
        ServiceBookingService.selectAvailabilityCandidates(List.of(userId1, userId2), List.of()));
  }

  @Test
  void selectEqualDistributionCandidatesPrefersUsersWithFewestUpcomingEvents() {
    ID leastLoaded1 = new ID("00000000-0000-0000-0000-000000000141");
    ID busiest = new ID("00000000-0000-0000-0000-000000000142");
    ID leastLoaded2 = new ID("00000000-0000-0000-0000-000000000143");

    List<ID> candidates =
        ServiceBookingService.selectEqualDistributionCandidates(
            List.of(leastLoaded1, busiest, leastLoaded2),
            List.of(
                serviceEvent(leastLoaded1),
                serviceEvent(busiest),
                serviceEvent(busiest),
                serviceEvent(busiest),
                serviceEvent(leastLoaded2)));

    assertEquals(List.of(leastLoaded1, leastLoaded2), candidates);
  }

  @Test
  void selectEqualDistributionCandidatesIncludesUsersWithZeroUpcomingEvents() {
    ID noEvents = new ID("00000000-0000-0000-0000-000000000151");
    ID hasEvents = new ID("00000000-0000-0000-0000-000000000152");

    List<ID> candidates =
        ServiceBookingService.selectEqualDistributionCandidates(
            List.of(noEvents, hasEvents),
            List.of(serviceEvent(hasEvents), serviceEvent(hasEvents)));

    assertEquals(List.of(noEvents), candidates);
  }

  @Test
  void compareNullableInstantTreatsNullAsOlderForAvailabilityParity() {
    assertTrue(ServiceBookingService.compareNullableInstant(null, instant(1)) < 0);
    assertTrue(ServiceBookingService.compareNullableInstant(instant(1), null) > 0);
    assertEquals(0, ServiceBookingService.compareNullableInstant(null, null));
  }

  private static TimeSpan span(long startMillis, long endMillis) {
    return new TimeSpan(instant(startMillis), instant(endMillis));
  }

  private static Instant instant(long millis) {
    return Instant.ofEpochMilli(millis);
  }

  private static ServiceWithUsers serviceWithUsers(String variant, Object data, ID... userIds) {
    ID serviceId = new ID("10000000-0000-0000-0000-000000000000");
    List<ServiceResource> users =
        List.of(userIds).stream()
            .map(userId -> new ServiceResource(userId, serviceId, null, 0, 0, 0, null))
            .toList();
    return new ServiceWithUsers(
        serviceId,
        new ID("20000000-0000-0000-0000-000000000000"),
        users,
        new ServiceMultiPersonOptions(variant, data),
        null);
  }

  private static com.meetsmore.nittei.domain.CalendarEvent serviceEvent(ID userId) {
    return new com.meetsmore.nittei.domain.CalendarEvent(
        new ID("30000000-0000-0000-0000-" + userId.value().substring(userId.value().length() - 12)),
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        null,
        instant(0),
        null,
        true,
        instant(1),
        instant(2),
        instant(3),
        null,
        List.of(),
        null,
        null,
        null,
        null,
        userId,
        null,
        List.of(),
        new ID("40000000-0000-0000-0000-000000000000"),
        null);
  }
}
