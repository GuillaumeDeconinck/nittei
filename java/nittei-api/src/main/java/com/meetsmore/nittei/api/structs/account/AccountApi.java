package com.meetsmore.nittei.api.structs.account;

import com.meetsmore.nittei.api.structs.event.EventDtos;
import com.meetsmore.nittei.domain.Account;
import com.meetsmore.nittei.domain.CalendarEventSort;
import com.meetsmore.nittei.domain.DateTimeQuery;
import com.meetsmore.nittei.domain.IDQuery;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.domain.RecurrenceQuery;
import com.meetsmore.nittei.domain.StringQuery;
import java.util.List;

public final class AccountApi {

    private AccountApi() {
    }

    public record AccountResponse(AccountDtos.AccountDTO account) {
        public static AccountResponse from(Account account) {
            return new AccountResponse(AccountDtos.AccountDTO.from(account));
        }
    }

    public record CreateAccountRequestBody(String code) {
    }

    public record CreateAccountResponseBody(AccountDtos.AccountDTO account, String secretApiKey) {
        public static CreateAccountResponseBody from(Account account) {
            return new CreateAccountResponseBody(AccountDtos.AccountDTO.from(account), account.secretApiKey());
        }
    }

    public record SetAccountPubKeyRequestBody(String publicJwtKey) {
    }

    public record SetAccountWebhookRequestBody(String webhookUrl) {
    }

    public record AddAccountIntegrationRequestBody(
        String clientId,
        String clientSecret,
        String redirectUri,
        IntegrationProvider provider
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
