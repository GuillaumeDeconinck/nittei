package com.meetsmore.nittei.api.calendar;

import com.meetsmore.nittei.api.shared.BaseApiController;
import com.meetsmore.nittei.api.structs.calendar.CalendarApi;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CalendarController extends BaseApiController {

    private final CalendarApplicationService calendarService;

    public CalendarController(CalendarApplicationService calendarService) {
        this.calendarService = calendarService;
    }

    @PostMapping("/user/{userId}/calendar")
    public ResponseEntity<CalendarApi.CalendarResponse> createCalendarAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String userId,
        @Valid @RequestBody CalendarApi.CreateCalendarRequestBody body
    ) {
        return calendarService.createCalendarAdmin(headers, userId, body);
    }

    @GetMapping("/user/{userId}/calendar")
    public ResponseEntity<CalendarApi.GetCalendarsByUserAPIResponse> getCalendarsAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String userId,
        @Valid @ModelAttribute CalendarApi.GetCalendarsByUserQueryParams query
    ) {
        return calendarService.getCalendarsAdmin(headers, userId, query);
    }

    @GetMapping("/calendar/meta")
    public ResponseEntity<CalendarApi.GetCalendarsByMetaAPIResponse> getCalendarsByMetaAdmin(
        @RequestHeader HttpHeaders headers,
        @Valid @ModelAttribute CalendarApi.GetCalendarsByMetaQueryParams query
    ) {
        return calendarService.getCalendarsByMetaAdmin(headers, query);
    }

    @GetMapping("/user/calendar/{calendarId}")
    public ResponseEntity<CalendarApi.CalendarResponse> getCalendarAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId
    ) {
        return calendarService.getCalendarAdmin(headers, calendarId);
    }

    @DeleteMapping("/user/calendar/{calendarId}")
    public ResponseEntity<CalendarApi.CalendarResponse> deleteCalendarAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId
    ) {
        return calendarService.deleteCalendarAdmin(headers, calendarId);
    }

    @PutMapping("/user/calendar/{calendarId}")
    public ResponseEntity<CalendarApi.CalendarResponse> updateCalendarAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId,
        @Valid @RequestBody CalendarApi.UpdateCalendarRequestBody body
    ) {
        return calendarService.updateCalendarAdmin(headers, calendarId, body);
    }

    @GetMapping("/user/calendar/{calendarId}/events")
    public ResponseEntity<CalendarApi.GetCalendarEventsAPIResponse> getCalendarEventsAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId,
        @Valid @ModelAttribute CalendarApi.GetCalendarEventsQueryParams query
    ) {
        return calendarService.getCalendarEventsAdmin(headers, calendarId, query);
    }

    @GetMapping("/user/calendar/{calendarId}/ical")
    public ResponseEntity<String> exportCalendarIcalAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId,
        @Valid @ModelAttribute CalendarApi.GetCalendarEventsIcalQueryParams query
    ) {
        return calendarService.exportCalendarIcalAdmin(headers, calendarId, query);
    }

    @GetMapping("/user/{userId}/calendar/provider/google")
    public ResponseEntity<CalendarApi.GetGoogleCalendarsAPIResponse> getGoogleCalendarsAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String userId,
        @Valid @ModelAttribute CalendarApi.GetGoogleCalendarsQueryParams query
    ) {
        return calendarService.getGoogleCalendarsAdmin(headers, userId, query);
    }

    @GetMapping("/user/{userId}/calendar/provider/outlook")
    public ResponseEntity<CalendarApi.GetOutlookCalendarsAPIResponse> getOutlookCalendarsAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String userId,
        @Valid @ModelAttribute CalendarApi.GetOutlookCalendarsQueryParams query
    ) {
        return calendarService.getOutlookCalendarsAdmin(headers, userId, query);
    }

    @PutMapping("/user/{userId}/calendar/sync")
    public ResponseEntity<CalendarApi.CalendarResponse> addSyncCalendarAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String userId,
        @Valid @RequestBody CalendarApi.AddSyncCalendarRequestBody body
    ) {
        return calendarService.addSyncCalendarAdmin(headers, userId, body);
    }

    @DeleteMapping("/user/{userId}/calendar/sync")
    public ResponseEntity<CalendarApi.CalendarResponse> removeSyncCalendarAdmin(
        @RequestHeader HttpHeaders headers,
        @PathVariable String userId,
        @Valid @RequestBody CalendarApi.RemoveSyncCalendarRequestBody body
    ) {
        return calendarService.removeSyncCalendarAdmin(headers, userId, body);
    }

    @PostMapping("/calendar")
    public ResponseEntity<CalendarApi.CalendarResponse> createCalendar(
        @RequestHeader HttpHeaders headers,
        @Valid @RequestBody CalendarApi.CreateCalendarRequestBody body
    ) {
        return calendarService.createCalendar(headers, body);
    }

    @GetMapping("/calendar")
    public ResponseEntity<CalendarApi.GetCalendarsByUserAPIResponse> getCalendars(
        @RequestHeader HttpHeaders headers,
        @Valid @ModelAttribute CalendarApi.GetCalendarsByUserQueryParams query
    ) {
        return calendarService.getCalendars(headers, query);
    }

    @GetMapping("/calendar/{calendarId}")
    public ResponseEntity<CalendarApi.CalendarResponse> getCalendar(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId
    ) {
        return calendarService.getCalendar(headers, calendarId);
    }

    @DeleteMapping("/calendar/{calendarId}")
    public ResponseEntity<CalendarApi.CalendarResponse> deleteCalendar(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId
    ) {
        return calendarService.deleteCalendar(headers, calendarId);
    }

    @PutMapping("/calendar/{calendarId}")
    public ResponseEntity<CalendarApi.CalendarResponse> updateCalendar(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId,
        @Valid @RequestBody CalendarApi.UpdateCalendarRequestBody body
    ) {
        return calendarService.updateCalendar(headers, calendarId, body);
    }

    @GetMapping("/calendar/{calendarId}/events")
    public ResponseEntity<CalendarApi.GetCalendarEventsAPIResponse> getCalendarEvents(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId,
        @Valid @ModelAttribute CalendarApi.GetCalendarEventsQueryParams query
    ) {
        return calendarService.getCalendarEvents(headers, calendarId, query);
    }

    @GetMapping("/calendar/{calendarId}/ical")
    public ResponseEntity<String> exportCalendarIcal(
        @RequestHeader HttpHeaders headers,
        @PathVariable String calendarId,
        @Valid @ModelAttribute CalendarApi.GetCalendarEventsIcalQueryParams query
    ) {
        return calendarService.exportCalendarIcal(headers, calendarId, query);
    }

    @GetMapping("/calendar/provider/google")
    public ResponseEntity<CalendarApi.GetGoogleCalendarsAPIResponse> getGoogleCalendars(
        @RequestHeader HttpHeaders headers,
        @Valid @ModelAttribute CalendarApi.GetGoogleCalendarsQueryParams query
    ) {
        return calendarService.getGoogleCalendars(headers, query);
    }

    @GetMapping("/calendar/provider/outlook")
    public ResponseEntity<CalendarApi.GetOutlookCalendarsAPIResponse> getOutlookCalendars(
        @RequestHeader HttpHeaders headers,
        @Valid @ModelAttribute CalendarApi.GetOutlookCalendarsQueryParams query
    ) {
        return calendarService.getOutlookCalendars(headers, query);
    }
}
