package com.meetsmore.nittei.api.user;

import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.api.shared.auth.RequestContextResolver;
import com.meetsmore.nittei.api.structs.calendar.CalendarApi;
import com.meetsmore.nittei.api.structs.user.UserApi;
import com.meetsmore.nittei.api.structs.user.UserDtos;
import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.EventExpansion;
import com.meetsmore.nittei.domain.EventInstance;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.Metadata;
import com.meetsmore.nittei.domain.TimeSpan;
import com.meetsmore.nittei.domain.User;
import com.meetsmore.nittei.domain.UserIntegration;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import com.meetsmore.nittei.infra.services.CodeTokenRequest;
import com.meetsmore.nittei.infra.services.CodeTokenResponse;
import com.meetsmore.nittei.infra.services.ProviderOAuthService;
import com.meetsmore.nittei.utils.config.AppConfig;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class UserApplicationService {

    private final NitteiContext ctx;
    private final RequestContextResolver auth;
    private final ProviderOAuthService providerOAuthService;
    private final AppConfig appConfig;

    public UserApplicationService(
        NitteiContext ctx,
        RequestContextResolver auth,
        ProviderOAuthService providerOAuthService,
        AppConfig appConfig
    ) {
        this.ctx = ctx;
        this.auth = auth;
        this.providerOAuthService = providerOAuthService;
        this.appConfig = appConfig;
    }

    public ResponseEntity<UserApi.UserResponse> createUser(
        HttpHeaders headers,
        UserApi.CreateUserRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        ID userId = body != null && body.userId() != null ? body.userId() : ID.random();

        if (ctx.repos().users().find(userId).isPresent()) {
            throw new NitteiApiException(NitteiErrorCode.CONFLICT, "A user with that userId already exists");
        }
        if (body != null && body.externalId() != null && ctx.repos().users().getByExternalId(body.externalId()).isPresent()) {
            throw new NitteiApiException(NitteiErrorCode.CONFLICT, "A user with that externalId already exists");
        }

        User user = new User(userId, account.id(), body == null ? null : body.metadata(), body == null ? null : body.externalId());
        ctx.repos().users().insert(user);
        return ResponseEntity.status(201).body(UserApi.UserResponse.from(user));
    }

    public ResponseEntity<UserApi.GetUsersByMetaAPIResponse> getUsersByMeta(
        HttpHeaders headers,
        UserApi.GetUsersByMetaQueryParams query
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        var result = ctx.repos().users().findByMetadata(
            new MetadataFindQuery(
                Metadata.of(query.key(), query.value()),
                query.skip() == null ? 0 : query.skip(),
                query.limit() == null ? 20 : query.limit(),
                account.id()
            )
        ).stream().map(UserDtos.UserDTO::from).toList();
        return ResponseEntity.ok(new UserApi.GetUsersByMetaAPIResponse(result));
    }

    public ResponseEntity<UserApi.UserResponse> getUser(HttpHeaders headers, String userId) {
        var account = auth.requireAdminAccount(headers, ctx);
        User user = auth.requireAccountUser(account, auth.parseId(userId, "Malformed userId"), ctx);
        return ResponseEntity.ok(UserApi.UserResponse.from(user));
    }

    public ResponseEntity<UserApi.UserResponse> getUserByExternalId(HttpHeaders headers, String externalId) {
        var account = auth.requireAdminAccount(headers, ctx);
        User user = ctx.repos().users().getByExternalId(externalId)
            .filter(u -> u.accountId().equals(account.id()))
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(UserApi.UserResponse.from(user));
    }

    public ResponseEntity<UserApi.UserResponse> updateUser(
        HttpHeaders headers,
        String userId,
        UserApi.UpdateUserRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        User user = auth.requireAccountUser(account, auth.parseId(userId, "Malformed userId"), ctx);
        User updated = new User(
            user.id(),
            user.accountId(),
            body != null && body.metadata() != null ? body.metadata() : user.metadata(),
            body != null && body.externalId() != null ? body.externalId() : user.externalId()
        );
        ctx.repos().users().save(updated);
        return ResponseEntity.ok(UserApi.UserResponse.from(updated));
    }

    public ResponseEntity<UserApi.UserResponse> deleteUser(HttpHeaders headers, String userId) {
        var account = auth.requireAdminAccount(headers, ctx);
        User user = auth.requireAccountUser(account, auth.parseId(userId, "Malformed userId"), ctx);
        User deleted = ctx.repos().users().delete(user.id())
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.INTERNAL_ERROR, "Delete failed"));
        return ResponseEntity.ok(UserApi.UserResponse.from(deleted));
    }

    public ResponseEntity<UserApi.UserResponse> oauthIntegrationAdmin(
        HttpHeaders headers,
        String userId,
        UserApi.OAuthIntegrationRequestBody body
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        User user = auth.requireAccountUser(account, auth.parseId(userId, "Malformed userId"), ctx);
        integrateOAuth(user, body);
        return ResponseEntity.ok(UserApi.UserResponse.from(user));
    }

    public ResponseEntity<UserApi.UserResponse> removeIntegrationAdmin(
        HttpHeaders headers,
        String userId,
        String provider
    ) {
        var account = auth.requireAdminAccount(headers, ctx);
        User user = auth.requireAccountUser(account, auth.parseId(userId, "Malformed userId"), ctx);
        removeOAuth(user, parseProvider(provider));
        return ResponseEntity.ok(UserApi.UserResponse.from(user));
    }

    public ResponseEntity<UserApi.UserResponse> getMe(HttpHeaders headers) {
        var userCtx = auth.requireUserContext(headers, ctx);
        return ResponseEntity.ok(UserApi.UserResponse.from(userCtx.user()));
    }

    public ResponseEntity<UserApi.UserResponse> oauthIntegration(
        HttpHeaders headers,
        UserApi.OAuthIntegrationRequestBody body
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        integrateOAuth(userCtx.user(), body);
        return ResponseEntity.ok(UserApi.UserResponse.from(userCtx.user()));
    }

    public ResponseEntity<UserApi.UserResponse> removeIntegration(
        HttpHeaders headers,
        String provider
    ) {
        var userCtx = auth.requireUserContext(headers, ctx);
        removeOAuth(userCtx.user(), parseProvider(provider));
        return ResponseEntity.ok(UserApi.UserResponse.from(userCtx.user()));
    }

    public ResponseEntity<CalendarApi.GetUserFreeBusyAPIResponse> getUserFreebusy(
        HttpHeaders headers,
        String userId,
        CalendarApi.GetUserFreeBusyQueryParams query,
        String calendarIdsRaw
    ) {
        var account = auth.requirePublicAccount(headers, ctx);
        User user = auth.requireAccountUser(account, auth.parseId(userId, "Malformed userId"), ctx);
        TimeSpan timespan = validateAndBuildTimespan(query.startTime(), query.endTime());

        List<Calendar> allCalendars = ctx.repos().calendars().findByUser(user.id());
        List<ID> requestedCalendarIds = parseIdsCsv(calendarIdsRaw);
        List<Calendar> selectedCalendars = requestedCalendarIds.isEmpty()
            ? allCalendars
            : allCalendars.stream().filter(c -> requestedCalendarIds.contains(c.id())).toList();

        if (selectedCalendars.isEmpty()) {
            return ResponseEntity.ok(new CalendarApi.GetUserFreeBusyAPIResponse(List.of(), user.id().value()));
        }

        List<ID> selectedCalendarIds = selectedCalendars.stream().map(Calendar::id).toList();
        Map<ID, Calendar> calendarsById = selectedCalendars.stream().collect(Collectors.toMap(Calendar::id, c -> c));
        List<EventInstance> expanded = EventExpansion.expandAllEventsAndRemoveExceptions(
            calendarsById,
            ctx.repos().events().findBusyEventsAndRecurringEventsForCalendars(
                selectedCalendarIds,
                timespan,
                Boolean.TRUE.equals(query.includeTentative())
            ),
            timespan
        );

        List<EventInstance> mergedBusy = EventExpansion.toCompatibleInstances(
            expanded.stream().filter(EventInstance::busy).toList()
        );

        return ResponseEntity.ok(new CalendarApi.GetUserFreeBusyAPIResponse(mergedBusy, user.id().value()));
    }

    public ResponseEntity<Map<ID, List<EventInstance>>> getMultipleUsersFreebusy(
        HttpHeaders headers,
        CalendarApi.MultipleFreeBusyRequestBody body
    ) {
        var account = auth.requirePublicAccount(headers, ctx);
        if (body == null || body.userIds() == null) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Missing userIds");
        }
        TimeSpan timespan = validateAndBuildTimespan(body.startTime(), body.endTime());

        for (ID userId : body.userIds()) {
            auth.requireAccountUser(account, userId, ctx);
        }

        Map<ID, List<EventInstance>> grouped = new HashMap<>();
        for (ID userId : body.userIds()) {
            List<Calendar> calendars = ctx.repos().calendars().findByUser(userId);
            if (calendars.isEmpty()) {
                grouped.put(userId, List.of());
                continue;
            }

            Map<ID, Calendar> calendarsById = calendars.stream().collect(Collectors.toMap(Calendar::id, c -> c));
            List<EventInstance> instances = new ArrayList<>();
            for (Calendar calendar : calendars) {
                List<EventInstance> expanded = EventExpansion.expandAllEventsAndRemoveExceptions(
                    calendarsById,
                    ctx.repos().events().findByCalendar(calendar.id(), timespan),
                    timespan
                );
                instances.addAll(expanded);
            }
            instances.sort((a, b) -> a.startTime().compareTo(b.startTime()));
            grouped.put(userId, instances);
        }

        return ResponseEntity.ok(grouped);
    }

    private TimeSpan validateAndBuildTimespan(Instant start, Instant end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "The provided start_ts and end_ts are invalid");
        }
        long duration = end.toEpochMilli() - start.toEpochMilli();
        if (duration > appConfig.getEventInstancesQueryDurationLimit()) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "The provided start_ts and end_ts are invalid");
        }
        return new TimeSpan(start, end);
    }

    private List<ID> parseIdsCsv(String raw) {
        if (raw == null) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .flatMap(s -> {
                try {
                    UUID.fromString(s);
                    return java.util.stream.Stream.of(new ID(s));
                } catch (IllegalArgumentException ignored) {
                    return java.util.stream.Stream.empty();
                }
            })
            .toList();
    }

    private void integrateOAuth(User user, UserApi.OAuthIntegrationRequestBody body) {
        if (body == null || body.provider() == null || body.code() == null || body.code().isBlank()) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Missing OAuth request data");
        }

        var accountIntegration = ctx.repos().accountIntegrations().find(user.accountId()).stream()
            .filter(i -> i.provider() == body.provider())
            .findFirst()
            .orElseThrow(() -> new NitteiApiException(NitteiErrorCode.CONFLICT, "Account does not support this provider"));

        boolean userAlreadyIntegrated = ctx.repos().userIntegrations().find(user.id()).stream()
            .anyMatch(i -> i.provider() == body.provider());
        if (userAlreadyIntegrated) {
            throw new NitteiApiException(NitteiErrorCode.CONFLICT, "User already has an integration for this provider");
        }

        CodeTokenResponse token = providerOAuthService.exchangeCodeToken(
            body.provider(),
            new CodeTokenRequest(accountIntegration.clientId(), accountIntegration.clientSecret(), accountIntegration.redirectUri(), body.code())
        );

        long now = Instant.now().toEpochMilli();
        ctx.repos().userIntegrations().insert(
            new UserIntegration(
                user.id(),
                user.accountId(),
                body.provider(),
                token.refreshToken(),
                token.accessToken(),
                now + token.expiresIn() * 1000
            )
        );
    }

    private void removeOAuth(User user, IntegrationProvider provider) {
        boolean exists = ctx.repos().userIntegrations().find(user.id()).stream().anyMatch(i -> i.provider() == provider);
        if (!exists) {
            throw new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Integration not found");
        }
        ctx.repos().userIntegrations().delete(user.id(), provider);
    }

    private IntegrationProvider parseProvider(String provider) {
        if (provider == null) {
            throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Missing provider");
        }
        return switch (provider.toLowerCase()) {
            case "google" -> IntegrationProvider.GOOGLE;
            case "outlook" -> IntegrationProvider.OUTLOOK;
            default -> throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Unsupported provider");
        };
    }
}
