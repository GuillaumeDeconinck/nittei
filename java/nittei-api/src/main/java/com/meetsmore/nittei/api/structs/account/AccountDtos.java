package com.meetsmore.nittei.api.structs.account;

import com.meetsmore.nittei.domain.Account;
import com.meetsmore.nittei.domain.AccountSettings;
import com.meetsmore.nittei.domain.AccountWebhookSettings;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.PEMKey;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record AccountDTO(ID id, PEMKey publicJwtKey, AccountSettingsDTO settings) {
        public static AccountDTO from(Account account) {
            return new AccountDTO(
                account.id(),
                account.publicJwtKey(),
                AccountSettingsDTO.from(account.settings())
            );
        }
    }

    public record AccountSettingsDTO(AccountWebhookSettingsDTO webhook) {
        public static AccountSettingsDTO from(AccountSettings settings) {
            AccountWebhookSettingsDTO webhook = settings == null || settings.webhook() == null
                ? null
                : AccountWebhookSettingsDTO.from(settings.webhook());
            return new AccountSettingsDTO(webhook);
        }
    }

    public record AccountWebhookSettingsDTO(String url, String key) {
        public static AccountWebhookSettingsDTO from(AccountWebhookSettings settings) {
            return new AccountWebhookSettingsDTO(settings.url(), settings.key());
        }
    }
}
