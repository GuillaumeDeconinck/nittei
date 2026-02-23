package com.meetsmore.nittei.infra.repos.postgres;

import com.meetsmore.nittei.domain.AccountIntegration;
import com.meetsmore.nittei.domain.ID;
import com.meetsmore.nittei.domain.IntegrationProvider;
import com.meetsmore.nittei.infra.repos.AccountIntegrationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresAccountIntegrationRepository implements AccountIntegrationRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresAccountIntegrationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(AccountIntegration integration) {
        jdbcTemplate.update(
            "INSERT INTO account_integrations(account_uid, client_id, client_secret, redirect_uri, provider) VALUES (?, ?, ?, ?, ?)",
            toUuid(integration.accountId()),
            integration.clientId(),
            integration.clientSecret(),
            integration.redirectUri(),
            providerToDb(integration.provider())
        );
    }

    @Override
    public List<AccountIntegration> find(ID accountId) {
        return jdbcTemplate.query(
            "SELECT account_uid, client_id, client_secret, redirect_uri, provider FROM account_integrations WHERE account_uid = ?",
            (rs, rowNum) -> new AccountIntegration(
                toId(rs.getObject("account_uid", UUID.class)),
                rs.getString("client_id"),
                rs.getString("client_secret"),
                rs.getString("redirect_uri"),
                providerFromDb(rs.getString("provider"))
            ),
            toUuid(accountId)
        );
    }

    @Override
    public void delete(ID accountId, IntegrationProvider provider) {
        int affected = jdbcTemplate.update(
            "DELETE FROM account_integrations WHERE account_uid = ? AND provider = ?",
            toUuid(accountId),
            providerToDb(provider)
        );
        if (affected != 1) {
            throw new IllegalStateException("Unable to delete account integration");
        }
    }

    private static String providerToDb(IntegrationProvider provider) {
        return switch (provider) {
            case GOOGLE -> "google";
            case OUTLOOK -> "outlook";
        };
    }

    private static IntegrationProvider providerFromDb(String provider) {
        return "outlook".equalsIgnoreCase(provider) ? IntegrationProvider.OUTLOOK : IntegrationProvider.GOOGLE;
    }

    private static UUID toUuid(ID id) {
        return UUID.fromString(id.value());
    }

    private static ID toId(UUID id) {
        return new ID(id.toString());
    }
}
