package com.meetsmore.nittei.api.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.meetsmore.nittei.api.shared.BaseApiController;
import com.meetsmore.nittei.api.structs.event.EventApi;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class EventController extends BaseApiController {

  private final EventApplicationService eventService;

  public EventController(EventApplicationService eventService) {
    this.eventService = eventService;
  }

  @PostMapping("/user/{userId}/events")
  public ResponseEntity<EventApi.CalendarEventResponse> createEventAdmin(
      @RequestHeader HttpHeaders headers,
      @PathVariable String userId,
      @Valid @RequestBody EventApi.CreateEventRequestBody body) {
    return eventService.createEventAdmin(headers, userId, body);
  }

  @PostMapping("/user/{userId}/events/batch")
  public ResponseEntity<EventApi.CreateBatchEventsAPIResponse> createBatchEventsAdmin(
      @RequestHeader HttpHeaders headers,
      @PathVariable String userId,
      @Valid @RequestBody EventApi.CreateBatchEventsRequestBody body) {
    return eventService.createBatchEventsAdmin(headers, userId, body);
  }

  @PostMapping("/events/search")
  public ResponseEntity<EventApi.SearchEventsAPIResponse> searchEventsAdmin(
      @RequestHeader HttpHeaders headers,
      @Valid @RequestBody EventApi.SearchEventsRequestBody body) {
    return eventService.searchEventsAdmin(headers, body);
  }

  @GetMapping("/events/meta")
  public ResponseEntity<EventApi.GetEventsByMetaAPIResponse> getEventsByMetaAdmin(
      @RequestHeader HttpHeaders headers,
      @Valid @ModelAttribute EventApi.GetEventsByMetaQueryParams query) {
    return eventService.getEventsByMetaAdmin(headers, query);
  }

  @PostMapping("/events/timespan")
  public ResponseEntity<EventApi.GetEventsForUsersInTimeSpanAPIResponse> getEventsInTimespanAdmin(
      @RequestHeader HttpHeaders headers,
      @Valid @RequestBody EventApi.GetEventsForUsersInTimeSpanBody body) {
    return eventService.getEventsInTimespanAdmin(headers, body);
  }

  @GetMapping("/user/{userId}/events")
  public ResponseEntity<EventApi.GetEventsByCalendarsAPIResponse> getEventsByCalendarsAdmin(
      @RequestHeader HttpHeaders headers,
      @PathVariable String userId,
      @Valid @ModelAttribute EventApi.GetEventsByCalendarsQueryParams query) {
    return eventService.getEventsByCalendarsAdmin(headers, userId, query);
  }

  @GetMapping("/user/events/external_id/{externalId}")
  public ResponseEntity<EventApi.GetEventsByExternalIdAPIResponse> getEventByExternalIdAdmin(
      @RequestHeader HttpHeaders headers, @PathVariable String externalId) {
    return eventService.getEventByExternalIdAdmin(headers, externalId);
  }

  @GetMapping("/user/events/{eventId}")
  public ResponseEntity<EventApi.CalendarEventResponse> getEventAdmin(
      @RequestHeader HttpHeaders headers, @PathVariable String eventId) {
    return eventService.getEventAdmin(headers, eventId);
  }

  @PatchMapping("/user/events/{eventId}")
  public ResponseEntity<EventApi.CalendarEventResponse> updateEventAdmin(
      @RequestHeader HttpHeaders headers,
      @PathVariable String eventId,
      @Valid @RequestBody JsonNode rawBody) {
    return eventService.updateEventAdmin(headers, eventId, rawBody);
  }

  @DeleteMapping("/user/events/{eventId}")
  public ResponseEntity<EventApi.CalendarEventResponse> deleteEventAdmin(
      @RequestHeader HttpHeaders headers, @PathVariable String eventId) {
    return eventService.deleteEventAdmin(headers, eventId);
  }

  @GetMapping("/user/events/{eventId}/instances")
  public ResponseEntity<EventApi.GetEventInstancesAPIResponse> getEventInstancesAdmin(
      @RequestHeader HttpHeaders headers,
      @PathVariable String eventId,
      @Valid @ModelAttribute EventApi.GetEventInstancesQueryParams query) {
    return eventService.getEventInstancesAdmin(headers, eventId, query);
  }

  @PostMapping("/user/events/delete_many")
  public ResponseEntity<Void> deleteManyEventsAdmin(
      @RequestHeader HttpHeaders headers,
      @Valid @RequestBody EventApi.DeleteManyEventsRequestBody body) {
    return eventService.deleteManyEventsAdmin(headers, body);
  }

  @PostMapping("/events")
  public ResponseEntity<EventApi.CalendarEventResponse> createEvent(
      @RequestHeader HttpHeaders headers,
      @Valid @RequestBody EventApi.CreateEventRequestBody body) {
    return eventService.createEvent(headers, body);
  }

  @GetMapping("/events/{eventId}")
  public ResponseEntity<EventApi.CalendarEventResponse> getEvent(
      @RequestHeader HttpHeaders headers, @PathVariable String eventId) {
    return eventService.getEvent(headers, eventId);
  }

  @PatchMapping("/events/{eventId}")
  public ResponseEntity<EventApi.CalendarEventResponse> updateEvent(
      @RequestHeader HttpHeaders headers,
      @PathVariable String eventId,
      @Valid @RequestBody JsonNode rawBody) {
    return eventService.updateEvent(headers, eventId, rawBody);
  }

  @DeleteMapping("/events/{eventId}")
  public ResponseEntity<EventApi.CalendarEventResponse> deleteEvent(
      @RequestHeader HttpHeaders headers, @PathVariable String eventId) {
    return eventService.deleteEvent(headers, eventId);
  }

  @GetMapping("/events/{eventId}/instances")
  public ResponseEntity<EventApi.GetEventInstancesAPIResponse> getEventInstances(
      @RequestHeader HttpHeaders headers,
      @PathVariable String eventId,
      @Valid @ModelAttribute EventApi.GetEventInstancesQueryParams query) {
    return eventService.getEventInstances(headers, eventId, query);
  }
}
