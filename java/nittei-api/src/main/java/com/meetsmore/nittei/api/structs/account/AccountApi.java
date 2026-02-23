package com.meetsmore.nittei.api.structs.account;

import com.meetsmore.nittei.api.structs.event.EventDtos;
import com.meetsmore.nittei.domain.Account;
import com.meetsmore.nittei.domain.CalendarEventSort;
import com.meetsmore.nittei.domain.DateTimeQuery;
import com.meetsmore.nittei.domain.IDQuery;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.RecurrenceQuery;
import com.meetsmore.nittei.domain.StringQuery;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class AccountApi {

    private AccountApi() {
    }

    public record AccountResponse(AccountDtos.AccountDTO account) {
        public static AccountResponse from(Account account) {
            return new AccountResponse(AccountDtos.AccountDTO.from(account));
        }
    }

    public record CreateAccountRequestBody(
        String code
    ) {
    }

    public record CreateAccountResponseBody(AccountDtos.AccountDTO account, String secretApiKey) {
        public static CreateAccountResponseBody from(Account account) {
            return new CreateAccountResponseBody(AccountDtos.AccountDTO.from(account), account.secretApiKey());
        }
    }

    public record SetAccountPubKeyRequestBody(
        @NotBlank String publicJwtKey
    ) {
    }

    public record SetAccountWebhookRequestBody(
        @NotBlank String webhookUrl
    ) {
    }

    public record AddAccountIntegrationRequestBody(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String redirectUri,
        @NotNull IntegrationProvider provider
    ) {
    }

    public record RemoveAccountIntegrationPathParams(IntegrationProvider provider) {
    }

    public record AccountSearchEventsRequestBody(
        AccountSearchEventsRequestBodyFilter filter,
        CalendarEventSort sort,
        Integer limit
    ) {
    }

    public record AccountSearchEventsRequestBodyFilter(
        IDQuery eventUid,
        IDQuery userId,
        StringQuery externalId,
        StringQuery externalParentId,
        IDQuery groupId,
        DateTimeQuery startTime,
        DateTimeQuery endTime,
        StringQuery eventType,
        StringQuery status,
        IDQuery recurringEventUid,
        DateTimeQuery originalStartTime,
        RecurrenceQuery recurrence,
        Object metadata,
        DateTimeQuery createdAt,
        DateTimeQuery updatedAt
    ) {
    }

    public record SearchEventsAPIResponse(List<EventDtos.CalendarEventDTO> events) {
    }
}
