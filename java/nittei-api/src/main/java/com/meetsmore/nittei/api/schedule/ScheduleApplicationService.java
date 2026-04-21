package com.meetsmore.nittei.api.schedule;

import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.api.shared.auth.RequestContextResolver;
import com.meetsmore.nittei.api.structs.schedule.ScheduleApi;
import com.meetsmore.nittei.api.structs.schedule.ScheduleDtos;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Metadata;
import com.meetsmore.nittei.domain.Schedule;
import com.meetsmore.nittei.domain.ScheduleRule;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.repos.query.MetadataFindQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class ScheduleApplicationService {

  private final NitteiContext ctx;
  private final RequestContextResolver auth;

  public ScheduleApplicationService(NitteiContext ctx, RequestContextResolver auth) {
    this.ctx = ctx;
    this.auth = auth;
  }

  public ResponseEntity<ScheduleApi.ScheduleResponse> createScheduleAdmin(
      HttpHeaders headers, String userId, ScheduleApi.CreateScheduleRequestBody body) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID uid = auth.parseId(userId, "Malformed userId");
    auth.requireAccountUser(account, uid, ctx);
    Schedule schedule =
        new Schedule(
            ID.random(),
            uid,
            account.id(),
            effectiveRules(body == null ? null : body.rules()),
            body == null ? "UTC" : body.timezone(),
            body == null ? null : body.metadata());
    ctx.repos().schedules().insert(schedule);
    return ResponseEntity.status(201).body(ScheduleApi.ScheduleResponse.from(schedule));
  }

  public ResponseEntity<ScheduleApi.ScheduleResponse> getScheduleAdmin(
      HttpHeaders headers, String scheduleId) {
    var account = auth.requireAdminAccount(headers, ctx);
    Schedule schedule =
        auth.requireAccountSchedule(account, auth.parseId(scheduleId, "Malformed scheduleId"), ctx);
    return ResponseEntity.ok(ScheduleApi.ScheduleResponse.from(schedule));
  }

  public ResponseEntity<ScheduleApi.GetSchedulesByMetaAPIResponse> getSchedulesByMetaAdmin(
      HttpHeaders headers, ScheduleApi.GetSchedulesByMetaQueryParams query) {
    var account = auth.requireAdminAccount(headers, ctx);
    var schedules =
        ctx
            .repos()
            .schedules()
            .findByMetadata(
                new MetadataFindQuery(
                    Metadata.of(query.key(), query.value()),
                    query.skip() == null ? 0 : query.skip(),
                    query.limit() == null ? 20 : query.limit(),
                    account.id()))
            .stream()
            .map(ScheduleDtos.ScheduleDTO::from)
            .toList();
    return ResponseEntity.ok(new ScheduleApi.GetSchedulesByMetaAPIResponse(schedules));
  }

  public ResponseEntity<ScheduleApi.ScheduleResponse> deleteScheduleAdmin(
      HttpHeaders headers, String scheduleId) {
    var account = auth.requireAdminAccount(headers, ctx);
    ID sid = auth.parseId(scheduleId, "Malformed scheduleId");
    Schedule schedule = auth.requireAccountSchedule(account, sid, ctx);
    ctx.repos().schedules().delete(sid);
    return ResponseEntity.ok(ScheduleApi.ScheduleResponse.from(schedule));
  }

  public ResponseEntity<ScheduleApi.ScheduleResponse> updateScheduleAdmin(
      HttpHeaders headers, String scheduleId, ScheduleApi.UpdateScheduleRequestBody body) {
    var account = auth.requireAdminAccount(headers, ctx);
    Schedule current =
        auth.requireAccountSchedule(account, auth.parseId(scheduleId, "Malformed scheduleId"), ctx);
    Schedule updated =
        new Schedule(
            current.id(),
            current.userId(),
            current.accountId(),
            body.rules() == null ? current.rules() : body.rules(),
            body.timezone() == null ? current.timezone() : body.timezone(),
            body.metadata() == null ? current.metadata() : body.metadata());
    ctx.repos().schedules().save(updated);
    return ResponseEntity.ok(ScheduleApi.ScheduleResponse.from(updated));
  }

  public ResponseEntity<ScheduleApi.ScheduleResponse> createSchedule(
      HttpHeaders headers, ScheduleApi.CreateScheduleRequestBody body) {
    var userCtx = auth.requireUserContext(headers, ctx);
    Schedule schedule =
        new Schedule(
            ID.random(),
            userCtx.user().id(),
            userCtx.account().id(),
            effectiveRules(body == null ? null : body.rules()),
            body == null ? "UTC" : body.timezone(),
            body == null ? null : body.metadata());
    ctx.repos().schedules().insert(schedule);
    return ResponseEntity.status(201).body(ScheduleApi.ScheduleResponse.from(schedule));
  }

  public ResponseEntity<ScheduleApi.ScheduleResponse> getSchedule(
      HttpHeaders headers, String scheduleId) {
    var userCtx = auth.requireUserContext(headers, ctx);
    Schedule schedule =
        ctx.repos()
            .schedules()
            .find(auth.parseId(scheduleId, "Malformed scheduleId"))
            .filter(s -> s.userId().equals(userCtx.user().id()))
            .orElseThrow(
                () -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Schedule not found"));
    return ResponseEntity.ok(ScheduleApi.ScheduleResponse.from(schedule));
  }

  public ResponseEntity<ScheduleApi.ScheduleResponse> deleteSchedule(
      HttpHeaders headers, String scheduleId) {
    var userCtx = auth.requireUserContext(headers, ctx);
    ID sid = auth.parseId(scheduleId, "Malformed scheduleId");
    Schedule schedule =
        ctx.repos()
            .schedules()
            .find(sid)
            .filter(s -> s.userId().equals(userCtx.user().id()))
            .orElseThrow(
                () -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Schedule not found"));
    ctx.repos().schedules().delete(sid);
    return ResponseEntity.ok(ScheduleApi.ScheduleResponse.from(schedule));
  }

  public ResponseEntity<ScheduleApi.ScheduleResponse> updateSchedule(
      HttpHeaders headers, String scheduleId, ScheduleApi.UpdateScheduleRequestBody body) {
    var userCtx = auth.requireUserContext(headers, ctx);
    ID sid = auth.parseId(scheduleId, "Malformed scheduleId");
    Schedule current =
        ctx.repos()
            .schedules()
            .find(sid)
            .filter(s -> s.userId().equals(userCtx.user().id()))
            .orElseThrow(
                () -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Schedule not found"));
    Schedule updated =
        new Schedule(
            current.id(),
            current.userId(),
            current.accountId(),
            body.rules() == null ? current.rules() : body.rules(),
            body.timezone() == null ? current.timezone() : body.timezone(),
            body.metadata() == null ? current.metadata() : body.metadata());
    ctx.repos().schedules().save(updated);
    return ResponseEntity.ok(ScheduleApi.ScheduleResponse.from(updated));
  }

  private List<ScheduleRule> effectiveRules(List<ScheduleRule> requestedRules) {
    if (requestedRules != null) {
      return requestedRules;
    }
    return defaultRules();
  }

  private List<ScheduleRule> defaultRules() {
    List<ScheduleRule> rules = new ArrayList<>();
    rules.add(defaultWeekdayRule("Mon"));
    rules.add(defaultWeekdayRule("Tue"));
    rules.add(defaultWeekdayRule("Wed"));
    rules.add(defaultWeekdayRule("Thu"));
    rules.add(defaultWeekdayRule("Fri"));
    rules.add(new ScheduleRule("wday", "Sat", List.of()));
    rules.add(new ScheduleRule("wday", "Sun", List.of()));
    return rules;
  }

  private ScheduleRule defaultWeekdayRule(String weekday) {
    Map<String, Object> start = new LinkedHashMap<>();
    start.put("hours", 9);
    start.put("minutes", 0);

    Map<String, Object> end = new LinkedHashMap<>();
    end.put("hours", 17);
    end.put("minutes", 30);

    Map<String, Object> interval = new LinkedHashMap<>();
    interval.put("start", start);
    interval.put("end", end);

    return new ScheduleRule("wday", weekday, List.of(interval));
  }
}
