package com.meetsmore.nittei.api.account;

import com.meetsmore.nittei.api.error.NitteiApiException;
import com.meetsmore.nittei.api.error.NitteiErrorCode;
import com.meetsmore.nittei.api.shared.auth.RequestContextResolver;
import com.meetsmore.nittei.api.structs.account.AccountApi;
import com.meetsmore.nittei.api.structs.event.EventDtos;
import com.meetsmore.nittei.domain.Account;
import com.meetsmore.nittei.domain.AccountIntegration;
import com.meetsmore.nittei.domain.AccountSettings;
import com.meetsmore.nittei.domain.AccountWebhookSettings;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.PEMKey;
import com.meetsmore.nittei.infra.context.NitteiContext;
import com.meetsmore.nittei.infra.repos.query.SearchEventsForAccountParams;
import com.meetsmore.nittei.infra.repos.query.SearchEventsParams;
import com.meetsmore.nittei.utils.secret.RandomSecretGenerator;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class AccountApplicationService {

  private final NitteiContext ctx;
  private final RequestContextResolver auth;

  public AccountApplicationService(NitteiContext ctx, RequestContextResolver auth) {
    this.ctx = ctx;
    this.auth = auth;
  }

  public ResponseEntity<AccountApi.CreateAccountResponseBody> createAccount(
      AccountApi.CreateAccountRequestBody body) {
    if (body == null || body.code() == null || body.code().isBlank()) {
      throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Invalid code provided");
    }
    if (!body.code().equals(ctx.config().createAccountSecretCode())) {
      throw new NitteiApiException(NitteiErrorCode.UNAUTHORIZED, "Invalid code provided");
    }

    Account account =
        new Account(
            ID.random(),
            RandomSecretGenerator.createRandomSecret(32),
            null,
            new AccountSettings(null));
    ctx.repos().accounts().insert(account);
    return ResponseEntity.status(201).body(AccountApi.CreateAccountResponseBody.from(account));
  }

  public ResponseEntity<AccountApi.AccountResponse> getAccount(HttpHeaders headers) {
    Account account = auth.requireAdminAccount(headers, ctx);
    Account fresh =
        ctx.repos()
            .accounts()
            .find(account.id())
            .orElseThrow(
                () -> new NitteiApiException(NitteiErrorCode.NOT_FOUND, "Account not found"));
    return ResponseEntity.ok(AccountApi.AccountResponse.from(fresh));
  }

  public ResponseEntity<AccountApi.AccountResponse> setAccountPubKey(
      HttpHeaders headers, AccountApi.SetAccountPubKeyRequestBody body) {
    Account account = auth.requireAdminAccount(headers, ctx);
    PEMKey key =
        body == null || body.publicJwtKey() == null ? null : new PEMKey(body.publicJwtKey());
    Account updated = new Account(account.id(), account.secretApiKey(), key, account.settings());
    ctx.repos().accounts().save(updated);
    return ResponseEntity.ok(AccountApi.AccountResponse.from(updated));
  }

  public ResponseEntity<AccountApi.AccountResponse> setAccountWebhook(
      HttpHeaders headers, AccountApi.SetAccountWebhookRequestBody body) {
    Account account = auth.requireAdminAccount(headers, ctx);
    if (body == null || body.webhookUrl() == null || !body.webhookUrl().startsWith("https://")) {
      throw new NitteiApiException(
          NitteiErrorCode.BAD_CLIENT_DATA, "Malformed url or scheme is not https");
    }
    AccountSettings settings =
        new AccountSettings(new AccountWebhookSettings(body.webhookUrl(), null));
    Account updated =
        new Account(account.id(), account.secretApiKey(), account.publicJwtKey(), settings);
    ctx.repos().accounts().save(updated);
    return ResponseEntity.ok(AccountApi.AccountResponse.from(updated));
  }

  public ResponseEntity<AccountApi.AccountResponse> deleteAccountWebhook(HttpHeaders headers) {
    Account account = auth.requireAdminAccount(headers, ctx);
    AccountSettings settings = new AccountSettings(null);
    Account updated =
        new Account(account.id(), account.secretApiKey(), account.publicJwtKey(), settings);
    ctx.repos().accounts().save(updated);
    return ResponseEntity.ok(AccountApi.AccountResponse.from(updated));
  }

  public ResponseEntity<AccountApi.AccountResponse> addAccountIntegration(
      HttpHeaders headers, AccountApi.AddAccountIntegrationRequestBody body) {
    Account account = auth.requireAdminAccount(headers, ctx);
    if (body == null || body.provider() == null) {
      throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Missing provider");
    }
    boolean exists =
        ctx.repos().accountIntegrations().find(account.id()).stream()
            .anyMatch(i -> i.provider() == body.provider());
    if (exists) {
      throw new NitteiApiException(
          NitteiErrorCode.CONFLICT, "Account already has an integration for that provider");
    }
    ctx.repos()
        .accountIntegrations()
        .insert(
            new AccountIntegration(
                account.id(),
                body.clientId(),
                body.clientSecret(),
                body.redirectUri(),
                body.provider()));
    Account fresh = ctx.repos().accounts().find(account.id()).orElse(account);
    return ResponseEntity.ok(AccountApi.AccountResponse.from(fresh));
  }

  public ResponseEntity<AccountApi.AccountResponse> removeAccountIntegration(
      HttpHeaders headers, String provider) {
    Account account = auth.requireAdminAccount(headers, ctx);
    var p = parseProvider(provider);
    boolean exists =
        ctx.repos().accountIntegrations().find(account.id()).stream()
            .anyMatch(i -> i.provider() == p);
    if (!exists) {
      throw new NitteiApiException(
          NitteiErrorCode.NOT_FOUND, "Did not find integration for provider");
    }
    ctx.repos().accountIntegrations().delete(account.id(), p);
    Account fresh = ctx.repos().accounts().find(account.id()).orElse(account);
    return ResponseEntity.ok(AccountApi.AccountResponse.from(fresh));
  }

  public ResponseEntity<AccountApi.SearchEventsAPIResponse> accountSearchEvents(
      HttpHeaders headers, AccountApi.AccountSearchEventsRequestBody body) {
    Account account = auth.requireAdminAccount(headers, ctx);
    AccountApi.AccountSearchEventsRequestBodyFilter filter = body == null ? null : body.filter();
    SearchEventsParams params =
        new SearchEventsParams(
            filter == null ? null : filter.eventUid(),
            filter == null ? null : filter.userId(),
            filter == null ? null : filter.externalId(),
            filter == null ? null : filter.externalParentId(),
            filter == null ? null : filter.startTime(),
            filter == null ? null : filter.endTime(),
            filter == null ? null : filter.status(),
            filter == null ? null : filter.eventType(),
            filter == null ? null : filter.recurringEventUid(),
            filter == null ? null : filter.originalStartTime(),
            filter == null ? null : filter.recurrence(),
            filter == null ? null : filter.metadata(),
            filter == null ? null : filter.createdAt(),
            filter == null ? null : filter.updatedAt());

    Integer limit = body == null ? null : body.limit();
    if (limit != null && limit <= 0) {
      throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Limit should be positive");
    }

    List<EventDtos.CalendarEventDTO> events =
        ctx
            .repos()
            .events()
            .searchEventsForAccount(
                new SearchEventsForAccountParams(
                    account.id(), params, body == null ? null : body.sort(), limit))
            .stream()
            .map(EventDtos.CalendarEventDTO::from)
            .toList();

    return ResponseEntity.ok(new AccountApi.SearchEventsAPIResponse(events));
  }

  private IntegrationProvider parseProvider(String raw) {
    if (raw == null) {
      throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Missing provider");
    }
    return switch (raw.toLowerCase()) {
      case "google" -> IntegrationProvider.GOOGLE;
      case "outlook" -> IntegrationProvider.OUTLOOK;
      default ->
          throw new NitteiApiException(NitteiErrorCode.BAD_CLIENT_DATA, "Unsupported provider");
    };
  }
}
