package com.meetsmore.nittei.api.shared.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.domain.Account;
import com.meetsmore.nittei.domain.Calendar;
import com.meetsmore.nittei.domain.CalendarEvent;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.Schedule;
import com.meetsmore.nittei.domain.Service;
import com.meetsmore.nittei.domain.User;
import com.meetsmore.nittei.infra.context.NitteiContext;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class RequestContextResolver {

  private static final String NITTEI_ACCOUNT_HEADER = "nittei-account";
  private static final String NITTEI_USER_ID_HEADER = "nittei-user-id";

  public Account requireAdminAccount(HttpHeaders headers, NitteiContext ctx) {
    AuthGuards.requireAdmin(headers);
    String apiKey = headers.getFirst(AuthGuards.NITTEI_X_API_KEY_HEADER);
    return ctx.repos()
        .accounts()
        .findByApiKey(apiKey)
        .orElseThrow(
            () -> new NitteiApiException(NitteiErrorCode.UNAUTHORIZED, "Invalid x-api-key header"));
  }

  public Account requirePublicAccount(HttpHeaders headers, NitteiContext ctx) {
    String accountHeader = headers.getFirst(NITTEI_ACCOUNT_HEADER);
    if (accountHeader == null || accountHeader.isBlank()) {
      return requireAdminAccount(headers, ctx);
    }

    ID accountId = parseId(accountHeader, "Malformed nittei-account header");
    return ctx.repos()
        .accounts()
        .find(accountId)
        .orElseThrow(
            () ->
                new NitteiApiException(
                    NitteiErrorCode.UNIDENTIFIABLE_CLIENT, "Could not identify account"));
  }

  public UserContext requireUserContext(HttpHeaders headers, NitteiContext ctx) {
    AuthGuards.requireUser(headers);
    Account account = requirePublicAccount(headers, ctx);

    ID userId = resolveUserIdFromJwt(headers, account);
    User user =
        ctx.repos()
            .users()
            .findByAccountId(userId, account.id())
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.UNAUTHORIZED, "Unable to find user from credentials"));

    return new UserContext(account, user);
  }

  public User requireAccountUser(Account account, ID userId, NitteiContext ctx) {
    User user =
        ctx.repos()
            .users()
            .find(userId)
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "A user with id: " + userId.value() + ", was not found"));
    if (!account.id().equals(user.accountId())) {
      throw new NitteiApiException(
          NitteiErrorCode.NOT_FOUND, "A user with id: " + userId.value() + ", was not found");
    }
    return user;
  }

  public Calendar requireAccountCalendar(Account account, ID calendarId, NitteiContext ctx) {
    Calendar calendar =
        ctx.repos()
            .calendars()
            .find(calendarId)
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "The calendar with id: " + calendarId.value() + ", was not found"));
    if (!account.id().equals(calendar.accountId())) {
      throw new NitteiApiException(
          NitteiErrorCode.NOT_FOUND,
          "The calendar with id: " + calendarId.value() + ", was not found");
    }
    return calendar;
  }

  public Schedule requireAccountSchedule(Account account, ID scheduleId, NitteiContext ctx) {
    Schedule schedule =
        ctx.repos()
            .schedules()
            .find(scheduleId)
            .orElseThrow(
                () -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Schedule not found"));
    if (!account.id().equals(schedule.accountId())) {
      throw new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Schedule not found");
    }
    return schedule;
  }

  public CalendarEvent requireAccountEvent(Account account, ID eventId, NitteiContext ctx) {
    CalendarEvent event =
        ctx.repos()
            .events()
            .find(eventId)
            .orElseThrow(
                () ->
                    new NitteiApiException(
                        NitteiErrorCode.NOT_FOUND,
                        "The calendar event with id: " + eventId.value() + ", was not found"));
    if (!account.id().equals(event.accountId())) {
      throw new NitteiApiException(
          NitteiErrorCode.NOT_FOUND,
          "The calendar event with id: " + eventId.value() + ", was not found");
    }
    return event;
  }

  public Service requireAccountService(Account account, ID serviceId, NitteiContext ctx) {
    Service service =
        ctx.repos()
            .services()
            .find(serviceId)
            .orElseThrow(
                () -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service not found"));
    if (!account.id().equals(service.accountId())) {
      throw new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Service not found");
    }
    return service;
  }

  public ID parseId(String raw, String message) {
    try {
      UUID.fromString(raw);
      return new ID(raw);
    } catch (Exception e) {
      throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, message);
    }
  }

  private ID resolveUserIdFromJwt(HttpHeaders headers, Account account) {
    String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader == null
        || authHeader.isBlank()
        || !authHeader.toLowerCase().startsWith("bearer ")) {
      throw new NitteiApiException(
          NitteiErrorCode.UNAUTHORIZED, "Missing or invalid Authorization header");
    }
    String token = authHeader.substring(7).trim();
    if (token.isBlank()) {
      throw new NitteiApiException(NitteiErrorCode.UNAUTHORIZED, "Missing token");
    }

    try {
      RSAPublicKey publicKey = toRsaPublicKey(account);
      String userId =
          JWT.require(Algorithm.RSA256(publicKey, null))
              .build()
              .verify(token)
              .getClaim("nitteiUserId")
              .asString();
      if (userId == null || userId.isBlank()) {
        throw new NitteiApiException(NitteiErrorCode.UNAUTHORIZED, "Missing nitteiUserId claim");
      }
      return parseId(userId, "Malformed nitteiUserId claim");
    } catch (NitteiApiException e) {
      throw e;
    } catch (Exception e) {
      throw new NitteiApiException(NitteiErrorCode.UNAUTHORIZED, "Unable to verify token");
    }
  }

  private RSAPublicKey toRsaPublicKey(Account account) throws Exception {
    if (account.publicJwtKey() == null
        || account.publicJwtKey().value() == null
        || account.publicJwtKey().value().isBlank()) {
      throw new NitteiApiException(
          NitteiErrorCode.UNAUTHORIZED, "Account does not support user tokens");
    }
    String pem =
        account
            .publicJwtKey()
            .value()
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
    byte[] decoded = Base64.getDecoder().decode(pem);
    X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
  }

  public record UserContext(Account account, User user) {}
}
