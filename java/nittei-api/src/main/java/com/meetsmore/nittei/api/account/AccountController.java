package com.meetsmore.nittei.api.account;

import com.meetsmore.nittei.api.shared.BaseApiController;
import com.meetsmore.nittei.api.structs.account.AccountApi;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AccountController extends BaseApiController {

    private final AccountApplicationService accountService;

    public AccountController(AccountApplicationService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/account")
    public ResponseEntity<AccountApi.CreateAccountResponseBody> createAccount(
        @RequestBody AccountApi.CreateAccountRequestBody body
    ) {
        return accountService.createAccount(body);
    }

    @GetMapping("/account")
    public ResponseEntity<AccountApi.AccountResponse> getAccount(@RequestHeader HttpHeaders headers) {
        return accountService.getAccount(headers);
    }

    @PutMapping("/account/pubkey")
    public ResponseEntity<AccountApi.AccountResponse> setAccountPubKey(
        @RequestHeader HttpHeaders headers,
        @RequestBody AccountApi.SetAccountPubKeyRequestBody body
    ) {
        return accountService.setAccountPubKey(headers, body);
    }

    @PutMapping("/account/webhook")
    public ResponseEntity<AccountApi.AccountResponse> setAccountWebhook(
        @RequestHeader HttpHeaders headers,
        @RequestBody AccountApi.SetAccountWebhookRequestBody body
    ) {
        return accountService.setAccountWebhook(headers, body);
    }

    @DeleteMapping("/account/webhook")
    public ResponseEntity<AccountApi.AccountResponse> deleteAccountWebhook(@RequestHeader HttpHeaders headers) {
        return accountService.deleteAccountWebhook(headers);
    }

    @PutMapping("/account/integration")
    public ResponseEntity<AccountApi.AccountResponse> addAccountIntegration(
        @RequestHeader HttpHeaders headers,
        @RequestBody AccountApi.AddAccountIntegrationRequestBody body
    ) {
        return accountService.addAccountIntegration(headers, body);
    }

    @DeleteMapping("/account/integration/{provider}")
    public ResponseEntity<AccountApi.AccountResponse> removeAccountIntegration(
        @RequestHeader HttpHeaders headers,
        @PathVariable String provider
    ) {
        return accountService.removeAccountIntegration(headers, provider);
    }

    @PostMapping("/account/events/search")
    public ResponseEntity<AccountApi.SearchEventsAPIResponse> accountSearchEvents(
        @RequestHeader HttpHeaders headers,
        @RequestBody AccountApi.AccountSearchEventsRequestBody body
    ) {
        return accountService.accountSearchEvents(headers, body);
    }
}
