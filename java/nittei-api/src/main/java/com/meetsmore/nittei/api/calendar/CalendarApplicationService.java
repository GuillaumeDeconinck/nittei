package com.meetsmore.nittei.api.calendar;

import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.api.shared.auth.RequestContextResolver;
import com.meetsmore.nittei.api.structs.calendar.CalendarApi;
import com.meetsmore.nittei.api.structs.calendar.CalendarDtos;
import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.CalendarSettings;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.EventWithInstances;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Metadata;
import com.meetsmore.nittei.domain.SyncedCalendar;
import com.meetsmore.nittei.domain.TimeSpan;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import com.meetsmore.nittei.infra.services.ProviderCalendarService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class CalendarApplicationService {

    private final NitteiContext ctx;
    private final RequestContextResolver auth;
    private final ProviderCalendarService providerCalendarService;

    public CalendarApplicationService(NitteiContext ctx, RequestContextResolver auth, ProviderCalendarService providerCalendarService) {
        this.ctx = ctx;
        this.auth = auth;
        this.providerCalendarService = providerCalendarService;
    }

    public ResponseEntity<CalendarApi.CalendarResponse> createCalendarAdmin(
        HttpHeaders headers,
        String userId,
        CalendarApi.CreateCalendarRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        auth.requireAccountUser(account, uid, ctx);
        Calendar calendar = buildCalendar(uid, account.id(), body);
        ctx.repos().calendars().insert(calendar);
        return ResponseEntity.status(201).body(CalendarApi.CalendarResponse.from(calendar));
    }

    public ResponseEntity<CalendarApi.GetCalendarsByUserAPIResponse> getCalendarsAdmin(
        HttpHeaders headers,
        String userId,
        CalendarApi.GetCalendarsByUserQueryParams query
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        auth.requireAccountUser(account, uid, ctx);
        List<Calendar> calendars = ctx.repos().calendars().findByUser(uid);
        if (query != null && query.key() != null && !query.key().isBlank()) {
            calendars = calendars.stream().filter(c -> query.key().equals(c.key())).toList();
        }
        return ResponseEntity.ok(new CalendarApi.GetCalendarsByUserAPIResponse(
            calendars.stream().map(CalendarDtos.CalendarDTO::from).toList()
        ));
    }

    public ResponseEntity<CalendarApi.GetCalendarsByMetaAPIResponse> getCalendarsByMetaAdmin(
        HttpHeaders headers,
        CalendarApi.GetCalendarsByMetaQueryParams query
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        List<Calendar> calendars = ctx.repos().calendars().findByMetadata(
            new MetadataFindQuery(
                Metadata.of(query.key(), query.value()),
                query.skip() == null ? 0 : query.skip(),
                query.limit() == null ? 20 : query.limit(),
                account.id()
            )
        );
        return ResponseEntity.ok(new CalendarApi.GetCalendarsByMetaAPIResponse(
            calendars.stream().map(CalendarDtos.CalendarDTO::from).toList()
        ));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> getCalendarAdmin(
        HttpHeaders headers,
        String calendarId
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        Calendar calendar = auth.requireAccountCalendar(account, auth.parseId(calendarId, "Malformed calendarId"), ctx);
        return ResponseEntity.ok(CalendarApi.CalendarResponse.from(calendar));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> deleteCalendarAdmin(
        HttpHeaders headers,
        String calendarId
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        Calendar calendar = auth.requireAccountCalendar(account, auth.parseId(calendarId, "Malformed calendarId"), ctx);
        ctx.repos().calendars().delete(calendar.id());
        return ResponseEntity.ok(CalendarApi.CalendarResponse.from(calendar));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> updateCalendarAdmin(
        HttpHeaders headers,
        String calendarId,
        CalendarApi.UpdateCalendarRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        Calendar current = auth.requireAccountCalendar(account, auth.parseId(calendarId, "Malformed calendarId"), ctx);
        Calendar updated = new Calendar(
            current.id(),
            current.userId(),
            current.accountId(),
            body != null && body.name() != null ? body.name() : current.name(),
            current.key(),
            body != null && body.settings() != null
                ? new CalendarSettings(
                    body.settings().weekStart() == null ? current.settings().weekStart() : body.settings().weekStart(),
                    body.settings().timezone() == null ? current.settings().timezone() : body.settings().timezone()
                )
                : current.settings(),
            body != null && body.metadata() != null ? body.metadata() : current.metadata()
        );
        ctx.repos().calendars().save(updated);
        return ResponseEntity.ok(CalendarApi.CalendarResponse.from(updated));
    }

    public ResponseEntity<CalendarApi.GetCalendarEventsAPIResponse> getCalendarEventsAdmin(
        HttpHeaders headers,
        String calendarId,
        CalendarApi.GetCalendarEventsQueryParams query
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        Calendar calendar = auth.requireAccountCalendar(account, auth.parseId(calendarId, "Malformed calendarId"), ctx);
        var events = getCalendarEvents(calendar.id(), query).stream().map(this::toEventWithSingleInstance).toList();
        return ResponseEntity.ok(CalendarApi.GetCalendarEventsAPIResponse.from(calendar, events));
    }

    public ResponseEntity<String> exportCalendarIcalAdmin(
        HttpHeaders headers,
        String calendarId,
        CalendarApi.GetCalendarEventsIcalQueryParams query
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        Calendar calendar = auth.requireAccountCalendar(account, auth.parseId(calendarId, "Malformed calendarId"), ctx);
        List<CalendarEvent> events = getCalendarEvents(calendar.id(), new CalendarApi.GetCalendarEventsQueryParams(query.startTime(), query.endTime()));
        String ical = buildIcal(events);
        return ResponseEntity.ok(ical);
    }

    public ResponseEntity<CalendarApi.GetGoogleCalendarsAPIResponse> getGoogleCalendarsAdmin(
        HttpHeaders headers,
        String userId,
        CalendarApi.GetGoogleCalendarsQueryParams query
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        var user = auth.requireAccountUser(account, uid, ctx);
        List<com.meetsmore.nittei.domain.providers.google.GoogleCalendarListEntry> calendars;
        try {
            calendars = providerCalendarService.listGoogleCalendars(user, query == null ? null : query.minAccessRole(), ctx);
        } catch (Exception e) {
            throw new NitteiApiException(NitteiErrorCode.INTERNAL_ERROR, "Unable to list google calendars");
        }
        if (calendars == null) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "The user is not connected to google.");
        }
        return ResponseEntity.ok(new CalendarApi.GetGoogleCalendarsAPIResponse(calendars));
    }

    public ResponseEntity<CalendarApi.GetOutlookCalendarsAPIResponse> getOutlookCalendarsAdmin(
        HttpHeaders headers,
        String userId,
        CalendarApi.GetOutlookCalendarsQueryParams query
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        var user = auth.requireAccountUser(account, uid, ctx);
        List<com.meetsmore.nittei.domain.providers.outlook.OutlookCalendar> calendars;
        try {
            calendars = providerCalendarService.listOutlookCalendars(user, query == null ? null : query.minAccessRole(), ctx);
        } catch (Exception e) {
            throw new NitteiApiException(NitteiErrorCode.INTERNAL_ERROR, "Unable to list outlook calendars");
        }
        if (calendars == null) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "The user is not connected to outlook.");
        }
        return ResponseEntity.ok(new CalendarApi.GetOutlookCalendarsAPIResponse(calendars));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> addSyncCalendarAdmin(
        HttpHeaders headers,
        String userId,
        CalendarApi.AddSyncCalendarRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        auth.requireAccountUser(account, uid, ctx);
        Calendar calendar = auth.requireAccountCalendar(account, body.calendarId(), ctx);
        ctx.repos().calendarSynced().insert(new SyncedCalendar(body.provider(), calendar.id(), uid, body.extCalendarId()));
        return ResponseEntity.ok(CalendarApi.CalendarResponse.from(calendar));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> removeSyncCalendarAdmin(
        HttpHeaders headers,
        String userId,
        CalendarApi.RemoveSyncCalendarRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID uid = auth.parseId(userId, "Malformed userId");
        auth.requireAccountUser(account, uid, ctx);
        Calendar calendar = auth.requireAccountCalendar(account, body.calendarId(), ctx);
        ctx.repos().calendarSynced().delete(new SyncedCalendar(body.provider(), calendar.id(), uid, body.extCalendarId()));
        return ResponseEntity.ok(CalendarApi.CalendarResponse.from(calendar));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> createCalendar(
        HttpHeaders headers,
        CalendarApi.CreateCalendarRequestBody body
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        Calendar calendar = buildCalendar(userCtx.user().id(), userCtx.account().id(), body);
        ctx.repos().calendars().insert(calendar);
        return ResponseEntity.status(201).body(CalendarApi.CalendarResponse.from(calendar));
    }

    public ResponseEntity<CalendarApi.GetCalendarsByUserAPIResponse> getCalendars(
        HttpHeaders headers,
        CalendarApi.GetCalendarsByUserQueryParams query
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        List<Calendar> calendars = ctx.repos().calendars().findByUser(userCtx.user().id());
        if (query != null && query.key() != null && !query.key().isBlank()) {
            calendars = calendars.stream().filter(c -> query.key().equals(c.key())).toList();
        }
        return ResponseEntity.ok(new CalendarApi.GetCalendarsByUserAPIResponse(
            calendars.stream().map(CalendarDtos.CalendarDTO::from).toList()
        ));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> getCalendar(
        HttpHeaders headers,
        String calendarId
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        Calendar calendar = ctx.repos().calendars().find(auth.parseId(calendarId, "Malformed calendarId"))
            .filter(c -> c.userId().equals(userCtx.user().id()))
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "The calendar with id: " + calendarId + ", was not found"));
        return ResponseEntity.ok(CalendarApi.CalendarResponse.from(calendar));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> deleteCalendar(
        HttpHeaders headers,
        String calendarId
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        ID cid = auth.parseId(calendarId, "Malformed calendarId");
        Calendar calendar = ctx.repos().calendars().find(cid)
            .filter(c -> c.userId().equals(userCtx.user().id()))
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "The calendar with id: " + calendarId + ", was not found"));
        ctx.repos().calendars().delete(cid);
        return ResponseEntity.ok(CalendarApi.CalendarResponse.from(calendar));
    }

    public ResponseEntity<CalendarApi.CalendarResponse> updateCalendar(
        HttpHeaders headers,
        String calendarId,
        CalendarApi.UpdateCalendarRequestBody body
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        ID cid = auth.parseId(calendarId, "Malformed calendarId");
        Calendar current = ctx.repos().calendars().find(cid)
            .filter(c -> c.userId().equals(userCtx.user().id()))
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "The calendar with id: " + calendarId + ", was not found"));
        Calendar updated = new Calendar(
            current.id(),
            current.userId(),
            current.accountId(),
            body != null && body.name() != null ? body.name() : current.name(),
            current.key(),
            body != null && body.settings() != null
                ? new CalendarSettings(
                    body.settings().weekStart() == null ? current.settings().weekStart() : body.settings().weekStart(),
                    body.settings().timezone() == null ? current.settings().timezone() : body.settings().timezone()
                )
                : current.settings(),
            body != null && body.metadata() != null ? body.metadata() : current.metadata()
        );
        ctx.repos().calendars().save(updated);
        return ResponseEntity.ok(CalendarApi.CalendarResponse.from(updated));
    }

    public ResponseEntity<CalendarApi.GetCalendarEventsAPIResponse> getCalendarEvents(
        HttpHeaders headers,
        String calendarId,
        CalendarApi.GetCalendarEventsQueryParams query
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        Calendar calendar = ctx.repos().calendars().find(auth.parseId(calendarId, "Malformed calendarId"))
            .filter(c -> c.userId().equals(userCtx.user().id()))
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "The calendar with id: " + calendarId + ", was not found"));
        var events = getCalendarEvents(calendar.id(), query).stream().map(this::toEventWithSingleInstance).toList();
        return ResponseEntity.ok(CalendarApi.GetCalendarEventsAPIResponse.from(calendar, events));
    }

    public ResponseEntity<String> exportCalendarIcal(
        HttpHeaders headers,
        String calendarId,
        CalendarApi.GetCalendarEventsIcalQueryParams query
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        Calendar calendar = ctx.repos().calendars().find(auth.parseId(calendarId, "Malformed calendarId"))
            .filter(c -> c.userId().equals(userCtx.user().id()))
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "The calendar with id: " + calendarId + ", was not found"));
        List<CalendarEvent> events = getCalendarEvents(calendar.id(), new CalendarApi.GetCalendarEventsQueryParams(query.startTime(), query.endTime()));
        String ical = buildIcal(events);
        return ResponseEntity.ok(ical);
    }

    public ResponseEntity<CalendarApi.GetGoogleCalendarsAPIResponse> getGoogleCalendars(
        HttpHeaders headers,
        CalendarApi.GetGoogleCalendarsQueryParams query
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        List<com.meetsmore.nittei.domain.providers.google.GoogleCalendarListEntry> calendars;
        try {
            calendars = providerCalendarService.listGoogleCalendars(userCtx.user(), query == null ? null : query.minAccessRole(), ctx);
        } catch (Exception e) {
            throw new NitteiApiException(NitteiErrorCode.INTERNAL_ERROR, "Unable to list google calendars");
        }
        if (calendars == null) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "The user is not connected to google.");
        }
        return ResponseEntity.ok(new CalendarApi.GetGoogleCalendarsAPIResponse(calendars));
    }

    public ResponseEntity<CalendarApi.GetOutlookCalendarsAPIResponse> getOutlookCalendars(
        HttpHeaders headers,
        CalendarApi.GetOutlookCalendarsQueryParams query
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        List<com.meetsmore.nittei.domain.providers.outlook.OutlookCalendar> calendars;
        try {
            calendars = providerCalendarService.listOutlookCalendars(userCtx.user(), query == null ? null : query.minAccessRole(), ctx);
        } catch (Exception e) {
            throw new NitteiApiException(NitteiErrorCode.INTERNAL_ERROR, "Unable to list outlook calendars");
        }
        if (calendars == null) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "The user is not connected to outlook.");
        }
        return ResponseEntity.ok(new CalendarApi.GetOutlookCalendarsAPIResponse(calendars));
    }

    private Calendar buildCalendar(ID userId, ID accountId, CalendarApi.CreateCalendarRequestBody body) {
        DayOfWeek weekStart = body == null || body.weekStart() == null ? DayOfWeek.MONDAY : body.weekStart();
        String timezone = body == null || body.timezone() == null ? "UTC" : body.timezone();
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new NitteiApiException(NitteiErrorCode.UNPROCESSABLE_ENTITY, "failed to parse timezone: '" + timezone + "'");
        }

        if (body != null && body.key() != null && body.key().isBlank()) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Calendar key cannot be empty");
        }
        String key = body == null || body.key() == null ? ID.random().value() : body.key();
        return new Calendar(
            ID.random(),
            userId,
            accountId,
            body == null ? null : body.name(),
            key,
            new CalendarSettings(weekStart, timezone),
            body == null ? null : body.metadata()
        );
    }

    private List<CalendarEvent> getCalendarEvents(ID calendarId, CalendarApi.GetCalendarEventsQueryParams query) {
        if (query == null || query.startTime() == null || query.endTime() == null) {
            return ctx.repos().events().findByCalendar(
                calendarId,
                new TimeSpan(Instant.EPOCH, Instant.now().plusSeconds(60L * 60 * 24 * 365 * 50))
            );
        }
        return ctx.repos().events().findByCalendar(calendarId, new TimeSpan(query.startTime(), query.endTime()));
    }

    private EventWithInstances toEventWithSingleInstance(CalendarEvent event) {
        return new EventWithInstances(event, List.of(new EventInstance(event.startTime(), event.endTime(), event.busy())));
    }

    private String buildIcal(List<CalendarEvent> events) {
        return "BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//nittei-java//EN\n"
            + events.stream()
                .map(e -> "BEGIN:VEVENT\nUID:" + e.id().value() + "\nDTSTART:" + e.startTime() + "\nDTEND:" + e.endTime() + "\nEND:VEVENT\n")
                .reduce("", String::concat)
            + "END:VCALENDAR\n";
    }
}
